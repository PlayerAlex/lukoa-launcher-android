package moe.lukoa.launcher

enum class LauncherSecondaryPageRefreshTarget {
    VersionManagement,
    Backup,
    TavernConfig,
    ServerPlugins,
    QuickImport,
    ChatGuard,
    StorageCleanup,
    GitStash,
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
                LauncherSecondaryPage.TavernConfig -> LauncherSecondaryPageRefreshTarget.TavernConfig
                LauncherSecondaryPage.ServerPlugins -> LauncherSecondaryPageRefreshTarget.ServerPlugins
                LauncherSecondaryPage.QuickImport -> LauncherSecondaryPageRefreshTarget.QuickImport
                LauncherSecondaryPage.ChatGuard -> LauncherSecondaryPageRefreshTarget.ChatGuard
                LauncherSecondaryPage.StorageCleanup -> LauncherSecondaryPageRefreshTarget.StorageCleanup
                LauncherSecondaryPage.GitStash -> LauncherSecondaryPageRefreshTarget.GitStash
                LauncherSecondaryPage.ExtensionManagement,
                LauncherSecondaryPage.HealthCheck,
                LauncherSecondaryPage.RepairTools,
                LauncherSecondaryPage.Debug,
                LauncherSecondaryPage.TaskCenter,
                null -> null
            }
        }
        if (actionInProgress) return null
        return pendingTarget.also { pendingTarget = null }
    }
}
