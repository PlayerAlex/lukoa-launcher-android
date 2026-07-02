package moe.lukoa.launcher

import android.app.Service
import android.content.Intent
import android.os.IBinder

class TaskRemovedWatcherService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        LauncherStateStore(applicationContext).markClearOnNextLaunch()
        stopSelf()
    }
}
