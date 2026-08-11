package moe.lukoa.launcher

enum class LauncherSecondaryPageRefreshTarget {
    VersionManagement,
    Backup,
}

class LauncherSecondaryPageRefreshTracker {
    private var observedPage: LauncherSecondaryPage? = null
    private var pendingTarget: LauncherSecondaryPageRefreshTarget? = null

    fun next(
        page: LauncherSecondaryPage?,
        actionInProgress: Boolean,
    ): LauncherSecondaryPageRefreshTarget? {
        if (page != observedPage) {
            observedPage = page
            pendingTarget = when (page) {
                LauncherSecondaryPage.VersionManagement ->
                    LauncherSecondaryPageRefreshTarget.VersionManagement

                LauncherSecondaryPage.Backup -> LauncherSecondaryPageRefreshTarget.Backup
                LauncherSecondaryPage.ExtensionManagement,
                null -> null
            }
        }
        if (actionInProgress) return null
        return pendingTarget.also { pendingTarget = null }
    }
}
