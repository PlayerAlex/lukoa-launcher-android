package moe.lukoa.launcher

object TavernVersionCatalog {
    fun requiresOfficialCatalog(target: TavernVersionChoice?): Boolean {
        return target != null && target.kind != TavernVersionKind.Custom
    }

    fun matchesCurrentMirror(
        target: TavernVersionChoice?,
        currentRepoUrl: String,
    ): Boolean {
        if (!requiresOfficialCatalog(target)) return true
        val targetRepoUrl = target?.repoUrl.orEmpty().ifBlank { currentRepoUrl }
        return sameRepoUrl(targetRepoUrl, currentRepoUrl)
    }

    fun listMatchesCurrentMirror(
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
    ): Boolean {
        if (!officialVersions.hasData) return true
        return sameRepoUrl(officialVersions.repoUrl, currentRepoUrl)
    }

    fun containsChoice(
        officialVersions: TavernOfficialVersions,
        target: TavernVersionChoice?,
    ): Boolean {
        if (!requiresOfficialCatalog(target)) return true
        val selected = target ?: return false
        return officialVersions.all.any { candidate ->
            candidate.kind == selected.kind &&
                candidate.target.equals(selected.target, ignoreCase = true) &&
                candidate.name.equals(selected.name, ignoreCase = true)
        }
    }
}
