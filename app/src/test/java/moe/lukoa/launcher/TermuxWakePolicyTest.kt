package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxWakePolicyTest {
    @Test
    fun `should not wake on initial resume`() {
        assertFalse(
            shouldWake(
                hasCompletedInitialResume = false,
            ),
        )
    }

    @Test
    fun `should wake on foreground resume when termux background is not granted`() {
        assertTrue(shouldWake())
    }

    @Test
    fun `should not wake when termux background is already granted`() {
        assertFalse(
            shouldWake(
                termuxBackgroundRunPermissionGranted = true,
            ),
        )
    }

    @Test
    fun `should not wake when termux is missing or run command is blocked`() {
        assertFalse(shouldWake(termuxInstalled = false))
        assertFalse(shouldWake(runCommandPermissionGranted = false))
    }

    @Test
    fun `should not wake during active wake flow`() {
        assertFalse(shouldWake(wakeInProgress = true))
        assertFalse(shouldWake(wakeScheduled = true))
    }

    @Test
    fun `should not wake inside cooldown windows`() {
        assertFalse(
            shouldWake(
                nowMillis = 20_000L,
                lastWakeAtMillis = 19_000L,
            ),
        )
        assertFalse(
            shouldWake(
                nowMillis = 60_000L,
                lastResumeWakeAtMillis = 30_000L,
            ),
        )
    }

    private fun shouldWake(
        hasCompletedInitialResume: Boolean = true,
        termuxInstalled: Boolean = true,
        runCommandPermissionGranted: Boolean = true,
        termuxBackgroundRunPermissionGranted: Boolean = false,
        wakeInProgress: Boolean = false,
        wakeScheduled: Boolean = false,
        nowMillis: Long = 60_000L,
        lastWakeAtMillis: Long = 0L,
        lastResumeWakeAtMillis: Long = 0L,
    ): Boolean {
        return TermuxWakePolicy.shouldWakeOnForegroundResume(
            hasCompletedInitialResume = hasCompletedInitialResume,
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            wakeInProgress = wakeInProgress,
            wakeScheduled = wakeScheduled,
            nowMillis = nowMillis,
            lastWakeAtMillis = lastWakeAtMillis,
            lastResumeWakeAtMillis = lastResumeWakeAtMillis,
        )
    }
}
