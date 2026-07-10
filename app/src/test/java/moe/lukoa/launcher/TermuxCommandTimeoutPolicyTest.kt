package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TermuxCommandTimeoutPolicyTest {
    @Test
    fun `long task timeouts preserve existing wait windows`() {
        assertEquals(15 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-install"))
        assertEquals(10 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-update"))
        assertEquals(20 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("termux-bootstrap"))
    }

    @Test
    fun `short commands use centralized timeout values`() {
        assertEquals(24_000L, TermuxCommandTimeoutPolicy.timeoutMillis("status"))
        assertEquals(4_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-version-startup"))
        assertEquals(
            TermuxCommandTimeoutPolicy.DEFAULT_TIMEOUT_MS,
            TermuxCommandTimeoutPolicy.timeoutMillis("unknown"),
        )
    }
}
