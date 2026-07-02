package moe.lukoa.launcher

import android.content.Context

object AutoBackupRetentionManager {
    fun enforceConfiguredLimit(
        context: Context,
        reason: String,
    ): List<String> {
        val appContext = context.applicationContext
        val config = LauncherStateStore(appContext).readAutoBackupConfig()
        if (!config.enabled) {
            return BackupLibraryFiles.listLibraryArchives(appContext)
        }

        val result = BackupLibraryFiles.pruneAutoLibraryArchives(appContext, config.keepCount)
        if (result.deletedPaths.isNotEmpty()) {
            val message = buildString {
                append("自动备份库已按保留数量清理 ")
                append(result.deletedPaths.size)
                append(" 个最旧备份。")
                if (reason.isNotBlank()) {
                    append(" 来源=")
                    append(reason)
                }
            }
            RuntimeLogArchive.appendApp(appContext, message)
            LauncherStateStore(appContext).appendAppLogMessage(message)
        }
        return result.remainingPaths
    }

    fun maybePruneAfterTermuxResult(
        context: Context,
        result: TermuxCommandResult,
    ) {
        if (!isSuccessfulAutoBackupResult(result)) return
        enforceConfiguredLimit(context, reason = "termux-auto-backup")
    }

    private fun isSuccessfulAutoBackupResult(result: TermuxCommandResult): Boolean {
        if (result.exitCode != 0) return false
        val output = result.stdout
        return output.contains("==== SillyTavern backup ====") &&
            output.contains("kind=auto") &&
            output.contains("archive=")
    }
}
