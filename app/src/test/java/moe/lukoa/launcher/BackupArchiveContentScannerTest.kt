package moe.lukoa.launcher

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupArchiveContentScannerTest {
    @Test
    fun `scanner summarizes user data extensions config and manifest without extracting`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/chats/chat.jsonl",
            "SillyTavern/data/default-user/characters/Mint.png",
            "SillyTavern/public/scripts/extensions/third-party/Mint/manifest.json",
            "SillyTavern/config.yaml",
            "LUKOA_BACKUP_MANIFEST.txt",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(5, summary.entryCount)
        assertTrue(summary.hasUserData)
        assertTrue(summary.hasExtensions)
        assertTrue(summary.hasConfiguration)
        assertTrue(summary.hasLukoaManifest)
        assertFalse(summary.truncated)
    }

    @Test
    fun `scanner rejects traversal and marks oversized listings truncated`() {
        val unsafe = tarGzip("../outside.txt")
        runCatching { BackupArchiveContentScanner.scan(ByteArrayInputStream(unsafe)) }
            .onSuccess { error("expected unsafe archive rejection") }

        val manyEntries = (0..BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES)
            .map { "SillyTavern/data/default-user/chats/$it.jsonl" }
            .toTypedArray()
        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(tarGzip(*manyEntries)))
        assertTrue(summary.truncated)
        assertEquals(BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES, summary.entryCount)
    }

    private fun tarGzip(vararg entries: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        GzipCompressorOutputStream(bytes).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                entries.forEach { name ->
                    val entry = TarArchiveEntry(name).apply { size = 0L }
                    tar.putArchiveEntry(entry)
                    tar.closeArchiveEntry()
                }
                tar.finish()
            }
        }
        return bytes.toByteArray()
    }
}
