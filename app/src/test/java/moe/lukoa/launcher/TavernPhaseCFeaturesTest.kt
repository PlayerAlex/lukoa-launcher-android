package moe.lukoa.launcher

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernPhaseCFeaturesTest {
    @Test
    fun `lan qr encoder produces valid ISO 18004 matrix with finder timing and format patterns`() {
        // Known ISO/IEC 18004 Table C.1 check: EC level L (01) + mask 0 (000) -> 0x77C4.
        assertEquals(0x77C4, LanQrCodeEncoder.formatInfoBits(0))

        val url = "http://192.168.1.108:8000"
        val matrix = LanQrCodeEncoder.encode(url)
        assertNotNull(matrix)
        matrix!!
        assertEquals(2, matrix.version) // 25 bytes fits Version 2-L (max 32 bytes)
        assertEquals(25, matrix.size)

        // Check 3 finder pattern centers & outer rings
        assertTrue(matrix.isDark(0, 0))
        assertTrue(matrix.isDark(3, 3))
        assertFalse(matrix.isDark(1, 1))
        assertTrue(matrix.isDark(0, matrix.size - 1))
        assertTrue(matrix.isDark(3, matrix.size - 4))
        assertTrue(matrix.isDark(matrix.size - 1, 0))
        assertTrue(matrix.isDark(matrix.size - 4, 3))

        // Separators around top-left finder must be white
        for (i in 0..7) {
            assertFalse(matrix.isDark(7, i))
            assertFalse(matrix.isDark(i, 7))
        }

        // Timing pattern on row 6 and col 6
        for (i in 8 until matrix.size - 8) {
            assertEquals(i % 2 == 0, matrix.isDark(6, i))
            assertEquals(i % 2 == 0, matrix.isDark(i, 6))
        }

        // Dark module at (4*version + 9, 8)
        assertTrue(matrix.isDark(4 * matrix.version + 9, 8))

        // Longer URLs up to Version 6 (134 bytes) succeed; empty or oversized return null
        val longMatrix = LanQrCodeEncoder.encode("http://192.168.100.200:8000/" + "a".repeat(90))
        assertNotNull(longMatrix)
        assertEquals(6, longMatrix!!.version)
        assertEquals(41, longMatrix.size)
        assertNull(LanQrCodeEncoder.encode(""))
        assertNull(LanQrCodeEncoder.encode("a".repeat(200)))
    }

    @Test
    fun `log panel filter detects errors excludes benign zero codes and filters keywords`() {
        val sample = listOf(
            "Starting SillyTavern...",
            "\"exitCode\": 0",
            "0 个错误，环境正常",
            "Error: EADDRINUSE address already in use 0.0.0.0:8000",
            "HTTP 429 Too Many Requests from upstream",
            "App：操作失败，请重试",
            "SillyTavern is listening on http://127.0.0.1:8000",
        ).joinToString("\n")

        val allResult = LogPanelFilter.filter(sample, errorsOnly = false, query = "")
        assertEquals(7, allResult.totalLines)
        assertEquals(7, allResult.matchedLines)
        assertEquals(3, allResult.errorLines)

        val errOnly = LogPanelFilter.filter(sample, errorsOnly = true, query = "")
        assertEquals(3, errOnly.matchedLines)
        assertTrue(errOnly.text.contains("EADDRINUSE"))
        assertTrue(errOnly.text.contains("429"))
        assertTrue(errOnly.text.contains("操作失败"))
        assertFalse(errOnly.text.contains("\"exitCode\": 0"))
        assertFalse(errOnly.text.contains("Starting SillyTavern"))

        val keyword = LogPanelFilter.filter(sample, errorsOnly = true, query = "429")
        assertEquals(1, keyword.matchedLines)
        assertEquals("HTTP 429 Too Many Requests from upstream", keyword.text)
    }

    @Test
    fun `node memory parser handles configured default and legacy output`() {
        val configuredOut = """
            ==== Lukoa node memory ====
            nodeMemory.configured=true
            nodeMemory.megabytes=4096
            ==== end Lukoa node memory ====
        """.trimIndent()
        val parsedConfigured = TavernNodeMemoryOutputParser.parse(configuredOut)
        assertNotNull(parsedConfigured)
        assertEquals(true, parsedConfigured!!.configured)
        assertEquals(4096, parsedConfigured.megabytes)
        assertEquals("4GB", parsedConfigured.displayLabel)

        val defaultOut = """
            ==== Lukoa node memory ====
            nodeMemory.configured=false
            nodeMemory.megabytes=
            ==== end Lukoa node memory ====
        """.trimIndent()
        val parsedDefault = TavernNodeMemoryOutputParser.parse(defaultOut)
        assertNotNull(parsedDefault)
        assertEquals(false, parsedDefault!!.configured)
        assertNull(parsedDefault.megabytes)
        assertEquals("未限制（默认）", parsedDefault.displayLabel)

        val legacyOut = "node.memory.mb=6144\nnode.memory.file=/tmp/node-memory.env\n"
        val parsedLegacy = TavernNodeMemoryOutputParser.parse(legacyOut)
        assertNotNull(parsedLegacy)
        assertEquals(6144, parsedLegacy!!.megabytes)
    }

    @Test
    fun `proxy test codec validates urls and parser decodes structured output`() {
        assertNull(TavernProxyTestCommandCodec.validateProxyUrl("http://127.0.0.1:7890"))
        assertNull(TavernProxyTestCommandCodec.validateProxyUrl("socks5h://127.0.0.1:1080"))
        assertNotNull(TavernProxyTestCommandCodec.validateProxyUrl(""))
        assertNotNull(TavernProxyTestCommandCodec.validateProxyUrl("ftp://127.0.0.1:21"))

        val encoded = TavernProxyTestCommandCodec.encode("http://127.0.0.1:7890")
        assertEquals("http://127.0.0.1:7890", TavernProxyTestCommandCodec.decode(encoded))

        val detailB64 = TavernProxyTestCommandCodec.encode("代理通道连接正常（HTTP 204，耗时 120ms）。")
        val output = """
            ==== Lukoa proxy test ====
            proxy.ok=true
            proxy.tcpOk=true
            proxy.httpCode=204
            proxy.latencyMs=120
            proxy.detail=$detailB64
            ==== end Lukoa proxy test ====
        """.trimIndent()
        val result = TavernProxyTestOutputParser.parse(output)
        assertNotNull(result)
        assertTrue(result!!.ok)
        assertTrue(result.tcpOk)
        assertEquals("204", result.httpCode)
        assertEquals(120, result.latencyMs)
        assertTrue(result.detail.contains("HTTP 204"))
    }

    @Test
    fun `quick import batch codec supports multi-file and chat character directories`() {
        assertNotNull(TavernQuickImportCommandCodec.validateChatCharacterDir(""))
        assertNotNull(TavernQuickImportCommandCodec.validateChatCharacterDir("../evil"))
        assertNotNull(TavernQuickImportCommandCodec.validateChatCharacterDir("a/b"))
        assertNull(TavernQuickImportCommandCodec.validateChatCharacterDir("Seraphina"))

        val encoded = TavernQuickImportCommandCodec.encodeBatch(
            kind = TavernQuickImportKind.Chat,
            stagedPaths = listOf(
                "/storage/emulated/0/Download/LukoaLauncher/imports/chat1.jsonl",
                "/storage/emulated/0/Download/LukoaLauncher/imports/chat2.jsonl",
            ),
            userHandle = "alice",
            chatCharacterDir = "露科亚",
        )
        val decoded = TavernQuickImportCommandCodec.decodeBatch(encoded)
        assertNotNull(decoded)
        assertEquals(TavernQuickImportKind.Chat, decoded!!.kind)
        assertEquals("alice", decoded.userHandle)
        assertEquals("露科亚", decoded.chatCharacterDir)
        assertEquals(2, decoded.stagedPaths.size)

        // Chat import without character dir is rejected
        val invalidChat = TavernQuickImportCommandCodec.encodeBatch(
            kind = TavernQuickImportKind.Chat,
            stagedPaths = listOf("/storage/emulated/0/Download/LukoaLauncher/imports/chat1.jsonl"),
            userHandle = "default-user",
            chatCharacterDir = "",
        )
        assertNull(TavernQuickImportCommandCodec.decodeBatch(invalidChat))

        // Paths parser extracts chat character directories
        val charsB64 = TavernQuickImportCommandCodec.encodeBase64Url("Alice\nSeraphina\n露科亚")
        val pathsOut = """
            ==== SillyTavern quick import paths ====
            import.dataRoot=/data/SillyTavern/data
            import.userRoot=/data/SillyTavern/data/default-user
            import.dir.chats=/data/SillyTavern/data/default-user/chats
            import.chatChars=$charsB64
            ==== end SillyTavern quick import paths ====
        """.trimIndent()
        val parsedPaths = TavernQuickImportOutputParser.parsePaths(pathsOut)
        assertNotNull(parsedPaths)
        assertEquals(listOf("Alice", "Seraphina", "露科亚"), parsedPaths!!.chatCharacterDirs)

        // Batch result parser decodes targetFiles list
        val filesB64 = TavernQuickImportCommandCodec.encodeBase64Url("/p/a.jsonl\n/p/b.jsonl")
        val resultOut = """
            ==== SillyTavern quick import ====
            import.kind=chat
            import.count=2
            import.targetFile=/p/b.jsonl
            import.targetFiles=$filesB64
            ==== end SillyTavern quick import ====
        """.trimIndent()
        assertEquals(listOf("/p/a.jsonl", "/p/b.jsonl"), TavernQuickImportOutputParser.parseImportedTargetFiles(resultOut))
    }

    @Test
    fun `backup archive scanner detects lightweight user-data-only manifest scope`() {
        val tarGzBytes = buildTarGz(
            mapOf(
                "LUKOA_BACKUP_MANIFEST.txt" to "schema=lukoa-sillytavern-backup-v2\nkind=manual-data\nbackupScope=user-data\n",
                "SillyTavern/data/default-user/characters/hero.png" to "png-bytes",
                "SillyTavern/config.yaml" to "listen: false\n",
            ),
        )
        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(tarGzBytes))
        assertTrue(summary.hasLukoaManifest)
        assertTrue(summary.hasUserData)
        assertTrue(summary.hasConfiguration)
        assertTrue(summary.isUserDataOnlyArchive)

        val fullTarGzBytes = buildTarGz(
            mapOf(
                "LUKOA_BACKUP_MANIFEST.txt" to "schema=lukoa-sillytavern-backup-v2\nkind=manual\nbackupScope=full\n",
                "SillyTavern/data/default-user/characters/hero.png" to "png-bytes",
            ),
        )
        val fullSummary = BackupArchiveContentScanner.scan(ByteArrayInputStream(fullTarGzBytes))
        assertFalse(fullSummary.isUserDataOnlyArchive)
    }

    @Test
    fun `documentation search finds topics across all three chapters`() {
        val hits429 = searchDocumentationTopics("429")
        assertTrue(hits429.any { it.chapterTitle.contains("第一章") })

        val hitsLan = searchDocumentationTopics("二维码")
        assertTrue(hitsLan.any { it.chapterTitle.contains("第三章") })

        val hitsLightBackup = searchDocumentationTopics("轻量备份")
        assertTrue(hitsLightBackup.any { it.chapterTitle.contains("第三章") })

        assertTrue(searchDocumentationTopics("   ").isEmpty())
    }

    @Test
    fun `auto backup schedule time formats cleanly`() {
        assertEquals("暂无", formatAutoBackupScheduleTime(0L))
        assertEquals("01-01 00:00", formatAutoBackupScheduleTime(1_704_067_200_000L, ZoneOffset.UTC))
    }

    private fun buildTarGz(entries: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        GzipCompressorOutputStream(out).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                entries.forEach { (name, content) ->
                    val bytes = content.toByteArray(StandardCharsets.UTF_8)
                    val entry = TarArchiveEntry(name).apply { size = bytes.size.toLong() }
                    tar.putArchiveEntry(entry)
                    tar.write(bytes)
                    tar.closeArchiveEntry()
                }
            }
        }
        return out.toByteArray()
    }
}
