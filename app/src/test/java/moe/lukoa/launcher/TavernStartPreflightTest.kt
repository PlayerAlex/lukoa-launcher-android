package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernStartPreflightTest {
    @Test
    fun `port conflict blocks start without offering unsafe stop action`() {
        val result = TavernStartPreflight.evaluate(
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            doctorReport = healthyDoctorReport().copy(
                portConflict = true,
                portListening = true,
                processDetected = false,
                httpOk = false,
            ),
        )

        assertFalse(result.ok)
        assertEquals("启动前发现问题", result.title)
        assertTrue(result.summary.contains("端口"))
        assertTrue(result.details.any { it.contains("重启 Termux/手机") })
        assertNull(result.action)
    }

    @Test
    fun `clone using main managed directory is blocked before start`() {
        val result = TavernStartPreflight.evaluate(
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            doctorReport = healthyDoctorReport(),
            activeProfile = TavernProfileDefaults.profileForId("profile-2").copy(
                tavernDir = "~/LukoaLauncher/SillyTavern",
            ),
        )

        assertFalse(result.ok)
        assertTrue(result.summary.contains("路径"))
        assertTrue(result.details.any { it.contains("会一直保留给主实例") })
        assertEquals(TavernStartPreflightActionType.OpenPathSettings, result.action?.type)
    }

    private fun healthyDoctorReport(): TavernDoctorReport {
        return TavernDoctorReport(
            tavernDir = "~/SillyTavern",
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
