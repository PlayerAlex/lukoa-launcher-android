package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TermuxStructuredOutputParserTest {
    @Test
    fun `background result restores all management snapshots`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val parsed = TermuxStructuredOutputParser.parse(
            output = """
                directory=/data/data/com.termux/files/home/SillyTavern
                package.version=1.13.4
                official.repo=https://github.com/SillyTavern/SillyTavern.git
                stable.1.name=1.13.4
                stable.1.target=1.13.4
                current.label=官方源
                current.uri=https://packages.termux.dev/apt/termux-main
                ==== SillyTavern upload limit ====
                uploadLimit.currentMb=128
                uploadLimit.patchState=active
                ==== SillyTavern users ====
                user.record=${encoded("main")}|${encoded("默认用户")}|true|true|true|2048
                ==== end SillyTavern users ====
                ==== SillyTavern extensions ====
                extension.root=${encoded("/extensions")}
                extension.record=${encoded("Lukoa")}|${encoded("露科亚扩展")}|${encoded("1.0")}|true
                ==== end SillyTavern extensions ====
            """.trimIndent(),
            nowMillis = 1234L,
        )

        assertEquals("1.13.4", parsed.versionInfo?.packageVersion)
        assertEquals("1.13.4", parsed.officialVersions?.stable?.single()?.target)
        assertEquals(1234L, parsed.termuxRepoStatus?.updatedAtMillis)
        assertEquals(128, parsed.uploadLimitStatus?.currentMegabytes)
        assertEquals("默认用户", parsed.users?.single()?.name)
        assertEquals("露科亚扩展", parsed.extensions?.extensions?.single()?.displayName)
    }

    @Test
    fun `unrelated output has no structured snapshots`() {
        val parsed = TermuxStructuredOutputParser.parse("plain log")

        assertEquals(TermuxStructuredOutput(), parsed)
        assertNotNull(parsed)
    }
}
