package moe.lukoa.launcher

data class PendingManualBackupRecoveryResult(
    val archivePath: String,
    val fileName: String,
)

object PendingManualBackupRecovery {
    private const val RECENT_BACKUP_TOLERANCE_MS = 90_000L

    fun recover(
        startedAtMillis: Long,
        expectedLabel: String,
        archives: List<BackupLibraryArchiveDetails>,
    ): PendingManualBackupRecoveryResult? {
        if (startedAtMillis <= 0L) return null
        val recentManualArchives = archives
            .asSequence()
            .filter { isManualBackupArchive(it.termuxReadablePath) }
            .filter { it.modifiedAtMillis >= startedAtMillis - RECENT_BACKUP_TOLERANCE_MS }
            .sortedByDescending { it.modifiedAtMillis }
            .toList()
        if (recentManualArchives.isEmpty()) return null

        val expectedFileName = LauncherInputGuards.backupFileNameForLabel(expectedLabel.trim())
        if (!expectedFileName.isNullOrBlank()) {
            return recentManualArchives
                .firstOrNull { it.fileName.equals(expectedFileName, ignoreCase = true) }
                ?.let {
                    PendingManualBackupRecoveryResult(
                        archivePath = it.termuxReadablePath,
                        fileName = it.fileName,
                    )
                }
        }

        return recentManualArchives.singleOrNull()?.let {
            PendingManualBackupRecoveryResult(
                archivePath = it.termuxReadablePath,
                fileName = it.fileName,
            )
        }
    }

    private fun isManualBackupArchive(path: String): Boolean {
        val normalized = path.trim().replace('\\', '/')
        return normalized.contains("/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}/", ignoreCase = true)
    }
}
