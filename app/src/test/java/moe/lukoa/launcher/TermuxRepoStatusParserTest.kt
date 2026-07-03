package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxRepoStatusParserTest {
    @Test
    fun `parse uses explicit current repo fields`() {
        val report = TermuxRepoStatusParser.parse(
            """
            current.label=清华源
            current.uri=https://mirrors.tuna.tsinghua.edu.cn/termux/termux-main
            """.trimIndent(),
            nowMillis = 42L,
        )

        requireNotNull(report)
        assertEquals("清华源", report.label)
        assertEquals("https://mirrors.tuna.tsinghua.edu.cn/termux/termux-main", report.uri)
        assertEquals(42L, report.updatedAtMillis)
        assertTrue(report.hasData)
    }

    @Test
    fun `parse falls back to label inferred from uri`() {
        val report = TermuxRepoStatusParser.parse(
            """
            uri=https://packages.termux.dev/apt/termux-main
            """.trimIndent(),
        )

        requireNotNull(report)
        assertEquals("官方源", report.label)
    }

    @Test
    fun `parse returns null when uri is missing`() {
        assertNull(TermuxRepoStatusParser.parse("current.label=清华源"))
        assertNull(TermuxRepoStatusParser.parse(""))
    }
}
