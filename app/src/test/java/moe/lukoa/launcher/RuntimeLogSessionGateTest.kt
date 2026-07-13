package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeLogSessionGateTest {
    @Test
    fun `first offline snapshot is discarded after reopening cleared session`() {
        val gate = RuntimeLogSessionGate(discardFirstSnapshot = true)

        assertFalse(gate.shouldAppendSnapshot())
        assertTrue(gate.shouldAppendSnapshot())
    }

    @Test
    fun `manual clear rearms one snapshot discard`() {
        val gate = RuntimeLogSessionGate(discardFirstSnapshot = false)

        assertTrue(gate.shouldAppendSnapshot())
        gate.discardNextSnapshot()
        assertFalse(gate.shouldAppendSnapshot())
        assertTrue(gate.shouldAppendSnapshot())
    }
}