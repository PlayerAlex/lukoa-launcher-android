package moe.lukoa.launcher

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class TermuxCommandResult(
    val executionId: Int,
    val command: String,
    val nonce: String?,
    val hasResultBundle: Boolean,
    val timeMillis: Long,
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val errCode: Int?,
    val errMessage: String,
    val stdoutOriginalLength: String,
    val stderrOriginalLength: String,
    val raw: String,
) {
    val hasInternalError: Boolean
        get() = errCode != null && errCode != -1

    val isStructurallyValid: Boolean
        get() = hasResultBundle && exitCode != null

    val stableKey: String
        get() = listOf(
            executionId,
            command,
            timeMillis,
            exitCode ?: "no-exit",
            errCode ?: "no-err",
            stdoutOriginalLength,
            stderrOriginalLength,
            stdout.hashCode(),
            stderr.hashCode(),
            raw.hashCode(),
        ).joinToString("|")
}

object TermuxResultStore {
    private const val PREFS = "termux_results"
    private const val MAX_HISTORY = 24
    private const val KEY_EXECUTION_ID = "execution_id"
    private const val KEY_COMMAND = "command"
    private const val KEY_NONCE = "nonce"
    private const val KEY_HAS_RESULT_BUNDLE = "has_result_bundle"
    private const val KEY_TIME = "time"
    private const val KEY_STDOUT = "stdout"
    private const val KEY_STDERR = "stderr"
    private const val KEY_EXIT_CODE = "exit_code"
    private const val KEY_HAS_EXIT_CODE = "has_exit_code"
    private const val KEY_ERR_CODE = "err_code"
    private const val KEY_HAS_ERR_CODE = "has_err_code"
    private const val KEY_ERR_MESSAGE = "err_message"
    private const val KEY_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"
    private const val KEY_STDERR_ORIGINAL_LENGTH = "stderr_original_length"
    private const val KEY_RAW = "raw"
    private const val KEY_HISTORY = "history"

