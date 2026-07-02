package moe.lukoa.launcher

object TermuxPermissionSignals {
    fun externalAppsBlocked(text: String): Boolean {
        return text.contains("allow-external-apps", ignoreCase = true) ||
            text.contains("RunCommandService requires", ignoreCase = true) ||
            text.contains("termux.properties", ignoreCase = true)
    }
}
