package moe.lukoa.launcher

enum class LiveLogRefreshReason {
    Periodic,
    ForegroundResume,
}

object LiveLogRefreshPolicy {
    private const val PERIODIC_MAX_BYTES = 65_536
    // Termux truncates the stdout it returns to the app at about 100KB. The script pages through
    // any backlog, so a larger single request would only lose the truncated part.
    private const val FOREGROUND_RESUME_MAX_BYTES = 65_536

    // While the app stays open every byte is paged through. After coming back from the background
    // only the newest window is fetched: the panel shows the latest lines anyway, and paging a 2MB
    // backlog through it re-laid out the whole panel on every page, freezing the app on resume.
    private const val PERIODIC_CATCHUP_BYTES = 2_097_152
    private const val FOREGROUND_RESUME_CATCHUP_BYTES = 65_536

    const val BACKLOG_DELAY_MS = 400L
    const val ACTIVE_DELAY_MS = 900L
    private const val IDLE_DELAYS_MS = 1_500L
    private const val LONG_IDLE_DELAY_MS = 3_000L
    private const val LONG_IDLE_AFTER_POLLS = 4
    const val STOPPED_DELAY_MS = 5_000L

    private val pendingPattern = Regex("""(?m)^liveLog\.pending=(\d+)""")
    private val bytesPattern = Regex("""(?m)^liveLog\.bytes=(\d+)""")

    fun maxBytes(reason: LiveLogRefreshReason): Int {
        return when (reason) {
            LiveLogRefreshReason.Periodic -> PERIODIC_MAX_BYTES
            LiveLogRefreshReason.ForegroundResume -> FOREGROUND_RESUME_MAX_BYTES
        }
    }

    fun catchupBytes(reason: LiveLogRefreshReason): Int {
        return when (reason) {
            LiveLogRefreshReason.Periodic -> PERIODIC_CATCHUP_BYTES
            LiveLogRefreshReason.ForegroundResume -> FOREGROUND_RESUME_CATCHUP_BYTES
        }
    }

    fun pendingBytes(output: String): Long = pendingPattern.find(output)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    fun sentBytes(output: String): Long = bytesPattern.find(output)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    // Every poll starts a shell in Termux, so only poll quickly while there is something to read:
    // drain a backlog right away, keep pace while the log is moving, and slow down when it is quiet.
    fun nextDelayMillis(
        pendingBytes: Long,
        sentBytes: Long,
        idlePolls: Int,
        tavernRunning: Boolean,
    ): Long = when {
        pendingBytes > 0 -> BACKLOG_DELAY_MS
        sentBytes > 0 -> ACTIVE_DELAY_MS
        !tavernRunning -> STOPPED_DELAY_MS
        idlePolls >= LONG_IDLE_AFTER_POLLS -> LONG_IDLE_DELAY_MS
        else -> IDLE_DELAYS_MS
    }
}
