package moe.lukoa.launcher

data class BackupArchiveSelection(
    val normalizedPath: String = "",
    val errorMessage: String? = null,
) {
    val isValid: Boolean
        get() = errorMessage == null
}

object LauncherBackupUiPolicy {
    fun selectArchive(path: String, actionLabel: String): BackupArchiveSelection {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            return BackupArchiveSelection(errorMessage = "没有选中要${actionLabel}的备份。")
        }
        val validationMessage = LauncherInputGuards.validateBackupArchivePath(normalized)
        if (validationMessage != null) {
            return BackupArchiveSelection(errorMessage = "备份路径无效，不能$actionLabel：$validationMessage")
        }
        return BackupArchiveSelection(normalizedPath = normalized)
    }

    fun defaultRenameName(path: String): String {
        return path.trim()
            .substringAfterLast('/')
            .removeSuffix(".tar.gz")
            .take(48)
    }

    fun duplicateRenamePath(
        archivePath: String,
        newName: String,
        backupHistory: List<String>,
    ): String? {
        val normalizedPath = archivePath.trim()
        val targetFileName = LauncherInputGuards.backupFileNameForLabel(newName.trim()) ?: return null
        return backupHistory.firstOrNull { existingPath ->
            existingPath.trim() != normalizedPath && existingPath.substringAfterLast('/') == targetFileName
        }
    }
}
