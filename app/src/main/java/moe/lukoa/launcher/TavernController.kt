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
            "tavern-force-cleanup" -> runner.runForceCleanupTavern()
            "tavern-version" -> runner.runTavernVersion()
            "tavern-version-startup" -> runner.runTavernVersion()
            "tavern-doctor" -> runner.runTavernDoctor()
            "tavern-repair-dependencies" -> runner.runTavernRepairDependencies()
            "tavern-reset-theme" -> runner.runTavernResetTheme()
            "tavern-node-memory" -> runner.runTavernNodeMemory(parsed.argument)
            "tavern-upload-limit-status" -> runner.runTavernUploadLimitStatus()
            "tavern-upload-limit-set" -> runner.runTavernUploadLimitSet(parsed.argument)
            "tavern-users-list" -> runner.runTavernUsersList()
            "tavern-user-create" -> runner.runTavernUserCreate(parsed.argument)
            "tavern-user-rename" -> runner.runTavernUserRename(parsed.argument)
            "tavern-user-delete" -> runner.runTavernUserDelete(parsed.argument)
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
            else -> runner.runAction(parsed.name)
        }
        update(dispatchMessage(parsed.name, dispatch), "", false)
        if (!dispatch.sent) return

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
                if (ok && parsed.name == "start") {
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

    fun refreshLogSnapshot(scope: CoroutineScope, updateTermuxLog: (String, Boolean) -> Unit) {
        val startTime = System.currentTimeMillis()
        val dispatch = runner.runLiveLogDeltaSnapshot()
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
        update("正在打开 Termux 酒馆窗口。", "", false)
        scope.launch {
            val dispatch = runner.runForegroundTavernConsole()
            val message = if (dispatch.sent) {
                delay(250)
                val woke = runner.wakeTermux()
                if (woke) {
                    "启动命令已发送。"
                } else {
                    "启动命令已发送，但系统没有自动打开 Termux。请手动打开 Termux 查看启动日志，先不要重复点启动。"
                }
            } else {
                dispatch.message
            }
            update(message, "", dispatch.sent && !message.contains("失败"))
        }
    }

    fun openTavern(update: LauncherUpdate) {
        val result = runner.openTavern()
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
            "tavern-force-cleanup" -> "强制清理命令已发送到 Termux。"
            "start" -> "启动命令已发送到 Termux。"
            "tavern-version" -> "正在读取酒馆版本。"
            "tavern-version-startup" -> "正在检测酒馆版本。"
            "tavern-doctor" -> "正在体检当前环境。"
            "tavern-repair-dependencies" -> "正在安全重装酒馆依赖，Termux 前台会显示进度。"
            "tavern-reset-theme" -> "正在查找并重置当前用户的网页主题。"
            "tavern-node-memory" -> "正在设置 Node.js 内存上限。"
            "tavern-upload-limit-status" -> "正在识别当前聊天记录上传限制。"
            "tavern-upload-limit-set" -> "正在修改聊天记录上传限制。"
            "tavern-users-list" -> "正在读取当前酒馆用户。"
            "tavern-user-create" -> "正在创建酒馆用户。"
            "tavern-user-rename" -> "正在修改用户显示名称。"
            "tavern-user-delete" -> "正在删除用户账户并保留数据目录。"
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
            else -> dispatch.message
        }
    }

    private fun resultMessage(command: String, ok: Boolean, result: TermuxCommandResult? = null): String {
        val stdout = result?.stdout.orEmpty()
        return when (command) {
            "status" -> if (ok) "状态已刷新。" else "状态查询失败。"
            "log" -> if (ok) "酒馆运行日志已同步。" else "同步酒馆运行日志失败。"
            "stop" -> if (ok) "停止命令已返回。" else "停止酒馆失败。"
            "tavern-force-cleanup" -> if (ok) "强制清理已返回。" else "强制清理残留进程失败。"
            "start" -> if (ok) "启动命令已返回。" else "启动酒馆失败。"
            "tavern-version" -> if (ok) "酒馆版本已读取。" else "读取酒馆版本失败。"
            "tavern-version-startup" -> if (ok) "酒馆版本已读取。" else "检测酒馆版本失败。"
            "tavern-doctor" -> if (ok) "体检已完成。" else "体检失败。"
            "tavern-repair-dependencies" -> if (ok) "酒馆依赖已修复。" else "修复酒馆依赖失败，旧依赖已尽量恢复。"
            "tavern-reset-theme" -> if (ok) "网页主题已重置。" else "没有找到可安全修改的主题设置。"
            "tavern-node-memory" -> if (ok) "Node.js 内存上限已保存。" else "设置 Node.js 内存上限失败。"
            "tavern-upload-limit-status" -> if (ok) "上传限制已检查，请查看操作反馈中的当前值。" else "无法识别当前版本的上传限制。"
            "tavern-upload-limit-set" -> if (ok) "上传限制已修改，重启酒馆后生效。" else "修改上传限制失败，源文件未被强行改动。"
            "tavern-users-list" -> if (ok) "酒馆用户已读取。" else "读取酒馆用户失败。"
            "tavern-user-create" -> if (ok) "酒馆用户已创建。" else "创建酒馆用户失败。"
            "tavern-user-rename" -> if (ok) "用户显示名称已修改。" else "修改用户显示名称失败。"
            "tavern-user-delete" -> if (ok) "用户账户已删除，数据目录仍然保留。" else "删除用户账户失败。"
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
