package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxExecutionIdentityTest {
    @Test
    fun `execution id continues across normal values`() {
        assertEquals(1002, TermuxExecutionIdentity.nextExecutionId(1001))
        assertEquals(50_001, TermuxExecutionIdentity.nextExecutionId(50_000))
    }

    @Test
    fun `execution id repairs invalid or exhausted values`() {
        assertEquals(
            TermuxExecutionIdentity.FIRST_EXECUTION_ID,
            TermuxExecutionIdentity.nextExecutionId(0),
        )
        assertEquals(
            TermuxExecutionIdentity.FIRST_EXECUTION_ID,
            TermuxExecutionIdentity.nextExecutionId(Int.MAX_VALUE),
        )
    }

    @Test
    fun `nonce fallback always produces positive execution id`() {
        assertTrue(TermuxExecutionIdentity.fallbackExecutionId("nonce-123") > 0)
        assertTrue(TermuxExecutionIdentity.fallbackExecutionId("") > 0)
    }
}
