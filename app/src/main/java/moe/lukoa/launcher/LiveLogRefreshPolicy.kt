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

    const val BACKLOG_DELAY_MS = 150L
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
