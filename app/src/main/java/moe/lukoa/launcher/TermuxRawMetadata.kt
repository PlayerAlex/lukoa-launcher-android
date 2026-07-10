package moe.lukoa.launcher

object TermuxRawMetadata {
    fun build(entries: Map<String, Any?>): String {
        return entries.mapNotNull { (key, value) -> formatEntry(key, value) }
            .joinToString("\n")
    }

    private fun formatEntry(key: String, value: Any?): String? {
        if (value is Map<*, *>) {
            val nested = value.entries.mapNotNull { entry ->
                val nestedKey = entry.key?.toString() ?: return@mapNotNull null
                formatEntry(nestedKey, entry.value)
            }
            if (nested.isEmpty()) return null
            return "$key={${nested.joinToString(", ")}}"
        }
        if (key in KNOWN_KEYS) return null
        return "$key=$value"
    }

    private val KNOWN_KEYS = setOf(
        TermuxCommandRunner.EXTRA_EXECUTION_ID,
        TermuxCommandRunner.EXTRA_LUKOA_COMMAND,
        TermuxCommandRunner.EXTRA_LUKOA_NONCE,
        "com.termux.service.extra.RUN_COMMAND_RESULT_STDOUT",
        "stdout",
        "com.termux.service.extra.RUN_COMMAND_RESULT_STDERR",
        "stderr",
        "com.termux.service.extra.RUN_COMMAND_RESULT_EXIT_CODE",
        "exitCode",
        "com.termux.service.extra.RUN_COMMAND_RESULT_ERR",
        "err",
        "com.termux.service.extra.RUN_COMMAND_RESULT_ERRMSG",
        "errmsg",
        "com.termux.service.extra.RUN_COMMAND_RESULT_STDOUT_ORIGINAL_LENGTH",
        "stdout_original_length",
        "com.termux.service.extra.RUN_COMMAND_RESULT_STDERR_ORIGINAL_LENGTH",
        "stderr_original_length",
    )
}
