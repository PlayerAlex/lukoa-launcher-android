package moe.lukoa.launcher

data class TermuxDisplayLogReduceResult(
    val displayChunk: String,
    val trackedRecentLogBody: String,
)

object TermuxDisplayLogReducer {
    fun reduce(
        output: String,
        lastTrackedRecentLogBody: String,
    ): TermuxDisplayLogReduceResult {
        if (output.isBlank()) {
            return TermuxDisplayLogReduceResult(
                displayChunk = "",
                trackedRecentLogBody = lastTrackedRecentLogBody,
            )
        }

        TermuxLogDelta.extractRecentLogBody(output)?.let { recentBody ->
            val displayBody = when {
                recentBody.isBlank() -> ""
                lastTrackedRecentLogBody.isBlank() -> TermuxLogDelta.firstImportantSnapshot(recentBody)
                else -> TermuxLogDelta.newSuffix(lastTrackedRecentLogBody, recentBody)
            }
            return TermuxDisplayLogReduceResult(
                displayChunk = replaceMarkedBody(
                    output = output,
                    startPrefix = "==== SillyTavern recent log:",
                    endPrefix = "==== end SillyTavern recent log",
                    replacementBody = displayBody,
                ),
                trackedRecentLogBody = recentBody.ifBlank { lastTrackedRecentLogBody },
            )
        }

        TermuxLogDelta.extractLiveLogBody(output)?.let { liveBody ->
            return TermuxDisplayLogReduceResult(
                displayChunk = replaceMarkedBody(
                    output = output,
                    startPrefix = "==== SillyTavern live log:",
                    endPrefix = "==== end SillyTavern live log",
                    replacementBody = liveBody,
                ),
                trackedRecentLogBody = TermuxLogDelta.appendLiveDelta(lastTrackedRecentLogBody, liveBody),
            )
        }

        return TermuxDisplayLogReduceResult(
            displayChunk = output.trim(),
            trackedRecentLogBody = lastTrackedRecentLogBody,
        )
    }

    private fun replaceMarkedBody(
        output: String,
        startPrefix: String,
        endPrefix: String,
        replacementBody: String,
    ): String {
        val lines = output.lineSequence().toList()
        val startIndex = lines.indexOfFirst { it.startsWith(startPrefix) }
        if (startIndex < 0) return output.trim()
        val endIndex = lines.indexOfFirstAfter(startIndex + 1) { it.startsWith(endPrefix) }
        val rebuilt = buildList {
            addAll(lines.take(startIndex))
            if (replacementBody.isNotBlank()) {
                add(lines[startIndex])
                addAll(replacementBody.lineSequence().toList())
                if (endIndex >= 0) {
                    add(lines[endIndex])
                }
            }
            if (endIndex >= 0) {
                addAll(lines.drop(endIndex + 1))
            }
        }
        return rebuilt.joinToString("\n").trim()
    }

    private inline fun List<String>.indexOfFirstAfter(
        startIndex: Int,
        predicate: (String) -> Boolean,
    ): Int {
        for (index in startIndex until size) {
            if (predicate(this[index])) return index
        }
        return -1
    }
}
