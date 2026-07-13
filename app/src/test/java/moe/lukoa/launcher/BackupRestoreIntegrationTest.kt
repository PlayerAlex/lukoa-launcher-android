@file:Suppress("DEPRECATION")

package moe.lukoa.launcher

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import java.util.zip.GZIPOutputStream
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboCursor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRestoreIntegrationTest {
    private lateinit var context: Context
    private val libraryPathsToDelete = linkedSetOf<String>()
    private val filesToDelete = linkedSetOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lukoa_operation_lock", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        libraryPathsToDelete.toList().asReversed().forEach { path ->
            runCatching { BackupLibraryFiles.deleteLibraryArchive(context, path) }
        }
        filesToDelete.toList().asReversed().forEach { file ->
            runCatching { file.delete() }
        }
        LauncherStateStore(context).saveAutoBackupConfig(
            enabled = false,
            intervalMinutes = 360,
            keepCount = 5,
        )
        context.getSharedPreferences("lukoa_operation_lock", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `external gzip import is readable and produces restore preview`() {
        val testId = uniqueTestId()
        val sourceFile = File(externalFixtureDirectory(), "$testId.tgz")
        val expectedBytes = writeGzip(sourceFile, "backup payload for $testId")
        val sourceUri = registerReadableContentUri(sourceFile)

        val imported = ExternalBackupImporter.copyToBackupLibrary(context, sourceUri)
        if (imported.termuxReadablePath.isNotBlank()) {
            libraryPathsToDelete += imported.termuxReadablePath
        }

        assertTrue(imported.message, imported.ok)
        assertEquals("$testId.tar.gz", imported.termuxReadablePath.fileName())
        assertEquals(expectedBytes.size.toLong(), BackupLibraryFiles.openLibrarySource(context, imported.termuxReadablePath).size)
        assertArrayEquals(
            expectedBytes,
            BackupLibraryFiles.openLibrarySource(context, imported.termuxReadablePath)
                .openInput()
                .use { it.readBytes() },
        )
        assertTrue(BackupLibraryFiles.listLibraryArchives(context).contains(imported.termuxReadablePath))

        val preview = BackupRestorePreviewResolver.resolve(
            context = context,
            archivePath = imported.termuxReadablePath,
            restoreTargetDir = "~/SillyTavern",
        )

        assertEquals(imported.termuxReadablePath, preview.archivePath)
        assertEquals("$testId.tar.gz", preview.backupName)
        assertEquals(expectedBytes.size.toLong(), preview.sizeBytes)
        assertNotNull(preview.modifiedAtMillis)
        assertTrue(preview.modifiedAtMillis!! > 0L)
        assertEquals("~/SillyTavern", preview.restoreTargetDir)
    }

    @Test
    fun `library archive can be copied renamed and deleted without changing bytes`() {
        val testId = uniqueTestId()
        val originalFile = File(manualBackupDirectory(), "$testId.tar.gz")
        val expectedBytes = writeGzip(originalFile, "copy rename delete payload for $testId")
        libraryPathsToDelete += originalFile.absolutePath

        val copied = BackupLibraryFiles.copyLibraryArchive(context, originalFile.absolutePath)
        libraryPathsToDelete += copied.termuxReadablePath

        assertNotEquals(originalFile.absolutePath, copied.termuxReadablePath)
        assertEquals(expectedBytes.size.toLong(), copied.size)
        assertArrayEquals(
            expectedBytes,
            BackupLibraryFiles.openLibrarySource(context, copied.termuxReadablePath)
                .openInput()
                .use { it.readBytes() },
        )

        val renamed = BackupLibraryFiles.renameLibraryArchive(
            context = context,
            archivePath = copied.termuxReadablePath,
            newName = "$testId-renamed",
        )
        libraryPathsToDelete -= copied.termuxReadablePath
        libraryPathsToDelete += renamed.termuxReadablePath

        assertEquals("$testId-renamed.tar.gz", renamed.fileName)
        assertFalse(File(copied.termuxReadablePath).exists())
        assertTrue(BackupLibraryFiles.canReadLibrarySource(context, renamed.termuxReadablePath))
        assertArrayEquals(
            expectedBytes,
            BackupLibraryFiles.openLibrarySource(context, renamed.termuxReadablePath)
                .openInput()
                .use { it.readBytes() },
        )

        BackupLibraryFiles.deleteLibraryArchive(context, originalFile.absolutePath)
        libraryPathsToDelete -= originalFile.absolutePath
        BackupLibraryFiles.deleteLibraryArchive(context, renamed.termuxReadablePath)
        libraryPathsToDelete -= renamed.termuxReadablePath

        assertFalse(originalFile.exists())
        assertFalse(File(renamed.termuxReadablePath).exists())
        assertFalse(BackupLibraryFiles.canReadLibrarySource(context, originalFile.absolutePath))
        assertFalse(BackupLibraryFiles.canReadLibrarySource(context, renamed.termuxReadablePath))
    }

    @Test
    fun `configured retention removes oldest automatic backup and preserves manual backup`() {
        val testId = uniqueTestId()
        val manual = File(manualBackupDirectory(), "$testId-manual.tar.gz")
        val oldest = File(autoBackupDirectory(), "$testId-auto-oldest.tar.gz")
        val middle = File(autoBackupDirectory(), "$testId-auto-middle.tar.gz")
        val newest = File(autoBackupDirectory(), "$testId-auto-newest.tar.gz")
        listOf(manual, oldest, middle, newest).forEachIndexed { index, file ->
            writeGzip(file, "retention payload $index for $testId")
            assertTrue(file.setLastModified(10_000L + index * 1_000L))
            libraryPathsToDelete += file.absolutePath
        }
        LauncherStateStore(context).saveAutoBackupConfig(
            enabled = true,
            intervalMinutes = 360,
            keepCount = 2,
        )

        val remaining = AutoBackupRetentionManager.enforceConfiguredLimit(
            context = context,
            reason = "robolectric-integration-test",
        )

        assertTrue(manual.exists())
        assertFalse(oldest.exists())
        assertTrue(middle.exists())
        assertTrue(newest.exists())
        assertFalse(remaining.contains(oldest.absolutePath))
        assertTrue(remaining.contains(manual.absolutePath))
        assertTrue(remaining.contains(middle.absolutePath))
        assertTrue(remaining.contains(newest.absolutePath))
        libraryPathsToDelete -= oldest.absolutePath
    }

    private fun writeGzip(file: File, payload: String): ByteArray {
        val parent = requireNotNull(file.parentFile)
        assertTrue(parent.exists() || parent.mkdirs())
        GZIPOutputStream(file.outputStream()).use { output ->
            output.write(payload.toByteArray(Charsets.UTF_8))
        }
        filesToDelete += file
        return file.readBytes()
    }

    private fun registerReadableContentUri(file: File): Uri {
        val uri = Uri.parse("content://moe.lukoa.launcher.robolectric/${file.name}")
        val cursor = RoboCursor().apply {
            setColumnNames(listOf(OpenableColumns.DISPLAY_NAME))
            setResults(arrayOf(arrayOf(file.name)))
        }
        shadowOf(context.contentResolver).apply {
            setCursor(uri, cursor)
            registerInputStreamSupplier(uri) { file.inputStream() }
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun manualBackupDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BackupLibraryFiles.MANUAL_RELATIVE_DIR,
        )
    }

    @Suppress("DEPRECATION")
    private fun autoBackupDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            BackupLibraryFiles.AUTO_RELATIVE_DIR,
        )
    }

    @Suppress("DEPRECATION")
    private fun externalFixtureDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "LukoaLauncher/test-fixtures",
        )
    }

    private fun uniqueTestId(): String {
        return "lukoa-it-${UUID.randomUUID().toString().take(8)}"
    }

    private fun String.fileName(): String {
        return trim().replace('\\', '/').substringAfterLast('/')
    }
}
