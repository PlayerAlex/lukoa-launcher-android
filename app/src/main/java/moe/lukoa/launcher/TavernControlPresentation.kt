package moe.lukoa.launcher

internal fun tavernPrimaryActionLabel(
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    actionInProgress: Boolean,
    busyLabel: String?,
    primaryEnabled: Boolean,
    primaryDisabledReason: String?,
): String = when {
    actionInProgress -> "${busyLabel ?: "处理中"}..."
    tavernStarting || tavernRunning -> "停止酒馆"
    primaryEnabled -> "启动酒馆"
    primaryDisabledReason?.contains("权限") == true -> "先修权限"
    primaryDisabledReason?.contains("Termux", ignoreCase = true) == true -> "先安装 Termux"
    primaryDisabledReason?.contains("没检测到酒馆") == true -> "先安装酒馆"
    primaryDisabledReason?.contains("检测") == true -> "先检测酒馆"
    primaryDisabledReason?.contains("安装") == true -> "先安装酒馆"
    else -> "暂不可用"
}
