package moe.lukoa.launcher

import android.content.Context
import android.content.SharedPreferences

data class OperationLockSnapshot(
    val label: String,
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
    val ownerToken: String = "",
) {
    fun remainingMillis(nowMillis: Long): Long {
        return (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    }

    fun elapsedMillis(nowMillis: Long): Long {
        return (nowMillis - startedAtMillis).coerceAtLeast(0L)
    }
}

object OperationLockOwnership {
    fun canRelease(snapshot: OperationLockSnapshot?, ownerToken: String): Boolean {
        return ownerToken.isNotBlank() && snapshot?.ownerToken == ownerToken
    }

    fun canReleaseUnowned(snapshot: OperationLockSnapshot?): Boolean {
        return snapshot != null && snapshot.ownerToken.isBlank()
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
    private const val KEY_OWNER_TOKEN = "owner_token"
    private val storeLock = Any()

    fun acquire(
        context: Context,
        label: String,
        timeoutMs: Long,
        ownerToken: String = "",
    ): Boolean {
        val safeLabel = label.trim().ifBlank { "处理中" }
        val startedAtMillis = System.currentTimeMillis()
        val safeUntil = startedAtMillis + timeoutMs.coerceAtLeast(5_000L)
        return synchronized(storeLock) {
            if (activeLocked(context, startedAtMillis) != null) {
                return@synchronized false
            }
            prefs(context)
                .edit()
                .putString(KEY_LABEL, safeLabel)
                .putLong(KEY_STARTED_AT, startedAtMillis)
                .putLong(KEY_UNTIL, safeUntil)
                .putString(KEY_OWNER_TOKEN, ownerToken.trim())
                .apply()
            true
        }
    }

    fun release(context: Context): Boolean {
        return synchronized(storeLock) {
            if (!OperationLockOwnership.canReleaseUnowned(activeLocked(context, System.currentTimeMillis()))) {
                return@synchronized false
            }
            clear(prefs(context))
            true
        }
    }

    fun releaseOwned(context: Context, ownerToken: String): Boolean {
        return synchronized(storeLock) {
            val snapshot = activeLocked(context, System.currentTimeMillis())
            if (!OperationLockOwnership.canRelease(snapshot, ownerToken)) {
                return@synchronized false
            }
            clear(prefs(context))
            true
        }
    }

    fun active(context: Context, nowMillis: Long = System.currentTimeMillis()): OperationLockSnapshot? {
        return synchronized(storeLock) {
            activeLocked(context, nowMillis)
        }
    }

    fun activeLabel(context: Context): String? {
        return active(context)?.label
    }

    fun observe(context: Context, onChanged: (OperationLockSnapshot?) -> Unit): () -> Unit {
        val prefs = prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LABEL || key == KEY_STARTED_AT || key == KEY_UNTIL || key == KEY_OWNER_TOKEN) {
                onChanged(active(context))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun activeLocked(context: Context, nowMillis: Long): OperationLockSnapshot? {
        val prefs = prefs(context)
        val expiresAtMillis = prefs.getLong(KEY_UNTIL, 0L)
        if (expiresAtMillis <= nowMillis) {
            clear(prefs)
            return null
        }
        val label = prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val startedAtMillis = prefs.getLong(KEY_STARTED_AT, nowMillis)
            .takeIf { it in 1..nowMillis }
            ?: nowMillis
        return OperationLockSnapshot(
            label = label,
            startedAtMillis = startedAtMillis,
            expiresAtMillis = expiresAtMillis,
            ownerToken = prefs.getString(KEY_OWNER_TOKEN, "").orEmpty(),
        )
    }

    private fun clear(prefs: SharedPreferences) {
        prefs.edit()
            .remove(KEY_LABEL)
            .remove(KEY_STARTED_AT)
            .remove(KEY_UNTIL)
            .remove(KEY_OWNER_TOKEN)
            .apply()
    }
}
