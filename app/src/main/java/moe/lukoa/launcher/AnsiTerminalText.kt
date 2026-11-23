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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun TerminalText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = LukoaColors.currentStyle
    val annotated = remember(text, style) { AnsiTerminalText.toAnnotatedString(text) }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
        ),
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
                color = state.foreground ?: LukoaColors.TextPrimary,
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
        val monochrome = LukoaColors.currentStyle == LukoaThemeStyle.Monochrome
        return when (code) {
            30 -> LukoaColors.Dim
            31 -> LukoaColors.Danger
            32 -> LukoaColors.Primary
            33 -> LukoaColors.Accent
            34 -> LukoaColors.Primary
            35 -> if (monochrome) Color(0xFFCBBDB8) else Color(0xFFD38CFF)
            36 -> if (monochrome) Color(0xFFBFC4BE) else Color(0xFF67E8F9)
            37 -> LukoaColors.TextPrimary
            90 -> LukoaColors.TextSecondary
            91 -> if (monochrome) LukoaColors.Stop else Color(0xFFFF8B8B)
            92 -> if (monochrome) LukoaColors.Primary else Color(0xFF7AF0B2)
            93 -> if (monochrome) LukoaColors.Accent else Color(0xFFFFD166)
            94 -> if (monochrome) LukoaColors.TextPrimary else Color(0xFF8EC5FF)
            95 -> if (monochrome) Color(0xFFD6CAC4) else Color(0xFFE5A3FF)
            96 -> if (monochrome) Color(0xFFCBCFCA) else Color(0xFF88F5FF)
            97 -> if (monochrome) LukoaColors.PrimaryHover else Color.White
            else -> LukoaColors.TextPrimary
        }
    }

    private fun indexedColor(index: Int): Color {
        val normalized = index.coerceIn(0, 255)
        return when (normalized) {
            0 -> LukoaColors.Dim
            1 -> LukoaColors.Danger
            2 -> LukoaColors.Primary
            3 -> LukoaColors.Accent
            4 -> LukoaColors.Primary
            5 -> Color(0xFFD38CFF)
            6 -> Color(0xFF67E8F9)
            7 -> LukoaColors.TextPrimary
            in 8..15 -> ansiColor(90 + (normalized - 8))
            in 16..231 -> {
                val colorIndex = normalized - 16
                val levels = intArrayOf(0, 95, 135, 175, 215, 255)
                val red = levels[colorIndex / 36]
                val green = levels[(colorIndex % 36) / 6]
                val blue = levels[colorIndex % 6]
                Color(red, green, blue)
            }
            else -> {
                val gray = 8 + (normalized - 232) * 10
                Color(gray, gray, gray)
            }
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
