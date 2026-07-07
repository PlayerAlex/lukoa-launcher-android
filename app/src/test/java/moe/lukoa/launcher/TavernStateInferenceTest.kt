package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernStateInferenceTest {
    @Test
    fun `starting status with running true stays in starting state`() {
        val output = """
            {"status": "starting", "running": true, "exitCode": 75}
            SillyTavern launch command accepted, but HTTP endpoint is not ready
        """.trimIndent()

        assertNull(inferTavernRunning(output))
        assertTrue(inferTavernStarting(output))
    }

    @Test
    fun `foreground session exit clears starting state`() {
        val output = """
            {"status": "starting", "running": true, "exitCode": 0}
            SillyTavern is starting in Termux foreground log mode
            {"status": "stopped", "running": false, "exitCode": 130}
            SillyTavern foreground session exited
        """.trimIndent()

        assertFalse(inferTavernRunning(output) == true)
        assertTrue(inferTavernRunning(output) == false)
    }

    @Test
    fun `unreachable process still counts as running for duplicate start protection`() {
        val output = """
            {"status": "unreachable", "running": true, "exitCode": 75}
            SillyTavern process already exists, but HTTP endpoint is not responding
        """.trimIndent()

        assertTrue(inferTavernRunning(output) == true)
    }

    @Test
    fun `custom port conflict still counts as running`() {
        val output = """
            Error: Address 127.0.0.1:8001 is already in use
        """.trimIndent()

        assertTrue(inferTavernRunning(output) == true)
    }
}
