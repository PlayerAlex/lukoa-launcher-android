package moe.lukoa.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

data class LauncherLoadResult(
    val state: LauncherUiState,
    val startupRefreshRequested: Boolean,
)

data class AutoBackupConfigSnapshot(
    val enabled: Boolean,
    val intervalMinutes: Int,
    val keepCount: Int,
)

class LauncherStateStore(private val context: Context) {
    fun load(isTermuxInstalled: Boolean, allowColdStartFallback: Boolean): LauncherLoadResult {
        val defaults = defaultLauncherState(isTermuxInstalled)
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        if (
            prefs.getBoolean(KEY_CLEAR_ON_NEXT_LAUNCH, false) ||
            (allowColdStartFallback && prefs.getBoolean(KEY_CLEAR_ON_NEXT_COLD_START, false))
        ) {
            val clearedState = defaults.copy(
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, defaults.officialVersionsCache)
                    ?: defaults.officialVersionsCache,
                appLog = logEntry("App", "上次启动器已从后台任务中移除，已自动清除启动器显示日志。"),
            )
            saveClearedLaunchState(clearedState)
            return LauncherLoadResult(
                state = clearedState,
                startupRefreshRequested = true,
            )
        }

        val loadedAutoBackupIntervalMinutes = readSavedAutoBackupIntervalMinutes(prefs, defaults)
        val loadedAutoBackupKeepCount = prefs.getInt(KEY_AUTO_BACKUP_KEEP_COUNT, defaults.autoBackupKeepCount)
            .coerceIn(1, 50)
        val loadedTermuxReturnDelayMs = prefs.getLong(KEY_TERMUX_RETURN_DELAY_MS, defaults.termuxReturnDelayMs)
            .coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS)

        if (!prefs.contains(KEY_STATUS)) {
            val bootstrapState = defaults.copy(
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, defaults.officialVersionsCache)
                    ?: defaults.officialVersionsCache,
                autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, defaults.autoBackupEnabled),
                autoBackupIntervalMinutes = loadedAutoBackupIntervalMinutes,
                autoBackupKeepCount = loadedAutoBackupKeepCount,
                termuxReturnDelayMs = loadedTermuxReturnDelayMs,
            )
            return LauncherLoadResult(
                state = bootstrapState,
                startupRefreshRequested = allowColdStartFallback,
            )
        }

        val loadedState = LauncherUiState(
                status = prefs.getString(KEY_STATUS, null) ?: defaults.status,
                summary = prefs.getString(KEY_SUMMARY, null) ?: defaults.summary,
                termuxLog = prefs.getString(KEY_TERMUX_LOG, null) ?: defaults.termuxLog,
                appLog = prefs.getString(KEY_APP_LOG, null) ?: defaults.appLog,
                verified = prefs.getBoolean(KEY_VERIFIED, defaults.verified),
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, null)
                    ?: defaults.officialVersionsCache,
                autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, defaults.autoBackupEnabled),
                autoBackupIntervalMinutes = loadedAutoBackupIntervalMinutes,
                autoBackupKeepCount = loadedAutoBackupKeepCount,
                backupHistory = prefs.getString(KEY_BACKUP_HISTORY, null)
                    ?.lineSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.take(20)
                    ?.toList()
                    ?.let { BackupHistoryReducer.sanitize(it) }
                    ?: defaults.backupHistory,
                termuxReturnDelayMs = loadedTermuxReturnDelayMs,
            )
        val safeState = if (isTermuxInstalled) {
            loadedState
        } else {
            defaults.copy(
                officialVersionsCache = loadedState.officialVersionsCache,
                autoBackupEnabled = loadedState.autoBackupEnabled,
                autoBackupIntervalMinutes = loadedState.autoBackupIntervalMinutes,
                autoBackupKeepCount = loadedState.autoBackupKeepCount,
                termuxReturnDelayMs = loadedState.termuxReturnDelayMs,
                appLog = logEntry("App", "当前手机未检测到 Termux，已进入安装引导。"),
            )
        }

        return LauncherLoadResult(
            state = safeState,
            startupRefreshRequested = allowColdStartFallback,
        )
    }

    fun save(state: LauncherUiState) {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATUS, state.status)
            .putString(KEY_SUMMARY, state.summary)
            .putString(KEY_TERMUX_LOG, state.termuxLog)
            .putString(KEY_APP_LOG, state.appLog)
            .putBoolean(KEY_VERIFIED, state.verified)
            .putString(KEY_OFFICIAL_VERSIONS_CACHE, state.officialVersionsCache)
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, state.autoBackupEnabled)
            .putInt(
                KEY_AUTO_BACKUP_INTERVAL_MINUTES,
                state.autoBackupIntervalMinutes.coerceIn(
                    MIN_AUTO_BACKUP_INTERVAL_MINUTES,
                    MAX_AUTO_BACKUP_INTERVAL_MINUTES,
                ),
            )
            .putInt(
                KEY_AUTO_BACKUP_INTERVAL_HOURS,
                (state.autoBackupIntervalMinutes / 60).coerceIn(1, 12),
            )
            .putInt(KEY_AUTO_BACKUP_KEEP_COUNT, state.autoBackupKeepCount.coerceIn(1, 50))
            .putString(KEY_BACKUP_HISTORY, BackupHistoryReducer.sanitize(state.backupHistory).joinToString("\n"))
            .putLong(KEY_TERMUX_RETURN_DELAY_MS, state.termuxReturnDelayMs.coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS))
            .apply()
    }

    fun readAutoBackupConfig(): AutoBackupConfigSnapshot {
        val defaults = defaultLauncherState(isTermuxInstalled = true)
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val intervalMinutes = readSavedAutoBackupIntervalMinutes(prefs, defaults)
        return AutoBackupConfigSnapshot(
            enabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, defaults.autoBackupEnabled),
            intervalMinutes = intervalMinutes,
            keepCount = prefs.getInt(KEY_AUTO_BACKUP_KEEP_COUNT, defaults.autoBackupKeepCount).coerceIn(1, 50),
        )
    }

    @SuppressLint("ApplySharedPref")
    fun saveAutoBackupConfig(enabled: Boolean, intervalMinutes: Int, keepCount: Int) {
        val safeIntervalMinutes = intervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        )
        val safeKeepCount = keepCount.coerceIn(1, 50)
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled)
            .putInt(KEY_AUTO_BACKUP_INTERVAL_MINUTES, safeIntervalMinutes)
            .putInt(KEY_AUTO_BACKUP_INTERVAL_HOURS, (safeIntervalMinutes / 60).coerceIn(1, 12))
            .putInt(KEY_AUTO_BACKUP_KEEP_COUNT, safeKeepCount)
            .commit()
    }

    fun appendAppLogMessage(message: String) {
        if (message.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_APP_LOG, null).orEmpty()
        val updated = appendLog(current, "App", message)
        prefs.edit().putString(KEY_APP_LOG, updated).apply()
    }

    @SuppressLint("ApplySharedPref")
    fun markClearOnNextLaunch() {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CLEAR_ON_NEXT_LAUNCH, true)
            .commit()
    }

    @SuppressLint("ApplySharedPref")
    fun claimTermuxWakeSlot(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val lastWakeAt = prefs.getLong(KEY_LAST_TERMUX_WAKE_AT, 0L)
        if (nowMillis - lastWakeAt < TERMUX_WAKE_PERSISTENT_COOLDOWN_MS) {
            return false
        }
        prefs.edit()
            .putLong(KEY_LAST_TERMUX_WAKE_AT, nowMillis)
            .commit()
        return true
    }

    @SuppressLint("ApplySharedPref")
    fun armColdStartClearFallback() {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CLEAR_ON_NEXT_COLD_START, true)
            .commit()
    }

    private fun saveClearedLaunchState(state: LauncherUiState) {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CLEAR_ON_NEXT_LAUNCH)
            .remove(KEY_CLEAR_ON_NEXT_COLD_START)
            .putString(KEY_STATUS, state.status)
            .putString(KEY_SUMMARY, state.summary)
            .putString(KEY_TERMUX_LOG, state.termuxLog)
            .putString(KEY_APP_LOG, state.appLog)
            .putBoolean(KEY_VERIFIED, state.verified)
            .putString(KEY_OFFICIAL_VERSIONS_CACHE, state.officialVersionsCache)
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, state.autoBackupEnabled)
            .putInt(
                KEY_AUTO_BACKUP_INTERVAL_MINUTES,
                state.autoBackupIntervalMinutes.coerceIn(
                    MIN_AUTO_BACKUP_INTERVAL_MINUTES,
                    MAX_AUTO_BACKUP_INTERVAL_MINUTES,
                ),
            )
            .putInt(
                KEY_AUTO_BACKUP_INTERVAL_HOURS,
                (state.autoBackupIntervalMinutes / 60).coerceIn(1, 12),
            )
            .putInt(KEY_AUTO_BACKUP_KEEP_COUNT, state.autoBackupKeepCount.coerceIn(1, 50))
            .putString(KEY_BACKUP_HISTORY, BackupHistoryReducer.sanitize(state.backupHistory).joinToString("\n"))
            .putLong(KEY_TERMUX_RETURN_DELAY_MS, state.termuxReturnDelayMs.coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS))
            .apply()
    }

    private fun readSavedAutoBackupIntervalMinutes(
        prefs: SharedPreferences,
        defaults: LauncherUiState,
    ): Int {
        return if (prefs.contains(KEY_AUTO_BACKUP_INTERVAL_MINUTES)) {
            prefs.getInt(KEY_AUTO_BACKUP_INTERVAL_MINUTES, defaults.autoBackupIntervalMinutes)
        } else {
            prefs.getInt(KEY_AUTO_BACKUP_INTERVAL_HOURS, 6) * 60
        }.coerceIn(MIN_AUTO_BACKUP_INTERVAL_MINUTES, MAX_AUTO_BACKUP_INTERVAL_MINUTES)
    }

    private companion object {
        const val PREFS_UI_STATE = "launcher_ui_state"
        const val KEY_STATUS = "status"
        const val KEY_SUMMARY = "summary"
        const val KEY_TERMUX_LOG = "termux_log"
        const val KEY_APP_LOG = "app_log"
        const val KEY_VERIFIED = "verified"
        const val KEY_OFFICIAL_VERSIONS_CACHE = "official_versions_cache"
        const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        const val KEY_AUTO_BACKUP_INTERVAL_HOURS = "auto_backup_interval_hours"
        const val KEY_AUTO_BACKUP_INTERVAL_MINUTES = "auto_backup_interval_minutes"
        const val KEY_AUTO_BACKUP_KEEP_COUNT = "auto_backup_keep_count"
        const val KEY_BACKUP_HISTORY = "backup_history"
        const val KEY_TERMUX_RETURN_DELAY_MS = "termux_return_delay_ms"
        const val KEY_LAST_TERMUX_WAKE_AT = "last_termux_wake_at"
        const val KEY_CLEAR_ON_NEXT_LAUNCH = "clear_on_next_launch"
        const val KEY_CLEAR_ON_NEXT_COLD_START = "clear_on_next_cold_start"
        const val MIN_TERMUX_RETURN_DELAY_MS = 300L
        const val MAX_TERMUX_RETURN_DELAY_MS = 2_000L
        const val TERMUX_WAKE_PERSISTENT_COOLDOWN_MS = 8_000L
    }
}
