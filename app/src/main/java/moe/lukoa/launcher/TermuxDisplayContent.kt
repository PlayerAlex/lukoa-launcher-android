package moe.lukoa.launcher

data class TermuxDisplayContent(
    val commandText: String = "",
    val tavernRuntimeLogText: String = "",
)

object TermuxDisplayContentExtractor {
    private const val DIRECTORY_CANDIDATES_START = "==== SillyTavern directory candidates ===="
    private const val DIRECTORY_CANDIDATES_END = "==== end SillyTavern directory candidates ===="
    private const val LIVE_LOG_START = "==== SillyTavern live log:"
    private const val LIVE_LOG_END = "==== end SillyTavern live log"
    private const val RECENT_LOG_START = "==== SillyTavern recent log:"
    private const val RECENT_LOG_END = "==== end SillyTavern recent log"

    fun extract(output: String): TermuxDisplayContent {
        if (output.isBlank()) return TermuxDisplayContent()
        val normalized = TavernLogSignals.stripAnsi(output).replace("\r\n", "\n")
        return TermuxDisplayContent(
            commandText = extractCommandText(normalized),
            tavernRuntimeLogText = extractRuntimeLogText(normalized),
        )
    }

    private fun extractCommandText(output: String): String {
        val lines = output.lines()
        val visibleLines = mutableListOf<String>()
        var index = leadingJsonEndIndex(lines)
        while (index < lines.size) {
            val trimmed = lines[index].trim()
            index = when {
                trimmed.startsWith(DIRECTORY_CANDIDATES_START) -> skipMarkedSection(
                    lines = lines,
                    startIndex = index,
                    endPrefix = DIRECTORY_CANDIDATES_END,
                )

                trimmed.startsWith(LIVE_LOG_START) -> skipMarkedSection(
                    lines = lines,
                    startIndex = index,
                    endPrefix = LIVE_LOG_END,
                )

                trimmed.startsWith(RECENT_LOG_START) -> skipMarkedSection(
                    lines = lines,
                    startIndex = index,
                    endPrefix = RECENT_LOG_END,
                )

                trimmed.startsWith("liveLog.") ||
                    trimmed.startsWith("candidate.") ||
                    trimmed.startsWith("profile_id=") ||
                    trimmed.startsWith("runtime_state_dir=") -> {
                    index + 1
                }

                else -> {
                    visibleLines += lines[index]
                    index + 1
                }
            }
        }
        return TavernLogSignals.prepareForApp(visibleLines.joinToString("\n"))
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun extractRuntimeLogText(output: String): String {
        return listOfNotNull(
            TermuxLogDelta.extractRecentLogBody(output),
            TermuxLogDelta.extractLiveLogBody(output),
        ).map { TavernLogSignals.prepareForApp(it).trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
    }

    private fun leadingJsonEndIndex(lines: List<String>): Int {
        val firstNonBlank = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlank < 0 || lines[firstNonBlank].trim() != "{") {
            return 0
        }
        var depth = 0
        for (index in firstNonBlank until lines.size) {
            depth += lines[index].count { it == '{' }
            depth -= lines[index].count { it == '}' }
            if (depth <= 0) {
                return index + 1
            }
        }
        return 0
    }

    private fun skipMarkedSection(
        lines: List<String>,
        startIndex: Int,
        endPrefix: String,
    ): Int {
        for (index in startIndex + 1 until lines.size) {
            if (lines[index].trim().startsWith(endPrefix)) {
                return index + 1
            }
        }
        return lines.size
    }
}
