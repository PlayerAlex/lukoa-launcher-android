package moe.lukoa.launcher

object TermuxCommandTimeoutPolicy {
    const val DEFAULT_TIMEOUT_MS = 12_000L
    const val LOG_REFRESH_TIMEOUT_MS = 3_000L

    fun timeoutMillis(command: String): Long {
        return when (command) {
            "tavern-install" -> 15 * 60_000L
            "tavern-update",
            "tavern-rollback",
            "tavern-backup-manual",
            "tavern-backup-auto",
            "tavern-backup-delete",
            "tavern-backup-export",
            "tavern-backup-export-to",
            "tavern-backup-copy",
            "tavern-backup-import",
            "tavern-backup-rename",
            "tavern-restore",
            "tavern-migrate-dir",
            "tavern-delete-managed-profile-dir",
            "tavern-backup" -> 10 * 60_000L
            "start" -> 60_000L
            "status",
            "log",
            "tavern-version",
            "tavern-doctor",
            "tavern-backup-list" -> 24_000L
            "tavern-version-startup" -> 4_000L
            "tavern-official-versions",
            "termux-storage-permission" -> 60_000L
            "termux-repo-status" -> 24_000L
            "termux-repo",
            "termux-repo-custom" -> 2 * 60_000L
            "termux-bootstrap" -> 20 * 60_000L
            else -> DEFAULT_TIMEOUT_MS
        }
    }
}
