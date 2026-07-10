package moe.lukoa.launcher

class LauncherProfileCoordinator(
    val pathState: LauncherPathSettingsState,
    val mirrorState: LauncherMirrorSettingsState,
    private val statusUpdate: (String, String, Boolean) -> Unit,
    private val refreshActiveProfileState: (String) -> Unit,
    private val blockIfPendingTaskExists: (String) -> Boolean,
    private val runProfileMutationPendingCommand: (PendingLauncherTask, String, Long, String) -> Unit,
    private val beginBusy: (String, Long) -> Boolean,
    private val releaseBusy: () -> Unit,
    private val isTransientStatus: (String) -> Boolean,
    private val isActionInProgress: () -> Boolean,
    private val isTavernRunning: () -> Boolean,
    private val isTavernStarting: () -> Boolean,
    private val isTermuxInstalled: () -> Boolean,
    private val isRunCommandPermissionGranted: () -> Boolean,
    private val onCommand: (String, LauncherUpdate) -> Unit,
    private val onSaveTavernMirrorConfig: (TavernMirrorConfig) -> TavernMirrorSaveResult,
    private val onSaveTavernPathConfig: (TavernPathConfig) -> TavernPathSaveResult,
    private val onRestoreDefaultTavernPath: () -> TavernPathSaveResult,
    private val onCheckTavernMirror: (TavernMirrorConfig, (TavernMirrorProbeStatus) -> Unit) -> Unit,
    private val onTavernRepoChanged: (String) -> Unit,
) {
    fun saveTavernMirrorConfig(
        repoUrl: String = mirrorState.tavernRepoInput,
        npmRegistry: String = mirrorState.npmRegistryInput,
    ) {
        val previousRepoUrl = mirrorState.config.normalizedRepoUrl
        val nextConfig = TavernMirrorConfig(
            repoUrl = repoUrl.trim(),
            npmRegistry = npmRegistry.trim(),
        )
        val result = onSaveTavernMirrorConfig(nextConfig)
        val repoChanged = result.saved && !sameRepoUrl(previousRepoUrl, result.config.normalizedRepoUrl)
        mirrorState.applySavedConfig(result.config)
        if (repoChanged) {
            onTavernRepoChanged(result.config.normalizedRepoUrl)
        }
        statusUpdate(
            if (result.saved) {
                buildString {
                    append(result.message)
                    append("\n后续安装、读取官方版本、更新和回退会使用这个源。")
                    if (repoChanged) {
                        append("\n旧版本列表已清空，请按新源重新读取官方版本。")
                    }
                }
            } else {
                result.message
            },
            "",
            result.saved,
        )
    }

    fun saveTavernPathConfig(
        path: String = pathState.pathInput,
        portText: String = pathState.portInput,
    ) {
        val safePort = LauncherPathSettingsPolicy.resolvePort(portText, pathState.config.normalizedPort)
        val nextConfig = pathState.config.withUpdatedActiveProfile(
            tavernDir = path.trim(),
            port = safePort,
        )
        val result = onSaveTavernPathConfig(nextConfig)
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "${result.config.activeProfileLabel}已保存，后续启动、停止、版本读取和备份都会使用这个目录和端口。",
            )
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun chooseDetectedTavernDirectory(path: String) {
        pathState.directoryCandidates
            .firstOrNull { it.path == path }
            ?.takeIf { !it.selectable }
            ?.let { blocked ->
                statusUpdate(blocked.reason.ifBlank { "这个目录当前不能直接分配给这个实例。" }, "", false)
                return
            }
        pathState.clearDirectoryChoice()
        val result = onSaveTavernPathConfig(
            pathState.config.withUpdatedActiveProfilePathOnly(path),
        )
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "${result.config.activeProfileLabel}已切换到检测到的目录，端口保持 ${result.config.normalizedPort} 不变。",
            )
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun restoreDefaultTavernPath() {
        val result = onRestoreDefaultTavernPath()
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已恢复${result.config.activeProfileLabel}的默认路径和默认端口。")
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun selectTavernProfile(profileId: String) {
        val result = onSaveTavernPathConfig(pathState.config.withActiveProfile(profileId))
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已切换到${result.config.activeProfileLabel}。")
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun addTavernProfile() {
        val result = onSaveTavernPathConfig(pathState.config.addSuggestedProfile())
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已新建并切换到${result.config.activeProfileLabel}。")
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun requestRemoveCurrentTavernProfile() {
        when (
            val decision = TavernProfileRemovalGuard.evaluate(
                config = pathState.config,
                tavernRunning = isTavernRunning(),
                tavernStarting = isTavernStarting(),
                actionsLocked = isActionInProgress(),
            )
        ) {
            is TavernProfileRemovalDecision.Blocked -> statusUpdate(decision.message, "", false)
            is TavernProfileRemovalDecision.Confirm -> {
                pathState.pendingRemovalConfirmation = decision.confirmation
            }
        }
    }

    fun requestTavernPathMigration(
        targetPath: String,
        targetKind: TavernProfileMigrationTargetKind,
    ): Boolean {
        return when (
            val decision = TavernProfileMigrationGuard.evaluate(
                config = pathState.config,
                targetPath = targetPath,
                targetKind = targetKind,
                tavernRunning = isTavernRunning(),
                tavernStarting = isTavernStarting(),
                actionsLocked = isActionInProgress(),
            )
        ) {
            is TavernProfileMigrationDecision.Blocked -> {
                statusUpdate(decision.message, "", false)
                false
            }

            is TavernProfileMigrationDecision.Confirm -> {
                pathState.pendingMigrationConfirmation = decision.confirmation
                true
            }
        }
    }

    fun requestMigrateToManagedTavernPath() {
        val pathInfo = TavernProfilePathPolicy.describe(pathState.config.activeProfile)
        requestTavernPathMigration(
            targetPath = pathInfo.launcherManagedDefaultPath,
            targetKind = TavernProfileMigrationTargetKind.LauncherManaged,
        )
    }

    fun requestMigrateToTraditionalTavernPath() {
        val pathInfo = TavernProfilePathPolicy.describe(pathState.config.activeProfile)
        requestTavernPathMigration(
            targetPath = pathInfo.traditionalDefaultPath,
            targetKind = TavernProfileMigrationTargetKind.TraditionalDefault,
        )
    }

    fun openCustomTavernPathMigrationDialog() {
        pathState.customMigrationPathInput = ""
        pathState.showCustomMigrationDialog = true
    }

    fun confirmCustomTavernPathMigrationDialog() {
        if (
            requestTavernPathMigration(
                pathState.customMigrationPathInput,
                TavernProfileMigrationTargetKind.Custom,
            )
        ) {
            pathState.showCustomMigrationDialog = false
        }
    }

    fun confirmMigrateCurrentTavernPath() {
        val confirmation = pathState.pendingMigrationConfirmation ?: return
        if (blockIfPendingTaskExists("迁移酒馆目录")) return
        pathState.pendingMigrationConfirmation = null
        val encodedTargetPath = TavernProfileMigrationCommandCodec.encode(confirmation.targetPath)
        runProfileMutationPendingCommand(
            PendingLauncherTask(
                kind = PendingLauncherTaskKind.MigrateTavernDirectory,
                commandName = "tavern-migrate-dir",
                detail = "把${confirmation.profileName}迁移到${confirmation.targetPath}",
                startedAtMillis = System.currentTimeMillis(),
                profileId = confirmation.profileId,
                targetPath = confirmation.targetPath,
            ),
            "迁移酒馆目录",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-migrate-dir"),
            LauncherCommandCodec.encode("tavern-migrate-dir", encodedTargetPath),
        )
    }

    fun confirmRemoveCurrentTavernProfile() {
        val confirmation = pathState.pendingRemovalConfirmation ?: return
        pathState.pendingRemovalConfirmation = null
        if (confirmation.deletesProfileDirectory) {
            if (blockIfPendingTaskExists("删除实例")) {
                pathState.pendingRemovalConfirmation = confirmation
                return
            }
            runProfileMutationPendingCommand(
                PendingLauncherTask(
                    kind = PendingLauncherTaskKind.RemoveManagedProfileDirectory,
                    commandName = "tavern-delete-managed-profile-dir",
                    detail = "删除${confirmation.profileName}的托管目录",
                    startedAtMillis = System.currentTimeMillis(),
                    profileId = confirmation.profileId,
                    targetPath = confirmation.deletedDirectoryPath,
                ),
                "删除分身实例托管目录",
                TermuxCommandTimeoutPolicy.operationLockMillis("tavern-delete-managed-profile-dir"),
                "tavern-delete-managed-profile-dir",
            )
            return
        }
        val result = onSaveTavernPathConfig(pathState.config.removeProfile(confirmation.profileId))
        pathState.applySaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "已移除${confirmation.profileName}，现在切换到${result.config.activeProfileLabel}继续管理。原目录和备份都还保留着。",
            )
        } else {
            statusUpdate(result.message, "", false)
        }
    }

    fun useOfficialTavernMirror() {
        saveTavernMirrorConfig(
            repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
            npmRegistry = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
        )
    }

    fun useGithubProxyTavernMirror() {
        saveTavernMirrorConfig(
            repoUrl = TavernMirrorDefaults.GITHUB_PROXY_REPO,
            npmRegistry = TavernMirrorDefaults.NPMMIRROR_REGISTRY,
        )
    }

    fun useNpmMirrorOnly() {
        saveTavernMirrorConfig(
            repoUrl = mirrorState.tavernRepoInput,
            npmRegistry = TavernMirrorDefaults.NPMMIRROR_REGISTRY,
        )
    }

    fun checkTavernMirror() {
        if (isActionInProgress()) {
            statusUpdate("正在处理，完成后再检测镜像源。", "", false)
            return
        }
        mirrorState.probeStatus = TavernMirrorProbeStatus.checking(mirrorState.config)
        onCheckTavernMirror(mirrorState.config) { result ->
            mirrorState.probeStatus = result
            val ok = result.overallLevel != MirrorProbeLevel.Failed
            val message = when (result.overallLevel) {
                MirrorProbeLevel.Healthy -> "镜像源检测完成，当前源可用。"
                MirrorProbeLevel.Warning -> "镜像源检测完成，有提醒项，安装前建议看一眼。"
                MirrorProbeLevel.Failed -> "镜像源检测失败，请先换源或检查网络。"
                MirrorProbeLevel.Unknown -> "镜像源还没检测完成。"
            }
            statusUpdate(message, "", ok)
        }
    }

    fun readTermuxPackageMirrorStatus() {
        if (!canRunTermuxMirrorAction("读取 Termux 包源")) return
        if (!beginBusy("读取 Termux 包源", TermuxCommandTimeoutPolicy.operationLockMillis("termux-repo-status"))) return
        statusUpdate("正在读取当前 Termux 包源。", "", false)
        onCommand("termux-repo-status") { newStatus, termuxOutput, ok ->
            statusUpdate(newStatus, termuxOutput, ok)
            if (!isTransientStatus(newStatus)) releaseBusy()
        }
    }

    fun applyCustomTermuxPackageMirror() {
        val url = mirrorState.customTermuxRepoInput.trim().trimEnd('/')
        TavernMirrorValidator.validateTermuxAptUrl(url)?.let { reason ->
            statusUpdate("自定义 Termux 包源无效：$reason", "", false)
            return
        }
        if (!canRunTermuxMirrorAction("切换 Termux 包源")) return
        if (!beginBusy("切换自定义 Termux 包源", TermuxCommandTimeoutPolicy.operationLockMillis("termux-repo-custom"))) return
        mirrorState.customTermuxRepoInput = url
        statusUpdate("正在切换自定义 Termux 包源。", "", false)
        onCommand("termux-repo-custom::$url") { newStatus, termuxOutput, ok ->
            statusUpdate(newStatus, termuxOutput, ok)
            if (!isTransientStatus(newStatus)) releaseBusy()
        }
    }

    private fun canRunTermuxMirrorAction(actionName: String): Boolean {
        if (isActionInProgress()) {
            statusUpdate("正在处理，完成后再$actionName。", "", false)
            return false
        }
        if (!isTermuxInstalled()) {
            statusUpdate("先安装 Termux。", "", false)
            return false
        }
        if (!isRunCommandPermissionGranted()) {
            statusUpdate("先打开 Termux 调用权限。", "", false)
            return false
        }
        return true
    }
}
