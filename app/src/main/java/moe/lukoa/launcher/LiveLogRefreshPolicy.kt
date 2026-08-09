package moe.lukoa.launcher

enum class LiveLogRefreshReason {
    Periodic,
    ForegroundResume,
}

object LiveLogRefreshPolicy {
    private const val PERIODIC_MAX_BYTES = 65_536
    private const val FOREGROUND_RESUME_MAX_BYTES = 262_144

    fun maxBytes(reason: LiveLogRefreshReason): Int {
        return when (reason) {
            LiveLogRefreshReason.Periodic -> PERIODIC_MAX_BYTES
            LiveLogRefreshReason.ForegroundResume -> FOREGROUND_RESUME_MAX_BYTES
        }
    }
}
