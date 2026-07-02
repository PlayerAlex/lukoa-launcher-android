package moe.lukoa.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val result = TermuxResultParser.parse(intent)
        TermuxResultStore.save(context, result)
        AutoBackupRetentionManager.maybePruneAfterTermuxResult(context, result)
    }
}
