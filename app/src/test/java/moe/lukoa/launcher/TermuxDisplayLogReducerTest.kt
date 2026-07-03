package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TermuxDisplayLogReducerTest {
    @Test
    fun `recent log only keeps newly added body`() {
        val output = """
            status=running

            ==== SillyTavern recent log: /tmp/tavern.log ====
            line 1
            line 2
            line 3
            ==== end SillyTavern recent log ====
        """.trimIndent()

        val result = TermuxDisplayLogReducer.reduce(
            output = output,
            lastTrackedRecentLogBody = "line 1\nline 2",
        )

        assertEquals(
            """
                status=running

                ==== SillyTavern recent log: /tmp/tavern.log ====
                line 3
                ==== end SillyTavern recent log ====
            """.trimIndent(),
            result.displayChunk,
        )
        assertEquals("line 1\nline 2\nline 3", result.trackedRecentLogBody)
    }

    @Test
    fun `live log also advances tracked recent body`() {
        val output = """
            status=running

            ==== SillyTavern live log: /tmp/tavern.log ====
            line 3
            ==== end SillyTavern live log ====
        """.trimIndent()

        val result = TermuxDisplayLogReducer.reduce(
            output = output,
            lastTrackedRecentLogBody = "line 1\nline 2",
        )

        assertEquals("line 1\nline 2\nline 3", result.trackedRecentLogBody)
    }
}
