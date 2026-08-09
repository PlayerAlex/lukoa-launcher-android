package moe.lukoa.launcher

internal object AutoBackupSchedulePolicy {
    /**
     * Returns the stale scheduled work that should be cancelled before enqueuing [nextWorkName].
     * The work currently executing must be left alone, while a different queued work represents
     * a second schedule chain and must be replaced.
     */
    fun staleWorkToCancel(
        previousWorkName: String?,
        executingWorkName: String?,
        nextWorkName: String,
    ): String? {
        val previous = previousWorkName?.takeIf { it.isNotBlank() } ?: return null
        if (previous == nextWorkName || previous == executingWorkName) return null
        return previous
    }
}
