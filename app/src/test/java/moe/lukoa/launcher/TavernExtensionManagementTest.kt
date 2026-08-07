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
            extension.record=${encoded("Extension-A")}|${encoded("清凉扩展")}|${encoded("1.2.3")}|true
            ==== end SillyTavern extensions ====
            """.trimIndent(),
        )

        assertNotNull(parsed)
        assertEquals("/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/third-party", parsed?.rootDirectory)
        assertEquals("Extension-A", parsed?.extensions?.single()?.directoryName)
        assertEquals("清凉扩展", parsed?.extensions?.single()?.displayName)
        assertEquals("1.2.3", parsed?.extensions?.single()?.version)
        assertEquals(true, parsed?.extensions?.single()?.hasManifest)
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
}
