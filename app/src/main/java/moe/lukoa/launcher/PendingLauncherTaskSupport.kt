package moe.lukoa.launcher

data class PendingTaskRefreshTargets(
    val backupList: Boolean = false,
    val startupState: Boolean = false,
)

data class PendingTaskResolveResult(
    val ok: Boolean,
    val message: String,
    val refreshTargets: PendingTaskRefreshTargets = PendingTaskRefreshTargets(),
)

object PendingLauncherTaskSupport {
    fun selectedVersionTargetLabel(selectedVersion: TavernVersionChoice?): String {
        return selectedVersion?.label?.trim().orEmpty()
            .ifBlank { selectedVersion?.target?.trim().orEmpty() }
    }

    fun buildSafetyBackupLabel(
        operationPrefix: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        return "安全备份-$operationPrefix-$nowMillis"
    }

    fun latestResult(
        task: PendingLauncherTask,
        latestResult: TermuxResultDisplay?,
    ): TermuxResultDisplay? {
        return latestResult?.takeIf { it.command == task.commandName }
    }

    fun defaultTab(task: PendingLauncherTask): LauncherTab {
        return when (task.kind) {
            PendingLauncherTaskKind.ManualBackup,
            PendingLauncherTaskKind.RestoreBackup -> LauncherTab.Backup

            PendingLauncherTaskKind.InstallTavern,
            PendingLauncherTaskKind.UpdateTavern,
            PendingLauncherTaskKind.RollbackTavern -> LauncherTab.Version
        }
    }

    fun waitingRefreshTargets(task: PendingLauncherTask): PendingTaskRefreshTargets {
        return if (isSafetyBackupStage(task)) {
            PendingTaskRefreshTargets()
        } else {
            refreshTargetsForKind(task.kind)
        }
    }

    fun waitingMessage(task: PendingLauncherTask): String {
        return if (isSafetyBackupStage(task)) {
            "还没读到上次${task.title}前自动安全备份的最终回传。如果 Termux 还在跑，稍后再点一次“继续检查”。"
        } else {
            "还没读到上次${task.title}的最终回传。已先帮你重新检查相关状态；如果 Termux 还在跑，稍后再点一次“继续检查”。"
        }
    }

    fun conflictMessage(
        task: PendingLauncherTask,
        actionName: String,
    ): String {
        return "检测到上次${task.title}还没收尾。为避免把${actionName}和旧任务状态混在一起，请先继续检查或放弃那次任务。"
    }

    fun resolveLatestResult(
        task: PendingLauncherTask,
        latest: TermuxResultDisplay,
    ): PendingTaskResolveResult {
        if (isSafetyBackupStage(task)) {
            val backupPath = BackupHistoryReducer.extractCreatedBackupArchive(latest.output, latest.ok).orEmpty()
            return PendingTaskResolveResult(
                ok = latest.ok,
                message = when {
                    latest.ok && backupPath.isNotBlank() -> {
                        "已继续检查上次${task.title}：自动安全备份已经生成。\n安全备份在：$backupPath\n后续${task.title}还没确认继续开始，请重新点一次。"
                    }

                    latest.ok -> {
                        "已继续检查上次${task.title}：安全备份命令返回成功，但没有读到备份路径。为稳妥起见，请重新点一次。"
                    }

                    else -> {
                        "已继续检查上次${task.title}：自动安全备份失败，这次风险操作没有继续执行。"
                    }
                },
                refreshTargets = PendingTaskRefreshTargets(
                    backupList = latest.ok && backupPath.isNotBlank(),
                    startupState = false,
                ),
            )
        }

        val followUp = when {
            task.safetyBackupPath.isNotBlank() && latest.ok -> "\n自动安全备份已保留：${task.safetyBackupPath}"
            task.safetyBackupPath.isNotBlank() -> "\n自动安全备份还在：${task.safetyBackupPath}"
            else -> ""
        }
        return PendingTaskResolveResult(
            ok = latest.ok,
            message = "已继续检查上次${task.title}，已经收到结果。$followUp",
            refreshTargets = refreshTargetsForKind(task.kind),
        )
    }

    private fun isSafetyBackupStage(task: PendingLauncherTask): Boolean {
        return task.commandName == "tavern-backup" &&
            (
                task.kind == PendingLauncherTaskKind.UpdateTavern ||
                    task.kind == PendingLauncherTaskKind.RollbackTavern
                )
    }

    private fun refreshTargetsForKind(kind: PendingLauncherTaskKind): PendingTaskRefreshTargets {
        return when (kind) {
            PendingLauncherTaskKind.ManualBackup -> PendingTaskRefreshTargets(backupList = true)
            PendingLauncherTaskKind.RestoreBackup -> PendingTaskRefreshTargets(
                backupList = true,
                startupState = true,
            )

            PendingLauncherTaskKind.InstallTavern,
            PendingLauncherTaskKind.UpdateTavern,
            PendingLauncherTaskKind.RollbackTavern -> PendingTaskRefreshTargets(startupState = true)
        }
    }
}
