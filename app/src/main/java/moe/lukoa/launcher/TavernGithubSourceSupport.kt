package moe.lukoa.launcher

import java.net.URLDecoder
import java.util.Locale

data class TavernGithubSource(
    val repository: String,
    val proxyPrefix: String,
) {
    val tagsApiUrl: String
        get() = apiUrl("/tags?per_page=100")

    val branchesApiUrl: String
        get() = apiUrl("/branches?per_page=100")

    private fun apiUrl(path: String): String {
        val apiUrl = "https://api.github.com/repos/$repository$path"
        return if (proxyPrefix.isBlank()) apiUrl else proxyPrefix + apiUrl
    }
}

object TavernGithubSourceParser {
    private val repositoryPattern = Regex(
        """^https://github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\.git)?(?:/)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(repoUrl: String): TavernGithubSource? {
        val normalized = normalize(repoUrl)
        repositoryPattern.matchEntire(normalized)?.let { match ->
            return TavernGithubSource(
                repository = "${match.groupValues[1]}/${match.groupValues[2]}",
                proxyPrefix = "",
            )
        }

        val embeddedMarker = "https://github.com/"
        val embeddedIndex = normalized.lowercase(Locale.US).indexOf(embeddedMarker)
        if (embeddedIndex <= 0) return null

        val proxyPrefix = normalized.substring(0, embeddedIndex)
        val innerUrl = normalized.substring(embeddedIndex)
        val match = repositoryPattern.matchEntire(innerUrl) ?: return null
        return TavernGithubSource(
            repository = "${match.groupValues[1]}/${match.groupValues[2]}",
            proxyPrefix = proxyPrefix,
        )
    }

    private fun normalize(repoUrl: String): String {
        val trimmed = repoUrl.trim()
            .substringBefore("#")
            .substringBefore("?")
            .trimEnd('/')
        return runCatching {
            URLDecoder.decode(trimmed, Charsets.UTF_8.name())
        }.getOrDefault(trimmed)
    }
}
