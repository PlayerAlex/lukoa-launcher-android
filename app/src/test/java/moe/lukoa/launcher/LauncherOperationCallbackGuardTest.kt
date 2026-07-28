package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherOperationCallbackGuardTest {
    @Test
    fun `current callback can finish its own active operation`() {
        assertTrue(
            LauncherOperationCallbackGuard.isCurrent(
                expectedToken = 7,
                currentToken = 7,
                operationActive = true,
            ),
        )
    }

    @Test
    fun `late callback cannot touch a newer operation`() {
        assertFalse(
            LauncherOperationCallbackGuard.isCurrent(
                expectedToken = 7,
                currentToken = 8,
                operationActive = true,
            ),
        )
    }

    @Test
    fun `callback cannot finish an operation that already ended`() {
        assertFalse(
            LauncherOperationCallbackGuard.isCurrent(
                expectedToken = 7,
                currentToken = 7,
                operationActive = false,
            ),
        )
    }
}
