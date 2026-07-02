package moe.lukoa.launcher

import android.content.Intent
import android.os.Bundle

object TermuxResultParser {
    fun parse(intent: Intent?): TermuxCommandResult {
        val extras = intent?.extras
        val bundle = extras?.getBundle(EXTRA_PLUGIN_RESULT_BUNDLE)
            ?: extras?.getBundle(EXTRA_RESULT)
            ?: extras?.keySet()
                ?.asSequence()
                ?.mapNotNull { key -> extras.get(key) as? Bundle }
                ?.firstOrNull { candidate ->
                    candidate.containsAnyKey(KEY_STDOUT_KEYS) ||
                        candidate.containsAnyKey(KEY_STDERR_KEYS) ||
                        candidate.containsAnyKey(KEY_EXIT_CODE_KEYS)
                }

        val source = bundle ?: extras
        return TermuxCommandResult(
            executionId = extras?.getAnyInt(TermuxCommandRunner.EXTRA_EXECUTION_ID) ?: 0,
            command = extras?.getAnyString(TermuxCommandRunner.EXTRA_LUKOA_COMMAND).orEmpty(),
            nonce = extras?.getAnyString(TermuxCommandRunner.EXTRA_LUKOA_NONCE),
            hasResultBundle = bundle != null,
            timeMillis = System.currentTimeMillis(),
            stdout = source?.getAnyString(KEY_STDOUT_KEYS).orEmpty(),
            stderr = source?.getAnyString(KEY_STDERR_KEYS).orEmpty(),
            exitCode = source?.getAnyInt(KEY_EXIT_CODE_KEYS),
            errCode = source?.getAnyInt(KEY_ERR_KEYS),
            errMessage = source?.getAnyString(KEY_ERRMSG_KEYS).orEmpty(),
            stdoutOriginalLength = source?.getAnyString(KEY_STDOUT_ORIGINAL_LENGTH_KEYS).orEmpty(),
            stderrOriginalLength = source?.getAnyString(KEY_STDERR_ORIGINAL_LENGTH_KEYS).orEmpty(),
            raw = buildRawDump(extras),
        )
    }

    private fun Bundle.containsAnyKey(keys: List<String>): Boolean {
        return keys.any { containsKey(it) }
    }

    private fun Bundle.getAnyString(keys: List<String>): String? {
        return keys.firstNotNullOfOrNull { getAnyString(it) }
    }

    private fun Bundle.getAnyInt(keys: List<String>): Int? {
        return keys.firstNotNullOfOrNull { getAnyInt(it) }
    }

    private fun Bundle.getAnyString(key: String): String? {
        val value = get(key) ?: return null
        return when (value) {
            is String -> value
            is CharSequence -> value.toString()
            else -> value.toString()
        }
    }

    private fun Bundle.getAnyInt(key: String): Int? {
        val value = get(key) ?: return null
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun buildRawDump(extras: Bundle?): String {
        if (extras == null) return ""
        return extras.keySet().joinToString(separator = "\n") { key ->
            val value = extras.get(key)
            if (value is Bundle) {
                val bundleDump = value.keySet().joinToString(prefix = "{", postfix = "}") { innerKey ->
                    "$innerKey=${value.get(innerKey)}"
                }
                "$key=$bundleDump"
            } else {
                "$key=$value"
            }
        }
    }

    private const val EXTRA_PLUGIN_RESULT_BUNDLE = "com.termux.service.extra.PLUGIN_RESULT_BUNDLE"
    private const val EXTRA_RESULT = "result"
    private val KEY_STDOUT_KEYS = listOf("com.termux.service.extra.RUN_COMMAND_RESULT_STDOUT", "stdout")
    private val KEY_STDERR_KEYS = listOf("com.termux.service.extra.RUN_COMMAND_RESULT_STDERR", "stderr")
    private val KEY_EXIT_CODE_KEYS = listOf("com.termux.service.extra.RUN_COMMAND_RESULT_EXIT_CODE", "exitCode")
    private val KEY_ERR_KEYS = listOf("com.termux.service.extra.RUN_COMMAND_RESULT_ERR", "err")
    private val KEY_ERRMSG_KEYS = listOf("com.termux.service.extra.RUN_COMMAND_RESULT_ERRMSG", "errmsg")
    private val KEY_STDOUT_ORIGINAL_LENGTH_KEYS =
        listOf("com.termux.service.extra.RUN_COMMAND_RESULT_STDOUT_ORIGINAL_LENGTH", "stdout_original_length")
    private val KEY_STDERR_ORIGINAL_LENGTH_KEYS =
        listOf("com.termux.service.extra.RUN_COMMAND_RESULT_STDERR_ORIGINAL_LENGTH", "stderr_original_length")
}
