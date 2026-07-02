package moe.lukoa.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(text) { AnsiTerminalText.toAnnotatedString(text) }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
    )
}

object AnsiTerminalText {
    private const val ESC = '\u001B'

    fun toAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            var index = 0
            var state = StyleState()
            while (index < text.length) {
                val escIndex = text.indexOf(ESC, index)
                if (escIndex < 0) {
                    appendStyled(text.substring(index), state)
                    break
                }
                if (escIndex > index) {
                    appendStyled(text.substring(index, escIndex), state)
                }
                val parsed = parseEscape(text, escIndex, state)
                state = parsed.state
                index = parsed.nextIndex
            }
        }
    }

    private fun AnnotatedString.Builder.appendStyled(value: String, state: StyleState) {
        if (value.isEmpty()) return
        val start = length
        append(value)
        addStyle(
            SpanStyle(
                color = state.foreground ?: LukoaColors.Text,
                background = state.background ?: Color.Unspecified,
                fontWeight = if (state.bold) FontWeight.Bold else FontWeight.Normal,
            ),
            start,
            length,
        )
    }

    private fun parseEscape(text: String, start: Int, state: StyleState): ParseResult {
        if (start + 1 >= text.length) return ParseResult(state, start + 1)
        val next = text[start + 1]
        if (next == ']') {
            val end = findOscEnd(text, start + 2)
            return ParseResult(state, end)
        }
        if (next != '[') return ParseResult(state, start + 2)

        var end = start + 2
        while (end < text.length && text[end] !in '@'..'~') {
            end += 1
        }
        if (end >= text.length) return ParseResult(state, text.length)
        val final = text[end]
        if (final != 'm') return ParseResult(state, end + 1)

        val params = text.substring(start + 2, end)
            .split(';')
            .map { it.toIntOrNull() ?: 0 }
            .ifEmpty { listOf(0) }
        return ParseResult(applySgr(params, state), end + 1)
    }

    private fun findOscEnd(text: String, start: Int): Int {
        var index = start
        while (index < text.length) {
            if (text[index] == '\u0007') return index + 1
            if (text[index] == ESC && index + 1 < text.length && text[index + 1] == '\\') {
                return index + 2
            }
            index += 1
        }
        return text.length
    }

    private fun applySgr(params: List<Int>, initial: StyleState): StyleState {
        var state = initial
        var index = 0
        while (index < params.size) {
            when (val code = params[index]) {
                0 -> state = StyleState()
                1 -> state = state.copy(bold = true)
                22 -> state = state.copy(bold = false)
                30, 31, 32, 33, 34, 35, 36, 37,
                90, 91, 92, 93, 94, 95, 96, 97 -> state = state.copy(foreground = ansiColor(code))
                39 -> state = state.copy(foreground = null)
                40, 41, 42, 43, 44, 45, 46, 47,
                100, 101, 102, 103, 104, 105, 106, 107 -> state = state.copy(background = ansiColor(code - 10).copy(alpha = 0.22f))
                49 -> state = state.copy(background = null)
                38, 48 -> {
                    val parsed = parseExtendedColor(params, index)
                    if (parsed != null) {
                        state = if (code == 38) {
                            state.copy(foreground = parsed.color)
                        } else {
                            state.copy(background = parsed.color.copy(alpha = 0.22f))
                        }
                        index = parsed.nextIndex
                    }
                }
            }
            index += 1
        }
        return state
    }

    private fun parseExtendedColor(params: List<Int>, index: Int): ExtendedColor? {
        val mode = params.getOrNull(index + 1) ?: return null
        return when (mode) {
            2 -> {
                val r = params.getOrNull(index + 2) ?: return null
                val g = params.getOrNull(index + 3) ?: return null
                val b = params.getOrNull(index + 4) ?: return null
                ExtendedColor(Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)), index + 4)
            }
            5 -> {
                val colorIndex = params.getOrNull(index + 2) ?: return null
                ExtendedColor(indexedColor(colorIndex), index + 2)
            }
            else -> null
        }
    }

    private fun ansiColor(code: Int): Color {
        return when (code) {
            30 -> LukoaColors.Dim
            31 -> LukoaColors.Danger
            32 -> LukoaColors.Accent
            33 -> LukoaColors.Amber
            34 -> LukoaColors.Info
            35 -> Color(0xFFD38CFF)
            36 -> Color(0xFF67E8F9)
            37 -> LukoaColors.Text
            90 -> LukoaColors.Muted
            91 -> Color(0xFFFF8B8B)
            92 -> Color(0xFF7AF0B2)
            93 -> Color(0xFFFFD166)
            94 -> Color(0xFF8EC5FF)
            95 -> Color(0xFFE5A3FF)
            96 -> Color(0xFF88F5FF)
            97 -> Color.White
            else -> LukoaColors.Text
        }
    }

    private fun indexedColor(index: Int): Color {
        return when (index.coerceIn(0, 255)) {
            0 -> LukoaColors.Dim
            1 -> LukoaColors.Danger
            2 -> LukoaColors.Accent
            3 -> LukoaColors.Amber
            4 -> LukoaColors.Info
            5 -> Color(0xFFD38CFF)
            6 -> Color(0xFF67E8F9)
            7 -> LukoaColors.Text
            in 8..15 -> ansiColor(90 + (index - 8))
            else -> LukoaColors.Text
        }
    }

    private data class StyleState(
        val foreground: Color? = null,
        val background: Color? = null,
        val bold: Boolean = false,
    )

    private data class ParseResult(
        val state: StyleState,
        val nextIndex: Int,
    )

    private data class ExtendedColor(
        val color: Color,
        val nextIndex: Int,
    )
}
