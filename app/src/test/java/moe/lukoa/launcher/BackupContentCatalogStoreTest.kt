package moe.lukoa.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupContentCatalogStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(BackupContentCatalogStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `summary survives store recreation and is invalidated when archive changes`() {
        val details = archiveDetails(size = 2_048L, modifiedAtMillis = 123L)
        val summary = BackupArchiveContentSummary(
            entryCount = 2,
            hasUserData = true,
            hasExtensions = false,
            hasConfiguration = false,
            hasLukoaManifest = true,
            truncated = false,
            groups = listOf(
                BackupArchiveContentGroup(
                    kind = BackupArchiveContentKind.CharacterCards,
                    entryCount = 1,
                    names = listOf("薄荷"),
                    namesTruncated = false,
                ),
            ),
        )

        BackupContentCatalogStore(context).write(details, summary)

        assertEquals(summary, BackupContentCatalogStore(context).read(details))
        assertNull(
            BackupContentCatalogStore(context).read(
                details.copy(size = details.size + 1L),
            ),
        )
    }

    private fun archiveDetails(size: Long, modifiedAtMillis: Long) = BackupLibraryArchiveDetails(
        fileName = "sd-test.tar.gz",
        termuxReadablePath = "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-test.tar.gz",
        size = size,
        modifiedAtMillis = modifiedAtMillis,
    )
}
