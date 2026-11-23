package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernStartReadinessPolicyTest {
    @Test
    fun `blocked cached preflight is never reused so fixes can retry immediately`() {
        val staleProcessReport = healthReport(
            checkedAtMillis = 10_000L,
            doctorReport = healthyDoctor().copy(processDetected = true, httpOk = false),
        )
        val alreadyRunningReport = healthReport(
            checkedAtMillis = 10_000L,
            doctorReport = healthyDoctor().copy(httpOk = true),
        )

        assertNull(
            TavernStartReadinessPolicy.reusableCachedPreflight(
                healthCheckReport = staleProcessReport,
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                termuxExternalAppsBlocked = false,
                tavernRunning = false,
                activeProfile = TavernProfileDefaults.profileForId("main"),
                lastLaunchReadinessSnapshotAtMillis = 0L,
                lastEnvironmentMutationAtMillis = 0L,
                nowMillis = 15_000L,
            ),
        )
        assertNull(
            TavernStartReadinessPolicy.reusableCachedPreflight(
                healthCheckReport = alreadyRunningReport,
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                termuxExternalAppsBlocked = false,
                tavernRunning = false,
                activeProfile = TavernProfileDefaults.profileForId("main"),
                lastLaunchReadinessSnapshotAtMillis = 0L,
                lastEnvironmentMutationAtMillis = 0L,
                nowMillis = 15_000L,
            ),
        )
    }

    @Test
    fun `healthy cached preflight is reused until mutated expired or profile changes`() {
        val report = healthReport(
            checkedAtMillis = 10_000L,
            doctorReport = healthyDoctor(),
        )
        val mainProfile = TavernProfileDefaults.profileForId("main")

        val reused = TavernStartReadinessPolicy.reusableCachedPreflight(
            healthCheckReport = report,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            tavernRunning = false,
            activeProfile = mainProfile,
            lastLaunchReadinessSnapshotAtMillis = 9_000L,
            lastEnvironmentMutationAtMillis = 9_000L,
            nowMillis = 20_000L,
        )
        assertNotNull(reused)
        assertTrue(reused?.ok == true)

        // Mutating the environment invalidates the cached preflight.
        assertNull(
            TavernStartReadinessPolicy.reusableCachedPreflight(
                healthCheckReport = report,
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                termuxExternalAppsBlocked = false,
                tavernRunning = false,
                activeProfile = mainProfile,
                lastLaunchReadinessSnapshotAtMillis = 9_000L,
                lastEnvironmentMutationAtMillis = 12_000L,
                nowMillis = 20_000L,
            ),
        )

        // Changing the active profile port invalidates the cached preflight.
        assertNull(
            TavernStartReadinessPolicy.reusableCachedPreflight(
                healthCheckReport = report,
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                termuxExternalAppsBlocked = false,
                tavernRunning = false,
                activeProfile = mainProfile.copy(port = 8005),
                lastLaunchReadinessSnapshotAtMillis = 9_000L,
                lastEnvironmentMutationAtMillis = 0L,
                nowMillis = 20_000L,
            ),
        )

        // Expiring past the cache window invalidates the cached preflight.
        assertNull(
            TavernStartReadinessPolicy.reusableCachedPreflight(
                healthCheckReport = report,
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                termuxExternalAppsBlocked = false,
                tavernRunning = false,
                activeProfile = mainProfile,
                lastLaunchReadinessSnapshotAtMillis = 0L,
                lastEnvironmentMutationAtMillis = 0L,
                nowMillis = 10_000L + TavernStartReadinessPolicy.START_PREFLIGHT_CACHE_WINDOW_MS + 1L,
            ),
        )
    }

    @Test
    fun `fast path start rejects reserved clone paths and post-snapshot mutations`() {
        val validVersion = TavernVersionInfo(hasData = true, packageVersion = "1.14.0")
        val mainProfile = TavernProfileDefaults.profileForId("main")
        val invalidClone = TavernProfileDefaults.profileForId("profile-2").copy(
            tavernDir = "~/LukoaLauncher/SillyTavern",
        )

        assertTrue(
            TavernStartReadinessPolicy.canFastPathStart(
                tavernRunning = false,
                tavernStarting = false,
                termuxKnownMissing = false,
                termuxPermissionBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = true,
                tavernVersionInfo = validVersion,
                activeProfile = mainProfile,
                lastLaunchReadinessSnapshotAtMillis = 10_000L,
                lastEnvironmentMutationAtMillis = 5_000L,
                nowMillis = 20_000L,
            ),
        )

        assertFalse(
            TavernStartReadinessPolicy.canFastPathStart(
                tavernRunning = false,
                tavernStarting = false,
                termuxKnownMissing = false,
                termuxPermissionBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = true,
                tavernVersionInfo = validVersion,
                activeProfile = invalidClone,
                lastLaunchReadinessSnapshotAtMillis = 10_000L,
                lastEnvironmentMutationAtMillis = 0L,
                nowMillis = 20_000L,
            ),
        )

        assertFalse(
            TavernStartReadinessPolicy.canFastPathStart(
                tavernRunning = false,
                tavernStarting = false,
                termuxKnownMissing = false,
                termuxPermissionBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = true,
                tavernVersionInfo = validVersion,
                activeProfile = mainProfile,
                lastLaunchReadinessSnapshotAtMillis = 10_000L,
                lastEnvironmentMutationAtMillis = 15_000L,
                nowMillis = 20_000L,
            ),
        )
    }

    private fun healthReport(
        checkedAtMillis: Long,
        doctorReport: TavernDoctorReport,
    ): LauncherHealthReport {
        return LauncherHealthReport(
            checkedAtMillis = checkedAtMillis,
            doctorReport = doctorReport,
        )
    }

    private fun healthyDoctor(): TavernDoctorReport {
        return TavernDoctorReport(
            tavernDir = "~/SillyTavern",
            port = 8000,
            gitAvailable = true,
            nodeAvailable = true,
            npmAvailable = true,
            curlAvailable = true,
            tavernDirExists = true,
            packageJsonExists = true,
            startEntryExists = true,
            gitRepo = true,
            pidFilePresent = false,
            processDetected = false,
            httpOk = false,
            portListening = false,
            portConflict = false,
            summaryLevel = TavernDoctorLevel.Healthy,
            summaryMessage = "环境正常",
        )
    }
}
