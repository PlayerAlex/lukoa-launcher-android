package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernForceCleanupSupportTest {
    @Test
    fun `stop failure suggests force releasing the port`() {
        val suggestion = TavernForceCleanupSupport.detect(
            doctorReport = null,
            status = "停止酒馆失败。",
            summary = "普通停止后酒馆还没完全停下",
        )

        assertNotNull(suggestion)
        assertEquals(TavernForceCleanupKind.StopDidNotExit, suggestion?.kind)
        assertEquals("强制释放端口", suggestion?.buttonLabel)
    }

    @Test
    fun `detected stale process suggests force cleanup`() {
        val suggestion = TavernForceCleanupSupport.detect(
            doctorReport = healthyDoctorReport().copy(
                processDetected = true,
                httpOk = false,
            ),
            status = "启动前发现问题",
            summary = "已经检测到酒馆进程，但网页当前没有正常响应，先处理现有进程更稳。",
        )

        assertNotNull(suggestion)
        assertEquals(TavernForceCleanupKind.ResidualProcess, suggestion?.kind)
        assertTrue(suggestion?.reasonDetail?.contains("旧进程") == true)
    }

    @Test
    fun `build confirmation keeps current profile details`() {
        val suggestion = TavernForceCleanupSupport.detect(
            doctorReport = healthyDoctorReport().copy(portConflict = true),
            status = "检测到酒馆端口被别的进程占用。",
            summary = "酒馆端口已被别的进程占用",
        )
        val confirmation = TavernForceCleanupSupport.buildConfirmation(
            profile = TavernProfileDefaults.profileForId("profile-2").copy(
                name = "分身二号",
                tavernDir = "~/LukoaLauncher/SillyTavern2",
                port = 8002,
            ),
            suggestion = requireNotNull(suggestion),
        )

        assertEquals("分身二号", confirmation.profileName)
        assertEquals("~/LukoaLauncher/SillyTavern2", confirmation.profilePath)
        assertEquals(8002, confirmation.profilePort)
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
