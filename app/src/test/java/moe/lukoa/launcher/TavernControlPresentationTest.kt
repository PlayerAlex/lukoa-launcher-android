package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TavernControlPresentationTest {
    @Test
    fun unknownInstallState_promptsDetectionInsteadOfInstallation() {
        assertEquals(
            "先检测酒馆",
            label(enabled = false, reason = "请先检测酒馆或安装。"),
        )
    }

    @Test
    fun knownMissingInstall_promptsInstallation() {
        assertEquals(
            "先安装酒馆",
            label(enabled = false, reason = "没检测到酒馆，请先安装。"),
        )
    }

    @Test
    fun runningState_alwaysOffersStop() {
        assertEquals(
            "停止酒馆",
            tavernPrimaryActionLabel(
                tavernRunning = true,
                tavernStarting = false,
                actionInProgress = false,
                busyLabel = null,
                primaryEnabled = true,
                primaryDisabledReason = null,
            ),
        )
    }

    @Test
    fun missingTermux_hasSpecificAction() {
        assertEquals(
            "先安装 Termux",
            label(enabled = false, reason = "请先安装并打开 Termux。"),
        )
    }

    private fun label(enabled: Boolean, reason: String?): String = tavernPrimaryActionLabel(
        tavernRunning = false,
        tavernStarting = false,
        actionInProgress = false,
        busyLabel = null,
        primaryEnabled = enabled,
        primaryDisabledReason = reason,
    )
}