    fun save(context: Context, result: TermuxCommandResult) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val history = if (result.command == "log") {
            readHistory(prefs)
        } else {
            (listOf(result) + readHistory(prefs))
                .distinctBy { it.stableKey }
                .take(MAX_HISTORY)
        }
        prefs
            .edit()
            .putInt(KEY_EXECUTION_ID, result.executionId)
            .putString(KEY_COMMAND, result.command)
            .putString(KEY_NONCE, result.nonce)
            .putBoolean(KEY_HAS_RESULT_BUNDLE, result.hasResultBundle)
            .putLong(KEY_TIME, result.timeMillis)
            .putString(KEY_STDOUT, result.stdout)
            .putString(KEY_STDERR, result.stderr)
            .putBoolean(KEY_HAS_EXIT_CODE, result.exitCode != null)
            .putInt(KEY_EXIT_CODE, result.exitCode ?: 0)
            .putBoolean(KEY_HAS_ERR_CODE, result.errCode != null)
            .putInt(KEY_ERR_CODE, result.errCode ?: 0)
            .putString(KEY_ERR_MESSAGE, result.errMessage)
            .putString(KEY_STDOUT_ORIGINAL_LENGTH, result.stdoutOriginalLength)
            .putString(KEY_STDERR_ORIGINAL_LENGTH, result.stderrOriginalLength)
            .putString(KEY_RAW, result.raw)
            .putString(KEY_HISTORY, encodeHistory(history))
            .apply()
    }

    fun latest(context: Context): TermuxCommandResult? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_TIME, 0L)
        if (time == 0L) return null

        return TermuxCommandResult(
            executionId = prefs.getInt(KEY_EXECUTION_ID, 0),
            command = prefs.getString(KEY_COMMAND, "").orEmpty(),
            nonce = prefs.getString(KEY_NONCE, null),
            hasResultBundle = prefs.getBoolean(KEY_HAS_RESULT_BUNDLE, false),
            timeMillis = time,
            stdout = prefs.getString(KEY_STDOUT, "").orEmpty(),
            stderr = prefs.getString(KEY_STDERR, "").orEmpty(),
            exitCode = if (prefs.getBoolean(KEY_HAS_EXIT_CODE, false)) {
                prefs.getInt(KEY_EXIT_CODE, 0)
            } else {
                null
            },
            errCode = if (prefs.getBoolean(KEY_HAS_ERR_CODE, false)) {
                prefs.getInt(KEY_ERR_CODE, 0)
            } else {
                null
            },
            errMessage = prefs.getString(KEY_ERR_MESSAGE, "").orEmpty(),
            stdoutOriginalLength = prefs.getString(KEY_STDOUT_ORIGINAL_LENGTH, "").orEmpty(),
            stderrOriginalLength = prefs.getString(KEY_STDERR_ORIGINAL_LENGTH, "").orEmpty(),
            raw = prefs.getString(KEY_RAW, "").orEmpty(),
        )
    }

    fun recent(context: Context): List<TermuxCommandResult> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (listOfNotNull(latest(context)) + readHistory(prefs))
            .distinctBy { it.stableKey }
            .take(MAX_HISTORY + 1)
    }

    private fun readHistory(prefs: SharedPreferences): List<TermuxCommandResult> {
        val raw = prefs.getString(KEY_HISTORY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(decodeResult(item))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeHistory(history: List<TermuxCommandResult>): String {
        val array = JSONArray()
        history.forEach { result ->
            array.put(JSONObject().apply {
                put(KEY_EXECUTION_ID, result.executionId)
                put(KEY_COMMAND, result.command)
                put(KEY_NONCE, result.nonce)
                put(KEY_HAS_RESULT_BUNDLE, result.hasResultBundle)
                put(KEY_TIME, result.timeMillis)
                put(KEY_STDOUT, result.stdout)
                put(KEY_STDERR, result.stderr)
                put(KEY_HAS_EXIT_CODE, result.exitCode != null)
                put(KEY_EXIT_CODE, result.exitCode ?: 0)
                put(KEY_HAS_ERR_CODE, result.errCode != null)
                put(KEY_ERR_CODE, result.errCode ?: 0)
                put(KEY_ERR_MESSAGE, result.errMessage)
                put(KEY_STDOUT_ORIGINAL_LENGTH, result.stdoutOriginalLength)
                put(KEY_STDERR_ORIGINAL_LENGTH, result.stderrOriginalLength)
                put(KEY_RAW, result.raw)
            })
        }
        return array.toString()
    }

    private fun decodeResult(item: JSONObject): TermuxCommandResult {
        return TermuxCommandResult(
            executionId = item.optInt(KEY_EXECUTION_ID, 0),
            command = item.optString(KEY_COMMAND, ""),
            nonce = item.optString(KEY_NONCE).takeIf { item.has(KEY_NONCE) && it.isNotBlank() },
            hasResultBundle = item.optBoolean(KEY_HAS_RESULT_BUNDLE, false),
            timeMillis = item.optLong(KEY_TIME, 0L),
            stdout = item.optString(KEY_STDOUT, ""),
            stderr = item.optString(KEY_STDERR, ""),
            exitCode = if (item.optBoolean(KEY_HAS_EXIT_CODE, false)) item.optInt(KEY_EXIT_CODE, 0) else null,
            errCode = if (item.optBoolean(KEY_HAS_ERR_CODE, false)) item.optInt(KEY_ERR_CODE, 0) else null,
            errMessage = item.optString(KEY_ERR_MESSAGE, ""),
            stdoutOriginalLength = item.optString(KEY_STDOUT_ORIGINAL_LENGTH, ""),
            stderrOriginalLength = item.optString(KEY_STDERR_ORIGINAL_LENGTH, ""),
            raw = item.optString(KEY_RAW, ""),
        )
    }
}
