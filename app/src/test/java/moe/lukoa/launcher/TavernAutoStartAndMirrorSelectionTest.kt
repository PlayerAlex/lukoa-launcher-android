package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TavernAutoStartAndMirrorSelectionTest {
    @Test
    fun `auto start waits while startup status check has not completed`() {
        val decision = TavernAutoStartPolicy.decide(
            enabledAtLaunch = true,
            enabledNow = true,
            alreadyHandled = false,
            startupRefreshScheduled = true,
            startupRefreshCompleted = false,
            startupRefreshInFlight = false,
            termuxReady = true,
            tavernInstallDetected = true,
            tavernRunning = false,
            tavernStarting = false,
            localHttpResponding = false,
            actionInProgress = false,
        )

        assertEquals(AutoStartDecision.Wait, decision)
    }

    @Test
    fun `auto start skips immediately when tavern is already running or local http responds`() {
        val fromFlag = TavernAutoStartPolicy.decide(
            enabledAtLaunch = true,
            enabledNow = true,
            alreadyHandled = false,
            startupRefreshScheduled = true,
            startupRefreshCompleted = false,
            startupRefreshInFlight = true,
            termuxReady = true,
            tavernInstallDetected = true,
            tavernRunning = true,
            tavernStarting = false,
            localHttpResponding = false,
            actionInProgress = false,
        )
        val fromLocalHttp = TavernAutoStartPolicy.decide(
            enabledAtLaunch = true,
            enabledNow = true,
            alreadyHandled = false,
            startupRefreshScheduled = true,
            startupRefreshCompleted = false,
            startupRefreshInFlight = true,
            termuxReady = true,
            tavernInstallDetected = true,
            tavernRunning = false,
            tavernStarting = false,
            localHttpResponding = true,
            actionInProgress = false,
        )

        assertEquals(AutoStartDecision.SkipAlreadyRunning, fromFlag)
        assertEquals(AutoStartDecision.SkipAlreadyRunning, fromLocalHttp)
    }

    @Test
    fun `auto start does not fire mid-session when toggled on inside settings`() {
        val decision = TavernAutoStartPolicy.decide(
            enabledAtLaunch = false,
            enabledNow = true,
            alreadyHandled = false,
            startupRefreshScheduled = true,
            startupRefreshCompleted = true,
            startupRefreshInFlight = false,
            termuxReady = true,
            tavernInstallDetected = true,
            tavernRunning = false,
            tavernStarting = false,
            localHttpResponding = false,
            actionInProgress = false,
        )

        assertEquals(AutoStartDecision.SkipDisabledOrBlocked, decision)
    }

    @Test
    fun `auto start fires once startup status confirms stopped and installed`() {
        val decision = TavernAutoStartPolicy.decide(
            enabledAtLaunch = true,
            enabledNow = true,
            alreadyHandled = false,
            startupRefreshScheduled = true,
            startupRefreshCompleted = true,
            startupRefreshInFlight = false,
            termuxReady = true,
            tavernInstallDetected = true,
            tavernRunning = false,
            tavernStarting = false,
            localHttpResponding = false,
            actionInProgress = false,
        )

        assertEquals(AutoStartDecision.StartTavern, decision)
    }

    @Test
    fun `mirror selection prefers fastest healthy proxy when official fails or is much slower`() {
        val pickedWhenOfficialFailed = TavernMirrorSelectionPolicy.pickBest(
            candidates = listOf(
                MirrorCandidateScore(TavernMirrorDefaults.OFFICIAL_REPO, true, MirrorProbeLevel.Failed, null),
                MirrorCandidateScore(TavernMirrorDefaults.GITHUB_PROXY_REPO, false, MirrorProbeLevel.Healthy, 260L),
                MirrorCandidateScore(TavernMirrorDefaults.GITHUB_PROXY_ALT_REPO, false, MirrorProbeLevel.Healthy, 140L),
            ),
            fallbackUrl = TavernMirrorDefaults.OFFICIAL_REPO,
        )
        assertEquals(TavernMirrorDefaults.GITHUB_PROXY_ALT_REPO, pickedWhenOfficialFailed?.url)

        val pickedWhenOfficialClose = TavernMirrorSelectionPolicy.pickBest(
            candidates = listOf(
                MirrorCandidateScore(TavernMirrorDefaults.OFFICIAL_REPO, true, MirrorProbeLevel.Healthy, 320L),
                MirrorCandidateScore(TavernMirrorDefaults.GITHUB_PROXY_REPO, false, MirrorProbeLevel.Healthy, 190L),
            ),
            fallbackUrl = TavernMirrorDefaults.OFFICIAL_REPO,
            officialGraceMs = 450L,
        )
        assertEquals(TavernMirrorDefaults.OFFICIAL_REPO, pickedWhenOfficialClose?.url)

        val allFailed = TavernMirrorSelectionPolicy.pickBest(
            candidates = listOf(
                MirrorCandidateScore(TavernMirrorDefaults.OFFICIAL_REPO, true, MirrorProbeLevel.Failed, null),
                MirrorCandidateScore(TavernMirrorDefaults.GITHUB_PROXY_REPO, false, MirrorProbeLevel.Failed, null),
            ),
            fallbackUrl = "https://other.example/repo.git",
        )
        assertNull(allFailed)
    }
}
