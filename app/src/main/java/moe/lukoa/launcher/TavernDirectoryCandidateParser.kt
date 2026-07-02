package moe.lukoa.launcher

object TavernDirectoryCandidateParser {
    private const val START_MARKER = "==== SillyTavern directory candidates ===="
    private const val END_MARKER = "==== end SillyTavern directory candidates ===="
    private val linePattern = Regex("""candidate\.\d+=(.+)""")

    fun parse(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val section = text.substringAfter(START_MARKER, missingDelimiterValue = "")
            .substringBefore(END_MARKER)
            .trim()
        if (section.isBlank()) return emptyList()
        return section.lineSequence()
            .map { it.trim() }
            .mapNotNull { line ->
                linePattern.matchEntire(line)?.groupValues?.getOrNull(1)?.trim()
            }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }
}
