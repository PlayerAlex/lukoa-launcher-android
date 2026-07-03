package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TermuxLogDeltaTest {
    @Test
    fun `extract live log body returns content between markers`() {
        val body = TermuxLogDelta.extractLiveLogBody(
            """
            something else
            ==== SillyTavern live log: follow ====
            line 1
            line 2
            ==== end SillyTavern live log ====
            """.trimIndent(),
        )

        assertEquals("line 1\nline 2", body)
    }

    @Test
    fun `extract live log body returns null without marker`() {
        assertNull(TermuxLogDelta.extractLiveLogBody("plain output"))
    }

    @Test
    fun `new suffix handles direct append and overlap`() {
        assertEquals("line 3", TermuxLogDelta.newSuffix("line 1\nline 2", "line 1\nline 2\nline 3"))
        assertEquals("tail", TermuxLogDelta.newSuffix("abc", "abctail"))
        assertEquals("next", TermuxLogDelta.newSuffix("line 2", "line 2\nnext"))
    }

    @Test
    fun `new suffix returns current when overlap cannot be found`() {
        assertEquals("fresh log", TermuxLogDelta.newSuffix("old", "fresh log"))
        assertEquals("", TermuxLogDelta.newSuffix("", "fresh log"))
    }
}
