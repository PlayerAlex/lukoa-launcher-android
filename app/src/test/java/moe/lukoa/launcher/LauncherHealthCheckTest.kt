package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherHealthCheckTest {
    @Test
    fun `health check never reports normal while results remain unknown`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = true,
            termuxStoragePermissionBlocked = false,
            tavernRunning = false,
            mirrorProbeStatus = TavernMirrorProbeStatus(),
            doctorReport = null,
        )

        assertTrue(report.unknownCount > 0)
        assertEquals("体检结果未完整读到", report.summaryTitle)
        assertTrue(report.summaryDetail.contains("不能判断环境是否正常"))
    }

    @Test
    fun `partial doctor output stays unknown instead of becoming healthy`() {
        val healthyProbeItem = MirrorProbeItemStatus(
            level = MirrorProbeLevel.Healthy,
            message = "正常",
        )
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = true,
            termuxStoragePermissionBlocked = false,
            tavernRunning = false,
            mirrorProbeStatus = TavernMirrorProbeStatus(
                checkedAtMillis = 1L,
                repoStatus = healthyProbeItem,
                versionStatus = healthyProbeItem,
                npmStatus = healthyProbeItem,
            ),
            doctorReport = TavernDoctorReport(
                tavernDir = "~/SillyTavern",
                termuxRepoLabel = "官方源",
                termuxRepoUri = "https://packages.termux.dev/apt/termux-main",
                summaryLevel = TavernDoctorLevel.Unknown,
                summaryMessage = "只返回了部分字段",
            ),
        )

        assertEquals("体检结果未完整读到", report.summaryTitle)
        assertTrue(report.unknownCount >= 3)
        assertEquals(
            LauncherHealthLevel.Unknown,
            report.items.first { it.title == "运行与端口" }.level,
        )
    }

    @Test
    fun `health check points to termux background permission when it is the remaining blocker`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = false,
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

    @Test
    fun `health check does not skip port conflict to later permission actions`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = false,
            termuxBackgroundRunPermissionGranted = false,
            termuxStoragePermissionBlocked = false,
            tavernRunning = false,
            mirrorProbeStatus = TavernMirrorProbeStatus(),
            doctorReport = healthyDoctorReport().copy(
                portConflict = true,
                portListening = true,
                processDetected = false,
                httpOk = false,
            ),
        )

        assertEquals("先处理端口占用", report.summaryTitle)
        assertNull(report.primaryAction)
        val portItem = report.items.first { it.title == "运行与端口" }
        assertEquals(LauncherHealthLevel.Error, portItem.level)
        assertTrue(portItem.detail.contains("重启 Termux/手机"))
    }

    @Test
    fun `health check still avoids stop action when stale running flag meets port conflict`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = true,
            termuxStoragePermissionBlocked = false,
            tavernRunning = true,
            mirrorProbeStatus = TavernMirrorProbeStatus(),
            doctorReport = healthyDoctorReport().copy(
                portConflict = true,
                portListening = true,
                processDetected = false,
                httpOk = false,
            ),
        )

        assertNull(report.primaryAction)
    }

    @Test
    fun `complete tavern without git is a warning about update and rollback not an error`() {
        val report = LauncherHealthCheck.build(
            checkedAtMillis = 1L,
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsBlocked = false,
            backgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = true,
            termuxStoragePermissionBlocked = false,
            tavernRunning = false,
            mirrorProbeStatus = TavernMirrorProbeStatus(),
            doctorReport = healthyDoctorReport().copy(gitRepo = false),
        )

        val pathItem = report.items.first { it.title == "酒馆路径" }
        assertEquals(LauncherHealthLevel.Warning, pathItem.level)
        assertTrue(pathItem.detail.contains("可以正常启动"))
        assertTrue(pathItem.detail.contains("更新和回退"))
        assertEquals(0, report.items.count { it.level == LauncherHealthLevel.Error })
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
