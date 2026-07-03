package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusSummaryTest {
    @Test
    fun `settings notice highlights pending background permissions`() {
        val notice = PermissionStatusSummary.settingsNotice(
            termuxInstalled = true,
            runCommandPermissionGranted = true,
            termuxExternalAppsReady = true,
            launcherBackgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = false,
            allFilesAccessGranted = true,
            installUnknownAppsGranted = true,
            termuxStoragePermissionBlocked = false,
        )

        assertEquals("后台常驻还没完全放行", notice.title)
        assertTrue(notice.detail.contains("Termux 后台常驻"))
        assertEquals(PermissionNoticeTone.Warning, notice.tone)
    }

    @Test
    fun `launch reminder prefers background residency warning`() {
        val notice = PermissionStatusSummary.launchReminder(
            termuxInstalled = true,
            launcherBackgroundRunPermissionGranted = false,
            termuxBackgroundRunPermissionGranted = false,
            termuxStoragePermissionBlocked = true,
        )

        requireNotNull(notice)
        assertEquals("后台常驻还没完全放行", notice.title)
        assertTrue(notice.detail.contains("露科亚启动器和 Termux"))
    }

    @Test
    fun `launch reminder is null when no reminder is needed`() {
        val notice = PermissionStatusSummary.launchReminder(
            termuxInstalled = true,
            launcherBackgroundRunPermissionGranted = true,
            termuxBackgroundRunPermissionGranted = true,
            termuxStoragePermissionBlocked = false,
        )

        assertNull(notice)
    }
}
