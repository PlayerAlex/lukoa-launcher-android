package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogLinesWindowTest {
    @Test
    fun `short logs are shown whole without a load earlier entry`() {
        val window = "a\nb\nc".latestLinesWindow(5)

        assertEquals("a\nb\nc", window.text)
        assertFalse(window.hasEarlier)
    }

    @Test
    fun `long logs show the newest lines and offer earlier ones`() {
        val log = (1..10).joinToString("\n") { "line $it" }

        val firstPage = log.latestLinesWindow(3)
        val twoPages = log.latestLinesWindow(6)

        assertEquals("line 8\nline 9\nline 10", firstPage.text)
        assertTrue(firstPage.hasEarlier)
        assertEquals((5..10).joinToString("\n") { "line $it" }, twoPages.text)
        assertFalse(log.latestLinesWindow(10).hasEarlier)
    }
}
