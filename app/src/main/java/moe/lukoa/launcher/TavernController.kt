package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TermuxResultDisplay(
    val key: String,
    val command: String,
    val output: String,
    val ok: Boolean,
    val executionId: Int = 0,
    val nonce: String? = null,
    val timeMillis: Long = 0L,
    val profileId: String = "",
    val runtimeStateDir: String = "",
)

class TavernController(
    private val context: Context,
    private val runner: TermuxCommandRunner,
) {
    private val resultRepository by lazy { TermuxResultRepositoryProvider.get(context) }

    fun wakeTermuxThenReturn(scope: CoroutineScope, returnDelayMs: Long): Boolean {
        // A background no-op starts Termux without showing it. Opening the Termux screen while it has
        // no session makes Termux create an empty one, so only do that if the quiet path fails.
        if (runner.wakeTermuxQuietly()) return true
        val woke = runner.wakeTermux()
        if (woke) {
            scope.launch {
                delay(returnDelayMs.coerceIn(300L, 2_000L))
                runner.requestReturnToLauncher()
                delay(1200)
                returnToLauncher()
            }
        }
        return woke
    }

    fun handleSelftest(scope: CoroutineScope, update: LauncherUpdate) {
        val startTime = System.currentTimeMillis()
        val dispatch = runner.runSelftest()
        waitForNonceCommand(
            scope = scope,
            dispatch = dispatch,
            startTime = startTime,
            waitingMessage = "已发送自检，等待 Termux 返回。",
            successMessage = "Termux 调用成功。",
            failureMessage = "Termux 自检失败。",
            timeoutMessage = "未收到 Termux 返回。",
            update = update,
        )
    }

    fun handleInstallScript(scope: CoroutineScope, scriptText: String, update: LauncherUpdate) {
        val startTime = System.currentTimeMillis()
        val dispatch = runner.installOrRepairScript(scriptText)
        waitForNonceCommand(
            scope = scope,
            dispatch = dispatch,
            startTime = startTime,
            waitingMessage = "正在安装启动脚本。",
            successMessage = "脚本已安装，Termux 调用成功。",
            failureMessage = "脚本安装失败。",
            timeoutMessage = "安装命令已发送，但没收到返回。",
            update = update,
        )
    }

    fun handleCommand(scope: CoroutineScope, command: String, update: LauncherUpdate) {
        val parsed = LauncherCommandCodec.decode(command)
        val startTime = System.currentTimeMillis()
        val dispatch = when (parsed.name) {
            "log" -> runner.runLogSnapshot()
            "status" -> runner.runStatusSnapshot()
            "stop" -> runner.runStopTavern()
            "restart" -> runner.runRestartTavern()
            "tavern-chat-guard-status" -> runner.runTavernChatGuardStatus()
            "tavern-chat-guard-set" -> runner.runTavernChatGuardSet(parsed.argument)
            "tavern-chat-guard-clear-history" -> runner.runTavernChatGuardClearHistory()
            "tavern-config-read" -> runner.runTavernConfigRead()
            "tavern-config-set" -> runner.runTavernConfigSet(parsed.argument)
            "tavern-storage-scan" -> runner.runTavernStorageScan()
            "tavern-storage-clean" -> runner.runTavernStorageClean(parsed.argument)
            "tavern-server-plugins-list" -> runner.runTavernServerPluginsList()
            "tavern-server-plugins-install" -> runner.runTavernServerPluginInstall(parsed.argument)
            "tavern-server-plugins-update" -> runner.runTavernServerPluginUpdate(parsed.argument)
            "tavern-server-plugins-delete" -> runner.runTavernServerPluginDelete(parsed.argument)
            "tavern-git-stash-list" -> runner.runTavernGitStashList()
            "tavern-git-stash-save" -> runner.runTavernGitStashSave()
            "tavern-git-stash-apply" -> runner.runTavernGitStashApply(parsed.argument)
            "tavern-git-stash-drop" -> runner.runTavernGitStashDrop(parsed.argument)
            "tavern-quick-import-paths" -> runner.runTavernQuickImportPaths(parsed.argument)
            "tavern-quick-import" -> runner.runTavernQuickImport(parsed.argument)
            "tavern-quick-import-batch" -> runner.runTavernQuickImportBatch(parsed.argument)
            "tavern-proxy-test" -> runner.runTavernProxyTest(parsed.argument)
            "tavern-force-cleanup" -> runner.runForceCleanupTavern()
            "tavern-version" -> runner.runTavernVersion()
            "tavern-version-startup" -> runner.runTavernVersion()
            "tavern-doctor" -> runner.runTavernDoctor()
            "tavern-repair-dependencies" -> runner.runTavernRepairDependencies()
            "tavern-reset-theme" -> runner.runTavernResetTheme()
            "tavern-node-memory" -> runner.runTavernNodeMemory(parsed.argument)
            "tavern-node-memory-status" -> runner.runTavernNodeMemoryStatus()
            "tavern-node-memory-reset" -> runner.runTavernNodeMemoryReset()
            "tavern-upload-limit-status" -> runner.runTavernUploadLimitStatus()
            "tavern-upload-limit-set" -> runner.runTavernUploadLimitSet(parsed.argument)
            "tavern-upload-limit-reset" -> runner.runTavernUploadLimitReset()
            "tavern-users-list" -> runner.runTavernUsersList()
            "tavern-user-create" -> runner.runTavernUserCreate(parsed.argument)
            "tavern-user-delete" -> runner.runTavernUserDelete(parsed.argument)
            "tavern-extensions-list" -> runner.runTavernExtensionsList()
            "tavern-extensions-delete" -> runner.runTavernExtensionDelete(parsed.argument)
            "tavern-extensions-disable" -> runner.runTavernExtensionSetEnabled(parsed.argument, false)
            "tavern-extensions-enable" -> runner.runTavernExtensionSetEnabled(parsed.argument, true)
            "tavern-extensions-install" -> runner.runTavernExtensionInstall(parsed.argument)
            "tavern-extensions-check-updates" -> runner.runTavernExtensionCheckUpdates()
            "tavern-extensions-update" -> runner.runTavernExtensionUpdate(parsed.argument)
            "tavern-extensions-update-force" -> runner.runTavernExtensionUpdate(parsed.argument, force = true)
            "tavern-extensions-update-all" -> runner.runTavernExtensionUpdateAll()
            "tavern-extensions-rollback" -> runner.runTavernExtensionRollback(parsed.argument)
            "tavern-official-versions" -> runner.runTavernOfficialVersions()
            "termux-storage-permission" -> runner.requestTermuxStoragePermission()
            "termux-repo-status" -> runner.runTermuxPackageMirrorStatus()
            "termux-repo" -> runner.runTermuxPackageMirror(parsed.argument)
            "termux-repo-custom" -> runner.runTermuxPackageMirrorCustom(parsed.argument)
            "termux-bootstrap" -> runner.runTermuxBootstrap(parsed.argument)
            "tavern-install" -> runner.runTavernInstall(parsed.argument)
            "tavern-update" -> runner.runTavernUpdate(parsed.argument)
            "tavern-rollback" -> runner.runTavernRollback(parsed.argument)
            "tavern-backup",
            "tavern-backup-manual" -> runner.runTavernBackup("manual", null, parsed.argument)
            "tavern-backup-manual-data" -> runner.runTavernBackup("manual-data", null, parsed.argument)
            "tavern-backup-auto" -> runner.runTavernBackup("auto", parsed.argument?.toIntOrNull())
            "tavern-backup-list" -> runner.runTavernBackupList()
            "tavern-backup-delete" -> runner.runTavernBackupDelete(parsed.argument)
            "tavern-backup-export" -> runner.runTavernBackupExport(parsed.argument)
            "tavern-backup-export-to" -> runner.runTavernBackupExportTo(parsed.argument)
            "tavern-backup-copy" -> runner.runTavernBackupCopy(parsed.argument)
            "tavern-backup-import" -> runner.runTavernBackupImport(parsed.argument)
            "tavern-backup-rename" -> runner.runTavernBackupRename(parsed.argument)
            "tavern-restore" -> runner.runTavernRestore(parsed.argument)
            "tavern-migrate-dir" -> runner.runTavernMigrateDirectory(parsed.argument)
            "tavern-delete-managed-profile-dir" -> runner.runDeleteManagedProfileDirectory()
            "tavern-clone-profile-dir" -> runner.runCloneManagedProfileDirectory(parsed.argument)
            else -> runner.runAction(parsed.name)
        }
        update(dispatchMessage(parsed.name, dispatch), "", false)
        if (!dispatch.sent) return

        if (dispatch.presentation == TermuxCommandPresentation.Foreground) {
            scope.launch {
                delay(FOREGROUND_SESSION_SETTLE_MS)
                runner.wakeTermux()
            }
        }

        scope.launch {
            val result = waitForResult(
                executionId = dispatch.executionId,
                startTime = startTime,
                nonce = dispatch.nonce,
                expectedCommand = dispatch.displayCommand,
                timeoutMillis = TermuxCommandTimeoutPolicy.timeoutMillis(parsed.name),
            )
            if (result != null) {
                val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
                update(
                    resultMessage(parsed.name, ok, result),
                    rawResultOutput(result).ifBlank { TermuxOutputDisplayFormatter.format(result) },
                    ok,
                )
                if (ok && (parsed.name == "start" || parsed.name == "restart")) {
                    delay(600)
                    val openResult = runner.openTavern()
                    if (!openResult.sent) {
                        update(openResult.message, "", false)
                    }
                }
            } else {
                update("命令已发送，但没收到返回。", "", false)
            }
        }
    }

    fun refreshLogSnapshot(
        scope: CoroutineScope,
        reason: LiveLogRefreshReason,
        updateTermuxLog: (String, Boolean) -> Unit,
    ) {
        val startTime = System.currentTimeMillis()
        val dispatch = runner.runLiveLogDeltaSnapshot(reason)
        if (!dispatch.sent) {
            updateTermuxLog(dispatch.message, false)
            return
        }

        scope.launch {
            val result = waitForResult(
                executionId = dispatch.executionId,
                startTime = startTime,
                nonce = dispatch.nonce,
                expectedCommand = dispatch.displayCommand,
                timeoutMillis = TermuxCommandTimeoutPolicy.LOG_REFRESH_TIMEOUT_MS,
            )
            if (result != null) {
                val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
                updateTermuxLog(rawResultOutput(result).ifBlank { TermuxOutputDisplayFormatter.format(result) }, ok)
            } else {
                updateTermuxLog("日志同步暂未收到返回。", false)
            }
        }
    }

    fun handleLazyCommand(scope: CoroutineScope, command: String, update: LauncherUpdate) {
        update("正在执行：$command。", "", false)
        handleCommand(scope, command, update)
    }

    fun handleForegroundStart(scope: CoroutineScope, update: LauncherUpdate) {
        handleForegroundConsoleDispatch(
            scope = scope,
            initialMessage = "正在打开 Termux 酒馆窗口。",
            returnedMessage = "启动命令已返回。",
            failedMessage = "启动酒馆失败。",
            sentMessage = "启动命令已发送。",
            wakeFallbackMessage = "启动命令已发送，但系统没有自动打开 Termux。请手动打开 Termux 查看启动日志，先不要重复点启动。",
            dispatchAction = { runner.runForegroundTavernConsole() },
            update = update,
        )
    }

    fun handleForegroundRestart(scope: CoroutineScope, update: LauncherUpdate) {
        handleForegroundConsoleDispatch(
            scope = scope,
            initialMessage = "正在停止旧实例并在 Termux 前台重启酒馆。",
            returnedMessage = "重启命令已返回。",
            failedMessage = "重启酒馆失败。",
            sentMessage = "重启命令已发送。",
            wakeFallbackMessage = "重启命令已发送，但系统没有自动打开 Termux。请手动打开 Termux 查看启动日志，先不要重复点重启。",
            dispatchAction = { runner.runForegroundTavernRestart() },
            update = update,
        )
    }

    private fun handleForegroundConsoleDispatch(
        scope: CoroutineScope,
        initialMessage: String,
        returnedMessage: String,
        failedMessage: String,
        sentMessage: String,
        wakeFallbackMessage: String,
        dispatchAction: () -> CommandDispatch,
        update: LauncherUpdate,
    ) {
        update(initialMessage, "", false)
        scope.launch {
            val startTime = System.currentTimeMillis()
            val dispatch = withContext(Dispatchers.Default) {
                dispatchAction()
            }
            if (!dispatch.sent) {
                update(dispatch.message, "", false)
                return@launch
            }
            // Wait through the foreground session settle window while listening for an immediate
            // rejection or fast crash (e.g. external-apps blocked, port conflict, missing node).
            // In the normal case the foreground session stays open, so awaitResult returns null
            // after FOREGROUND_SESSION_SETTLE_MS and we bring Termux to the front.
            val immediateResult = waitForResult(
                executionId = dispatch.executionId,
                startTime = startTime,
                nonce = dispatch.nonce,
                expectedCommand = dispatch.displayCommand,
                timeoutMillis = FOREGROUND_SESSION_SETTLE_MS,
            )
            if (immediateResult != null) {
                val ok = immediateResult.isStructurallyValid &&
                    !immediateResult.hasInternalError &&
                    immediateResult.exitCode == 0
                val output = rawResultOutput(immediateResult).ifBlank {
                    TermuxOutputDisplayFormatter.format(immediateResult)
                }
                val message = when {
                    ok -> returnedMessage
                    TermuxPermissionSignals.externalAppsBlocked(output) ->
                        "Termux 外部调用未开启。请复制权限命令到 Termux 执行。"
                    else -> failedMessage
                }
                update(message, output, ok)
                return@launch
            }
            val woke = runner.wakeTermux()
            val message = if (woke) sentMessage else wakeFallbackMessage
            update(message, "", true)
        }
    }

    fun openTavern(update: LauncherUpdate, forceOpenUrl: Boolean = false) {
        val result = runner.openTavern(forceOpenUrl = forceOpenUrl)
        update(result.message, "", result.sent)
    }

    fun latestTermuxResultDisplay(): TermuxResultDisplay? {
        return TermuxResultStore.latest(context)?.toDisplay()
    }

    fun recentTermuxResultDisplays(): List<TermuxResultDisplay> {
        return TermuxResultStore.recent(context).map { it.toDisplay() }
    }

    private fun TermuxCommandResult.toDisplay(): TermuxResultDisplay {
        val ok = isStructurallyValid && !hasInternalError && exitCode == 0
        val rawOutput = rawResultOutput(this)
        val output = rawOutput.ifBlank { TermuxOutputDisplayFormatter.format(this) }
        val metadata = TavernTermuxResultMetadataParser.parse(rawOutput)
        return TermuxResultDisplay(
            key = stableKey,
            command = command.ifBlank { raw.lineSequence().firstOrNull().orEmpty().ifBlank { "Termux" } },
            output = output,
            ok = ok,
            executionId = executionId,
            nonce = nonce,
            timeMillis = timeMillis,
            profileId = metadata.profileId,
            runtimeStateDir = metadata.runtimeStateDir,
        )
    }

    fun exportLog(
        scope: CoroutineScope,
        state: LauncherUiState,
        mode: ExportLogMode,
        update: LauncherUpdate,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    SessionLogExporter.export(
                        context = context,
                        state = state,
                        mode = mode,
                    )
                }
            }
            result.onSuccess { file ->
                runCatching {
                    SharedFileSender.shareTextFile(context, file, "导出运行日志", "露科亚启动器运行日志")
                }.onSuccess {
                    val scopeText = when (mode) {
                        ExportLogMode.TermuxOnly -> "酒馆运行日志"
                        ExportLogMode.AppOnly -> "App 操作反馈"
                        ExportLogMode.Both -> "酒馆运行日志和 App 操作反馈"
                    }
                    update("已生成${scopeText}导出文件：${file.name}", "", true)
                }.onFailure { error ->
                    update("导出日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
                }
            }.onFailure { error ->
                update("导出日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
            }
        }
    }

    fun exportDiagnostic(scope: CoroutineScope, snapshot: DiagnosticSnapshot, update: LauncherUpdate) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DiagnosticLogExporter.export(context, snapshot) }
            }
            result.onSuccess { file ->
                runCatching {
                    SharedFileSender.shareTextFile(context, file, "导出诊断日志", "露科亚启动器诊断日志")
                }.onSuccess {
                    update("已生成诊断日志：${file.name}", "", true)
                }.onFailure { error ->
                    update("导出诊断日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
                }
            }.onFailure { error ->
                update("导出诊断日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
            }
        }
    }

    fun exportBackup(state: LauncherUiState, update: LauncherUpdate) {
        try {
            val file = VersionBackupManager.createBackup(context, state)
            SharedFileSender.shareTextFile(context, file, "导出备份", "露科亚启动器备份")
            update("已生成备份文件：${file.name}", "", true)
        } catch (error: Exception) {
            update("导出备份失败：${error.message ?: error.javaClass.simpleName}", "", false)
        }
    }

    fun exportVersionReport(update: LauncherUpdate) {
        try {
            val file = VersionBackupManager.createVersionReport(context)
            SharedFileSender.shareTextFile(context, file, "导出版本信息", "露科亚启动器版本信息")
            update("已生成版本信息文件：${file.name}", "", true)
        } catch (error: Exception) {
            update("导出版本信息失败：${error.message ?: error.javaClass.simpleName}", "", false)
        }
    }

    private fun waitForNonceCommand(
        scope: CoroutineScope,
        dispatch: CommandDispatch,
        startTime: Long,
        waitingMessage: String,
        successMessage: String,
        failureMessage: String,
        timeoutMessage: String,
        update: LauncherUpdate,
    ) {
        if (!dispatch.sent || dispatch.nonce == null) {
            update(dispatch.message, "", false)
            return
        }

        val nonce = dispatch.nonce
        update("$waitingMessage\nnonce=$nonce", "", false)

        scope.launch {
            val result = waitForResult(dispatch.executionId, startTime, nonce, dispatch.displayCommand)
            if (result != null && result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0) {
                update(successMessage, rawResultOutput(result).ifBlank { result.raw }, true)
            } else if (result != null) {
                update(failureMessage, rawResultOutput(result).ifBlank { TermuxOutputDisplayFormatter.format(result) }, false)
            } else {
                update("$timeoutMessage\n请检查 Termux 权限。", "", false)
            }
        }
    }

    private fun dispatchMessage(command: String, dispatch: CommandDispatch): String {
        if (!dispatch.sent) return dispatch.message
        return when (command) {
            "status" -> "正在查询酒馆状态。"
            "log" -> "正在读取酒馆运行日志。"
            "stop" -> "停止命令已发送到 Termux。"
            "restart" -> "正在后台重启酒馆，就绪后会自动打开浏览器。"
            "tavern-chat-guard-status" -> "正在读取聊天守护状态。"
            "tavern-chat-guard-set" -> "正在保存聊天守护设置。"
            "tavern-chat-guard-clear-history" -> "正在清空救回记录列表。"
            "tavern-config-read" -> "正在读取酒馆 config.yaml 配置。"
            "tavern-config-set" -> "正在备份并保存酒馆 config.yaml 配置。"
            "tavern-storage-scan" -> "正在扫描当前实例的磁盘占用。"
            "tavern-storage-clean" -> "正在执行安全空间清理。"
            "tavern-server-plugins-list" -> "正在读取当前酒馆服务器插件。"
            "tavern-server-plugins-install" -> "正在下载并安装服务器插件。"
            "tavern-server-plugins-update" -> "正在更新服务器插件。"
            "tavern-server-plugins-delete" -> "正在删除服务器插件。"
            "tavern-git-stash-list" -> "正在读取本地暂存记录。"
            "tavern-git-stash-save" -> "正在把本地程序改动收进暂存区。"
            "tavern-git-stash-apply" -> "正在恢复暂存的本地改动。"
            "tavern-git-stash-drop" -> "正在删除选中的暂存记录。"
            "tavern-quick-import-paths" -> "正在解析酒馆常用数据目录。"
            "tavern-quick-import" -> "正在把文件导入到酒馆目录。"
            "tavern-quick-import-batch" -> "正在批量把文件导入到酒馆目录。"
            "tavern-proxy-test" -> "正在测试代理连通性。"
            "tavern-extensions-update-all" -> "正在批量更新可安全更新的酒馆扩展（自动跳过本地改动）。"
            "tavern-force-cleanup" -> "强制清理命令已发送到 Termux。"
            "start" -> "正在后台静默启动酒馆，就绪后会自动打开浏览器。"
            "tavern-version" -> "正在读取酒馆版本。"
            "tavern-version-startup" -> "正在检测酒馆版本。"
            "tavern-doctor" -> "正在后台体检当前环境。"
            "tavern-repair-dependencies" -> "正在后台安全重装酒馆依赖。"
            "tavern-reset-theme" -> "正在后台查找并重置当前用户的网页主题。"
            "tavern-node-memory" -> "正在后台设置 Node.js 内存上限。"
            "tavern-node-memory-status" -> "正在读取当前实例的 Node.js 内存上限。"
            "tavern-node-memory-reset" -> "正在恢复 Node.js 默认内存设置。"
            "tavern-upload-limit-status" -> "正在后台识别当前聊天记录上传限制。"
            "tavern-upload-limit-set" -> "正在后台修改聊天记录上传限制。"
            "tavern-upload-limit-reset" -> "正在后台恢复当前酒馆版本的默认聊天文件大小。"
            "tavern-users-list" -> "正在读取当前酒馆用户。"
            "tavern-user-create" -> "正在创建酒馆用户。"
            "tavern-user-delete" -> "正在删除用户账户并保留数据目录。"
            "tavern-extensions-list" -> "正在读取当前酒馆扩展。"
            "tavern-extensions-delete" -> "正在删除选中的酒馆扩展。"
            "tavern-extensions-disable" -> "正在停用选中的酒馆扩展。"
            "tavern-extensions-enable" -> "正在启用选中的酒馆扩展。"
            "tavern-extensions-install" -> "正在下载并校验酒馆扩展。"
            "tavern-extensions-check-updates" -> "正在检查酒馆扩展更新。"
            "tavern-official-versions" -> "正在读取官方版本列表。"
            "termux-storage-permission" -> "正在请求 Termux 存储权限。"
            "termux-repo-status" -> "正在读取当前 Termux 包源。"
            "termux-repo" -> "正在切换 Termux 包源，Termux 前台会显示进度。"
            "termux-repo-custom" -> "正在切换自定义 Termux 包源。"
            "termux-bootstrap" -> "正在准备 Termux 环境，Termux 前台会显示进度。"
            "tavern-install" -> "正在安装酒馆，Termux 前台会显示进度。"
            "tavern-update" -> "正在更新酒馆源码，Termux 前台会显示进度。"
            "tavern-rollback" -> "正在回退酒馆版本，Termux 前台会显示进度。"
            "tavern-backup",
            "tavern-backup-manual" -> "正在生成备份到备份库，Termux 前台会显示进度。"
            "tavern-backup-manual-data" -> "正在后台生成轻量用户数据备份到手动备份库。"
            "tavern-backup-auto" -> "正在创建自动备份。"
            "tavern-backup-list" -> "正在读取酒馆备份目录。"
            "tavern-backup-delete" -> "正在删除酒馆备份。"
            "tavern-backup-export" -> "正在导出酒馆备份。"
            "tavern-backup-export-to" -> "正在导出到你选择的位置。"
            "tavern-backup-copy" -> "正在复制酒馆备份。"
            "tavern-backup-import" -> "正在导入酒馆备份。"
            "tavern-backup-rename" -> "正在重命名酒馆备份。"
            "tavern-restore" -> "正在应用酒馆备份，Termux 前台会显示进度。"
            "tavern-migrate-dir" -> "正在迁移当前实例的酒馆目录，Termux 前台会显示进度。"
            "tavern-delete-managed-profile-dir" -> "正在删除这个分身实例的托管目录。"
            "tavern-clone-profile-dir" -> "正在克隆当前酒馆到新的分身目录。"
            else -> dispatch.message
        }
    }

    private fun resultMessage(command: String, ok: Boolean, result: TermuxCommandResult? = null): String {
        val stdout = result?.stdout.orEmpty()
        return when (command) {
            "status" -> if (ok) "状态已刷新。" else "状态查询失败。"
            "log" -> if (ok) "酒馆运行日志已同步。" else "同步酒馆运行日志失败。"
            "stop" -> if (ok) "停止命令已返回。" else "停止酒馆失败。"
            "restart" -> if (ok) "酒馆已在后台重启并就绪。" else "重启酒馆失败。"
            "tavern-chat-guard-status" -> if (ok) "聊天守护状态已刷新。" else "读取聊天守护状态失败。"
            "tavern-chat-guard-set" -> if (ok) "聊天守护设置已保存。" else "保存聊天守护设置失败。"
            "tavern-chat-guard-clear-history" -> if (ok) "救回记录列表已清空（聊天文件未受影响）。" else "清空救回记录列表失败。"
            "tavern-config-read" -> if (ok) "酒馆核心配置已读取。" else "读取酒馆 config.yaml 配置失败。"
            "tavern-config-set" -> if (ok) "酒馆核心配置已保存并自动备份原文件，重启酒馆后生效。" else "保存酒馆配置失败，原 config.yaml 未被破坏。"
            "tavern-storage-scan" -> if (ok) "空间扫描已完成。" else "扫描磁盘占用失败。"
            "tavern-storage-clean" -> if (ok) "安全空间清理已完成，聊天和角色卡未受影响。" else "安全空间清理失败。"
            "tavern-server-plugins-list" -> if (ok) "服务器插件列表已读取。" else "读取服务器插件失败。"
            "tavern-server-plugins-install" -> if (ok) "服务器插件已安装，开启总开关并重启酒馆后生效。" else "安装服务器插件失败。"
            "tavern-server-plugins-update" -> if (ok) "服务器插件已更新，重启酒馆后生效。" else "更新服务器插件失败。"
            "tavern-server-plugins-delete" -> if (ok) "服务器插件已删除。" else "删除服务器插件失败。"
            "tavern-git-stash-list" -> if (ok) "本地暂存列表已刷新。" else "读取本地暂存列表失败。"
            "tavern-git-stash-save" -> if (ok) "本地改动已安全收进暂存区，现在可以正常更新或回退。" else "暂存本地改动失败，请先确认酒馆已停止。"
            "tavern-git-stash-apply" -> if (ok) "暂存改动已恢复到当前目录（原暂存记录仍保留）。" else "恢复暂存改动失败，可能存在版本代码冲突。"
            "tavern-git-stash-drop" -> if (ok) "选中的暂存记录已删除。" else "删除暂存记录失败。"
            "tavern-quick-import-paths" -> if (ok) "酒馆数据目录已解析。" else "解析酒馆数据目录失败。"
            "tavern-quick-import" -> if (ok) "文件已成功导入酒馆目录，刷新网页后可见。" else "导入文件到酒馆目录失败。"
            "tavern-quick-import-batch" -> if (ok) "批量文件已成功导入酒馆目录，刷新网页后可见。" else "批量导入文件到酒馆目录失败。"
            "tavern-proxy-test" -> if (ok) "代理连通性测试已完成，请查看检测结果。" else "代理连通性测试失败。"
            "tavern-extensions-update-all" -> if (ok) "批量更新扩展已完成（已自动跳过有本地改动的扩展）。" else "批量更新扩展失败。"
            "tavern-force-cleanup" -> if (ok) "强制清理已返回。" else "强制清理残留进程失败。"
            "start" -> if (ok) "酒馆已在后台启动并就绪。" else "启动酒馆失败。"
            "tavern-version" -> if (ok) "酒馆版本已读取。" else "读取酒馆版本失败。"
            "tavern-version-startup" -> if (ok) "酒馆版本已读取。" else "检测酒馆版本失败。"
            "tavern-doctor" -> if (ok) "体检已完成。" else "体检失败。"
            "tavern-repair-dependencies" -> if (ok) "酒馆依赖已修复。" else "修复酒馆依赖失败，旧依赖已尽量恢复。"
            "tavern-reset-theme" -> if (ok) "网页主题已重置。" else "没有找到可安全修改的主题设置。"
            "tavern-node-memory" -> if (ok) "Node.js 内存上限已保存。" else "设置 Node.js 内存上限失败。"
            "tavern-node-memory-status" -> if (ok) "Node.js 内存上限已读取。" else "读取 Node.js 内存上限失败。"
            "tavern-node-memory-reset" -> if (ok) "已恢复 Node.js 默认内存设置（下次启动生效）。" else "恢复 Node.js 默认内存设置失败。"
            "tavern-upload-limit-status" -> if (ok) "上传限制已检查，请查看操作反馈中的当前值。" else "无法识别当前版本的上传限制。"
            "tavern-upload-limit-set" -> if (ok) "上传限制已修改，重启酒馆后生效。" else "修改上传限制失败，源文件未被强行改动。"
            "tavern-upload-limit-reset" -> if (ok) "聊天文件大小已恢复为当前酒馆版本的默认值。" else "恢复默认聊天文件大小失败，原文件已尽量保留。"
            "tavern-users-list" -> if (ok) "酒馆用户已读取。" else "读取酒馆用户失败。"
            "tavern-user-create" -> if (ok) "酒馆用户已创建。" else "创建酒馆用户失败。"
            "tavern-user-delete" -> if (ok) "用户账户已删除，数据目录仍然保留。" else "删除用户账户失败。"
            "tavern-extensions-list" -> if (ok) "酒馆扩展已读取。" else "读取酒馆扩展失败。"
            "tavern-extensions-delete" -> if (ok) "酒馆扩展已删除。" else "删除酒馆扩展失败。"
            "tavern-extensions-disable" -> if (ok) "酒馆扩展已停用。" else "停用酒馆扩展失败，扩展目录未被删除。"
            "tavern-extensions-enable" -> if (ok) "酒馆扩展已启用。" else "启用酒馆扩展失败，停用副本仍然保留。"
            "tavern-extensions-install" -> if (ok) "酒馆扩展已安装，下次启动时生效。" else "安装酒馆扩展失败，没有覆盖已有扩展。"
            "tavern-extensions-check-updates" -> if (ok) "扩展更新状态已刷新。" else "检查扩展更新失败，没有修改扩展文件。"
            "tavern-official-versions" -> if (ok) "官方版本列表已读取。" else "读取官方版本失败。"
            "termux-storage-permission" -> if (ok) "Termux 存储权限已可用。" else "Termux 存储权限还没打开。"
            "termux-repo-status" -> if (ok) "当前 Termux 包源已读取。" else "读取 Termux 包源失败。"
            "termux-repo" -> if (ok) "Termux 包源已切换。" else "切换 Termux 包源失败。"
            "termux-repo-custom" -> if (ok) "自定义 Termux 包源已切换。" else "切换自定义 Termux 包源失败。"
            "termux-bootstrap" -> if (ok) "Termux 环境已准备好。" else "准备 Termux 环境失败。"
            "tavern-install" -> if (ok) "酒馆已安装。" else "安装酒馆失败。"
            "tavern-update" -> if (ok) "酒馆源码已更新。" else "更新酒馆失败。"
            "tavern-rollback" -> if (ok) "酒馆版本已回退。" else "回退酒馆版本失败。"
            "tavern-backup",
            "tavern-backup-manual" -> if (ok) "备份已生成到备份库。" else "生成备份失败。"
            "tavern-backup-manual-data" -> if (ok) "轻量用户数据备份已生成到手动备份库。" else "生成轻量数据备份失败。"
            "tavern-backup-auto" -> if (ok) "自动备份已创建。" else "创建自动备份失败。"
            "tavern-backup-list" -> if (ok) "酒馆备份目录已读取。" else "读取酒馆备份目录失败。"
            "tavern-backup-delete" -> if (ok) "酒馆备份已删除。" else "删除酒馆备份失败。"
            "tavern-backup-export" -> if (ok) {
                val exported = result?.stdout?.lineValue("exported.file").orEmpty()
                if (exported.isBlank()) {
                    "酒馆备份已导出到 Downloads/lukoa/exports。"
                } else {
                    "酒馆备份已导出：$exported"
                }
            } else {
                "导出酒馆备份失败。"
            }
            "tavern-backup-export-to" -> if (ok) {
                val exported = result?.stdout?.lineValue("exported.file").orEmpty()
                if (exported.isBlank()) {
                    "酒馆备份已导出。"
                } else {
                    "酒馆备份已导出：$exported"
                }
            } else {
                "导出酒馆备份失败。"
            }
            "tavern-backup-copy" -> if (ok) "酒馆备份已复制。" else "复制酒馆备份失败。"
            "tavern-backup-import" -> if (ok) "酒馆备份已导入。" else "导入酒馆备份失败。"
            "tavern-backup-rename" -> if (ok) "酒馆备份已重命名。" else "重命名酒馆备份失败。"
            "tavern-migrate-dir" -> if (ok) "酒馆目录已迁移。" else "迁移酒馆目录失败。"
            "tavern-delete-managed-profile-dir" -> if (ok) "分身实例托管目录已删除。" else "删除分身实例托管目录失败。"
            "tavern-clone-profile-dir" -> if (ok) "酒馆实例已克隆。" else "克隆酒馆实例失败，未登记新分身。"
            "tavern-restore" -> if (ok) {
                TavernRestoreAftercareMessage.successMessage(stdout)
            } else if (
                stdout.contains("termux-storage-permission", ignoreCase = true) ||
                    (
                        stdout.contains("Permission denied", ignoreCase = true) &&
                            stdout.contains("restore archive cannot be listed", ignoreCase = true)
                        )
            ) {
                "应用失败：Termux 没有存储权限。"
            } else {
                "应用酒馆备份失败。"
            }
            else -> if (ok) "操作完成。" else "操作失败。"
        }
    }

    private fun String.lineValue(key: String): String? {
        return lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun waitForResult(
        executionId: Int,
        startTime: Long,
        nonce: String?,
        expectedCommand: String,
        timeoutMillis: Long = TermuxCommandTimeoutPolicy.DEFAULT_TIMEOUT_MS,
    ): TermuxCommandResult? {
        return resultRepository.awaitResult(
            request = TermuxResultRequest(
                executionId = executionId,
                startTimeMillis = startTime,
                expectedCommand = expectedCommand,
                nonce = nonce,
            ),
            timeoutMillis = timeoutMillis,
        )
    }

    private fun rawResultOutput(result: TermuxCommandResult): String {
        return buildList {
            result.stdout.trim().takeIf { it.isNotBlank() }?.let(::add)
            result.stderr.trim().takeIf { it.isNotBlank() }?.let(::add)
            result.raw.trim().takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString("\n").trim()
    }

    private companion object {
        // RUN_COMMAND creates the foreground session asynchronously. Bringing Termux to the front
        // before it exists makes Termux open an extra empty session next to the tavern's.
        const val FOREGROUND_SESSION_SETTLE_MS = 1_500L
    }

    private fun returnToLauncher() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // If Android blocks the return hop, keep the app alive and let the user reopen it.
        }
    }
}
