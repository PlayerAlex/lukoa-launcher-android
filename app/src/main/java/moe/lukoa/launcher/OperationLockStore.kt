package moe.lukoa.launcher

import android.content.Context

object OperationLockStore {
    private const val PREFS = "lukoa_operation_lock"
    private const val KEY_LABEL = "label"
    private const val KEY_UNTIL = "until"

    fun acquire(context: Context, label: String, timeoutMs: Long) {
        val safeLabel = label.trim().ifBlank { "处理中" }
        val safeUntil = System.currentTimeMillis() + timeoutMs.coerceAtLeast(5_000L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LABEL, safeLabel)
            .putLong(KEY_UNTIL, safeUntil)
            .apply()
    }

    fun release(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LABEL)
            .remove(KEY_UNTIL)
            .apply()
    }

    fun activeLabel(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (until <= System.currentTimeMillis()) {
            release(context)
            return null
        }
        return prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() }
    }
}
