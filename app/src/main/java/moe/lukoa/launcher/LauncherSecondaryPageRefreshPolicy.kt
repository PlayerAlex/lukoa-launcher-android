package moe.lukoa.launcher

enum class LauncherSecondaryPageRefreshTarget {
    VersionManagement,
    Backup,
    ExtensionManagement,
    TavernConfig,
    ServerPlugins,
    QuickImport,
    HealthCheck,
    ChatGuard,
    StorageCleanup,
    RepairTools,
    GitStash,
    UserManagement,
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
                LauncherSecondaryPage.ExtensionManagement ->
                    LauncherSecondaryPageRefreshTarget.ExtensionManagement

                LauncherSecondaryPage.TavernConfig -> LauncherSecondaryPageRefreshTarget.TavernConfig
                LauncherSecondaryPage.ServerPlugins -> LauncherSecondaryPageRefreshTarget.ServerPlugins
                LauncherSecondaryPage.QuickImport -> LauncherSecondaryPageRefreshTarget.QuickImport
                LauncherSecondaryPage.HealthCheck -> LauncherSecondaryPageRefreshTarget.HealthCheck
                LauncherSecondaryPage.ChatGuard -> LauncherSecondaryPageRefreshTarget.ChatGuard
                LauncherSecondaryPage.StorageCleanup -> LauncherSecondaryPageRefreshTarget.StorageCleanup
                LauncherSecondaryPage.RepairTools -> LauncherSecondaryPageRefreshTarget.RepairTools
                LauncherSecondaryPage.GitStash -> LauncherSecondaryPageRefreshTarget.GitStash
                LauncherSecondaryPage.UserManagement -> LauncherSecondaryPageRefreshTarget.UserManagement
                LauncherSecondaryPage.Debug,
                LauncherSecondaryPage.TaskCenter,
                null -> null
            }
        }
        if (actionInProgress) return null
        return pendingTarget.also { pendingTarget = null }
    }
}
