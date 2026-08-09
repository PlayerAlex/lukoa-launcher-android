package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveLogRefreshPolicyTest {
    @Test
    fun `periodic refresh keeps the regular bounded window`() {
        assertEquals(
            65_536,
            LiveLogRefreshPolicy.maxBytes(LiveLogRefreshReason.Periodic),
        )
    }

    @Test
    fun `foreground resume gets one larger catch up window`() {
        assertEquals(
            262_144,
            LiveLogRefreshPolicy.maxBytes(LiveLogRefreshReason.ForegroundResume),
        )
    }
}
