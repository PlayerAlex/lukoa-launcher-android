package moe.lukoa.launcher

data class LauncherUiState(
    val status: String,
    val summary: String,
    val termuxLog: String,
    val appLog: String,
    val verified: Boolean,
    val officialVersionsCache: String = "",
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalMinutes: Int = 360,
    val autoBackupKeepCount: Int = 5,
    val backupHistory: List<String> = emptyList(),
    val termuxReturnDelayMs: Long = 600L,
)

fun defaultLauncherState(isTermuxInstalled: Boolean): LauncherUiState {
    return LauncherUiState(
        status = if (isTermuxInstalled) "Termux 已安装" else "未检测到 Termux",
        summary = if (isTermuxInstalled) "准备就绪" else "请先安装并打开 Termux",
        termuxLog = "暂无 Termux 回传。",
        appLog = logEntry("App", if (isTermuxInstalled) "检测到 Termux 已安装。" else "未检测到 Termux。"),
        verified = false,
    )
}
