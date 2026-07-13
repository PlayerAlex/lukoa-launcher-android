package moe.lukoa.launcher

object TermuxResultHistoryPolicy {
    const val MAX_HISTORY_RESULTS = 24
    const val MAX_HISTORY_TEXT_CHARACTERS = 16_000

    private const val MAX_STDOUT_CHARACTERS = 12_000
    private const val MAX_STDERR_CHARACTERS = 3_000
    private const val MAX_RAW_CHARACTERS = 1_000

    fun forHistory(result: TermuxCommandResult): TermuxCommandResult {
        return result.copy(
            stdout = result.stdout.takeLast(MAX_STDOUT_CHARACTERS),
            stderr = result.stderr.takeLast(MAX_STDERR_CHARACTERS),
            raw = result.raw.takeLast(MAX_RAW_CHARACTERS),
        )
    }

    fun textCharacterCount(result: TermuxCommandResult): Int {
        return result.stdout.length + result.stderr.length + result.raw.length
    }
}
