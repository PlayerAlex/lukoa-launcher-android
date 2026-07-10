package moe.lukoa.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        TermuxResultIngress.enqueue(
            context = context,
            intent = intent,
            onComplete = pendingResult::finish,
        )
    }
}
