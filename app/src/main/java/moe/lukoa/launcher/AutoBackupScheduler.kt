package moe.lukoa.launcher

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val PREFS = "lukoa_auto_backup_scheduler"
    private const val KEY_NEXT_TRIGGER_AT = "next_trigger_at"
    private const val KEY_LAST_DISPATCH_AT = "last_dispatch_at"
    private const val KEY_WORK_NAME = "work_name"
    private const val WORK_NAME_PREFIX = "lukoa_auto_backup_"
    private const val WORK_TAG = "lukoa_auto_backup"
    private const val DUE_SOON_DELAY_MS = 5_000L
    private const val BUSY_RETRY_DELAY_MS = 2 * 60 * 1000L
    private const val DUPLICATE_GUARD_MS = 60 * 1000L

    fun syncFromState(context: Context, enabled: Boolean, intervalMinutes: Int, resetCountdown: Boolean) {
        if (!enabled) {
            cancel(context)
            return
        }
        val safeIntervalMinutes = intervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        )
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val storedTriggerAt = prefs.getLong(KEY_NEXT_TRIGGER_AT, 0L)
        val triggerAt = when {
            resetCountdown -> now + safeIntervalMinutes * 60_000L
            storedTriggerAt <= 0L -> now + safeIntervalMinutes * 60_000L
            storedTriggerAt <= now -> now + DUE_SOON_DELAY_MS
            else -> storedTriggerAt
        }
        scheduleAt(context, triggerAt)
    }

    fun onLegacyAlarm(context: Context) {
        runScheduledBackup(context, source = "legacy-alarm")
    }

    fun onWorkTriggered(context: Context) {
        runScheduledBackup(context, source = "work")
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val manager = WorkManager.getInstance(appContext)
        prefs(appContext).edit()
            .remove(KEY_NEXT_TRIGGER_AT)
            .remove(KEY_WORK_NAME)
            .apply()
        manager.cancelAllWorkByTag(WORK_TAG)
    }

    fun nextTriggerAt(context: Context): Long {
        return prefs(context.applicationContext).getLong(KEY_NEXT_TRIGGER_AT, 0L)
    }

    private fun runScheduledBackup(context: Context, source: String) {
        val appContext = context.applicationContext
        val config = LauncherStateStore(appContext).readAutoBackupConfig()
        if (!config.enabled) {
            cancel(appContext)
            return
        }

        val now = System.currentTimeMillis()
        val prefs = prefs(appContext)
        val lastDispatchAt = prefs.getLong(KEY_LAST_DISPATCH_AT, 0L)
        if (now - lastDispatchAt < DUPLICATE_GUARD_MS) {
            scheduleAt(
                context = appContext,
                triggerAtMillis = now + config.intervalMinutes.coerceIn(
                    MIN_AUTO_BACKUP_INTERVAL_MINUTES,
                    MAX_AUTO_BACKUP_INTERVAL_MINUTES,
                ) * 60_000L,
                cancelPrevious = false,
            )
            return
        }

        val activeLabel = OperationLockStore.activeLabel(appContext)
        if (activeLabel != null) {
            val message = "自动备份到点了，但当前正在处理：$activeLabel。已顺延 2 分钟。"
            RuntimeLogArchive.appendApp(appContext, message)
            LauncherStateStore(appContext).appendAppLogMessage(message)
            scheduleAt(
                context = appContext,
                triggerAtMillis = now + BUSY_RETRY_DELAY_MS,
                cancelPrevious = false,
            )
            return
        }

        scheduleAt(
            context = appContext,
            triggerAtMillis = now + config.intervalMinutes.coerceIn(
                MIN_AUTO_BACKUP_INTERVAL_MINUTES,
                MAX_AUTO_BACKUP_INTERVAL_MINUTES,
            ) * 60_000L,
            cancelPrevious = false,
        )
        prefs.edit().putLong(KEY_LAST_DISPATCH_AT, now).apply()

        val runner = TermuxCommandRunner(appContext)
        val dispatch = runner.runTavernBackup("auto", config.keepCount)
        val message = if (dispatch.sent) {
            "自动备份到点了，已在后台发送命令。来源=$source"
        } else {
            "自动备份到点了，但后台发送失败：${dispatch.message}。来源=$source"
        }
        RuntimeLogArchive.appendApp(appContext, message)
        LauncherStateStore(appContext).appendAppLogMessage(message)
    }

    private fun scheduleAt(
        context: Context,
        triggerAtMillis: Long,
        cancelPrevious: Boolean = true,
    ) {
        val appContext = context.applicationContext
        val safeTriggerAt = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val workName = "$WORK_NAME_PREFIX$safeTriggerAt"
        val prefs = prefs(appContext)
        val previousWorkName = prefs.getString(KEY_WORK_NAME, null)
        if (cancelPrevious && !previousWorkName.isNullOrBlank() && previousWorkName != workName) {
            WorkManager.getInstance(appContext).cancelUniqueWork(previousWorkName)
        }

        val delayMs = (safeTriggerAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        prefs.edit()
            .putLong(KEY_NEXT_TRIGGER_AT, safeTriggerAt)
            .putString(KEY_WORK_NAME, workName)
            .apply()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
