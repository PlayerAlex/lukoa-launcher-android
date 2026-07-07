package moe.lukoa.launcher

data class TavernDirectoryCandidateOption(
    val path: String,
    val selectable: Boolean,
    val reason: String = "",
) {
    val displayPath: String
        get() = TavernPathNormalizer.toDisplayPath(path)
}

object TavernDirectoryCandidateGuard {
    fun resolve(
        config: TavernPathConfig,
        candidates: List<String>,
    ): List<TavernDirectoryCandidateOption> {
        val occupiedProfiles = config.availableProfiles
            .filterNot { it.id == config.activeProfile.id }
            .associateBy { normalizeComparablePath(it.normalizedTavernDir) }

        return candidates
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeComparablePath)
            .map { candidate ->
                val occupiedBy = occupiedProfiles[normalizeComparablePath(candidate)]
                if (occupiedBy == null) {
                    TavernDirectoryCandidateOption(
                        path = candidate,
                        selectable = true,
                    )
                } else {
                    TavernDirectoryCandidateOption(
                        path = candidate,
                        selectable = false,
                        reason = "这个目录已经被${occupiedBy.normalizedName}使用。",
                    )
                }
            }
    }

    private fun normalizeComparablePath(value: String): String {
        val normalized = value.trim().replace('\\', '/').trimEnd('/')
        if (normalized.isBlank()) return ""
        return when {
            normalized == "~" || normalized == "\$HOME" -> TavernPathDefaults.TERMUX_HOME_DIR
            normalized.startsWith("~/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${normalized.removePrefix("~/")}".trimEnd('/')

            normalized.startsWith("\$HOME/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${normalized.removePrefix("\$HOME/")}".trimEnd('/')

            else -> normalized
        }
    }
}
