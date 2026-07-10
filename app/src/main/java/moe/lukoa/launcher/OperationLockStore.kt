package moe.lukoa.launcher

import android.content.Context

data class OperationLockSnapshot(
    val label: String,
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
) {
    fun remainingMillis(nowMillis: Long): Long {
        return (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    }

    fun elapsedMillis(nowMillis: Long): Long {
        return (nowMillis - startedAtMillis).coerceAtLeast(0L)
    }
}

data class RestoredOperationLock(
    val label: String,
    val busyStartedAtElapsedMillis: Long,
    val remainingMillis: Long,
)

object OperationLockRecovery {
    fun restore(
        snapshot: OperationLockSnapshot?,
        nowMillis: Long,
        elapsedRealtimeMillis: Long,
    ): RestoredOperationLock? {
        snapshot ?: return null
        val remainingMillis = snapshot.remainingMillis(nowMillis)
        if (remainingMillis <= 0L) return null
        return RestoredOperationLock(
            label = snapshot.label,
            busyStartedAtElapsedMillis = (
                elapsedRealtimeMillis - snapshot.elapsedMillis(nowMillis)
                ).coerceAtLeast(0L),
            remainingMillis = remainingMillis,
        )
    }
}

object OperationLockStore {
    private const val PREFS = "lukoa_operation_lock"
    private const val KEY_LABEL = "label"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_UNTIL = "until"

    fun acquire(context: Context, label: String, timeoutMs: Long) {
        val safeLabel = label.trim().ifBlank { "处理中" }
        val startedAtMillis = System.currentTimeMillis()
        val safeUntil = startedAtMillis + timeoutMs.coerceAtLeast(5_000L)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LABEL, safeLabel)
            .putLong(KEY_STARTED_AT, startedAtMillis)
            .putLong(KEY_UNTIL, safeUntil)
            .apply()
    }

    fun release(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LABEL)
            .remove(KEY_STARTED_AT)
            .remove(KEY_UNTIL)
            .apply()
    }

    fun active(context: Context, nowMillis: Long = System.currentTimeMillis()): OperationLockSnapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiresAtMillis = prefs.getLong(KEY_UNTIL, 0L)
        if (expiresAtMillis <= nowMillis) {
            release(context)
            return null
        }
        val label = prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() } ?: return null
        val startedAtMillis = prefs.getLong(KEY_STARTED_AT, nowMillis)
            .takeIf { it in 1..nowMillis }
            ?: nowMillis
        return OperationLockSnapshot(
            label = label,
            startedAtMillis = startedAtMillis,
            expiresAtMillis = expiresAtMillis,
        )
    }

    fun activeLabel(context: Context): String? {
        return active(context)?.label
    }
}
