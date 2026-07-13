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
            .associateBy { TavernComparablePath.normalize(it.normalizedTavernDir) }

        return candidates
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy(TavernComparablePath::normalize)
            .map { candidate ->
                TavernProfileReservedPathPolicy.candidateBlockedReason(config.activeProfile, candidate)?.let { reason ->
                    return@map TavernDirectoryCandidateOption(
                        path = candidate,
                        selectable = false,
                        reason = reason,
                    )
                }
                val occupiedBy = occupiedProfiles[TavernComparablePath.normalize(candidate)]
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

}
