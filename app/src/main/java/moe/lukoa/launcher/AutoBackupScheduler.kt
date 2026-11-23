package moe.lukoa.launcher

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AutoBackupRuntimeStatus(
    val nextTriggerAtMillis: Long = 0L,
    val lastRunAtMillis: Long = 0L,
    val lastRunOk: Boolean? = null,
    val lastRunMessage: String = "",
)

fun formatAutoBackupScheduleTime(
    timeMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (timeMillis <= 0L) return "暂无"
    return runCatching {
        Instant.ofEpochMilli(timeMillis)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }.getOrElse { "暂无" }
}

object AutoBackupScheduler {
    private const val PREFS = "lukoa_auto_backup_scheduler"
    private const val KEY_NEXT_TRIGGER_AT = "next_trigger_at"
    private const val KEY_LAST_DISPATCH_AT = "last_dispatch_at"
    private const val KEY_LAST_RUN_AT = "last_run_at"
    private const val KEY_LAST_RUN_OK = "last_run_ok"
    private const val KEY_LAST_RUN_HAS_RESULT = "last_run_has_result"
    private const val KEY_LAST_RUN_MESSAGE = "last_run_message"
    private const val KEY_WORK_NAME = "work_name"
    private const val WORK_NAME_PREFIX = "lukoa_auto_backup_"
    private const val WORK_TAG = "lukoa_auto_backup"
    internal const val INPUT_WORK_NAME = "auto_backup_work_name"
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

    fun onWorkTriggered(context: Context, executingWorkName: String?) {
        runScheduledBackup(
            context = context,
            source = "work",
            executingWorkName = executingWorkName,
        )
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

    fun readRuntimeStatus(context: Context): AutoBackupRuntimeStatus {
        val p = prefs(context.applicationContext)
        val hasResult = p.getBoolean(KEY_LAST_RUN_HAS_RESULT, false)
        return AutoBackupRuntimeStatus(
            nextTriggerAtMillis = p.getLong(KEY_NEXT_TRIGGER_AT, 0L),
            lastRunAtMillis = p.getLong(KEY_LAST_RUN_AT, 0L),
            lastRunOk = if (hasResult) p.getBoolean(KEY_LAST_RUN_OK, false) else null,
            lastRunMessage = p.getString(KEY_LAST_RUN_MESSAGE, "").orEmpty(),
        )
    }

    fun recordLastRunResult(
        context: Context,
        ok: Boolean,
        message: String,
        atMillis: Long = System.currentTimeMillis(),
    ) {
        prefs(context.applicationContext).edit()
            .putLong(KEY_LAST_RUN_AT, atMillis)
            .putBoolean(KEY_LAST_RUN_HAS_RESULT, true)
            .putBoolean(KEY_LAST_RUN_OK, ok)
            .putString(KEY_LAST_RUN_MESSAGE, message.take(80))
            .apply()
    }

    private fun runScheduledBackup(
        context: Context,
        source: String,
        executingWorkName: String? = null,
    ) {
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
                executingWorkName = executingWorkName,
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
                executingWorkName = executingWorkName,
            )
            return
        }

        val lockToken = UUID.randomUUID().toString().replace("-", "")
        val acquired = OperationLockStore.acquire(
            context = appContext,
            label = AutoBackupOperationLockPolicy.LABEL,
            timeoutMs = TermuxCommandTimeoutPolicy.operationLockMillis("tavern-backup-auto"),
            ownerToken = lockToken,
        )
        if (!acquired) {
            val currentLabel = OperationLockStore.activeLabel(appContext) ?: "其他操作"
            val message = "自动备份到点了，但当前正在处理：$currentLabel。已顺延 2 分钟。"
            RuntimeLogArchive.appendApp(appContext, message)
            LauncherStateStore(appContext).appendAppLogMessage(message)
            scheduleAt(
                context = appContext,
                triggerAtMillis = now + BUSY_RETRY_DELAY_MS,
                executingWorkName = executingWorkName,
            )
            return
        }

        scheduleAt(
            context = appContext,
            triggerAtMillis = now + config.intervalMinutes.coerceIn(
                MIN_AUTO_BACKUP_INTERVAL_MINUTES,
                MAX_AUTO_BACKUP_INTERVAL_MINUTES,
            ) * 60_000L,
            executingWorkName = executingWorkName,
        )
        prefs.edit().putLong(KEY_LAST_DISPATCH_AT, now).apply()

        val runner = TermuxCommandRunner(appContext)
        val dispatch = runner.runTavernBackup(
            kind = "auto",
            keepCount = config.keepCount,
            nonce = lockToken,
        )
        if (!dispatch.sent) {
            OperationLockStore.releaseOwned(appContext, lockToken)
            recordLastRunResult(appContext, ok = false, message = "后台发送失败", atMillis = now)
        } else {
            recordLastRunResult(appContext, ok = true, message = "已触发，等待完成", atMillis = now)
        }
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
        executingWorkName: String? = null,
    ) {
        val appContext = context.applicationContext
        val safeTriggerAt = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        val workName = "$WORK_NAME_PREFIX$safeTriggerAt"
        val prefs = prefs(appContext)
        val previousWorkName = prefs.getString(KEY_WORK_NAME, null)
        AutoBackupSchedulePolicy.staleWorkToCancel(
            previousWorkName = previousWorkName,
            executingWorkName = executingWorkName,
            nextWorkName = workName,
        )?.let { staleWorkName ->
            WorkManager.getInstance(appContext).cancelUniqueWork(staleWorkName)
        }

        val delayMs = (safeTriggerAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(
                Data.Builder()
                    .putString(INPUT_WORK_NAME, workName)
                    .build(),
            )
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

    fun releaseOperationLockAfterResult(context: Context, result: TermuxCommandResult): Boolean {
        val ownerToken = result.nonce?.trim().orEmpty()
        val released = OperationLockStore.releaseOwned(context.applicationContext, ownerToken)
        val isAutoBackupResult = released ||
            result.command == "tavern-backup-auto" ||
            (result.command == "tavern-backup" && result.stdout.contains("kind=auto"))
        if (isAutoBackupResult) {
            val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
            recordLastRunResult(
                context = context.applicationContext,
                ok = ok,
                message = if (ok) "成功" else "失败",
                atMillis = result.timeMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            )
        }
        return released
    }
}
