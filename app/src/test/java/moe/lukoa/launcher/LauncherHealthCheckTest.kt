package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherHealthCheckTest {
    @Test
    fun `health check points to termux background permission when it is the remaining blocker`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = false,
            allFilesAccessGranted = true,
            installUnknownAppsGranted = true,
            termuxStoragePermissionBlocked = false,
            tavernRunning = false,
            mirrorProbeStatus = TavernMirrorProbeStatus(),
            doctorReport = null,
        )

        assertEquals(
            LauncherHealthActionType.RequestTermuxBackgroundRunPermission,
            report.primaryAction?.type,
        )
        val permissionItem = report.items.first { it.title == "系统权限" }
        assertEquals(LauncherHealthLevel.Warning, permissionItem.level)
        assertTrue(permissionItem.detail.contains("Termux 后台常驻"))
        assertTrue(permissionItem.detail.contains("首次启动酒馆"))
    }
}
