package moe.lukoa.launcher

object LauncherPathSettingsPolicy {
    fun customMigrationError(pathInput: String): String? {
        val trimmed = pathInput.trim()
        if (trimmed.isBlank()) {
            return "请先填写要迁移到的目录。"
        }
        return TavernPathValidator.validate(trimmed)
    }

    fun resolvePort(portInput: String, currentPort: Int): Int {
        return portInput.trim().toIntOrNull() ?: currentPort
    }
}
