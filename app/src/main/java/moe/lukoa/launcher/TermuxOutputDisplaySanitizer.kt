package moe.lukoa.launcher

object TermuxOutputDisplaySanitizer {
    private const val LIVE_LOG_PREFIX = "==== SillyTavern live log:"
    private const val LIVE_LOG_END_PREFIX = "==== end SillyTavern live log"
    private const val RECENT_LOG_PREFIX = "==== SillyTavern recent log:"
    private const val RECENT_LOG_END_PREFIX = "==== end SillyTavern recent log"
    private val ansiOsc = Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)")
    private val ansiNonColorCsi = Regex("\u001B\\[(?![0-9;]*m)[0-?]*[ -/]*[@-~]")

    fun sanitize(text: String): String {
        if (text.isBlank()) return ""
        val cleaned = text
            .replace(ansiOsc, "")
            .replace(ansiNonColorCsi, "")
            .replace("\r\n", "\n")
        return cleaned.lineSequence()
            .filterNot(::isDisplayWrapperLine)
            .joinToString("\n")
            .trim()
    }

    private fun isDisplayWrapperLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith(LIVE_LOG_PREFIX) ||
            trimmed.startsWith(LIVE_LOG_END_PREFIX) ||
            trimmed.startsWith(RECENT_LOG_PREFIX) ||
            trimmed.startsWith(RECENT_LOG_END_PREFIX)
    }
}
