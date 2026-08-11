package moe.lukoa.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

data class LauncherLoadResult(
    val state: LauncherUiState,
    val startupRefreshRequested: Boolean,
    val displayLogsCleared: Boolean = false,
)

data class AutoBackupConfigSnapshot(
    val enabled: Boolean,
    val intervalMinutes: Int,
    val keepCount: Int,
)

class LauncherStateStore(private val context: Context) {
    private val autoBackupConfigStore = AutoBackupConfigStore(context)

    fun load(isTermuxInstalled: Boolean, allowColdStartFallback: Boolean): LauncherLoadResult {
        val defaults = defaultLauncherState(isTermuxInstalled)
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val autoBackupConfig = autoBackupConfigStore.read()
        if (!isTermuxInstalled && prefs.contains(KEY_LAST_HEALTH_CHECK)) {
            prefs.edit().remove(KEY_LAST_HEALTH_CHECK).apply()
        }
        val savedHealthCheck = if (isTermuxInstalled) {
            LauncherHealthSnapshotCodec.decode(prefs.getString(KEY_LAST_HEALTH_CHECK, null))
        } else {
            null
        }
        val loadedTermuxReturnDelayMs = prefs.getLong(KEY_TERMUX_RETURN_DELAY_MS, defaults.termuxReturnDelayMs)
            .coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS)
        if (
            prefs.getBoolean(KEY_CLEAR_ON_NEXT_LAUNCH, false) ||
            (allowColdStartFallback && prefs.getBoolean(KEY_CLEAR_ON_NEXT_COLD_START, false))
        ) {
            val clearedState = defaults.copy(
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, defaults.officialVersionsCache)
                    ?: defaults.officialVersionsCache,
                autoBackupEnabled = autoBackupConfig.enabled,
                autoBackupIntervalMinutes = autoBackupConfig.intervalMinutes,
                autoBackupKeepCount = autoBackupConfig.keepCount,
                termuxReturnDelayMs = loadedTermuxReturnDelayMs,
                lastHealthCheck = savedHealthCheck,
                appLog = logEntry("App", "上次启动器已从后台任务中移除，已自动清除启动器显示日志。"),
            )
            saveClearedLaunchState(clearedState)
            return LauncherLoadResult(
                state = clearedState,
                startupRefreshRequested = true,
                displayLogsCleared = true,
            )
        }

        if (!prefs.contains(KEY_STATUS)) {
            val bootstrapState = defaults.copy(
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, defaults.officialVersionsCache)
                    ?: defaults.officialVersionsCache,
                autoBackupEnabled = autoBackupConfig.enabled,
                autoBackupIntervalMinutes = autoBackupConfig.intervalMinutes,
                autoBackupKeepCount = autoBackupConfig.keepCount,
                termuxReturnDelayMs = loadedTermuxReturnDelayMs,
                lastHealthCheck = savedHealthCheck,
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
                tavernRuntimeLog = prefs.getString(KEY_TAVERN_RUNTIME_LOG, null) ?: defaults.tavernRuntimeLog,
                appLog = prefs.getString(KEY_APP_LOG, null) ?: defaults.appLog,
                verified = prefs.getBoolean(KEY_VERIFIED, defaults.verified),
                officialVersionsCache = prefs.getString(KEY_OFFICIAL_VERSIONS_CACHE, null)
                    ?: defaults.officialVersionsCache,
                autoBackupEnabled = autoBackupConfig.enabled,
                autoBackupIntervalMinutes = autoBackupConfig.intervalMinutes,
                autoBackupKeepCount = autoBackupConfig.keepCount,
                backupHistory = prefs.getString(KEY_BACKUP_HISTORY, null)
                    ?.lineSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.take(20)
                    ?.toList()
                    ?.let { BackupHistoryReducer.sanitize(it) }
                    ?: defaults.backupHistory,
                termuxReturnDelayMs = loadedTermuxReturnDelayMs,
                lastHealthCheck = savedHealthCheck,
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
            .putString(KEY_TAVERN_RUNTIME_LOG, state.tavernRuntimeLog)
            .putString(KEY_APP_LOG, state.appLog)
            .putBoolean(KEY_VERIFIED, state.verified)
            .putString(KEY_OFFICIAL_VERSIONS_CACHE, state.officialVersionsCache)
            .putString(KEY_BACKUP_HISTORY, BackupHistoryReducer.sanitize(state.backupHistory).joinToString("\n"))
            .putLong(KEY_TERMUX_RETURN_DELAY_MS, state.termuxReturnDelayMs.coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS))
            .putLastHealthCheck(state.lastHealthCheck)
            .apply()
    }

    fun readAutoBackupConfig(): AutoBackupConfigSnapshot {
        return autoBackupConfigStore.read()
    }

    fun readTermuxReturnDelayMs(): Long {
        val defaults = defaultLauncherState(isTermuxInstalled = true)
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TERMUX_RETURN_DELAY_MS, defaults.termuxReturnDelayMs)
            .coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS)
    }

    fun saveAutoBackupConfig(enabled: Boolean, intervalMinutes: Int, keepCount: Int) {
        autoBackupConfigStore.save(enabled, intervalMinutes, keepCount)
    }

    fun appendAppLogMessage(message: String) {
        if (message.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_APP_LOG, null).orEmpty()
        val updated = appendLog(current, "App", message)
        prefs.edit().putString(KEY_APP_LOG, updated).apply()
    }

    fun hasSeenFirstTavernStartGuide(): Boolean {
        return context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_TAVERN_START_GUIDE_SEEN, false)
    }

    fun markFirstTavernStartGuideSeen() {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_TAVERN_START_GUIDE_SEEN, true)
            .apply()
    }

    @SuppressLint("ApplySharedPref")
    fun markClearOnNextLaunch() {
        context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CLEAR_ON_NEXT_LAUNCH, true)
            .commit()
    }

    fun hasRecentTermuxWake(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        val lastWakeAt = prefs.getLong(KEY_LAST_TERMUX_WAKE_AT, 0L)
        return nowMillis - lastWakeAt < TERMUX_WAKE_PERSISTENT_COOLDOWN_MS
    }

    @SuppressLint("ApplySharedPref")
    fun recordTermuxWake(nowMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_TERMUX_WAKE_AT, nowMillis)
            .commit()
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
            .putString(KEY_TAVERN_RUNTIME_LOG, state.tavernRuntimeLog)
            .putString(KEY_APP_LOG, state.appLog)
            .putBoolean(KEY_VERIFIED, state.verified)
            .putString(KEY_OFFICIAL_VERSIONS_CACHE, state.officialVersionsCache)
            .putString(KEY_BACKUP_HISTORY, BackupHistoryReducer.sanitize(state.backupHistory).joinToString("\n"))
            .putLong(KEY_TERMUX_RETURN_DELAY_MS, state.termuxReturnDelayMs.coerceIn(MIN_TERMUX_RETURN_DELAY_MS, MAX_TERMUX_RETURN_DELAY_MS))
            .putLastHealthCheck(state.lastHealthCheck)
            .apply()
    }

    private fun SharedPreferences.Editor.putLastHealthCheck(
        snapshot: LauncherHealthSnapshot?,
    ): SharedPreferences.Editor {
        return if (snapshot == null) {
            remove(KEY_LAST_HEALTH_CHECK)
        } else {
            putString(KEY_LAST_HEALTH_CHECK, LauncherHealthSnapshotCodec.encode(snapshot))
        }
    }

    private companion object {
        const val PREFS_UI_STATE = "launcher_ui_state"
        const val KEY_STATUS = "status"
        const val KEY_SUMMARY = "summary"
        const val KEY_TERMUX_LOG = "termux_log"
        const val KEY_TAVERN_RUNTIME_LOG = "tavern_runtime_log"
        const val KEY_APP_LOG = "app_log"
        const val KEY_VERIFIED = "verified"
        const val KEY_OFFICIAL_VERSIONS_CACHE = "official_versions_cache"
        const val KEY_BACKUP_HISTORY = "backup_history"
        const val KEY_TERMUX_RETURN_DELAY_MS = "termux_return_delay_ms"
        const val KEY_LAST_HEALTH_CHECK = "last_health_check"
        const val KEY_LAST_TERMUX_WAKE_AT = "last_termux_wake_at"
        const val KEY_FIRST_TAVERN_START_GUIDE_SEEN = "first_tavern_start_guide_seen"
        const val KEY_CLEAR_ON_NEXT_LAUNCH = "clear_on_next_launch"
        const val KEY_CLEAR_ON_NEXT_COLD_START = "clear_on_next_cold_start"
        const val MIN_TERMUX_RETURN_DELAY_MS = 300L
        const val MAX_TERMUX_RETURN_DELAY_MS = 2_000L
        const val TERMUX_WAKE_PERSISTENT_COOLDOWN_MS = 8_000L
    }
}
