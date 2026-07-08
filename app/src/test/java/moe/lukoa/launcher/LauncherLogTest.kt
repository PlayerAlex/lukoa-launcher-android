package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLogTest {
    @Test
    fun `append raw log keeps termux content unchanged`() {
        val appended = appendRawLog(
            current = "暂无 Termux 前台回传。",
            text = """
                line 1
                line 2
            """.trimIndent(),
        )

        assertEquals(
            """
                line 1
                line 2
            """.trimIndent(),
            appended,
        )
    }

    @Test
    fun `append raw log only uses blank line as separator`() {
        val appended = appendRawLog(
            current = "line 1",
            text = "line 2",
        )

        assertEquals("line 1\n\nline 2", appended)
    }
}
