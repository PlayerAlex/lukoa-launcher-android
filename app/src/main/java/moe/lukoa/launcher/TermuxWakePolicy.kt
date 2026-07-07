package moe.lukoa.launcher

data class TermuxWakeResult(
    val ok: Boolean,
    val message: String,
)

object TermuxWakePolicy {
    const val DEFAULT_WAKE_COOLDOWN_MS = 12_000L
    const val DEFAULT_RESUME_WAKE_COOLDOWN_MS = 45_000L

    fun shouldWakeOnForegroundResume(
        hasCompletedInitialResume: Boolean,
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxBackgroundRunPermissionGranted: Boolean,
        wakeInProgress: Boolean,
        wakeScheduled: Boolean,
        nowMillis: Long,
        lastWakeAtMillis: Long,
        lastResumeWakeAtMillis: Long,
        wakeCooldownMs: Long = DEFAULT_WAKE_COOLDOWN_MS,
        resumeWakeCooldownMs: Long = DEFAULT_RESUME_WAKE_COOLDOWN_MS,
    ): Boolean {
        if (!hasCompletedInitialResume) return false
        if (!termuxInstalled) return false
        if (!runCommandPermissionGranted) return false
        if (termuxBackgroundRunPermissionGranted) return false
        if (wakeInProgress || wakeScheduled) return false
        if (nowMillis - lastWakeAtMillis < wakeCooldownMs) return false
        if (nowMillis - lastResumeWakeAtMillis < resumeWakeCooldownMs) return false
        return true
    }
}
