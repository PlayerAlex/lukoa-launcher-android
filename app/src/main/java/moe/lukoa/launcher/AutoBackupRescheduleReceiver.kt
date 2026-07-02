package moe.lukoa.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoBackupRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val config = LauncherStateStore(context.applicationContext).readAutoBackupConfig()
        AutoBackupScheduler.syncFromState(
            context = context.applicationContext,
            enabled = config.enabled,
            intervalMinutes = config.intervalMinutes,
            resetCountdown = false,
        )
    }
}
