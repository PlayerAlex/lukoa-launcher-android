package moe.lukoa.launcher

object TavernLogSignals {
    private const val SESSION_MARKER = "===== Lukoa launcher foreground session ====="

    private val liveMarkers = listOf(
        "SillyTavern is listening",
        "SillyTavern is already running",
        "Streaming request in progress",
        "Streaming request failed",
        "Generating",
        "prompt:",
        "model:",
    )

    private val snapshotMarkers = listOf(
        "Installing Node Modules...",
        "Entering SillyTavern...",
        "Compiling frontend libraries...",
        "Launching in a browser:",
        "SillyTavern is listening",
        "SillyTavern is already running",
        "Streaming request in progress",
        "Streaming request failed",
        "Generating",
        "prompt:",
        "model:",
    )

    private val importantMarkers = snapshotMarkers + listOf(
        "model not found",
        "invalid_request_error",
        "too many requests",
        "rate limit",
        "unauthorized",
        "invalid api key",
        "forbidden",
        "context length",
        "maximum context",
        "token limit",
        "econnrefused",
        "enotfound",
        "etimedout",
        "error",
        "failed",
    )

    private val stopMarkers = listOf(
        "SillyTavern foreground session exited",
        "SillyTavern process exited immediately",
        "SillyTavern stopped",
        "SillyTavern force cleanup completed",
        "process is not running",
    )

    fun hasRecentLiveSignal(text: String): Boolean {
        val lines = prepareForAnalysis(text).lineSequence().takeLastCompat(160)
        val lastLive = lines.indexOfLastMatching(liveMarkers, ignoreCase = false)
        if (lastLive < 0) return false
        val lastStop = lines.indexOfLastMatching(stopMarkers, ignoreCase = false)
        return lastStop < lastLive
    }

    fun importantTail(text: String, beforeLines: Int = 24, afterLines: Int = 42): String {
        val lines = latestSessionLines(text)
        if (lines.isEmpty()) return ""
        val importantIndex = lines.indexOfLastMatching(importantMarkers, ignoreCase = true)
        if (importantIndex < 0) return ""
        val start = (importantIndex - beforeLines).coerceAtLeast(0)
        val end = (importantIndex + afterLines + 1).coerceAtMost(lines.size)
        return lines.subList(start, end)
            .joinToString("\n")
            .trim()
    }

    fun latestSessionTail(text: String, maxLines: Int = 72): String {
        val lines = latestSessionLines(text)
        if (lines.isEmpty()) return ""
        return lines.takeLast(maxLines)
            .joinToString("\n")
            .trim()
    }

    fun latestForegroundSession(text: String): String {
        val preparedLines = prepareForApp(text).lines()
        if (preparedLines.isEmpty()) return ""
        val plainLines = preparedLines.map(::stripAnsi)
        val sessionStart = plainLines.indexOfLast { it.contains(SESSION_MARKER) }
        if (sessionStart < 0) return ""
        return preparedLines.subList(sessionStart, preparedLines.size)
            .joinToString("\n")
            .trim()
    }

    fun prepareForApp(value: String): String {
        if (value.isBlank()) return ""
        return value
            .replace("\r\n", "\n")
            .trimEnd()
    }

    fun stripAnsi(value: String): String {
        return value
            .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "")
    }

    private fun prepareForAnalysis(value: String): String {
        return stripAnsi(prepareForApp(value))
    }

    private fun latestSessionLines(text: String): List<String> {
        val preparedLines = prepareForAnalysis(text).lines()
        if (preparedLines.isEmpty()) return emptyList()
        val sessionStart = preparedLines.indexOfLast { it.contains(SESSION_MARKER) }
        return if (sessionStart >= 0) {
            preparedLines.subList(sessionStart, preparedLines.size)
        } else {
            preparedLines
        }
    }


    private fun Sequence<String>.takeLastCompat(count: Int): List<String> {
        return toList().takeLast(count)
    }

    private fun List<String>.indexOfLastMatching(markers: List<String>, ignoreCase: Boolean): Int {
        for (index in indices.reversed()) {
            val line = this[index]
            if (markers.any { marker -> line.contains(marker, ignoreCase = ignoreCase) }) {
                return index
            }
        }
        return -1
    }
}
