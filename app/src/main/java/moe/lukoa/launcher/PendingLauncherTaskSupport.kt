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

data class PendingTaskUiVisibility(
    val showRecoveryUi: Boolean,
    val showBusyUi: Boolean,
)

object PendingLauncherTaskSupport {
    fun taskCenterStatus(
        task: PendingLauncherTask?,
        activeLockLabel: String?,
    ): String {
        return when {
            task != null && !activeLockLabel.isNullOrBlank() -> "${task.title}进行中"
            task != null -> "${task.title}待确认"
            !activeLockLabel.isNullOrBlank() -> activeLockLabel
            else -> "当前空闲"
        }
    }

    fun recentTaskResults(
        recentResults: List<TermuxResultDisplay>,
        limit: Int = 4,
    ): List<TermuxResultDisplay> {
        if (limit <= 0) return emptyList()
        return recentResults.asSequence()
            .filter { TASK_RESULT_TITLES.containsKey(it.command) }
            .sortedByDescending { it.timeMillis }
            .distinctBy { it.key }
            .take(limit)
            .toList()
    }

    fun taskResultTitle(result: TermuxResultDisplay): String {
        return TASK_RESULT_TITLES[result.command] ?: result.command.ifBlank { "后台任务" }
    }

    fun uiVisibility(
        task: PendingLauncherTask?,
        operationActive: Boolean,
        operationRestored: Boolean,
    ): PendingTaskUiVisibility {
        val showRecoveryUi = task != null && (!operationActive || operationRestored)
        return PendingTaskUiVisibility(
            showRecoveryUi = showRecoveryUi,
            showBusyUi = operationActive && !showRecoveryUi,
        )
    }

    fun shouldShowRecoveryUi(
        task: PendingLauncherTask?,
        operationActive: Boolean,
        operationRestored: Boolean = false,
    ): Boolean = uiVisibility(
        task = task,
        operationActive = operationActive,
        operationRestored = operationRestored,
    ).showRecoveryUi

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
        recentResults: List<TermuxResultDisplay>,
    ): TermuxResultDisplay? {
        return recentResults.firstOrNull {
            it.command == task.commandName &&
                (task.startedAtMillis <= 0L || it.timeMillis >= task.startedAtMillis) &&
                matchesPendingTaskProfile(task, it)
        }
    }

    fun defaultTab(task: PendingLauncherTask): LauncherTab {
        return when (task.kind) {
            PendingLauncherTaskKind.ManualBackup,
            PendingLauncherTaskKind.RestoreBackup -> LauncherTab.Backup

            PendingLauncherTaskKind.InstallTavern,
            PendingLauncherTaskKind.UpdateTavern,
            PendingLauncherTaskKind.RollbackTavern -> LauncherTab.Version

            PendingLauncherTaskKind.MigrateTavernDirectory,
            PendingLauncherTaskKind.RemoveManagedProfileDirectory,
            PendingLauncherTaskKind.CloneTavernProfile -> LauncherTab.Settings
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
            "还没读到上次${task.title}前自动安全备份的最终结果。如果 Termux 还在运行，稍后再点一次“检查上次操作的结果”。"
        } else {
            "还没读到上次${task.title}的最终结果。已经先刷新相关状态；如果 Termux 还在运行，稍后再点一次“检查上次操作的结果”。"
        }
    }

    fun conflictMessage(
        task: PendingLauncherTask,
        actionName: String,
    ): String {
        return "上次${task.title}的结果还没确认。为了避免把${actionName}和旧任务混在一起，请先检查上次操作的结果，或确认不再跟踪。"
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
                        "已继续检查上次${task.title}：安全备份命令返回成功，但没读到备份路径。为稳妥起见，请重新点一次。"
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

    private fun matchesPendingTaskProfile(
        task: PendingLauncherTask,
        latest: TermuxResultDisplay,
    ): Boolean {
        return TavernTermuxResultProfileScope.matches(
            profileId = task.profileId,
            result = latest,
            requireMetadata = true,
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
            PendingLauncherTaskKind.RollbackTavern,
            PendingLauncherTaskKind.MigrateTavernDirectory,
            PendingLauncherTaskKind.RemoveManagedProfileDirectory,
            PendingLauncherTaskKind.CloneTavernProfile -> {
                PendingTaskRefreshTargets(startupState = true)
            }
        }
    }

    private val TASK_RESULT_TITLES = mapOf(
        "tavern-install" to "安装酒馆",
        "tavern-update" to "更新酒馆",
        "tavern-rollback" to "回退酒馆",
        "tavern-backup" to "创建酒馆备份",
        "tavern-restore" to "应用酒馆备份",
        "tavern-migrate-dir" to "迁移酒馆目录",
        "tavern-delete-managed-profile-dir" to "删除分身实例目录",
        "tavern-clone-profile-dir" to "克隆酒馆实例",
    )
}
