package moe.lukoa.launcher

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoBackupRetentionWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val reason = inputData.getString(KEY_REASON).orEmpty()
        AutoBackupRetentionManager.enforceConfiguredLimit(applicationContext, reason)
        return Result.success()
    }

    companion object {
        const val KEY_REASON = "reason"
    }
}
