package moe.lukoa.launcher

object TermuxExecutionIdentity {
    const val FIRST_EXECUTION_ID = 1001

    fun nextExecutionId(previous: Int): Int {
        return if (previous < FIRST_EXECUTION_ID || previous == Int.MAX_VALUE) {
            FIRST_EXECUTION_ID
        } else {
            previous + 1
        }
    }

    fun fallbackExecutionId(nonce: String): Int {
        return nonce.hashCode().and(Int.MAX_VALUE).coerceAtLeast(1)
    }
}
