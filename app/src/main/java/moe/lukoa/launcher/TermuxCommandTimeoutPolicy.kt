package moe.lukoa.launcher

object TermuxCommandTimeoutPolicy {
    const val DEFAULT_TIMEOUT_MS = 12_000L
    const val LOG_REFRESH_TIMEOUT_MS = 3_000L
    const val OPERATION_LOCK_GRACE_MS = 5_000L

    private val explicitTimeouts = mapOf(
        "tavern-install" to 15 * 60_000L,
        "tavern-update" to 10 * 60_000L,
        "tavern-rollback" to 10 * 60_000L,
        "tavern-backup" to 10 * 60_000L,
        "tavern-backup-manual" to 10 * 60_000L,
        "tavern-backup-auto" to 10 * 60_000L,
        "tavern-backup-delete" to 10 * 60_000L,
        "tavern-backup-export" to 10 * 60_000L,
        "tavern-backup-export-to" to 10 * 60_000L,
        "tavern-backup-copy" to 10 * 60_000L,
        "tavern-backup-import" to 10 * 60_000L,
        "tavern-backup-rename" to 10 * 60_000L,
        "tavern-restore" to 10 * 60_000L,
        "tavern-migrate-dir" to 10 * 60_000L,
        "tavern-delete-managed-profile-dir" to 10 * 60_000L,
        "start" to 60_000L,
        "stop" to 24_000L,
        "tavern-force-cleanup" to 24_000L,
        "status" to 24_000L,
        "log" to 24_000L,
        "tavern-version" to 24_000L,
        "tavern-doctor" to 24_000L,
        "tavern-repair-dependencies" to 15 * 60_000L,
        "tavern-reset-theme" to 60_000L,
        "tavern-node-memory" to 60_000L,
        "tavern-upload-limit-status" to 60_000L,
        "tavern-upload-limit-set" to 60_000L,
        "tavern-backup-list" to 24_000L,
        "tavern-version-startup" to 4_000L,
        "tavern-official-versions" to 60_000L,
        "termux-storage-permission" to 60_000L,
        "termux-repo-status" to 24_000L,
        "termux-repo" to 2 * 60_000L,
        "termux-repo-custom" to 2 * 60_000L,
        "termux-bootstrap" to 20 * 60_000L,
    )

    fun timeoutMillis(command: String): Long {
        return explicitTimeouts[command] ?: DEFAULT_TIMEOUT_MS
    }

    fun hasExplicitTimeout(command: String): Boolean {
        return command in explicitTimeouts
    }

    fun operationLockMillis(command: String): Long {
        return timeoutMillis(command) + OPERATION_LOCK_GRACE_MS
    }

    fun chainedOperationLockMillis(vararg commands: String): Long {
        return commands.sumOf(::timeoutMillis) + OPERATION_LOCK_GRACE_MS
    }
}
