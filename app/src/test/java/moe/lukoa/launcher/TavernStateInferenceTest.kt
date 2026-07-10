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
    fun `log snapshot candidate process cannot promote stopped state to running`() {
        val output = """
            {"status": "log", "running": true, "exitCode": 75}
            SillyTavern live log synced; process exists but HTTP endpoint is not responding
        """.trimIndent()

        assertTrue(isTavernLogStatusReport(output))
        assertTrue(inferTavernRunning(output) == true)
        assertNull(inferTavernRunningFromLogSnapshot(output))
        assertTrue(inferTavernStartingFromLogSnapshot(output))
    }

    @Test
    fun `runtime log json cannot override launcher status envelope`() {
        val output = """
            {"status": "log", "running": true, "exitCode": 75, "message": "HTTP endpoint is not responding"}

            ==== SillyTavern live log ====
            {"status": "stopped", "running": false, "message": "user supplied json"}
            ==== end SillyTavern live log ====
        """.trimIndent()

        assertTrue(isTavernLogStatusReport(output))
        assertNull(inferTavernRunningFromLogSnapshot(output))
        assertTrue(inferTavernStartingFromLogSnapshot(output))
    }

    @Test
    fun `healthy log snapshot does not masquerade as starting`() {
        val output = """
            {"status": "log", "running": true, "exitCode": 0}
            SillyTavern live log synced
        """.trimIndent()

        assertFalse(inferTavernStartingFromLogSnapshot(output))
    }

    @Test
    fun `log snapshot can still confirm stopped state`() {
        val output = """
            {"status": "log", "running": false, "exitCode": 0}
            SillyTavern live log synced; process is not running
        """.trimIndent()

        assertTrue(inferTavernRunningFromLogSnapshot(output) == false)
        assertFalse(inferTavernStartingFromLogSnapshot(output))
    }

    @Test
    fun `custom port conflict clears running state and reports conflict`() {
        val output = """
            Error: Address 127.0.0.1:8001 is already in use
        """.trimIndent()

        assertTrue(inferTavernPortConflict(output))
        assertTrue(inferTavernRunning(output) == false)
    }
}
