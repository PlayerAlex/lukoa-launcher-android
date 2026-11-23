package moe.lukoa.launcher

enum class LauncherQuickFixActionType {
    RunHealthCheck,
    RequestRunPermission,
    CopyExternalAppsCommand,
    PrepareTermuxEnvironment,
    OpenPathSettings,
    OpenNetworkSettings,
    OpenRepairTools,
    ForceCleanupPort,
    RecheckTavernVersion,
    RequestTermuxStoragePermission,
}

data class LauncherQuickFixAction(
    val type: LauncherQuickFixActionType,
    val label: String,
)
