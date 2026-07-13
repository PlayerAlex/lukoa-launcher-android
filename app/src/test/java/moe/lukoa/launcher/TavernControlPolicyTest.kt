package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernControlPolicyTest {
    @Test
    fun `stop remains available while tavern is starting`() {
        assertTrue(shouldOfferStopTavern(tavernRunning = false, tavernStarting = true))
    }

    @Test
    fun `stop is available while tavern is running`() {
        assertTrue(shouldOfferStopTavern(tavernRunning = true, tavernStarting = false))
    }

    @Test
    fun `start is offered only when tavern is inactive`() {
        assertFalse(shouldOfferStopTavern(tavernRunning = false, tavernStarting = false))
    }

    @Test
    fun `active start command can be interrupted once starting is detected`() {
        assertTrue(
            canInterruptActiveTavernStart(
                actionInProgress = true,
                tavernStarting = true,
                busyLabel = TAVERN_START_BUSY_LABEL,
            ),
        )
    }

    @Test
    fun `unrelated busy command cannot be interrupted as a start`() {
        assertFalse(
            canInterruptActiveTavernStart(
                actionInProgress = true,
                tavernStarting = true,
                busyLabel = "更新酒馆",
            ),
        )
    }
}
