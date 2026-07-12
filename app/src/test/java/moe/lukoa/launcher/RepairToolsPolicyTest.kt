package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairToolsPolicyTest {
    @Test
    fun `repair commands have explicit operation timeouts`() {
        assertEquals(15 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-repair-dependencies"))
        assertEquals(60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-reset-theme"))
        assertEquals(60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-node-memory"))
        assertTrue(TermuxCommandTimeoutPolicy.hasExplicitTimeout("tavern-repair-dependencies"))
    }

    @Test
    fun `node memory command round trips through launcher codec`() {
        val encoded = LauncherCommandCodec.encode("tavern-node-memory", "4096")
        val decoded = LauncherCommandCodec.decode(encoded)
        assertEquals("tavern-node-memory", decoded.name)
        assertEquals("4096", decoded.argument)
    }
}
