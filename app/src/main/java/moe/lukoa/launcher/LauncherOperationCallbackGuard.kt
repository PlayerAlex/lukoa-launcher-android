package moe.lukoa.launcher

object LauncherOperationCallbackGuard {
    fun isCurrent(
        expectedToken: Int,
        currentToken: Int,
        operationActive: Boolean,
    ): Boolean = operationActive && expectedToken == currentToken
}
