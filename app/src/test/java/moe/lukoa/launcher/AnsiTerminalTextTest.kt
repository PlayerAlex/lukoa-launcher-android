package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsiTerminalTextTest {
    @Test
    fun `standard ansi colors render without escape text`() {
        val annotated = AnsiTerminalText.toAnnotatedString(
            "\u001B[31mred\u001B[0m plain \u001B[32mgreen\u001B[0m",
        )

        assertEquals("red plain green", annotated.text)
        assertEquals(LukoaColors.Danger, annotated.spanStyles.first { it.start == 0 && it.end == 3 }.item.color)
        assertEquals(LukoaColors.Accent, annotated.spanStyles.first { it.start == 10 && it.end == 15 }.item.color)
    }

    @Test
    fun `indexed and rgb ansi colors are preserved`() {
        val annotated = AnsiTerminalText.toAnnotatedString(
            "\u001B[38;5;196mhot\u001B[0m \u001B[38;2;12;34;56mrgb\u001B[0m",
        )

        assertEquals("hot rgb", annotated.text)
        assertEquals(Color(255, 0, 0), annotated.spanStyles.first { it.start == 0 && it.end == 3 }.item.color)
        assertEquals(Color(12, 34, 56), annotated.spanStyles.first { it.start == 4 && it.end == 7 }.item.color)
    }

    @Test
    fun `osc control sequences stay hidden from rendered text`() {
        val annotated = AnsiTerminalText.toAnnotatedString("before\u001B]0;title\u0007after")

        assertEquals("beforeafter", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
