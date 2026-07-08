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
        val lines = prepareForApp(text).lineSequence().takeLastCompat(160)
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

    fun prepareForApp(value: String): String {
        if (value.isBlank()) return ""
        val lines = stripAnsi(value).replace("\r\n", "\n").lines()
        val result = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trimEnd()
            when {
                trimmed.startsWith("Extensions available for") && trimmed.endsWith("[") -> {
                    val itemCount = countUntilListEnd(lines, index + 1)
                    result += "${trimmed.removeSuffix("[").trimEnd()} [已省略 ${itemCount.coerceAtLeast(1)} 项扩展]"
                    index = skipListBlock(lines, index + 1)
                }

                trimmed.startsWith("Available models:") && trimmed.endsWith("[") -> {
                    val itemCount = countUntilListEnd(lines, index + 1)
                    result += "Available models: [已省略 ${itemCount.coerceAtLeast(1)} 项模型]"
                    index = skipListBlock(lines, index + 1)
                }

                else -> {
                    result += line
                    index += 1
                }
            }
        }
        return result.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trimEnd()
    }

    fun stripAnsi(value: String): String {
        return value
            .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "")
    }

    private fun latestSessionLines(text: String): List<String> {
        val preparedLines = prepareForApp(text).lines()
        if (preparedLines.isEmpty()) return emptyList()
        val sessionStart = preparedLines.indexOfLast { it.contains(SESSION_MARKER) }
        return if (sessionStart >= 0) {
            preparedLines.subList(sessionStart, preparedLines.size)
        } else {
            preparedLines
        }
    }

    private fun countUntilListEnd(lines: List<String>, startIndex: Int): Int {
        var count = 0
        var index = startIndex
        while (index < lines.size && lines[index].trim() != "]") {
            if (lines[index].trim().isNotBlank()) {
                count += 1
            }
            index += 1
        }
        return count
    }

    private fun skipListBlock(lines: List<String>, startIndex: Int): Int {
        var index = startIndex
        while (index < lines.size) {
            if (lines[index].trim() == "]") {
                return index + 1
            }
            index += 1
        }
        return lines.size
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
