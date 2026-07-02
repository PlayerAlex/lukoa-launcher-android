package moe.lukoa.launcher

object TavernVersionSelection {
    fun versionManagementChoices(
        officialVersions: TavernOfficialVersions,
        current: TavernVersionInfo,
    ): TavernOfficialVersions {
        if (!current.hasData || current.notInstalled) return officialVersions
        return TavernOfficialVersions(
            stable = officialVersions.stable.filterNot { TavernVersionComparator.matchesCurrent(current, it) },
            test = officialVersions.test.filterNot { TavernVersionComparator.matchesCurrent(current, it) },
        )
    }

    fun normalizeForVersionManagement(
        officialVersions: TavernOfficialVersions,
        current: TavernVersionInfo,
        currentSelection: TavernVersionChoice?,
    ): TavernVersionChoice? {
        if (currentSelection?.kind == TavernVersionKind.Custom) return currentSelection
        return keepExistingOrRecommended(
            choices = versionManagementChoices(officialVersions, current),
            currentSelection = currentSelection,
        )
    }

    fun normalizeForInstall(
        officialVersions: TavernOfficialVersions,
        currentSelection: TavernVersionChoice?,
    ): TavernVersionChoice? {
        if (currentSelection?.kind == TavernVersionKind.Custom) return currentSelection
        return keepExistingOrRecommended(
            choices = officialVersions,
            currentSelection = currentSelection,
        )
    }

    fun recommendedInstallChoice(officialVersions: TavernOfficialVersions): TavernVersionChoice {
        return officialVersions.stable.firstOrNull()
            ?: officialVersions.test.firstOrNull()
            ?: TavernInstallDefaults.Release
    }

    private fun keepExistingOrRecommended(
        choices: TavernOfficialVersions,
        currentSelection: TavernVersionChoice?,
    ): TavernVersionChoice? {
        val selected = currentSelection
        if (selected != null) {
            choices.all.firstOrNull { candidate ->
                candidate.kind == selected.kind &&
                    candidate.target == selected.target &&
                    candidate.name == selected.name
            }?.let { return it }
        }

        return choices.stable.firstOrNull() ?: choices.test.firstOrNull()
    }
}
