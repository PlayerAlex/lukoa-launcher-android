package moe.lukoa.launcher

object TermuxLogDelta {
    fun extractLiveLogBody(output: String): String? {
        return extractMarkedBody(
            output = output,
            startPrefix = "==== SillyTavern live log:",
            endPrefix = "==== end SillyTavern live log",
        )
    }

    fun extractRecentLogBody(output: String): String? {
        return extractMarkedBody(
            output = output,
            startPrefix = "==== SillyTavern recent log:",
            endPrefix = "==== end SillyTavern recent log",
        )
    }

    private fun extractMarkedBody(
        output: String,
        startPrefix: String,
        endPrefix: String,
    ): String? {
        val lines = output.lineSequence().toList()
        val startIndex = lines.indexOfFirst { it.startsWith(startPrefix) }
        if (startIndex < 0) return null
        val endIndex = lines.indexOfFirstAfter(startIndex + 1) {
            it.startsWith(endPrefix)
        }
        val bodyLines = if (endIndex >= 0) {
            lines.subList(startIndex + 1, endIndex)
        } else {
            lines.drop(startIndex + 1)
        }
        return bodyLines.joinToString("\n").trimEnd()
    }

    fun newSuffix(previous: String, current: String): String {
        if (current.isBlank() || previous.isBlank()) return ""
        if (current == previous) return ""
        if (current.startsWith(previous)) {
            return current.substring(previous.length).trimStart('\n')
        }

        val maxOverlap = minOf(previous.length, current.length)
        for (size in maxOverlap downTo 1) {
            if (previous.regionMatches(previous.length - size, current, 0, size)) {
                return current.substring(size).trimStart('\n')
            }
        }
        return current
    }

    fun firstImportantSnapshot(current: String): String {
        return TavernLogSignals.importantTail(current)
    }

    private inline fun List<String>.indexOfFirstAfter(startIndex: Int, predicate: (String) -> Boolean): Int {
        for (index in startIndex until size) {
            if (predicate(this[index])) return index
        }
        return -1
    }
}
