package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernProfileRemovalGuardTest {
    @Test
    fun `main profile removal is blocked`() {
        val decision = TavernProfileRemovalGuard.evaluate(
            config = TavernPathConfig(),
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileRemovalDecision.Blocked)
        assertEquals(
            "主实例暂时不能删除。先切换到要删的分身实例，再删除它。",
            (decision as TavernProfileRemovalDecision.Blocked).message,
        )
    }

    @Test
    fun `running clone removal is blocked`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val decision = TavernProfileRemovalGuard.evaluate(
            config = config,
            tavernRunning = true,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileRemovalDecision.Blocked)
        assertEquals(
            "当前实例还在运行。请先停止这个实例，再删除它，避免删掉配置后找不回它的运行状态。",
            (decision as TavernProfileRemovalDecision.Blocked).message,
        )
    }

    @Test
    fun `stopped clone removal returns confirmation details`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "/data/data/com.termux/files/home/custom-clone",
                port = 9002,
            )

        val decision = TavernProfileRemovalGuard.evaluate(
            config = config,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileRemovalDecision.Confirm)
        val confirmation = (decision as TavernProfileRemovalDecision.Confirm).confirmation
        assertEquals("profile-2", confirmation.profileId)
        assertEquals("分身实例", confirmation.profileName)
        assertEquals("~/custom-clone", confirmation.profilePath)
        assertEquals(9002, confirmation.profilePort)
        assertEquals("主实例", confirmation.nextProfileName)
        assertEquals(1, confirmation.remainingProfileCount)
        assertFalse(confirmation.deletesProfileDirectory)
    }

    @Test
    fun `launcher managed clone removal marks directory deletion`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val decision = TavernProfileRemovalGuard.evaluate(
            config = config,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileRemovalDecision.Confirm)
        val confirmation = (decision as TavernProfileRemovalDecision.Confirm).confirmation
        assertTrue(confirmation.deletesProfileDirectory)
        assertEquals("~/LukoaLauncher/SillyTavern2", confirmation.deletedDirectoryPath)
    }
}
