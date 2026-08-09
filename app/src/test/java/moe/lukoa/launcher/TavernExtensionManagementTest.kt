package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TavernExtensionManagementTest {
    @Test
    fun `parser reads extension root and encoded records`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=${encoded("/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/third-party")}
            extension.disabledRoot=${encoded("/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/.lukoa-disabled-third-party")}
            extension.record=${encoded("Extension-A")}|${encoded("清凉扩展")}|${encoded("1.2.3")}|true|${encoded("Lukoa")}|1536|true
            extension.record=${encoded("Extension-B")}|${encoded("停用扩展")}|${encoded("2.0.0")}|true|${encoded("Lukoa")}|512|false
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertNotNull(parsed)
        assertEquals("/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/third-party", parsed?.rootDirectory)
        assertEquals(
            "/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/.lukoa-disabled-third-party",
            parsed?.disabledRootDirectory,
        )
        val enabled = parsed?.extensions?.first { it.directoryName == "Extension-A" }
        assertEquals("清凉扩展", enabled?.displayName)
        assertEquals("1.2.3", enabled?.version)
        assertEquals(true, enabled?.hasManifest)
        assertEquals("Lukoa", enabled?.author)
        assertEquals(1536L, enabled?.directoryKilobytes)
        assertEquals(true, enabled?.enabled)
        assertEquals(false, parsed?.extensions?.first { it.directoryName == "Extension-B" }?.enabled)
    }

    @Test
    fun `parser keeps legacy records enabled`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=${encoded("/extensions")}
            extension.record=${encoded("Legacy")}|${encoded("旧版记录")}|${encoded("1.0")}|true|${encoded("Lukoa")}|64
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertEquals(true, parsed?.extensions?.single()?.enabled)
    }

    @Test
    fun `parser reads git source and remote update status`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=${encoded("/extensions")}
            extension.record=${encoded("Mint")}|${encoded("Mint")}|${encoded("1.0")}|true|${encoded("Lukoa")}|64|true|${encoded("https://github.com/owner/Mint.git")}|abc1234|def5678|update_available
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        val extension = parsed?.extensions?.single()
        assertEquals("https://github.com/owner/Mint.git", extension?.repositoryUrl)
        assertEquals("abc1234", extension?.currentRevision)
        assertEquals("def5678", extension?.latestRevision)
        assertEquals(TavernExtensionUpdateStatus.UpdateAvailable, extension?.updateStatus)
    }

    @Test
    fun `parser maps unknown update status to a safe failure state`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=${encoded("/extensions")}
            extension.record=${encoded("Mint")}|${encoded("Mint")}|${encoded("1.0")}|true|||true|${encoded("https://github.com/owner/Mint.git")}|||unexpected
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertEquals(TavernExtensionUpdateStatus.CheckFailed, parsed?.extensions?.single()?.updateStatus)
    }

    @Test
    fun `parser accepts an empty extension list`() {
        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertNotNull(parsed)
        assertEquals(emptyList<TavernExtensionRecord>(), parsed?.extensions)
    }

    @Test
    fun `parser ignores unrelated or malformed output`() {
        assertNull(TavernExtensionOutputParser.parse("extension.record=broken"))
    }

    @Test
    fun `parser rejects an incomplete extension block`() {
        val output = """
            ==== SillyTavern extensions ====
            extension.root=
        """.trimIndent()

        assertNull(TavernExtensionOutputParser.parse(output))
    }

    @Test
    fun `parser uses only the latest complete extension block`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val parsed = TavernExtensionOutputParser.parse(
            """
            ==== SillyTavern extensions ====
            extension.root=${encoded("/old")}
            extension.record=${encoded("Old-Extension")}|${encoded("旧扩展")}|${encoded("1.0")}|true
            ==== end SillyTavern extensions ====
            unrelated output
            ==== SillyTavern extensions ====
            extension.root=${encoded("/current")}
            extension.record=${encoded("Current-Extension")}|${encoded("当前扩展")}|${encoded("2.0")}|true
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertEquals("/current", parsed?.rootDirectory)
        assertEquals(listOf("Current-Extension"), parsed?.extensions?.map { it.directoryName })
    }

    @Test
    fun `directory guard allows one safe segment`() {
        assertNull(TavernExtensionCommandCodec.validateDirectoryName("Extension-A"))
        assertNull(TavernExtensionCommandCodec.validateDirectoryName("作者 扩展"))
    }

    @Test
    fun `directory guard rejects traversal separators and control characters`() {
        listOf("", ".", "..", "../Extension-A", "folder/name", "folder\\name", " bad", "bad\nname")
            .forEach { unsafe ->
                assertNotNull("expected rejection for <$unsafe>", TavernExtensionCommandCodec.validateDirectoryName(unsafe))
            }
    }

    @Test
    fun `encoded directory name round trips only when safe`() {
        val encoded = TavernExtensionCommandCodec.encodeDirectoryName("Extension-A")
        assertEquals("Extension-A", TavernExtensionCommandCodec.decodeDirectoryName(encoded))
        assertNull(TavernExtensionCommandCodec.decodeDirectoryName("Li4"))
        assertNull(TavernExtensionCommandCodec.decodeDirectoryName("not valid base64 ***"))
    }

    @Test
    fun `github repository guard normalizes a public extension repository`() {
        assertEquals(
            "https://github.com/owner/Extension-A.git",
            TavernExtensionCommandCodec.normalizeRepositoryUrl(
                " https://github.com/owner/Extension-A/ ",
            ),
        )
        assertEquals(
            "Extension-A",
            TavernExtensionCommandCodec.repositoryDirectoryName(
                "https://github.com/owner/Extension-A.git",
            ),
        )
    }

    @Test
    fun `github repository guard rejects unsafe or ambiguous sources`() {
        listOf(
            "",
            "http://github.com/owner/repo",
            "https://example.com/owner/repo",
            "https://user:token@github.com/owner/repo",
            "https://github.com/owner/repo?ref=main",
            "https://github.com/owner/repo/extra",
            "https://github.com/../repo",
            "https://github.com/owner/repo name",
        ).forEach { unsafe ->
            assertNotNull(
                "expected rejection for <$unsafe>",
                TavernExtensionCommandCodec.validateRepositoryUrl(unsafe),
            )
        }
    }

    @Test
    fun `encoded github repository round trips only when safe`() {
        val encoded = TavernExtensionCommandCodec.encodeRepositoryUrl(
            "https://github.com/owner/Extension-A",
        )

        assertEquals(
            "https://github.com/owner/Extension-A.git",
            TavernExtensionCommandCodec.decodeRepositoryUrl(encoded),
        )
        assertNull(
            TavernExtensionCommandCodec.decodeRepositoryUrl(
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "https://example.com/owner/repo".toByteArray(StandardCharsets.UTF_8),
                ),
            ),
        )
    }

    @Test
    fun `extension target path does not invent an absolute root`() {
        assertEquals(
            "/extensions/Extension-A",
            extensionTargetDirectory("/extensions/", "Extension-A"),
        )
        assertEquals(
            "Extension-A",
            extensionTargetDirectory("", "Extension-A"),
        )
    }

    @Test
    fun `extension state selects the matching enabled or disabled root`() {
        val state = TavernExtensionManagementState(
            rootDirectory = "/extensions/third-party",
            disabledRootDirectory = "/extensions/.lukoa-disabled-third-party",
        )

        assertEquals(
            "/extensions/third-party/Enabled",
            extensionTargetDirectory(state, TavernExtensionRecord("Enabled", "Enabled", "", true)),
        )
        assertEquals(
            "/extensions/.lukoa-disabled-third-party/Disabled",
            extensionTargetDirectory(
                state,
                TavernExtensionRecord("Disabled", "Disabled", "", true, enabled = false),
            ),
        )
    }

}
