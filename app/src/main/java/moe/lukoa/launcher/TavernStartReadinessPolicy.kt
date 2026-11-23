package moe.lukoa.launcher

object TavernStartReadinessPolicy {
    const val START_FAST_PATH_WINDOW_MS = 60_000L
    const val START_PREFLIGHT_CACHE_WINDOW_MS = 120_000L
    const val FOREGROUND_START_HINT_MS = 45_000L
    const val FOREGROUND_START_TIMEOUT_MS = 180_000L

    /**
     * Reuses a recent doctor preflight only when it passed cleanly and nothing has invalidated it
     * since. A blocked/failed preflight is never cached so fixing the issue (stopping tavern,
     * force-cleaning a stale process, preparing Termux, or closing a conflicting app) can retry
     * immediately without waiting out the cache window.
     */
    fun reusableCachedPreflight(
        healthCheckReport: LauncherHealthReport?,
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsBlocked: Boolean,
        tavernRunning: Boolean,
        activeProfile: TavernProfile,
        lastLaunchReadinessSnapshotAtMillis: Long,
        lastEnvironmentMutationAtMillis: Long,
        nowMillis: Long,
    ): TavernStartPreflightResult? {
        val report = healthCheckReport ?: return null
        val doctorReport = report.doctorReport ?: return null
        if (report.checkedAtMillis <= 0L) return null
        if (nowMillis - report.checkedAtMillis > START_PREFLIGHT_CACHE_WINDOW_MS) return null
        if (lastLaunchReadinessSnapshotAtMillis > report.checkedAtMillis) return null
        if (lastEnvironmentMutationAtMillis > report.checkedAtMillis) return null
        if (doctorReport.port != activeProfile.normalizedPort) return null
        if (
            doctorReport.tavernDir.isNotBlank() &&
            !TavernComparablePath.same(doctorReport.tavernDir, activeProfile.normalizedTavernDir)
        ) {
            return null
        }
        if (tavernRunning && doctorReport.httpOk == false) return null

        val evaluated = TavernStartPreflight.evaluate(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
            doctorReport = doctorReport,
            activeProfile = activeProfile,
        )
        return evaluated.takeIf { it.ok }
    }

    fun canFastPathStart(
        tavernRunning: Boolean,
        tavernStarting: Boolean,
        termuxKnownMissing: Boolean,
        termuxPermissionBlocked: Boolean,
        termuxSetupRecommended: Boolean,
        tavernInstallDetected: Boolean?,
        tavernVersionInfo: TavernVersionInfo,
        activeProfile: TavernProfile,
        lastLaunchReadinessSnapshotAtMillis: Long,
        lastEnvironmentMutationAtMillis: Long,
        nowMillis: Long,
    ): Boolean {
        if (tavernRunning || tavernStarting) return false
        if (termuxKnownMissing || termuxPermissionBlocked) return false
        if (termuxSetupRecommended) return false
        if (tavernInstallDetected != true) return false
        if (!tavernVersionInfo.hasData || tavernVersionInfo.notInstalled) return false
        if (TavernProfileReservedPathPolicy.startBlockedMessage(activeProfile) != null) return false
        if (lastLaunchReadinessSnapshotAtMillis <= 0L) return false
        if (lastEnvironmentMutationAtMillis > lastLaunchReadinessSnapshotAtMillis) return false
        return nowMillis - lastLaunchReadinessSnapshotAtMillis <= START_FAST_PATH_WINDOW_MS
    }
}
