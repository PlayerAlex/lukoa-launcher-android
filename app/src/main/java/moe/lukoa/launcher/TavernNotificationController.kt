package moe.lukoa.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object TavernNotificationStore {
    private const val PREFS = "lukoa_tavern_notification"
    private const val KEY_ENABLED = "running_notification_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        return enabled
    }
}

object TavernNotificationController {
    const val CHANNEL_ID = "lukoa_tavern_status"
    const val NOTIFICATION_ID = 1001
    const val ACTION_OPEN_TAVERN = "moe.lukoa.launcher.action.NOTIFICATION_OPEN_TAVERN"
    const val ACTION_RESTART_TAVERN = "moe.lukoa.launcher.action.NOTIFICATION_RESTART_TAVERN"
    const val ACTION_STOP_TAVERN = "moe.lukoa.launcher.action.NOTIFICATION_STOP_TAVERN"

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun syncRunningNotification(
        context: Context,
        running: Boolean,
        instanceLabel: String,
        port: Int,
        enabled: Boolean = TavernNotificationStore.isEnabled(context),
    ) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (!running || !enabled || !canPostNotifications(appContext)) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        ensureChannel(manager)
        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(appContext, 2000, openAppIntent, piFlags)

        fun actionPi(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(appContext, TavernNotificationActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(appContext, requestCode, intent, piFlags)
        }

        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lukoa_launcher)
            .setContentTitle("酒馆运行中（${instanceLabel.ifBlank { "主实例" }}）")
            .setContentText("http://127.0.0.1:$port，点击按钮可快速打开、重启或停止")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPi)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "打开酒馆",
                    actionPi(ACTION_OPEN_TAVERN, 2001),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "重启",
                    actionPi(ACTION_RESTART_TAVERN, 2002),
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "停止",
                    actionPi(ACTION_STOP_TAVERN, 2003),
                ).build(),
            )
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    fun cancel(context: Context) {
        val manager = context.applicationContext.getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "酒馆运行状态控制",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "在酒馆运行时提供通知栏快速打开、重启和停止按钮。"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
