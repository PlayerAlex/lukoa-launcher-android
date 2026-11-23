package moe.lukoa.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TavernNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val runner = TermuxCommandRunner(appContext)
        when (intent?.action) {
            TavernNotificationController.ACTION_OPEN_TAVERN -> {
                runner.openTavern()
            }

            TavernNotificationController.ACTION_RESTART_TAVERN -> {
                val dispatch = runner.runRestartTavern()
                RuntimeLogArchive.appendApp(
                    appContext,
                    if (dispatch.sent) "通知栏快捷操作：已发送重启酒馆命令。" else "通知栏重启失败：${dispatch.message}",
                )
            }

            TavernNotificationController.ACTION_STOP_TAVERN -> {
                val dispatch = runner.runStopTavern()
                if (dispatch.sent) {
                    TavernNotificationController.cancel(appContext)
                }
                RuntimeLogArchive.appendApp(
                    appContext,
                    if (dispatch.sent) "通知栏快捷操作：已发送停止酒馆命令。" else "通知栏停止失败：${dispatch.message}",
                )
            }
        }
    }
}
