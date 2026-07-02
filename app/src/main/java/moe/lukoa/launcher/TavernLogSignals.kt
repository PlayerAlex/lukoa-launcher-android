package moe.lukoa.launcher

object TavernLogSignals {
    private val liveMarkers = listOf(
        "SillyTavern is listening",
        "SillyTavern is already running",
        "Streaming request in progress",
        "Streaming request failed",
        "Available models:",
        "Extensions available",
        "Generating",
        "prompt:",
        "model:",
    )

    private val importantMarkers = liveMarkers + listOf(
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
        "process is not running",
    )

    fun hasRecentLiveSignal(text: String): Boolean {
        val lines = stripAnsi(text).lineSequence().takeLastCompat(160)
        val lastLive = lines.indexOfLastMatching(liveMarkers, ignoreCase = false)
        if (lastLive < 0) return false
        val lastStop = lines.indexOfLastMatching(stopMarkers, ignoreCase = false)
        return lastStop < lastLive
    }

    fun importantTail(text: String, beforeLines: Int = 24, afterLines: Int = 42): String {
        val lines = stripAnsi(text).lines()
        if (lines.isEmpty()) return ""
        val importantIndex = lines.indexOfLastMatching(importantMarkers, ignoreCase = true)
        if (importantIndex < 0) return ""
        val start = (importantIndex - beforeLines).coerceAtLeast(0)
        val end = (importantIndex + afterLines + 1).coerceAtMost(lines.size)
        return lines.subList(start, end)
            .joinToString("\n")
            .trim()
    }

    fun stripAnsi(value: String): String {
        return value
            .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "")
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
