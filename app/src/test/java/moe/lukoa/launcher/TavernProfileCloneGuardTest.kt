package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernProfileCloneGuardTest {
    @Test
    fun `stopped current instance can clone into next independent managed slot`() {
        val decision = TavernProfileCloneGuard.evaluate(
            config = TavernPathConfig(),
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileCloneDecision.Confirm)
        val confirmation = (decision as TavernProfileCloneDecision.Confirm).confirmation
        assertEquals("main", confirmation.sourceProfile.id)
        assertEquals("profile-2", confirmation.targetProfile.id)
        assertEquals("\$HOME/LukoaLauncher/SillyTavern2", confirmation.targetProfile.normalizedTavernDir)
        assertEquals(8001, confirmation.targetProfile.normalizedPort)
    }

    @Test
    fun `running starting or busy instance blocks clone`() {
        listOf(
            Triple(true, false, false),
            Triple(false, true, false),
            Triple(false, false, true),
        ).forEach { (running, starting, locked) ->
            val decision = TavernProfileCloneGuard.evaluate(
                config = TavernPathConfig(),
                tavernRunning = running,
                tavernStarting = starting,
                actionsLocked = locked,
            )
            assertTrue(decision is TavernProfileCloneDecision.Blocked)
        }
    }

    @Test
    fun `suggested slot skips path and port already reserved by custom profile`() {
        val occupied = TavernProfile(
            id = "custom-slot",
            name = "自定义",
            tavernDir = "\$HOME/LukoaLauncher/profile-2/SillyTavern",
            port = 8001,
        )
        val config = TavernPathConfig(profiles = listOf(TavernProfile(), occupied))

        val suggested = TavernProfileDefaults.suggestedClone(config.availableProfiles)

        assertEquals("profile-3", suggested.id)
        assertEquals(8002, suggested.port)
    }
}
