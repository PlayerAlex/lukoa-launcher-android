package moe.lukoa.launcher

object TavernDirectoryChoicePromptPolicy {
    private const val ENGLISH_PREFIX = "SillyTavern directory not found:"
    private const val CHINESE_PREFIX = "酒馆目录不存在："
    private const val CHINESE_PREFIX_ALT = "SillyTavern 目录不存在："

    private val knownSuffixes = listOf(
        ". Multiple candidates were found; choose one manually in the launcher.",
        ". A possible directory was found; confirm it manually in the launcher.",
    )

    fun shouldPrompt(
        text: String,
        currentPath: String,
    ): Boolean {
        if (text.isBlank()) return false
        if (TavernDirectoryCandidateParser.parse(text).isEmpty()) return false

        val missingPath = missingConfiguredPath(text) ?: return true
        return comparablePath(missingPath) == comparablePath(currentPath)
    }

    fun missingConfiguredPath(text: String): String? {
        return parseMissingPath(text, ENGLISH_PREFIX)
            ?: parseMissingPath(text, CHINESE_PREFIX)
            ?: parseMissingPath(text, CHINESE_PREFIX_ALT)
    }

    private fun parseMissingPath(
        text: String,
        prefix: String,
    ): String? {
        val startIndex = text.indexOf(prefix, ignoreCase = true)
        if (startIndex < 0) return null

        val remainder = text.substring(startIndex + prefix.length)
        val rawValue = remainder.substring(0, promptBoundaryIndex(remainder)).trim()
        if (rawValue.isBlank()) return null

        val withoutKnownSuffix = knownSuffixes.firstOrNull { suffix ->
            rawValue.endsWith(suffix, ignoreCase = true)
        }?.let { suffix ->
            rawValue.removeSuffix(suffix).trim()
        } ?: rawValue

        return withoutKnownSuffix
            .trim()
            .trimEnd('"', ',', '}', ']')
            .takeIf { it.isNotBlank() }
            ?.let(TavernPathNormalizer::normalize)
    }

    private fun promptBoundaryIndex(remainder: String): Int {
        return buildList {
            knownSuffixes.forEach { suffix ->
                remainder.indexOf(suffix, ignoreCase = true)
                    .takeIf { it >= 0 }
                    ?.let(::add)
            }
            listOf("\r", "\n", "\",", "\"}", "\"")
                .forEach { marker ->
                    remainder.indexOf(marker)
                        .takeIf { it >= 0 }
                        ?.let(::add)
                }
        }.minOrNull() ?: remainder.length
    }

    private fun comparablePath(value: String): String {
        return TavernPathNormalizer.toDisplayPath(
            TavernPathNormalizer.normalize(value),
        )
    }
}
