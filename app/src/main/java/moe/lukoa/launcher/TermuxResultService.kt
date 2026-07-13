package moe.lukoa.launcher

import android.app.Service
import android.content.Intent
import android.os.IBinder

class TermuxResultService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TermuxResultIngress.enqueue(
            context = this,
            intent = intent,
            onComplete = { stopSelf(startId) },
        )
        return START_NOT_STICKY
    }
}
