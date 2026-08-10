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
    fun `scanner groups named user content without returning the raw archive listing`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/characters/Mint.png",
            "SillyTavern/data/default-user/OpenAI Settings/CoolPreset.json",
            "SillyTavern/data/default-user/regex/CleanRegex.json",
            "SillyTavern/data/default-user/chats/Mint/2026-08-10.jsonl",
            "SillyTavern/data/default-user/worlds/MintWorld.json",
            "SillyTavern/public/scripts/extensions/third-party/MintTools/manifest.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(listOf("Mint"), summary.group(BackupArchiveContentKind.CharacterCards)?.names)
        assertEquals(listOf("CoolPreset"), summary.group(BackupArchiveContentKind.Presets)?.names)
        assertEquals(listOf("CleanRegex"), summary.group(BackupArchiveContentKind.RegexScripts)?.names)
        assertEquals(listOf("Mint"), summary.group(BackupArchiveContentKind.Chats)?.names)
        assertEquals(listOf("MintWorld"), summary.group(BackupArchiveContentKind.WorldBooks)?.names)
        assertEquals(listOf("MintTools"), summary.group(BackupArchiveContentKind.Extensions)?.names)
        assertEquals(6, summary.groups.sumOf { it.entryCount })
    }

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
    fun `scanner does not mistake nested character assets for cards or presets`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/characters/真正的角色卡.png",
            "SillyTavern/data/default-user/characters/真正的角色卡/emotions/joy.png",
            "SillyTavern/data/default-user/characters/真正的角色卡/emotions/neutral.webp",
            "SillyTavern/data/default-user/characters/真正的角色卡/context/Alpaca.json",
            "SillyTavern/data/default-user/instruct/真正的预设.json",
            "SillyTavern/data/default-user/instruct/资源目录/ChatML.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("真正的角色卡"),
            summary.group(BackupArchiveContentKind.CharacterCards)?.names,
        )
        assertEquals(
            listOf("真正的预设"),
            summary.group(BackupArchiveContentKind.Presets)?.names,
        )
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
