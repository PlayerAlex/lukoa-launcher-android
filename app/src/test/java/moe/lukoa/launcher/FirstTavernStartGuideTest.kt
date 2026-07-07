package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstTavernStartGuideTest {
    @Test
    fun `resolve returns iqoo guide when model contains iqoo`() {
        val guide = FirstTavernStartGuideResolver.resolve(
            brand = "vivo",
            manufacturer = "vivo",
            model = "iQOO Neo9 Pro",
        )

        assertEquals(FirstTavernStartGuideKind.IQooBackgroundPermission, guide.kind)
    }

    @Test
    fun `resolve returns small window guide for non iqoo devices`() {
        val guide = FirstTavernStartGuideResolver.resolve(
            brand = "Xiaomi",
            manufacturer = "Xiaomi",
            model = "23127PN0CC",
        )

        assertEquals(FirstTavernStartGuideKind.KeepTermuxInSmallWindow, guide.kind)
    }

    @Test
    fun `should show for first start before successful launch history`() {
        assertTrue(
            FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = false,
                tavernInstallDetected = true,
                tavernRunning = false,
                termuxLog = "npm install finished",
                appLog = "酒馆已安装",
            ),
        )
    }

    @Test
    fun `should skip iqoo background guide when termux background is already granted`() {
        assertFalse(
            FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = false,
                tavernInstallDetected = true,
                tavernRunning = false,
                termuxLog = "",
                appLog = "",
                guideKind = FirstTavernStartGuideKind.IQooBackgroundPermission,
                termuxBackgroundRunPermissionGranted = true,
            ),
        )
    }

    @Test
    fun `should not show after successful launch history exists`() {
        assertFalse(
            FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = false,
                tavernInstallDetected = true,
                tavernRunning = false,
                termuxLog = "SillyTavern is listening on IPv4: 127.0.0.1:8000",
                appLog = "",
            ),
        )
    }

    @Test
    fun `should not show after app has summarized a successful launch`() {
        assertFalse(
            FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = false,
                tavernInstallDetected = true,
                tavernRunning = false,
                termuxLog = "",
                appLog = "检测到酒馆正在运行。",
            ),
        )
    }

    @Test
    fun `should not show after already running endpoint status`() {
        assertFalse(
            FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = false,
                tavernInstallDetected = true,
                tavernRunning = false,
                termuxLog = "SillyTavern is already running and HTTP endpoint is responding",
                appLog = "",
            ),
        )
    }
}
