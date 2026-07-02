package moe.lukoa.launcher

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        AutoBackupScheduler.onWorkTriggered(applicationContext)
        return Result.success()
    }
}
