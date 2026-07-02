package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TermuxResultDisplay(
    val key: String,
    val command: String,
    val output: String,
    val ok: Boolean,
)

class TavernController(
    private val context: Context,
    private val runner: TermuxCommandRunner,
) {
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
        val parsed = parseCommand(command)
        val startTime = System.currentTimeMillis()
        val dispatch = when (parsed.name) {
            "log" -> runner.runLogSnapshot()
            "status" -> runner.runStatusSnapshot()
            "stop" -> runner.runStopTavern()
            "tavern-version" -> runner.runTavernVersion()
            "tavern-version-startup" -> runner.runTavernVersion()
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
            else -> runner.runAction(parsed.name)
        }
        update(dispatchMessage(parsed.name, dispatch), "", false)
        if (!dispatch.sent) return

        scope.launch {
            val result = waitForResult(
                executionId = dispatch.executionId,
                startTime = startTime,
                nonce = null,
                expectedCommand = dispatch.displayCommand,
                attempts = attemptsForCommand(parsed.name),
            )
            if (result != null) {
                val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
                update(resultMessage(parsed.name, ok, result), formatResultForDisplay(result), ok)
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
                nonce = null,
                expectedCommand = dispatch.displayCommand,
                attempts = 4,
            )
            if (result != null) {
                val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
                updateTermuxLog(formatResultForDisplay(result), ok)
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
                    "启动命令已发送，请手动打开 Termux。"
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
        val result = TermuxResultStore.latest(context) ?: return null
        val ok = result.isStructurallyValid && !result.hasInternalError && result.exitCode == 0
        return TermuxResultDisplay(
            key = result.stableKey,
            command = result.command.ifBlank { result.raw.lineSequence().firstOrNull().orEmpty().ifBlank { "Termux" } },
            output = formatResultForDisplay(result),
            ok = ok,
        )
    }

    fun exportLog(
        summary: String,
        status: String,
        termuxLog: String,
        appLog: String,
        mode: ExportLogMode,
        update: LauncherUpdate,
    ) {
        try {
            val file = SessionLogExporter.export(
                context = context,
                summary = summary,
                status = status,
                termuxLog = termuxLog,
                appLog = appLog,
                mode = mode,
            )
            SharedFileSender.shareTextFile(context, file, "导出运行日志", "露科亚启动器运行日志")
            val scopeText = when (mode) {
                ExportLogMode.TermuxOnly -> "Termux 调用"
                ExportLogMode.AppOnly -> "App 操作反馈"
                ExportLogMode.Both -> "Termux 调用和 App 操作反馈"
            }
            update("已生成${scopeText}导出文件：${file.name}", "", true)
        } catch (error: Exception) {
            update("导出日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
        }
    }

    fun exportDiagnostic(snapshot: DiagnosticSnapshot, update: LauncherUpdate) {
        try {
            val file = DiagnosticLogExporter.export(context, snapshot)
            SharedFileSender.shareTextFile(context, file, "导出诊断日志", "露科亚启动器诊断日志")
            update("已生成诊断日志：${file.name}", "", true)
        } catch (error: Exception) {
            update("导出诊断日志失败：${error.message ?: error.javaClass.simpleName}", "", false)
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
                update(successMessage, result.stdout.ifBlank { result.raw }, true)
            } else if (result != null) {
                update(failureMessage, formatResultForDisplay(result), false)
            } else {
                update("$timeoutMessage\n请检查 Termux 权限。", "", false)
            }
        }
    }

    private fun dispatchMessage(command: String, dispatch: CommandDispatch): String {
        if (!dispatch.sent) return dispatch.message
        return when (command) {
            "status" -> "正在查询酒馆状态。"
            "log" -> "正在读取 Termux 日志。"
            "stop" -> "停止命令已发送到 Termux。"
            "start" -> "启动命令已发送到 Termux。"
            "tavern-version" -> "正在读取酒馆版本。"
            "tavern-version-startup" -> "正在检测酒馆版本。"
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
            else -> dispatch.message
        }
    }

    private fun resultMessage(command: String, ok: Boolean, result: TermuxCommandResult? = null): String {
        val stdout = result?.stdout.orEmpty()
        return when (command) {
            "status" -> if (ok) "状态已刷新。" else "状态查询失败。"
            "log" -> if (ok) "日志已读取。" else "日志读取失败。"
            "stop" -> if (ok) "停止命令已返回。" else "停止酒馆失败。"
            "start" -> if (ok) "启动命令已返回。" else "启动酒馆失败。"
            "tavern-version" -> if (ok) "酒馆版本已读取。" else "读取酒馆版本失败。"
            "tavern-version-startup" -> if (ok) "酒馆版本已读取。" else "检测酒馆版本失败。"
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
            "tavern-restore" -> if (ok) {
                "酒馆备份已应用。"
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

    private fun attemptsForCommand(command: String): Int {
        return when (command) {
            "tavern-install" -> 1800
            "tavern-update",
            "tavern-rollback",
            "tavern-backup-manual",
            "tavern-backup-auto",
            "tavern-backup-delete",
            "tavern-backup-export",
            "tavern-backup-export-to",
            "tavern-backup-copy",
            "tavern-backup-import",
            "tavern-backup-rename",
            "tavern-restore",
            "tavern-backup" -> 1200
            "start" -> 120
            "status",
            "log",
            "tavern-version",
            "tavern-backup-list" -> 48
            "tavern-version-startup" -> 8
            "tavern-official-versions" -> 120
            "termux-storage-permission" -> 120
            "termux-repo-status" -> 48
            "termux-repo" -> 240
            "termux-repo-custom" -> 240
            "termux-bootstrap" -> 2400
            else -> 24
        }
    }

    private data class ParsedCommand(
        val name: String,
        val argument: String?,
    )

    private fun String.lineValue(key: String): String? {
        return lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseCommand(command: String): ParsedCommand {
        val parts = command.split("::", limit = 2)
        return ParsedCommand(
            name = parts.firstOrNull().orEmpty(),
            argument = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun waitForResult(
        executionId: Int,
        startTime: Long,
        nonce: String?,
        expectedCommand: String,
        attempts: Int = 24,
    ): TermuxCommandResult? {
        repeat(attempts) {
            delay(500)
            val result = TermuxResultStore.recent(context)
                .firstOrNull { matchesCurrentCommand(it, executionId, startTime, expectedCommand) }
            if (result != null && matchesCurrentCommand(result, executionId, startTime, expectedCommand)) {
                val combined = result.stdout + "\n" + result.stderr + "\n" + result.raw
                if (nonce == null || combined.contains(nonce)) {
                    return result
                }
            }
        }
        return null
    }

    private fun matchesCurrentCommand(
        result: TermuxCommandResult,
        executionId: Int,
        startTime: Long,
        expectedCommand: String,
    ): Boolean {
        if (result.timeMillis < startTime) return false
        if (result.executionId == executionId) return true
        if (result.executionId != 0) return false
        return expectedCommand.isNotBlank() && result.command == expectedCommand
    }

    private fun formatResultForDisplay(result: TermuxCommandResult): String {
        return cleanTerminalOutput(buildString {
            if (!result.hasResultBundle) appendLine("未收到 Termux 返回包。")
            if (result.hasInternalError) appendLine("Termux 内部错误：${result.errMessage.ifBlank { result.errCode.toString() }}")
            if (result.errCode == 150 || result.errMessage.contains("executable regular file not found", ignoreCase = true)) {
                appendLine("未找到 Termux 脚本，请重新打开启动器。")
            }
            if (TermuxPermissionSignals.externalAppsBlocked(result.errMessage + "\n" + result.raw)) {
                appendLine("Termux 外部调用未开启。请在启动器权限引导里复制命令，到 Termux 粘贴执行。")
            }
            if (result.exitCode != null) appendLine("exitCode=${result.exitCode}") else appendLine("缺少 exitCode。")
            if (result.stdout.isNotBlank()) appendLine(result.stdout.trim())
            if (result.stderr.isNotBlank()) appendLine(result.stderr.trim())
            if (isBlank() && result.raw.isNotBlank()) append(result.raw.trim())
        }.trim())
    }

    private fun cleanTerminalOutput(text: String): String {
        return text
            .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B\\[(?![0-9;]*m)[0-?]*[ -/]*[@-~]"), "")
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
