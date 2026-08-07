package moe.lukoa.launcher

import android.annotation.SuppressLint
import android.content.Context

class AutoBackupConfigStore(context: Context) {
    private val appContext = context.applicationContext

    fun read(): AutoBackupConfigSnapshot {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_ENABLED)) {
            return AutoBackupConfigSnapshot(
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                intervalMinutes = prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
                    .coerceIn(MIN_AUTO_BACKUP_INTERVAL_MINUTES, MAX_AUTO_BACKUP_INTERVAL_MINUTES),
                keepCount = prefs.getInt(KEY_KEEP_COUNT, DEFAULT_KEEP_COUNT).coerceIn(1, 50),
            )
        }

        val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val migrated = AutoBackupConfigSnapshot(
            enabled = legacyPrefs.getBoolean(LEGACY_KEY_ENABLED, false),
            intervalMinutes = if (legacyPrefs.contains(LEGACY_KEY_INTERVAL_MINUTES)) {
                legacyPrefs.getInt(LEGACY_KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
            } else {
                legacyPrefs.getInt(LEGACY_KEY_INTERVAL_HOURS, DEFAULT_INTERVAL_MINUTES / 60) * 60
            }.coerceIn(MIN_AUTO_BACKUP_INTERVAL_MINUTES, MAX_AUTO_BACKUP_INTERVAL_MINUTES),
            keepCount = legacyPrefs.getInt(LEGACY_KEY_KEEP_COUNT, DEFAULT_KEEP_COUNT).coerceIn(1, 50),
        )
        save(migrated.enabled, migrated.intervalMinutes, migrated.keepCount)
        return migrated
    }

    @SuppressLint("ApplySharedPref")
    fun save(enabled: Boolean, intervalMinutes: Int, keepCount: Int) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(
                KEY_INTERVAL_MINUTES,
                intervalMinutes.coerceIn(MIN_AUTO_BACKUP_INTERVAL_MINUTES, MAX_AUTO_BACKUP_INTERVAL_MINUTES),
            )
            .putInt(KEY_KEEP_COUNT, keepCount.coerceIn(1, 50))
            .commit()
    }

    companion object {
        internal const val PREFS_NAME = "lukoa_auto_backup_config"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_KEEP_COUNT = "keep_count"
        private const val DEFAULT_INTERVAL_MINUTES = 360
        private const val DEFAULT_KEEP_COUNT = 5

        private const val LEGACY_PREFS_NAME = "launcher_ui_state"
        private const val LEGACY_KEY_ENABLED = "auto_backup_enabled"
        private const val LEGACY_KEY_INTERVAL_HOURS = "auto_backup_interval_hours"
        private const val LEGACY_KEY_INTERVAL_MINUTES = "auto_backup_interval_minutes"
        private const val LEGACY_KEY_KEEP_COUNT = "auto_backup_keep_count"
    }
}
