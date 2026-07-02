package moe.lukoa.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoBackupAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AutoBackupScheduler.onLegacyAlarm(context.applicationContext)
    }
}
