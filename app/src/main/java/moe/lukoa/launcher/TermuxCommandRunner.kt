package moe.lukoa.launcher

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID

data class CommandDispatch(
    val sent: Boolean,
    val message: String,
    val executionId: Int = 0,
    val nonce: String? = null,
    val displayCommand: String = "",
)

class TermuxCommandRunner(private val context: Context) {
    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasRunCommandPermission(): Boolean {
        return context.checkSelfPermission(PERMISSION_RUN_COMMAND) == PackageManager.PERMISSION_GRANTED
    }

    fun runSelftest(): CommandDispatch {
        val nonce = UUID.randomUUID().toString().replace("-", "")
        return runCommand("selftest", listOf(nonce), nonce)
    }

    fun installOrRepairScript(scriptText: String): CommandDispatch {
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val installCommand = try {
            buildInstallCommand(scriptText, nonce)
        } catch (error: Exception) {
            return CommandDispatch(
                sent = false,
                message = "准备安装脚本失败：${error.message ?: error.javaClass.simpleName}",
                nonce = nonce,
                displayCommand = "install-script",
            )
        }
        return runCommand(
            command = "install-script",
            args = listOf("-c", installCommand),
            nonce = nonce,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "install-script",
        )
    }

    fun runAction(action: String): CommandDispatch {
        return runCommand(action, emptyList(), null)
    }

    fun runForegroundAction(action: String): CommandDispatch {
        return runCommand(action, emptyList(), null, background = false)
    }

    fun runForegroundTavernConsole(): CommandDispatch {
        return runCommand(
            command = "foreground-console",
            args = listOf("-c", buildForegroundConsoleCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "foreground-console",
            background = false,
        )
    }

    fun requestTermuxStoragePermission(): CommandDispatch {
        val returnCommand = "am start --activity-clear-top --activity-single-top --activity-reorder-to-front -n ${context.packageName}/.MainActivity >/dev/null 2>&1 || true"
        val storageCommand = """
            echo "Lukoa: requesting Termux storage permission."
            if ! command -v termux-setup-storage >/dev/null 2>&1; then
              echo "termux-storage-permission"
              echo "error=termux-setup-storage-not-found"
              exit 127
            fi
            termux-setup-storage
            sleep 1
            if { [ -d "${'$'}HOME/storage/downloads" ] && ls "${'$'}HOME/storage/downloads" >/dev/null 2>&1; } || ls "/storage/emulated/0/Download" >/dev/null 2>&1; then
              echo "storage.permission.ok=true"
              echo "Termux storage permission is available."
              $returnCommand
              exit 0
            fi
            echo "termux-storage-permission"
            echo "error=termux-storage-permission"
            echo "fix.command=termux-setup-storage"
            $returnCommand
            exit 73
        """.trimIndent()
        return runCommand(
            command = "termux-storage-permission-direct",
            args = listOf("-c", storageCommand),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "termux-storage-permission",
            background = false,
        )
    }

    fun runLogSnapshot(): CommandDispatch {
        return runCommand(
            command = "log-snapshot",
            args = listOf("-c", buildLogSnapshotCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "log",
        )
    }

    fun runLiveLogDeltaSnapshot(): CommandDispatch {
        return runCommand(
            command = "log-live-delta",
            args = listOf("-c", buildLiveLogDeltaCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "log",
        )
    }

    fun runStatusSnapshot(): CommandDispatch {
        return runCommand(
            command = "status-snapshot",
            args = listOf("-c", buildStatusSnapshotCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "status",
        )
    }

    fun runStopTavern(): CommandDispatch {
        return runCommand(
            command = "stop-direct",
            args = listOf("-c", buildStopCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "stop",
        )
    }

    fun runTavernVersion(): CommandDispatch {
        return runBundledScriptCommand(
            command = "tavern-version-direct",
            scriptCommand = "tavern-version",
            scriptArgs = emptyList(),
            displayCommand = "tavern-version",
        )
    }

    fun runTavernOfficialVersions(): CommandDispatch {
        return runBundledScriptCommand(
            command = "tavern-official-versions-direct",
            scriptCommand = "tavern-official-versions",
            scriptArgs = emptyList(),
            displayCommand = "tavern-official-versions",
        )
    }

    fun runTermuxPackageMirror(mode: String?): CommandDispatch {
        val normalized = mode.orEmpty().trim()
        if (normalized != "tuna" && normalized != "official") {
            return CommandDispatch(
                sent = false,
                message = "Termux 包源无效。",
                displayCommand = "termux-repo",
            )
        }
        return runCommand(
            command = "termux-repo-direct",
            args = listOf("-c", buildTermuxPackageMirrorCommand(normalized)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "termux-repo",
            background = false,
        )
    }

    fun runTermuxPackageMirrorStatus(): CommandDispatch {
        return runCommand(
            command = "termux-repo-status-direct",
            args = listOf("-c", buildTermuxPackageMirrorStatusCommand()),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "termux-repo-status",
            background = false,
        )
    }

    fun runTermuxPackageMirrorCustom(url: String?): CommandDispatch {
        val normalized = url.orEmpty().trim().trimEnd('/')
        TavernMirrorValidator.validateTermuxAptUrl(normalized)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "自定义 Termux 包源无效：$reason",
                displayCommand = "termux-repo",
            )
        }
        return runCommand(
            command = "termux-repo-custom-direct",
            args = listOf("-c", buildTermuxPackageMirrorCustomCommand(normalized)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "termux-repo",
            background = false,
        )
    }

    fun runTermuxBootstrap(configPolicy: String? = null): CommandDispatch {
        val safePolicy = AptConfigPolicyCodec.normalize(configPolicy).wireValue
        return runCommand(
            command = "termux-bootstrap-direct",
            args = listOf("-c", buildTermuxBootstrapCommand(safePolicy)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "termux-bootstrap",
            background = false,
        )
    }

    fun runTavernInstall(targetAndPolicy: String?): CommandDispatch {
        val decodedArgs = TavernInstallCommandCodec.decode(targetAndPolicy)
        val args = decodedArgs?.copy(
            target = decodedArgs.target.trim().ifBlank { TavernInstallDefaults.Release.target },
            repoUrl = decodedArgs.repoUrl.trim().ifBlank { mirrorConfig().normalizedRepoUrl },
        ) ?: run {
            val (target, configPolicy) = AptConfigPolicyCodec.decodeInstallTarget(targetAndPolicy)
            TavernInstallCommandArgs(
                target = target.trim().ifBlank { TavernInstallDefaults.Release.target },
                repoUrl = mirrorConfig().normalizedRepoUrl,
                configPolicy = configPolicy,
            )
        }
        val safeTarget = args.target
        LauncherInputGuards.validateVersionTarget(safeTarget)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "安装酒馆失败：目标版本无效。$reason",
                displayCommand = "tavern-install",
            )
        }
        TavernMirrorValidator.validateRepoUrl(args.repoUrl)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "安装酒馆失败：Git 源地址无效。$reason",
                displayCommand = "tavern-install",
            )
        }
        return runCommand(
            command = "tavern-install-direct",
            args = listOf("-c", buildTavernInstallCommand(args)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "tavern-install",
            background = false,
        )
    }

    fun runTavernUpdate(target: String?): CommandDispatch {
        val args = decodeVersionCommandArgs(target)
        val safeTarget = args.target
        LauncherInputGuards.validateVersionTarget(safeTarget)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "更新酒馆失败：目标版本无效。$reason",
                displayCommand = "tavern-update",
            )
        }
        TavernMirrorValidator.validateRepoUrl(args.repoUrl)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "更新酒馆失败：Git 源地址无效。$reason",
                displayCommand = "tavern-update",
            )
        }
        return runCommand(
            command = "tavern-update-direct",
            args = listOf("-c", buildTavernUpdateCommand(args)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "tavern-update",
            background = false,
        )
    }

    fun runTavernRollback(target: String?): CommandDispatch {
        val args = decodeVersionCommandArgs(target)
        val safeTarget = args.target
        LauncherInputGuards.validateVersionTarget(safeTarget)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "回退酒馆失败：目标版本无效。$reason",
                displayCommand = "tavern-rollback",
            )
        }
        TavernMirrorValidator.validateRepoUrl(args.repoUrl)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "回退酒馆失败：Git 源地址无效。$reason",
                displayCommand = "tavern-rollback",
            )
        }
        return runCommand(
            command = "tavern-rollback-direct",
            args = listOf("-c", buildTavernRollbackCommand(args)),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "tavern-rollback",
            background = false,
        )
    }

    fun runTavernBackup(kind: String, keepCount: Int?, label: String? = null): CommandDispatch {
        val safeKind = if (kind == "auto") "auto" else "manual"
        val scriptArgs = if (safeKind == "auto") {
            listOf("auto", (keepCount ?: 5).coerceIn(1, 50).toString())
        } else {
            val safeLabel = label.orEmpty().trim()
            LauncherInputGuards.validateManualBackupName(safeLabel)?.let { reason ->
                return CommandDispatch(
                    sent = false,
                    message = "生成备份失败：备份名称无效。$reason",
                    displayCommand = "tavern-backup",
                )
            }
            if (safeLabel.isBlank()) {
                listOf("manual")
            } else {
                listOf("manual", "5", safeLabel)
            }
        }
        return runBundledScriptCommand(
            command = "tavern-backup-direct",
            scriptCommand = "backup",
            scriptArgs = scriptArgs,
            displayCommand = "tavern-backup",
            background = safeKind == "auto",
        )
    }

    fun runTavernBackupList(): CommandDispatch {
        return runBundledScriptCommand(
            command = "tavern-backup-list-direct",
            scriptCommand = "backup-list",
            scriptArgs = emptyList(),
            displayCommand = "tavern-backup-list",
        )
    }

    fun runTavernBackupDelete(archivePath: String?): CommandDispatch {
        val safePath = archivePath.orEmpty().trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "删除备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-delete",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-delete-direct",
            scriptCommand = "backup-delete",
            scriptArgs = listOf(safePath),
            displayCommand = "tavern-backup-delete",
        )
    }

    fun runTavernBackupExport(archivePath: String?): CommandDispatch {
        val safePath = archivePath.orEmpty().trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "导出备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-export",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-export-direct",
            scriptCommand = "backup-export",
            scriptArgs = listOf(safePath),
            displayCommand = "tavern-backup-export",
        )
    }

    fun runTavernBackupExportTo(encodedArgument: String?): CommandDispatch {
        val exportArgs = BackupCommandCodec.decodeExportTo(encodedArgument)
            ?: return CommandDispatch(
                sent = false,
                message = "导出备份失败：导出参数损坏，请回到备份列表重新点一次。",
                displayCommand = "tavern-backup-export-to",
            )
        val safeSource = exportArgs.archivePath.trim()
        val safeDestination = exportArgs.destinationPath.trim()
        LauncherInputGuards.validateBackupArchivePath(safeSource)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "导出备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-export-to",
            )
        }
        LauncherInputGuards.validateBackupArchivePath(safeDestination)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "导出备份失败：导出位置无效。$reason",
                displayCommand = "tavern-backup-export-to",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-export-to-direct",
            scriptCommand = "backup-export-to",
            scriptArgs = listOf(safeSource, safeDestination),
            displayCommand = "tavern-backup-export-to",
        )
    }

    fun runTavernBackupCopy(archivePath: String?): CommandDispatch {
        val safePath = archivePath.orEmpty().trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "复制备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-copy",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-copy-direct",
            scriptCommand = "backup-copy",
            scriptArgs = listOf(safePath),
            displayCommand = "tavern-backup-copy",
        )
    }

    fun runTavernBackupImport(archivePath: String?): CommandDispatch {
        val safePath = archivePath.orEmpty().trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "导入备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-import",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-import-direct",
            scriptCommand = "backup-import",
            scriptArgs = listOf(safePath),
            displayCommand = "tavern-backup-import",
        )
    }

    fun runTavernBackupRename(encodedArgument: String?): CommandDispatch {
        val renameArgs = BackupCommandCodec.decodeRename(encodedArgument)
            ?: return CommandDispatch(
                sent = false,
                message = "重命名备份失败：命令参数损坏，请回到备份列表重新点一次。",
                displayCommand = "tavern-backup-rename",
            )
        val safePath = renameArgs.archivePath.trim()
        val safeName = renameArgs.newName.trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "重命名备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-backup-rename",
            )
        }
        LauncherInputGuards.validateBackupRequiredName(safeName)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "重命名备份失败：新名称无效。$reason",
                displayCommand = "tavern-backup-rename",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-backup-rename-direct",
            scriptCommand = "backup-rename",
            scriptArgs = listOf(safePath, safeName),
            displayCommand = "tavern-backup-rename",
        )
    }

    fun runTavernRestore(archivePath: String?): CommandDispatch {
        val safePath = archivePath.orEmpty().trim()
        LauncherInputGuards.validateBackupArchivePath(safePath)?.let { reason ->
            return CommandDispatch(
                sent = false,
                message = "应用备份失败：备份文件路径无效。$reason",
                displayCommand = "tavern-restore",
            )
        }
        return runBundledScriptCommand(
            command = "tavern-restore-direct",
            scriptCommand = "tavern-restore",
            scriptArgs = listOf(safePath),
            displayCommand = "tavern-restore",
            background = false,
        )
    }

    fun requestReturnToLauncher(): CommandDispatch {
        val returnCommand = "am start --activity-clear-top --activity-single-top --activity-reorder-to-front -n ${context.packageName}/.MainActivity >/dev/null 2>&1"
        return runCommand(
            command = "return-launcher",
            args = listOf("-c", returnCommand),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = "return-launcher",
        )
    }

    fun openTavern(port: Int = 8000): CommandDispatch {
        if (port !in 1..65535) {
            return CommandDispatch(sent = false, message = "打开网页失败：端口号不合法。")
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:$port"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            CommandDispatch(sent = true, message = "已请求浏览器打开酒馆网页。")
        } catch (error: Exception) {
            CommandDispatch(
                sent = false,
                message = "打开网页失败：${error.message ?: error.javaClass.simpleName}。请确认手机上有可用浏览器。",
            )
        }
    }

    fun wakeTermux(): Boolean {
        if (!isTermuxInstalled()) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            ?: return false
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
        )
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun runCommand(
        command: String,
        args: List<String>,
        nonce: String?,
        executablePath: String = TERMUX_SCRIPT_PATH,
        displayCommand: String = command,
        background: Boolean = true,
    ): CommandDispatch {
        if (!isTermuxInstalled()) {
            return CommandDispatch(
                sent = false,
                message = "未检测到 Termux，请先安装。",
                nonce = nonce,
                displayCommand = displayCommand,
            )
        }

        if (!hasRunCommandPermission()) {
            return CommandDispatch(
                sent = false,
                message = "缺少 RUN_COMMAND 权限。",
                nonce = nonce,
                displayCommand = displayCommand,
            )
        }

        val executionId = nextExecutionId()
        val resultIntent = Intent(context, TermuxResultReceiver::class.java)
            .putExtra(EXTRA_EXECUTION_ID, executionId)
            .putExtra(EXTRA_LUKOA_COMMAND, displayCommand)
            .putExtra(EXTRA_LUKOA_NONCE, nonce)
        val requestCode = executionId
        val flags = PendingIntent.FLAG_ONE_SHOT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, resultIntent, flags)

        val commandIntent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            putExtra(EXTRA_PATH, executablePath)
            putExtra(EXTRA_ARGUMENTS, if (executablePath == TERMUX_SCRIPT_PATH) {
                arrayOf(command, *args.toTypedArray())
            } else {
                args.toTypedArray()
            })
            putExtra(EXTRA_WORKDIR, TERMUX_HOME)
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        }

        return try {
            context.startService(commandIntent)
            CommandDispatch(
                sent = true,
                message = "命令已发送到 Termux：$displayCommand",
                executionId = executionId,
                nonce = nonce,
                displayCommand = displayCommand,
            )
        } catch (security: SecurityException) {
            CommandDispatch(
                sent = false,
                message = "Termux 拒绝调用，请检查权限。",
                nonce = nonce,
                displayCommand = displayCommand,
            )
        } catch (error: Exception) {
            CommandDispatch(
                sent = false,
                message = "发送命令失败：${error.message ?: error.javaClass.simpleName}",
                nonce = nonce,
                displayCommand = displayCommand,
            )
        }
    }

    private fun buildInstallCommand(scriptText: String, nonce: String): String {
        val normalized = scriptText.replace("\r\n", "\n").replace("\r", "\n")
        require(!normalized.contains(INSTALL_EOF_MARKER)) {
            "script contains reserved install marker"
        }
        return buildString {
            appendLine("set -eu")
            appendLine("mkdir -p \"\$HOME/.local/bin\" \"\$HOME/.local/state/lukoa-launcher\" \"\$HOME/.config/lukoa-launcher\" \"\$HOME/.termux\"")
            appendLine("cat > \"\$HOME/.local/bin/lukoa-tavern.sh\" <<'$INSTALL_EOF_MARKER'")
            appendLine(normalized)
            appendLine(INSTALL_EOF_MARKER)
            appendLine("chmod 700 \"\$HOME/.local/bin/lukoa-tavern.sh\"")
            appendLine("if [ ! -f \"\$HOME/.config/lukoa-launcher/config.env\" ]; then")
            appendLine("  printf 'TAVERN_DIR=\"%s\"\\nTAVERN_PORT=8000\\n' ${shellSingleQuoted(tavernPathConfig().normalizedTavernDir)} > \"\$HOME/.config/lukoa-launcher/config.env\"")
            appendLine("fi")
            appendMirrorExports(this)
            appendLine("if [ ! -f \"\$HOME/.termux/termux.properties\" ] || ! grep -q '^allow-external-apps=true' \"\$HOME/.termux/termux.properties\"; then")
            appendLine("  printf '\\nallow-external-apps=true\\n' >> \"\$HOME/.termux/termux.properties\"")
            appendLine("fi")
            appendLine("\"\$HOME/.local/bin/lukoa-tavern.sh\" selftest \"$nonce\"")
        }
    }

    private fun runBundledScriptCommand(
        command: String,
        scriptCommand: String,
        scriptArgs: List<String>,
        displayCommand: String,
        background: Boolean = true,
    ): CommandDispatch {
        val scriptText = try {
            context.assets.open("lukoa-tavern.sh").bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (error: Exception) {
            return CommandDispatch(
                sent = false,
                message = "读取内置 Termux 脚本失败：${error.message ?: error.javaClass.simpleName}",
                displayCommand = displayCommand,
            )
        }
        val installAndRunCommand = try {
            buildInstallAndRunScriptCommand(scriptText, scriptCommand, scriptArgs)
        } catch (error: Exception) {
            return CommandDispatch(
                sent = false,
                message = "准备 Termux 脚本失败：${error.message ?: error.javaClass.simpleName}",
                displayCommand = displayCommand,
            )
        }
        return runCommand(
            command = command,
            args = listOf("-c", installAndRunCommand),
            nonce = null,
            executablePath = TERMUX_SH_PATH,
            displayCommand = displayCommand,
            background = background,
        )
    }

    private fun buildInstallAndRunScriptCommand(
        scriptText: String,
        scriptCommand: String,
        scriptArgs: List<String>,
    ): String {
        val normalized = scriptText.replace("\r\n", "\n").replace("\r", "\n")
        require(!normalized.contains(INSTALL_EOF_MARKER)) {
            "script contains reserved install marker"
        }
        val quotedArgs = (listOf(scriptCommand) + scriptArgs).joinToString(" ") { shellSingleQuoted(it) }
        return buildString {
            appendLine("set -eu")
            appendLine("mkdir -p \"\$HOME/.local/bin\" \"\$HOME/.local/state/lukoa-launcher\" \"\$HOME/.config/lukoa-launcher\" \"\$HOME/.termux\"")
            appendLine("cat > \"\$HOME/.local/bin/lukoa-tavern.sh\" <<'$INSTALL_EOF_MARKER'")
            appendLine(normalized)
            appendLine(INSTALL_EOF_MARKER)
            appendLine("chmod 700 \"\$HOME/.local/bin/lukoa-tavern.sh\"")
            appendLine("if [ ! -f \"\$HOME/.config/lukoa-launcher/config.env\" ]; then")
            appendLine("  printf 'TAVERN_DIR=\"%s\"\\nTAVERN_PORT=8000\\n' ${shellSingleQuoted(tavernPathConfig().normalizedTavernDir)} > \"\$HOME/.config/lukoa-launcher/config.env\"")
            appendLine("fi")
            appendLine("if [ ! -f \"\$HOME/.termux/termux.properties\" ] || ! grep -q '^allow-external-apps=true' \"\$HOME/.termux/termux.properties\"; then")
            appendLine("  printf '\\nallow-external-apps=true\\n' >> \"\$HOME/.termux/termux.properties\"")
            appendLine("fi")
            appendMirrorExports(this)
            appendLine("exec \"\$HOME/.local/bin/lukoa-tavern.sh\" $quotedArgs")
        }
    }

    private fun shellSingleQuoted(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    private fun decodeVersionCommandArgs(value: String?): TavernVersionCommandArgs {
        val decoded = TavernVersionCommandCodec.decode(value)
        if (decoded != null) {
            return decoded.copy(
                target = decoded.target.trim(),
                repoUrl = decoded.repoUrl.trim().ifBlank { mirrorConfig().normalizedRepoUrl },
                commit = decoded.commit.trim(),
            )
        }
        return TavernVersionCommandArgs(
            target = value.orEmpty().trim(),
            repoUrl = mirrorConfig().normalizedRepoUrl,
        )
    }

    private fun mirrorConfig(): TavernMirrorConfig {
        return TavernMirrorStore(context).load()
    }

    private fun tavernPathConfig(): TavernPathConfig {
        return TavernPathStore(context).load()
    }

    private fun appendMirrorExports(builder: StringBuilder) {
        val mirrorConfig = mirrorConfig()
        val pathConfig = tavernPathConfig()
        builder.appendLine("export LUKOA_TAVERN_DIR=${shellSingleQuoted(pathConfig.normalizedTavernDir)}")
        builder.appendLine("export LUKOA_OFFICIAL_REPO=${shellSingleQuoted(mirrorConfig.normalizedRepoUrl)}")
        builder.appendLine("export LUKOA_NPM_REGISTRY=${shellSingleQuoted(mirrorConfig.normalizedNpmRegistry)}")
        builder.appendLine("export npm_config_registry=\"\$LUKOA_NPM_REGISTRY\"")
        builder.appendLine("export NPM_CONFIG_REGISTRY=\"\$LUKOA_NPM_REGISTRY\"")
    }

    private fun buildSharedShellPrelude(): String {
        val mirrorConfig = mirrorConfig()
        val pathConfig = tavernPathConfig()
        val appTavernDir = shellSingleQuoted(pathConfig.normalizedTavernDir)
        val appRepo = shellSingleQuoted(mirrorConfig.normalizedRepoUrl)
        val appNpmRegistry = shellSingleQuoted(mirrorConfig.normalizedNpmRegistry)
        return """
            APP_NAME="lukoa-launcher"
            HOME_DIR="${'$'}{LUKOA_HOME:-${'$'}{HOME:-/data/data/com.termux/files/home}}"
            STATE_DIR="${'$'}{LUKOA_STATE_DIR:-${'$'}HOME_DIR/.local/state/${'$'}APP_NAME}"
            CONFIG_FILE="${'$'}{LUKOA_CONFIG_FILE:-${'$'}HOME_DIR/.config/${'$'}APP_NAME/config.env}"
            DEFAULT_TAVERN_DIR="${'$'}HOME_DIR/SillyTavern"
            TAVERN_PORT="${'$'}{TAVERN_PORT:-8000}"
            mkdir -p "${'$'}STATE_DIR" "${'$'}HOME_DIR/.config/${'$'}APP_NAME"
            if [ -f "${'$'}CONFIG_FILE" ]; then
              . "${'$'}CONFIG_FILE"
            fi
            case "${'$'}TAVERN_PORT" in
              ''|*[!0-9]*) TAVERN_PORT=8000 ;;
            esac
            if [ "${'$'}TAVERN_PORT" -lt 1 ] || [ "${'$'}TAVERN_PORT" -gt 65535 ]; then
              TAVERN_PORT=8000
            fi
            LOG_FILE="${'$'}STATE_DIR/tavern.log"
            LOG_SYNC_CURSOR_FILE="${'$'}STATE_DIR/app-log.cursor"
            STATUS_FILE="${'$'}STATE_DIR/status.json"
            PID_FILE="${'$'}STATE_DIR/pid"
            ROLLBACK_FILE="${'$'}STATE_DIR/last-tavern-update-commit"
            APP_CONFIG_TAVERN_DIR=$appTavernDir
            APP_CONFIG_OFFICIAL_REPO=$appRepo
            APP_CONFIG_NPM_REGISTRY=$appNpmRegistry
            if [ -n "${'$'}{LUKOA_TAVERN_DIR:-}" ]; then
              TAVERN_DIR="${'$'}LUKOA_TAVERN_DIR"
            elif [ -z "${'$'}{TAVERN_DIR:-}" ]; then
              TAVERN_DIR="${'$'}APP_CONFIG_TAVERN_DIR"
            fi
            if [ -z "${'$'}{TAVERN_DIR:-}" ]; then
              TAVERN_DIR="${'$'}DEFAULT_TAVERN_DIR"
            fi
            OFFICIAL_REPO="${'$'}{LUKOA_OFFICIAL_REPO:-${'$'}APP_CONFIG_OFFICIAL_REPO}"
            NPM_REGISTRY="${'$'}{LUKOA_NPM_REGISTRY:-${'$'}APP_CONFIG_NPM_REGISTRY}"
            if [ -n "${'$'}NPM_REGISTRY" ]; then
              export npm_config_registry="${'$'}NPM_REGISTRY"
              export NPM_CONFIG_REGISTRY="${'$'}NPM_REGISTRY"
            fi
            expand_launcher_path() {
              path="${'$'}1"
              if [ -z "${'$'}path" ]; then
                printf "%s" "${'$'}path"
                return 0
              fi
              if [ "${'$'}path" = "~" ] || [ "${'$'}path" = "\${'$'}HOME" ]; then
                printf "%s" "${'$'}HOME_DIR"
                return 0
              fi
              if [ "${'$'}{path#~/}" != "${'$'}path" ]; then
                printf "%s/%s" "${'$'}HOME_DIR" "${'$'}{path#~/}"
                return 0
              fi
              if [ "${'$'}{path#\${'$'}HOME/}" != "${'$'}path" ]; then
                printf "%s/%s" "${'$'}HOME_DIR" "${'$'}{path#\${'$'}HOME/}"
                return 0
              fi
              printf "%s" "${'$'}path"
            }
            TAVERN_DIR="${'$'}(expand_launcher_path "${'$'}TAVERN_DIR")"
            looks_like_tavern_dir() {
              dir="${'$'}1"
              [ -n "${'$'}dir" ] || return 1
              [ -d "${'$'}dir" ] || return 1
              [ -f "${'$'}dir/package.json" ] || return 1
              if [ -f "${'$'}dir/start.sh" ] || [ -f "${'$'}dir/server.js" ]; then
                return 0
              fi
              return 1
            }
            append_tavern_candidate() {
              candidate="${'$'}1"
              [ -n "${'$'}candidate" ] || return 0
              if [ -z "${'$'}{TAVERN_CANDIDATE_LIST:-}" ]; then
                TAVERN_CANDIDATE_LIST="${'$'}candidate"
              else
                TAVERN_CANDIDATE_LIST="${'$'}TAVERN_CANDIDATE_LIST${'$'}(printf '\n%s' "${'$'}candidate")"
              fi
              TAVERN_CANDIDATE_COUNT=${'$'}((TAVERN_CANDIDATE_COUNT + 1))
              if [ -z "${'$'}{TAVERN_CANDIDATE_FIRST:-}" ]; then
                TAVERN_CANDIDATE_FIRST="${'$'}candidate"
              fi
            }
            collect_tavern_dir_candidates() {
              TAVERN_CANDIDATE_LIST=""
              TAVERN_CANDIDATE_COUNT=0
              TAVERN_CANDIDATE_FIRST=""
              DISCOVERED_TAVERN_DIR=""
            }
            discover_tavern_dir() {
              return 1
            }
            adopt_detected_tavern_dir() {
              return 1
            }
            emit_tavern_dir_candidates() {
              return 0
            }
            missing_tavern_dir_exit_code() {
              printf "66"
            }
            write_tavern_dir_error() {
              write_status "error" "SillyTavern directory not found: ${'$'}TAVERN_DIR" false 66
            }
            timestamp() {
              date -u +"%Y-%m-%dT%H:%M:%SZ"
            }
            json_escape() {
              printf "%s" "${'$'}1" | sed 's/\\/\\\\/g; s/"/\\"/g'
            }
            write_status() {
              status="${'$'}1"
              message="${'$'}2"
              running="${'$'}3"
              code="${'$'}{4:-0}"
              emit_status_direct "${'$'}status" "${'$'}message" "${'$'}running" "${'$'}code" > "${'$'}STATUS_FILE.${'$'}${'$'}"
              mv "${'$'}STATUS_FILE.${'$'}${'$'}" "${'$'}STATUS_FILE"
            }
            emit_status_direct() {
              status="${'$'}1"
              message="${'$'}2"
              running="${'$'}3"
              code="${'$'}{4:-0}"
              now="${'$'}(timestamp)"
              safe_message="${'$'}(json_escape "${'$'}message")"
              safe_dir="${'$'}(json_escape "${'$'}TAVERN_DIR")"
              cat <<EOF
            {
              "app": "${'$'}APP_NAME",
              "time": "${'$'}now",
              "status": "${'$'}status",
              "running": ${'$'}running,
              "exitCode": ${'$'}code,
              "message": "${'$'}safe_message",
              "tavernDir": "${'$'}safe_dir",
              "port": ${'$'}TAVERN_PORT,
              "logFile": "${'$'}LOG_FILE"
            }
            EOF
            }
            emit_status_snapshot() {
              if [ -f "${'$'}STATUS_FILE" ]; then
                cat "${'$'}STATUS_FILE"
              fi
              if [ -f "${'$'}LOG_FILE" ]; then
                printf "\n==== SillyTavern recent log: %s ====\n" "${'$'}LOG_FILE"
                if command -v tail >/dev/null 2>&1; then
                  tail -n "${'$'}{LUKOA_LOG_LINES:-240}" "${'$'}LOG_FILE" 2>/dev/null || true
                else
                  cat "${'$'}LOG_FILE" 2>/dev/null || true
                fi
                printf "\n==== end SillyTavern recent log ====\n"
              else
                printf "\nNo SillyTavern log file yet: %s\n" "${'$'}LOG_FILE"
              fi
            }
            file_size_bytes() {
              wc -c < "${'$'}1" 2>/dev/null | tr -d '[:space:]'
            }
            emit_live_log_delta() {
              if [ ! -f "${'$'}LOG_FILE" ]; then
                rm -f "${'$'}LOG_SYNC_CURSOR_FILE" 2>/dev/null || true
                printf "\nNo SillyTavern log file yet: %s\n" "${'$'}LOG_FILE"
                return 0
              fi
              size="${'$'}(file_size_bytes "${'$'}LOG_FILE")"
              case "${'$'}size" in ''|*[!0-9]*) size=0 ;; esac
              cursor="${'$'}(cat "${'$'}LOG_SYNC_CURSOR_FILE" 2>/dev/null || printf 0)"
              case "${'$'}cursor" in ''|*[!0-9]*) cursor=0 ;; esac
              if [ "${'$'}cursor" -gt "${'$'}size" ]; then
                cursor=0
              fi
              max_bytes="${'$'}{LUKOA_LIVE_LOG_MAX_BYTES:-65536}"
              case "${'$'}max_bytes" in ''|*[!0-9]*) max_bytes=65536 ;; esac
              new_bytes="${'$'}((size - cursor))"
              start="${'$'}((cursor + 1))"
              omitted=0
              if [ "${'$'}new_bytes" -gt "${'$'}max_bytes" ]; then
                omitted="${'$'}((new_bytes - max_bytes))"
                start="${'$'}((size - max_bytes + 1))"
                new_bytes="${'$'}max_bytes"
              fi
              printf "liveLog.cursor.before=%s\n" "${'$'}cursor"
              printf "liveLog.cursor.after=%s\n" "${'$'}size"
              printf "liveLog.bytes=%s\n" "${'$'}new_bytes"
              printf "\n==== SillyTavern live log: %s ====\n" "${'$'}LOG_FILE"
              if [ "${'$'}omitted" -gt 0 ]; then
                printf "[lukoa] 前面 %s 字节日志太多，已从最新部分继续同步。\n" "${'$'}omitted"
              fi
              if [ "${'$'}new_bytes" -gt 0 ]; then
                tail -c +"${'$'}start" "${'$'}LOG_FILE" 2>/dev/null || true
              fi
              printf "\n==== end SillyTavern live log ====\n"
              printf "%s\n" "${'$'}size" > "${'$'}LOG_SYNC_CURSOR_FILE.${'$'}${'$'}"
              mv "${'$'}LOG_SYNC_CURSOR_FILE.${'$'}${'$'}" "${'$'}LOG_SYNC_CURSOR_FILE"
            }
            http_probe_available() {
              command -v curl >/dev/null 2>&1
            }
            http_ok() {
              http_probe_available && curl -fsS --max-time 3 "http://127.0.0.1:${'$'}TAVERN_PORT/" >/dev/null 2>&1
            }
            is_running() {
              if [ ! -f "${'$'}PID_FILE" ]; then
                return 1
              fi
              pid="${'$'}(cat "${'$'}PID_FILE" 2>/dev/null || true)"
              case "${'$'}pid" in
                ''|*[!0-9]*) return 1 ;;
              esac
              kill -0 "${'$'}pid" 2>/dev/null
            }
            process_cwd_matches() {
              pid="${'$'}1"
              cwd="${'$'}(readlink "/proc/${'$'}pid/cwd" 2>/dev/null || true)"
              [ "${'$'}cwd" = "${'$'}TAVERN_DIR" ] && return 0
              case "${'$'}cwd" in
                "${'$'}TAVERN_DIR"/*) return 0 ;;
              esac
              return 1
            }
            process_matches_tavern() {
              pid="${'$'}1"
              args=""
              if [ -r "/proc/${'$'}pid/cmdline" ]; then
                args="${'$'}(tr '\000' ' ' 2>/dev/null < "/proc/${'$'}pid/cmdline" || true)"
              fi
              case "${'$'}args" in
                *"${'$'}TAVERN_DIR"*) return 0 ;;
              esac
              if process_cwd_matches "${'$'}pid"; then
                case "${'$'}args" in
                  *"server.js"*|*"start.sh"*) return 0 ;;
                esac
              fi
              return 1
            }
            candidate_pids() {
              current_pid="${'$'}${'$'}"
              parent_pid="${'$'}{PPID:-}"
              if command -v pgrep >/dev/null 2>&1; then
                {
                  pgrep -f "${'$'}TAVERN_DIR" 2>/dev/null || true
                  pgrep -f "server\\.js" 2>/dev/null || true
                  pgrep -f "start\\.sh" 2>/dev/null || true
                } | sort -u | while IFS= read -r pid; do
                  [ -n "${'$'}pid" ] || continue
                  [ "${'$'}pid" = "${'$'}current_pid" ] && continue
                  [ -n "${'$'}parent_pid" ] && [ "${'$'}pid" = "${'$'}parent_pid" ] && continue
                  process_matches_tavern "${'$'}pid" && printf "%s\n" "${'$'}pid"
                done
                return
              fi
              ps -A -o pid,args 2>/dev/null |
                grep -E "SillyTavern|server\\.js|start\\.sh" |
                grep -v grep |
                while IFS= read -r line; do
                  pid="${'$'}(printf "%s\n" "${'$'}line" | awk '{print ${'$'}1}')"
                  [ -n "${'$'}pid" ] || continue
                  [ "${'$'}pid" = "${'$'}current_pid" ] && continue
                  [ -n "${'$'}parent_pid" ] && [ "${'$'}pid" = "${'$'}parent_pid" ] && continue
                  process_matches_tavern "${'$'}pid" && printf "%s\n" "${'$'}pid"
                done || true
            }
            kill_candidate_pids() {
              pids="${'$'}(candidate_pids | tr '\n' ' ')"
              if [ -z "${'$'}pids" ]; then
                return 1
              fi
              kill ${'$'}pids 2>/dev/null || true
              sleep 1
              for pid in ${'$'}pids; do
                if kill -0 "${'$'}pid" 2>/dev/null; then
                  kill -9 "${'$'}pid" 2>/dev/null || true
                fi
              done
              return 0
            }
            open_tavern_browser() {
              command -v am >/dev/null 2>&1 && am start -a android.intent.action.VIEW -d "http://127.0.0.1:${'$'}TAVERN_PORT" >/dev/null 2>&1 || true
            }
            tavern_running_value() {
              if http_ok || is_running || [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
                printf "true"
              else
                printf "false"
              fi
            }
            start_script_runner() {
              if command -v bash >/dev/null 2>&1; then
                printf "bash"
              else
                printf "sh"
              fi
            }
            tracked_changes() {
              git status --porcelain --untracked-files=no 2>/dev/null || true
            }
            only_package_lock_tracked_changes() {
              changes="${'$'}(tracked_changes)"
              [ -n "${'$'}changes" ] || return 1
              printf "%s\n" "${'$'}changes" | grep -Ev '^[ MADRCU?!]{2} package-lock\.json${'$'}' >/dev/null 2>&1 && return 1
              return 0
            }
            cleanup_install_generated_changes() {
              if [ ! -d .git ]; then
                return 0
              fi
              if only_package_lock_tracked_changes; then
                printf "[%s] cleanup generated package-lock.json change\n" "${'$'}(timestamp)" >> "${'$'}LOG_FILE"
                git checkout -- package-lock.json >> "${'$'}LOG_FILE" 2>&1 || true
              fi
              return 0
            }
            has_tracked_changes() {
              [ -n "${'$'}(tracked_changes)" ]
            }
            remote_default_branch() {
              branch="${'$'}(git symbolic-ref --short refs/remotes/origin/HEAD 2>/dev/null | sed 's#^origin/##' || true)"
              if [ -z "${'$'}branch" ]; then
                branch="${'$'}(git remote show origin 2>/dev/null | sed -n 's/.*HEAD branch: //p' | head -n 1)"
              fi
              if [ -z "${'$'}branch" ]; then
                for candidate in release main master staging; do
                  if git show-ref --verify --quiet "refs/remotes/origin/${'$'}candidate"; then
                    branch="${'$'}candidate"
                    break
                  fi
                done
              fi
              printf "%s" "${'$'}branch"
            }
            valid_version_target() {
              target="${'$'}1"
              case "${'$'}target" in
                ''|-*|*::*|*..*|*//*|*/|*[!A-Za-z0-9._/@+-]*)
                  return 1
                  ;;
              esac
              return 0
            }
            checkout_requested_target() {
              target="${'$'}1"
              if [ -z "${'$'}target" ]; then
                return 64
              fi
              if ! valid_version_target "${'$'}target"; then
                return 64
              fi
              if git show-ref --verify --quiet "refs/remotes/origin/${'$'}target"; then
                if git show-ref --verify --quiet "refs/heads/${'$'}target"; then
                  git checkout "${'$'}target" >> "${'$'}LOG_FILE" 2>&1 &&
                    git merge --ff-only "origin/${'$'}target" >> "${'$'}LOG_FILE" 2>&1
                else
                  git checkout -B "${'$'}target" "origin/${'$'}target" >> "${'$'}LOG_FILE" 2>&1
                fi
                return "${'$'}?"
              fi
              if git show-ref --verify --quiet "refs/tags/${'$'}target"; then
                git checkout "tags/${'$'}target" >> "${'$'}LOG_FILE" 2>&1
                return "${'$'}?"
              fi
              git checkout "${'$'}target" >> "${'$'}LOG_FILE" 2>&1
            }
            checkout_update_target() {
              requested="${'$'}{1:-}"
              if [ -n "${'$'}requested" ]; then
                checkout_requested_target "${'$'}requested"
                return "${'$'}?"
              fi
              current_branch="${'$'}(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf HEAD)"
              upstream="${'$'}(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
              if [ "${'$'}current_branch" != "HEAD" ] && [ -n "${'$'}upstream" ]; then
                git pull --ff-only >> "${'$'}LOG_FILE" 2>&1
                return "${'$'}?"
              fi

              target_branch="${'$'}current_branch"
              if [ "${'$'}target_branch" = "HEAD" ] || ! git show-ref --verify --quiet "refs/remotes/origin/${'$'}target_branch"; then
                target_branch="${'$'}(remote_default_branch)"
              fi
              if [ -z "${'$'}target_branch" ]; then
                return 80
              fi

              if git show-ref --verify --quiet "refs/heads/${'$'}target_branch"; then
                git checkout "${'$'}target_branch" >> "${'$'}LOG_FILE" 2>&1 &&
                  git merge --ff-only "origin/${'$'}target_branch" >> "${'$'}LOG_FILE" 2>&1
              else
                git checkout -B "${'$'}target_branch" "origin/${'$'}target_branch" >> "${'$'}LOG_FILE" 2>&1
              fi
            }
            emit_official_versions() {
              if ! command -v git >/dev/null 2>&1; then
                return 69
              fi
              stable_file="${'$'}STATE_DIR/official-stable-tags.txt"
              heads_file="${'$'}STATE_DIR/official-heads.txt"
              git ls-remote --tags --refs "${'$'}OFFICIAL_REPO" 2>"${'$'}STATE_DIR/official-version-error.log" |
                awk '{ name=${'$'}2; sub(/^refs\/tags\//, "", name); if (name ~ /^v?[0-9]+(\.[0-9]+){1,3}${'$'}/) print name " " substr(${'$'}1, 1, 12) }' |
                sort -Vr |
                head -n 5 > "${'$'}stable_file"
              stable_code="${'$'}?"
              git ls-remote --heads "${'$'}OFFICIAL_REPO" 2>>"${'$'}STATE_DIR/official-version-error.log" > "${'$'}heads_file"
              heads_code="${'$'}?"
              if [ "${'$'}stable_code" -ne 0 ] || [ "${'$'}heads_code" -ne 0 ]; then
                return 70
              fi

              printf "official.repo=%s\n" "${'$'}OFFICIAL_REPO"
              i=1
              while IFS=' ' read -r tag commit; do
                [ -n "${'$'}tag" ] || continue
                printf "stable.%s.name=%s\n" "${'$'}i" "${'$'}tag"
                printf "stable.%s.target=%s\n" "${'$'}i" "${'$'}tag"
                printf "stable.%s.commit=%s\n" "${'$'}i" "${'$'}commit"
                i=${'$'}((i + 1))
              done < "${'$'}stable_file"

              i=1
              for branch in staging stats-2.0 release main dev develop testing test preview next canary beta alpha; do
                [ "${'$'}i" -le 3 ] || break
                line="${'$'}(awk -v ref="refs/heads/${'$'}branch" '${'$'}2 == ref { print ${'$'}1 " " ${'$'}2; exit }' "${'$'}heads_file")"
                [ -n "${'$'}line" ] || continue
                commit="${'$'}(printf "%s" "${'$'}line" | awk '{print substr(${'$'}1, 1, 12)}')"
                printf "test.%s.name=%s\n" "${'$'}i" "${'$'}branch"
                printf "test.%s.target=%s\n" "${'$'}i" "${'$'}branch"
                printf "test.%s.commit=%s\n" "${'$'}i" "${'$'}commit"
                i=${'$'}((i + 1))
              done
              return 0
            }
            install_node_dependencies() {
              if [ ! -f package.json ]; then
                return 0
              fi
              if ! command -v npm >/dev/null 2>&1; then
                return 69
              fi
              if [ -n "${'$'}{NPM_REGISTRY:-}" ]; then
                printf "[%s] npm registry=%s\n" "${'$'}(timestamp)" "${'$'}NPM_REGISTRY" >> "${'$'}LOG_FILE"
              fi
              npm install --no-audit --no-fund >> "${'$'}LOG_FILE" 2>&1
              npm_code="${'$'}?"
              cleanup_install_generated_changes
              return "${'$'}npm_code"
            }
            sync_origin_repo() {
              if [ -n "${'$'}{OFFICIAL_REPO:-}" ] && git remote get-url origin >/dev/null 2>&1; then
                git remote set-url origin "${'$'}OFFICIAL_REPO" >> "${'$'}LOG_FILE" 2>&1 || true
              fi
            }
            ensure_tavern_mutation_ready() {
              action="${'$'}1"
              if http_ok || is_running || [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
                write_status "error" "Please stop SillyTavern before ${'$'}action" true 77
                cat "${'$'}STATUS_FILE"
                return 77
              fi
              adopt_detected_tavern_dir >/dev/null 2>&1 || true
              if [ ! -d "${'$'}TAVERN_DIR" ]; then
                collect_tavern_dir_candidates
                write_tavern_dir_error
                cat "${'$'}STATUS_FILE"
                emit_tavern_dir_candidates
                return "${'$'}(missing_tavern_dir_exit_code)"
              fi
              if ! command -v git >/dev/null 2>&1; then
                write_status "error" "git command not found in Termux" false 69
                cat "${'$'}STATUS_FILE"
                return 69
              fi
              cd "${'$'}TAVERN_DIR" || {
                write_status "error" "failed to enter SillyTavern directory" false 74
                cat "${'$'}STATUS_FILE"
                return 74
              }
              if [ ! -d .git ]; then
                write_status "error" "SillyTavern directory is not a git repository" false 65
                cat "${'$'}STATUS_FILE"
                return 65
              fi
              cleanup_install_generated_changes
              if has_tracked_changes; then
                write_status "error" "SillyTavern has local tracked changes; update or rollback is blocked" false 78
                cat "${'$'}STATUS_FILE"
                printf "\n==== Git local changes ====\n"
                tracked_changes
                printf "==== end Git local changes ====\n"
                return 78
              fi
              sync_origin_repo
              return 0
            }
            emit_git_version_info() {
              printf "directory=%s\n" "${'$'}TAVERN_DIR"
              if [ -f package.json ]; then
                package_version="${'$'}(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' package.json | head -n 1)"
                [ -n "${'$'}package_version" ] && printf "package.version=%s\n" "${'$'}package_version"
              fi
              if command -v git >/dev/null 2>&1 && [ -d .git ]; then
                cleanup_install_generated_changes
                printf "git.branch=%s\n" "${'$'}(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf unknown)"
                printf "git.commit=%s\n" "${'$'}(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
                printf "git.describe=%s\n" "${'$'}(git describe --tags --always --dirty 2>/dev/null || printf unknown)"
                printf "git.remote=%s\n" "${'$'}(git remote get-url origin 2>/dev/null || printf unknown)"
                upstream="${'$'}(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
                [ -n "${'$'}upstream" ] && printf "git.upstream=%s\n" "${'$'}upstream"
                changes="${'$'}(tracked_changes)"
                if [ -n "${'$'}changes" ]; then
                  printf "git.localChanges=1\n"
                else
                  printf "git.localChanges=0\n"
                fi
                if [ -s "${'$'}ROLLBACK_FILE" ]; then
                  printf "rollback.target=%s\n" "${'$'}(cat "${'$'}ROLLBACK_FILE" 2>/dev/null || true)"
                fi
                printf "\n==== Git local changes ====\n"
                if [ -n "${'$'}changes" ]; then
                  printf "%s\n" "${'$'}changes"
                else
                  printf "clean\n"
                fi
              else
                printf "git=unavailable or not a git repository\n"
              fi
            }
        """.trimIndent()
    }

    private fun buildForegroundConsoleCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            open_browser_when_ready() {
              (
                i=0
                while [ "${'$'}i" -lt 60 ]; do
                  if http_ok; then
                    open_tavern_browser
                    exit 0
                  fi
                  i=${'$'}((i + 1))
                  sleep 1
                done
              ) &
            }
            if http_ok; then
              write_status "running" "SillyTavern is already running and HTTP endpoint is responding" true 0
              echo "SillyTavern is already running; opening browser."
              open_tavern_browser
              emit_status_snapshot
              exit 0
            fi
            if is_running; then
              if http_probe_available; then
                write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
                echo "SillyTavern process already exists, but HTTP endpoint is not responding."
                emit_status_snapshot
                exit 75
              fi
              write_status "running-unknown" "SillyTavern process already exists; install curl for HTTP verification" true 0
              echo "SillyTavern process already exists; install curl for HTTP verification."
              emit_status_snapshot
              exit 0
            fi
            if [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              if http_probe_available; then
                write_status "unreachable" "SillyTavern process already exists, but HTTP endpoint is not responding" true 75
                echo "SillyTavern process already exists, but HTTP endpoint is not responding."
                emit_status_snapshot
                exit 75
              fi
              write_status "running-unknown" "SillyTavern process already exists; install curl for HTTP verification" true 0
              echo "SillyTavern process already exists; install curl for HTTP verification."
              emit_status_snapshot
              exit 0
            fi
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            if [ ! -d "${'$'}TAVERN_DIR" ]; then
              collect_tavern_dir_candidates
              write_tavern_dir_error
              cat "${'$'}STATUS_FILE"
              emit_tavern_dir_candidates
              exit "${'$'}(missing_tavern_dir_exit_code)"
            fi
            if ! command -v node >/dev/null 2>&1; then
              write_status "error" "node command not found in Termux" false 69
              echo "node command not found in Termux"
              exit 69
            fi
            cd "${'$'}TAVERN_DIR" || {
              write_status "error" "failed to enter SillyTavern directory" false 74
              echo "failed to enter SillyTavern directory"
              exit 74
            }
            printf "\n[%s] ===== Lukoa launcher foreground session =====\n" "${'$'}(timestamp)" | tee -a "${'$'}LOG_FILE"
            printf "[%s] Termux foreground logging is enabled. Log file: %s\n" "${'$'}(timestamp)" "${'$'}LOG_FILE" | tee -a "${'$'}LOG_FILE"
            write_status "starting" "SillyTavern is starting in Termux foreground log mode" true 0
            printf "%s\n" "${'$'}${'$'}" > "${'$'}PID_FILE"
            open_browser_when_ready
            if [ -f "./start.sh" ]; then
              runner="${'$'}(start_script_runner)"
              printf "[%s] starting with %s ./start.sh in %s\n" "${'$'}(timestamp)" "${'$'}runner" "${'$'}TAVERN_DIR" | tee -a "${'$'}LOG_FILE"
              "${'$'}runner" ./start.sh 2>&1 | tee -a "${'$'}LOG_FILE"
              code="${'$'}?"
            elif [ -f "server.js" ]; then
              printf "[%s] starting with node server.js in %s\n" "${'$'}(timestamp)" "${'$'}TAVERN_DIR" | tee -a "${'$'}LOG_FILE"
              node server.js 2>&1 | tee -a "${'$'}LOG_FILE"
              code="${'$'}?"
            else
              write_status "error" "no start.sh or server.js found in SillyTavern directory" false 65
              echo "no start.sh or server.js found in SillyTavern directory"
              rm -f "${'$'}PID_FILE"
              exit 65
            fi
            rm -f "${'$'}PID_FILE"
            write_status "stopped" "SillyTavern foreground session exited" false "${'$'}code"
            exit "${'$'}code"
        """.trimIndent()
    }

    private fun buildLogSnapshotCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            if http_ok; then
              write_status "log" "SillyTavern recent log exported" true 0
            elif is_running; then
              write_status "log" "SillyTavern recent log exported; HTTP endpoint is not responding" true 75
            elif [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              write_status "log" "SillyTavern recent log exported; process exists but HTTP endpoint is not responding" true 75
            elif [ ! -d "${'$'}TAVERN_DIR" ]; then
              collect_tavern_dir_candidates
              write_tavern_dir_error
            else
              write_status "log" "SillyTavern recent log exported; process is not running" false 0
            fi
            cat "${'$'}STATUS_FILE"
            emit_tavern_dir_candidates
            if [ -f "${'$'}LOG_FILE" ]; then
              printf "\n==== SillyTavern recent log: %s ====\n" "${'$'}LOG_FILE"
              if command -v tail >/dev/null 2>&1; then
                tail -n "${'$'}{LUKOA_LOG_LINES:-240}" "${'$'}LOG_FILE" 2>/dev/null || true
              else
                cat "${'$'}LOG_FILE" 2>/dev/null || true
              fi
              printf "\n==== end SillyTavern recent log ====\n"
            else
              printf "\nNo SillyTavern log file yet: %s\n" "${'$'}LOG_FILE"
            fi
        """.trimIndent()
    }

    private fun buildLiveLogDeltaCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            if http_ok; then
              write_status "log" "SillyTavern live log synced" true 0
            elif is_running; then
              write_status "log" "SillyTavern live log synced; HTTP endpoint is not responding" true 75
            elif [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              write_status "log" "SillyTavern live log synced; process exists but HTTP endpoint is not responding" true 75
            elif [ ! -d "${'$'}TAVERN_DIR" ]; then
              collect_tavern_dir_candidates
              write_tavern_dir_error
            else
              write_status "log" "SillyTavern live log synced; process is not running" false 0
            fi
            cat "${'$'}STATUS_FILE"
            emit_tavern_dir_candidates
            emit_live_log_delta
        """.trimIndent()
    }

    private fun buildStatusSnapshotCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            if http_ok; then
              write_status "running" "SillyTavern HTTP endpoint is responding" true 0
            elif is_running; then
              if http_probe_available; then
                write_status "unreachable" "SillyTavern process exists, but HTTP endpoint is not responding" true 75
              else
                write_status "running-unknown" "SillyTavern process exists; install curl for HTTP verification" true 0
              fi
            elif [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              if http_probe_available; then
                write_status "unreachable" "SillyTavern process exists, but HTTP endpoint is not responding" true 75
              else
                write_status "running-unknown" "SillyTavern process exists; install curl for HTTP verification" true 0
              fi
            elif [ ! -d "${'$'}TAVERN_DIR" ]; then
              collect_tavern_dir_candidates
              write_tavern_dir_error
            else
              write_status "stopped" "SillyTavern process is not running" false 0
            fi
            cat "${'$'}STATUS_FILE"
            emit_tavern_dir_candidates
            if [ -f "${'$'}LOG_FILE" ]; then
              printf "\n==== SillyTavern recent log: %s ====\n" "${'$'}LOG_FILE"
              if command -v tail >/dev/null 2>&1; then
                tail -n "${'$'}{LUKOA_LOG_LINES:-240}" "${'$'}LOG_FILE" 2>/dev/null || true
              else
                cat "${'$'}LOG_FILE" 2>/dev/null || true
              fi
              printf "\n==== end SillyTavern recent log ====\n"
            else
              printf "\nNo SillyTavern log file yet: %s\n" "${'$'}LOG_FILE"
            fi
        """.trimIndent()
    }

    private fun buildStopCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            stopped_any=0
            if is_running; then
              pid="${'$'}(cat "${'$'}PID_FILE" 2>/dev/null || true)"
              kill "${'$'}pid" 2>/dev/null || true
              sleep 1
              if kill -0 "${'$'}pid" 2>/dev/null; then
                kill -9 "${'$'}pid" 2>/dev/null || true
              fi
              rm -f "${'$'}PID_FILE"
              stopped_any=1
            else
              rm -f "${'$'}PID_FILE"
            fi
            if [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              if kill_candidate_pids; then
                stopped_any=1
              fi
            fi
            if http_ok; then
              write_status "error" "SillyTavern HTTP endpoint is still responding after stop request" true 76
              emit_status_snapshot
              exit 76
            fi
            if [ "${'$'}stopped_any" = "1" ]; then
              write_status "stopped" "SillyTavern stopped" false 0
            else
              write_status "stopped" "SillyTavern was not running" false 0
            fi
            emit_status_snapshot
        """.trimIndent()
    }

    private fun buildTavernVersionCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            if [ ! -d "${'$'}TAVERN_DIR" ]; then
              collect_tavern_dir_candidates
              write_tavern_dir_error
              cat "${'$'}STATUS_FILE"
              emit_tavern_dir_candidates
              exit "${'$'}(missing_tavern_dir_exit_code)"
            fi
            cd "${'$'}TAVERN_DIR" || {
              write_status "error" "failed to enter SillyTavern directory" false 74
              cat "${'$'}STATUS_FILE"
              exit 74
            }
            write_status "version" "SillyTavern version information collected" "${'$'}(tavern_running_value)" 0
            cat "${'$'}STATUS_FILE"
            printf "\n==== SillyTavern version ====\n"
            emit_git_version_info
            printf "==== end SillyTavern version ====\n"
        """.trimIndent()
    }

    private fun buildTavernOfficialVersionsCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            write_status "official-versions" "Official SillyTavern versions collected" "${'$'}(tavern_running_value)" 0
            cat "${'$'}STATUS_FILE"
            printf "\n==== SillyTavern official versions ====\n"
            emit_official_versions
            code="${'$'}?"
            if [ "${'$'}code" -ne 0 ]; then
              write_status "error" "Failed to read official SillyTavern versions" false "${'$'}code"
              cat "${'$'}STATUS_FILE"
              printf "\n==== official version error ====\n"
              cat "${'$'}STATE_DIR/official-version-error.log" 2>/dev/null || true
              printf "\n==== end official version error ====\n"
              exit "${'$'}code"
            fi
            printf "==== end SillyTavern official versions ====\n"
        """.trimIndent()
    }

    private fun buildTermuxPackageMirrorCommand(mode: String): String {
        val label = if (mode == "tuna") "清华源" else "官方源"
        val quotedMode = shellSingleQuoted(mode)
        return """
            set -u
            ${buildSharedShellPrelude()}
            ${buildTermuxAptSourceHelpers()}
            selected_mode=$quotedMode
            write_termux_apt_source "${'$'}selected_mode"
            write_status "termux-repo" "Termux package mirror switched to $label" false 0
            cat "${'$'}STATUS_FILE"
            printf "\n==== Termux package mirror ====\n"
            printf "source=%s\n" "${'$'}(termux_apt_label_for_mode "${'$'}selected_mode")"
            printf "uri=%s\n" "${'$'}(termux_apt_uri_for_mode "${'$'}selected_mode")"
            printf "sourcesFile=%s\n" "${'$'}TERMUX_APT_SOURCES_FILE"
            printf "deb822File=%s\n" "${'$'}TERMUX_APT_DEB822_FILE"
            run_apt_update
            code="${'$'}?"
            printf "exitCode=%s\n" "${'$'}code"
            printf "==== end Termux package mirror ====\n"
            if [ "${'$'}code" -ne 0 ]; then
              if [ "${'$'}code" -eq 75 ]; then
                write_status "error" "Termux 正在安装或升级，请等它结束后再试" false 75
              else
                write_status "error" "Termux package mirror switched, but apt update failed" false "${'$'}code"
              fi
              cat "${'$'}STATUS_FILE"
            fi
            exit "${'$'}code"
        """.trimIndent()
    }

    private fun buildTermuxPackageMirrorStatusCommand(): String {
        return """
            set -u
            ${buildSharedShellPrelude()}
            ${buildTermuxAptSourceHelpers()}
            write_status "termux-repo-status" "Termux package mirror status collected" false 0
            cat "${'$'}STATUS_FILE"
            printf "\n==== Termux package mirror status ====\n"
            current_uri="${'$'}(termux_apt_current_uri)"
            printf "current.label=%s\n" "${'$'}(termux_apt_label_for_uri "${'$'}current_uri")"
            printf "current.uri=%s\n" "${'$'}current_uri"
            printf "sourcesFile=%s\n" "${'$'}TERMUX_APT_SOURCES_FILE"
            printf "deb822File=%s\n" "${'$'}TERMUX_APT_DEB822_FILE"
            printf "==== end Termux package mirror status ====\n"
        """.trimIndent()
    }

    private fun buildTermuxPackageMirrorCustomCommand(url: String): String {
        val quotedUrl = shellSingleQuoted(url)
        return """
            set -u
            ${buildSharedShellPrelude()}
            ${buildTermuxAptSourceHelpers()}
            custom_uri=$quotedUrl
            write_custom_termux_apt_source "${'$'}custom_uri"
            write_status "termux-repo" "Termux package mirror switched to custom source" false 0
            cat "${'$'}STATUS_FILE"
            printf "\n==== Termux package mirror ====\n"
            printf "source=%s\n" "自定义"
            printf "uri=%s\n" "${'$'}custom_uri"
            printf "current.label=%s\n" "${'$'}(termux_apt_label_for_uri "${'$'}custom_uri")"
            printf "current.uri=%s\n" "${'$'}custom_uri"
            printf "sourcesFile=%s\n" "${'$'}TERMUX_APT_SOURCES_FILE"
            printf "deb822File=%s\n" "${'$'}TERMUX_APT_DEB822_FILE"
            run_apt_update
            code="${'$'}?"
            printf "exitCode=%s\n" "${'$'}code"
            printf "==== end Termux package mirror ====\n"
            if [ "${'$'}code" -ne 0 ]; then
              if [ "${'$'}code" -eq 75 ]; then
                write_status "error" "Termux 正在安装或升级，请等它结束后再试" false 75
              else
                write_status "error" "Termux package mirror switched, but apt update failed" false "${'$'}code"
              fi
              cat "${'$'}STATUS_FILE"
            fi
            exit "${'$'}code"
        """.trimIndent()
    }

    private fun buildTermuxBootstrapCommand(configPolicy: String?): String {
        val quotedPolicy = shellSingleQuoted(AptConfigPolicyCodec.normalize(configPolicy).wireValue)
        return """
            set -u
            ${buildSharedShellPrelude()}
            ${buildTermuxAptSourceHelpers()}
            LUKOA_APT_CONFIG_POLICY=$quotedPolicy
            bootstrap_status() {
              bootstrap_status_value="${'$'}1"
              bootstrap_message="${'$'}2"
              bootstrap_running="${'$'}3"
              bootstrap_code="${'$'}{4:-0}"
              write_status "${'$'}bootstrap_status_value" "${'$'}bootstrap_message" "${'$'}bootstrap_running" "${'$'}bootstrap_code"
              emit_status_direct "${'$'}bootstrap_status_value" "${'$'}bootstrap_message" "${'$'}bootstrap_running" "${'$'}bootstrap_code"
            }
            bootstrap_status "termux-bootstrap" "Preparing Termux packages" false 0
            printf "\n==== Termux environment setup ====\n"
            if ! command -v apt-get >/dev/null 2>&1 && ! command -v apt >/dev/null 2>&1; then
              bootstrap_status "error" "apt command not found in Termux" false 69
              printf "apt command not found\n"
              printf "==== end Termux environment setup ====\n"
              exit 69
            fi
            export DEBIAN_FRONTEND=noninteractive
            write_termux_apt_source "tuna"
            printf "step=apt update\n"
            run_apt_update
            update_code="${'$'}?"
            printf "aptUpdateExitCode=%s\n" "${'$'}update_code"
            if [ "${'$'}update_code" -ne 0 ]; then
              printf "step=apt update fallback official\n"
              write_termux_apt_source "official"
              run_apt_update
              update_code="${'$'}?"
              printf "aptUpdateFallbackExitCode=%s\n" "${'$'}update_code"
            fi
            if [ "${'$'}update_code" -ne 0 ]; then
              if [ "${'$'}update_code" -eq 75 ]; then
                bootstrap_status "error" "Termux 正在安装或升级，请等它结束后再试" false 75
                printf "==== end Termux environment setup ====\n"
                exit 75
              fi
              bootstrap_status "error" "apt update failed; Termux package mirror is unavailable" false "${'$'}update_code"
              printf "==== end Termux environment setup ====\n"
              exit "${'$'}update_code"
            fi
            printf "step=apt full-upgrade\n"
            run_apt_full_upgrade
            upgrade_code="${'$'}?"
            printf "aptFullUpgradeExitCode=%s\n" "${'$'}upgrade_code"
            if [ "${'$'}upgrade_code" -eq 100 ]; then
              printf "step=dpkg configure recovery\n"
              run_dpkg_configure
              configure_code="${'$'}?"
              printf "dpkgConfigureExitCode=%s\n" "${'$'}configure_code"
              if [ "${'$'}configure_code" -eq 0 ]; then
                printf "step=apt fix-broken install\n"
                run_apt_fix_broken
                fix_code="${'$'}?"
                printf "aptFixBrokenExitCode=%s\n" "${'$'}fix_code"
                if [ "${'$'}fix_code" -eq 0 ]; then
                  printf "step=apt full-upgrade retry\n"
                  run_apt_full_upgrade
                  upgrade_code="${'$'}?"
                  printf "aptFullUpgradeRetryExitCode=%s\n" "${'$'}upgrade_code"
                else
                  upgrade_code="${'$'}fix_code"
                fi
              else
                upgrade_code="${'$'}configure_code"
              fi
            fi
            if [ "${'$'}upgrade_code" -ne 0 ]; then
              if [ "${'$'}upgrade_code" -eq 75 ]; then
                bootstrap_status "error" "Termux 正在安装或升级，请等它结束后再试" false 75
                printf "==== end Termux environment setup ====\n"
                exit 75
              fi
              bootstrap_status "error" "apt full-upgrade failed; Termux packages may be broken" false "${'$'}upgrade_code"
              printf "==== end Termux environment setup ====\n"
              exit "${'$'}upgrade_code"
            fi
            printf "step=apt install\n"
            run_apt_install git nodejs curl ca-certificates
            install_code="${'$'}?"
            printf "aptInstallExitCode=%s\n" "${'$'}install_code"
            if [ "${'$'}install_code" -eq 100 ]; then
              printf "step=dpkg configure recovery before install retry\n"
              run_dpkg_configure
              configure_code="${'$'}?"
              printf "dpkgConfigureBeforeInstallExitCode=%s\n" "${'$'}configure_code"
              if [ "${'$'}configure_code" -eq 0 ]; then
                printf "step=apt fix-broken install before install retry\n"
                run_apt_fix_broken
                fix_code="${'$'}?"
                printf "aptFixBrokenBeforeInstallExitCode=%s\n" "${'$'}fix_code"
                if [ "${'$'}fix_code" -eq 0 ]; then
                  printf "step=apt install retry\n"
                  run_apt_install git nodejs curl ca-certificates
                  install_code="${'$'}?"
                  printf "aptInstallRetryExitCode=%s\n" "${'$'}install_code"
                else
                  install_code="${'$'}fix_code"
                fi
              else
                install_code="${'$'}configure_code"
              fi
            fi
            missing=0
            for tool in git node npm curl; do
              if command -v "${'$'}tool" >/dev/null 2>&1; then
                printf "dependency.%s=ok\n" "${'$'}tool"
              else
                printf "dependency.%s=missing\n" "${'$'}tool"
                missing=1
              fi
            done
            if [ "${'$'}install_code" -ne 0 ] || [ "${'$'}missing" -ne 0 ]; then
              if [ "${'$'}install_code" -eq 75 ]; then
                bootstrap_status "error" "Termux 正在安装或升级，请等它结束后再试" false 75
                printf "==== end Termux environment setup ====\n"
                exit 75
              fi
              bootstrap_status "error" "Termux packages are still missing" false 69
              printf "==== end Termux environment setup ====\n"
              exit 69
            fi
            bootstrap_status "termux-bootstrap-ok" "Termux packages are ready" false 0
            printf "==== end Termux environment setup ====\n"
        """.trimIndent()
    }

    private fun buildTermuxAptSourceHelpers(): String {
        return """
            PREFIX_DIR="${'$'}{PREFIX:-/data/data/com.termux/files/usr}"
            TERMUX_APT_SOURCES_FILE="${'$'}PREFIX_DIR/etc/apt/sources.list"
            TERMUX_APT_DEB822_FILE="${'$'}PREFIX_DIR/etc/apt/sources.list.d/termux.sources"
            termux_apt_uri_for_mode() {
              case "${'$'}{1:-tuna}" in
                official)
                  printf "https://packages.termux.dev/apt/termux-main"
                  ;;
                *)
                  printf "https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main"
                  ;;
              esac
            }
            termux_apt_label_for_mode() {
              case "${'$'}{1:-tuna}" in
                official) printf "官方源" ;;
                *) printf "清华源" ;;
              esac
            }
            termux_apt_label_for_uri() {
              uri="${'$'}{1:-}"
              case "${'$'}uri" in
                *mirrors.tuna.tsinghua.edu.cn*) printf "清华源" ;;
                *packages.termux.dev*) printf "官方源" ;;
                "") printf "未读取" ;;
                *) printf "自定义" ;;
              esac
            }
            termux_apt_current_uri() {
              if [ -f "${'$'}TERMUX_APT_SOURCES_FILE" ]; then
                found_uri="${'$'}(awk '${'$'}1 == "deb" { print ${'$'}2; exit }' "${'$'}TERMUX_APT_SOURCES_FILE" 2>/dev/null || true)"
                if [ -n "${'$'}found_uri" ]; then
                  printf "%s" "${'$'}found_uri"
                  return 0
                fi
              fi
              if [ -f "${'$'}TERMUX_APT_DEB822_FILE" ]; then
                found_uri="${'$'}(awk 'tolower(${'$'}1) == "uris:" { print ${'$'}2; exit }' "${'$'}TERMUX_APT_DEB822_FILE" 2>/dev/null || true)"
                if [ -n "${'$'}found_uri" ]; then
                  printf "%s" "${'$'}found_uri"
                  return 0
                fi
              fi
              return 0
            }
            write_termux_apt_source() {
              mode="${'$'}{1:-tuna}"
              uri="${'$'}(termux_apt_uri_for_mode "${'$'}mode")"
              write_custom_termux_apt_source "${'$'}uri"
            }
            write_custom_termux_apt_source() {
              uri="${'$'}1"
              mkdir -p "${'$'}(dirname "${'$'}TERMUX_APT_SOURCES_FILE")" "${'$'}(dirname "${'$'}TERMUX_APT_DEB822_FILE")"
              stamp="${'$'}(date +%Y%m%d%H%M%S 2>/dev/null || printf now)"
              desired="deb ${'$'}uri stable main"
              current="${'$'}(cat "${'$'}TERMUX_APT_SOURCES_FILE" 2>/dev/null || true)"
              if [ "${'$'}current" != "${'$'}desired" ]; then
                [ -f "${'$'}TERMUX_APT_SOURCES_FILE" ] && cp "${'$'}TERMUX_APT_SOURCES_FILE" "${'$'}TERMUX_APT_SOURCES_FILE.lukoa-backup-${'$'}stamp" 2>/dev/null || true
                printf "%s\n" "${'$'}desired" > "${'$'}TERMUX_APT_SOURCES_FILE"
              fi
              if [ -f "${'$'}TERMUX_APT_DEB822_FILE" ]; then
                cp "${'$'}TERMUX_APT_DEB822_FILE" "${'$'}TERMUX_APT_DEB822_FILE.lukoa-backup-${'$'}stamp" 2>/dev/null || true
                rm -f "${'$'}TERMUX_APT_DEB822_FILE" 2>/dev/null || true
              fi
            }
            apt_lock_holders() {
              for lock_path in \
                "${'$'}PREFIX_DIR/var/lib/dpkg/lock-frontend" \
                "${'$'}PREFIX_DIR/var/lib/dpkg/lock" \
                "${'$'}PREFIX_DIR/var/lib/apt/lists/lock" \
                "${'$'}PREFIX_DIR/var/cache/apt/archives/lock"; do
                [ -e "${'$'}lock_path" ] || continue
                for fd_path in /proc/[0-9]*/fd/*; do
                  target="${'$'}(readlink "${'$'}fd_path" 2>/dev/null || true)"
                  [ "${'$'}target" = "${'$'}lock_path" ] || continue
                  pid="${'$'}{fd_path#/proc/}"
                  pid="${'$'}{pid%%/*}"
                  [ -n "${'$'}pid" ] && printf "%s\n" "${'$'}pid"
                done
              done | sort -u
            }
            wait_for_apt_locks() {
              deadline="${'$'}(( ${'$'}(date +%s 2>/dev/null || printf 0) + 90 ))"
              while :; do
                holders="${'$'}(apt_lock_holders | tr '\n' ' ' | sed 's/[[:space:]]*${'$'}//')"
                if [ -z "${'$'}holders" ]; then
                  return 0
                fi
                now="${'$'}(date +%s 2>/dev/null || printf 0)"
                if [ "${'$'}now" -ge "${'$'}deadline" ]; then
                  printf "aptLockHeld=1\n"
                  printf "aptLockPids=%s\n" "${'$'}holders"
                  printf "aptLockReason=Termux package manager is busy\n"
                  printf "aptLockAction=Open Termux and wait for the current install or upgrade to finish, then try again\n"
                  return 75
                fi
                printf "waitingForAptLockPids=%s\n" "${'$'}holders"
                sleep 2
              done
            }
            apt_config_option() {
              case "${'$'}{LUKOA_APT_CONFIG_POLICY:-keep}" in
                replace|new|confnew)
                  printf "%s" "--force-confnew"
                  ;;
                *)
                  printf "%s" "--force-confold"
                  ;;
              esac
            }
            run_logged() {
              rc_file="${'$'}STATE_DIR/run-exit-${'$'}${'$'}"
              rm -f "${'$'}rc_file" 2>/dev/null || true
              (
                "${'$'}@"
                printf "%s" "${'$'}?" > "${'$'}rc_file"
              ) 2>&1 | tee -a "${'$'}LOG_FILE"
              code="${'$'}(cat "${'$'}rc_file" 2>/dev/null || printf 1)"
              rm -f "${'$'}rc_file" 2>/dev/null || true
              case "${'$'}code" in ''|*[!0-9]*) code=1 ;; esac
              return "${'$'}code"
            }
            run_apt_update() {
              wait_for_apt_locks || return "${'$'}?"
              if command -v apt-get >/dev/null 2>&1; then
                run_logged apt-get update
              else
                run_logged apt update
              fi
            }
            run_apt_full_upgrade() {
              wait_for_apt_locks || return "${'$'}?"
              conf_option="${'$'}(apt_config_option)"
              if command -v apt-get >/dev/null 2>&1; then
                run_logged apt-get -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  full-upgrade
              else
                run_logged apt -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  full-upgrade
              fi
            }
            run_apt_install() {
              wait_for_apt_locks || return "${'$'}?"
              conf_option="${'$'}(apt_config_option)"
              if command -v apt-get >/dev/null 2>&1; then
                run_logged apt-get install -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  "${'$'}@"
              else
                run_logged apt install -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  "${'$'}@"
              fi
            }
            run_dpkg_configure() {
              wait_for_apt_locks || return "${'$'}?"
              if command -v dpkg >/dev/null 2>&1; then
                run_logged dpkg --force-confdef --force-confold --configure -a
              else
                return 69
              fi
            }
            run_apt_fix_broken() {
              wait_for_apt_locks || return "${'$'}?"
              conf_option="${'$'}(apt_config_option)"
              if command -v apt-get >/dev/null 2>&1; then
                run_logged apt-get -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  -f install
              else
                run_logged apt -y \
                  -o Dpkg::Options::=--force-confdef \
                  -o Dpkg::Options::="${'$'}conf_option" \
                  --fix-broken install
              fi
            }
        """.trimIndent()
    }

    private fun buildTavernInstallCommand(args: TavernInstallCommandArgs): String {
        val quotedTarget = shellSingleQuoted(args.target.ifBlank { TavernInstallDefaults.Release.target })
        val quotedRepoUrl = shellSingleQuoted(args.repoUrl)
        val quotedPolicy = shellSingleQuoted(args.configPolicy.wireValue)
        return """
            set -u
            ${buildSharedShellPrelude()}
            ${buildTermuxAptSourceHelpers()}
            target=$quotedTarget
            OFFICIAL_REPO=$quotedRepoUrl
            LUKOA_APT_CONFIG_POLICY=$quotedPolicy
            if http_ok || is_running || [ -n "${'$'}(candidate_pids | head -n 1)" ]; then
              write_status "error" "Please stop SillyTavern before installing" true 77
              cat "${'$'}STATUS_FILE"
              exit 77
            fi
            ensure_install_packages() {
              need=""
              command -v git >/dev/null 2>&1 || need="${'$'}need git"
              if ! command -v node >/dev/null 2>&1 || ! command -v npm >/dev/null 2>&1; then
                need="${'$'}need nodejs"
              fi
              command -v curl >/dev/null 2>&1 || need="${'$'}need curl"
              if [ -z "${'$'}need" ]; then
                return 0
              fi
              printf "step=pkg install%s\n" "${'$'}need"
              run_apt_update
              update_code="${'$'}?"
              printf "installDependencyAptUpdateExitCode=%s\n" "${'$'}update_code"
              if [ "${'$'}update_code" -ne 0 ]; then
                return "${'$'}update_code"
              fi
              run_apt_install ${'$'}need
              install_dependency_code="${'$'}?"
              printf "installDependencyExitCode=%s\n" "${'$'}install_dependency_code"
              if [ "${'$'}install_dependency_code" -eq 100 ]; then
                printf "step=dpkg configure recovery before tavern install\n"
                run_dpkg_configure
                configure_code="${'$'}?"
                printf "installDependencyDpkgConfigureExitCode=%s\n" "${'$'}configure_code"
                if [ "${'$'}configure_code" -eq 0 ]; then
                  printf "step=apt fix-broken install before tavern install\n"
                  run_apt_fix_broken
                  fix_code="${'$'}?"
                  printf "installDependencyFixBrokenExitCode=%s\n" "${'$'}fix_code"
                  if [ "${'$'}fix_code" -eq 0 ]; then
                    run_apt_install ${'$'}need
                    install_dependency_code="${'$'}?"
                    printf "installDependencyRetryExitCode=%s\n" "${'$'}install_dependency_code"
                  else
                    install_dependency_code="${'$'}fix_code"
                  fi
                else
                  install_dependency_code="${'$'}configure_code"
                fi
              fi
              return "${'$'}install_dependency_code"
            }
            ensure_install_packages
            dependency_code="${'$'}?"
            if [ "${'$'}dependency_code" -ne 0 ]; then
              write_status "error" "Termux dependency install failed before SillyTavern install" false "${'$'}dependency_code"
              cat "${'$'}STATUS_FILE"
              printf "\n==== SillyTavern install dependencies ====\n"
              printf "exitCode=%s\n" "${'$'}dependency_code"
              printf "==== end SillyTavern install ====\n"
              exit "${'$'}dependency_code"
            fi
            adopt_detected_tavern_dir >/dev/null 2>&1 || true
            collect_tavern_dir_candidates
            if [ "${'$'}{TAVERN_CANDIDATE_COUNT:-0}" -gt 1 ] && [ ! -e "${'$'}TAVERN_DIR" ]; then
              write_tavern_dir_error
              cat "${'$'}STATUS_FILE"
              emit_tavern_dir_candidates
              exit "${'$'}(missing_tavern_dir_exit_code)"
            fi
            if [ -e "${'$'}TAVERN_DIR" ] && [ -n "${'$'}(ls -A "${'$'}TAVERN_DIR" 2>/dev/null || true)" ]; then
              write_status "error" "SillyTavern directory already exists and is not empty" false 73
              cat "${'$'}STATUS_FILE"
              printf "\ndirectory=%s\n" "${'$'}TAVERN_DIR"
              exit 73
            fi
            parent="${'$'}(dirname "${'$'}TAVERN_DIR")"
            mkdir -p "${'$'}parent"
            printf "\n[%s] ===== Lukoa launcher tavern install =====\n" "${'$'}(timestamp)" >> "${'$'}LOG_FILE"
            printf "[%s] target=%s repo=%s\n" "${'$'}(timestamp)" "${'$'}target" "${'$'}OFFICIAL_REPO" >> "${'$'}LOG_FILE"
            git clone -b "${'$'}target" "${'$'}OFFICIAL_REPO" "${'$'}TAVERN_DIR" >> "${'$'}LOG_FILE" 2>&1
            clone_code="${'$'}?"
            if [ "${'$'}clone_code" -ne 0 ]; then
              write_status "error" "git clone failed; check tavern.log" false "${'$'}clone_code"
              cat "${'$'}STATUS_FILE"
              printf "\n==== Recent install log ====\n"
              tail -n 120 "${'$'}LOG_FILE" 2>/dev/null || true
              printf "==== end SillyTavern install ====\n"
              exit "${'$'}clone_code"
            fi
            cd "${'$'}TAVERN_DIR" || {
              write_status "error" "failed to enter SillyTavern directory" false 74
              cat "${'$'}STATUS_FILE"
              exit 74
            }
            npm_code=0
            install_node_dependencies
            npm_code="${'$'}?"
            if [ "${'$'}npm_code" -eq 0 ]; then
              write_status "installed" "SillyTavern installed successfully" false 0
              code=0
            else
              write_status "error" "npm install failed; check tavern.log" false "${'$'}npm_code"
              code="${'$'}npm_code"
            fi
            cat "${'$'}STATUS_FILE"
            printf "\n==== SillyTavern install ====\n"
            printf "directory=%s\n" "${'$'}TAVERN_DIR"
            printf "target=%s\n" "${'$'}target"
            printf "exitCode=%s\n" "${'$'}code"
            printf "npmExitCode=%s\n" "${'$'}npm_code"
            printf "\n==== Current SillyTavern version ====\n"
            emit_git_version_info
            printf "\n==== Recent install log ====\n"
            tail -n 120 "${'$'}LOG_FILE" 2>/dev/null || true
            printf "==== end SillyTavern install ====\n"
            exit "${'$'}code"
        """.trimIndent()
    }

    private fun buildTavernUpdateCommand(args: TavernVersionCommandArgs) : String {
        val quotedTarget = shellSingleQuoted(args.target)
        val quotedRepoUrl = shellSingleQuoted(args.repoUrl)
        return """
            set -u
            ${buildSharedShellPrelude()}
            requested_target=$quotedTarget
            OFFICIAL_REPO=$quotedRepoUrl
            ensure_tavern_mutation_ready "updating source files"
            preflight_code="${'$'}?"
            if [ "${'$'}preflight_code" -ne 0 ]; then
              exit "${'$'}preflight_code"
            fi
            if [ -z "${'$'}requested_target" ]; then
              write_status "error" "No SillyTavern update target selected" false 64
              cat "${'$'}STATUS_FILE"
              exit 64
            fi

            before_full="${'$'}(git rev-parse HEAD 2>/dev/null || printf unknown)"
            before="${'$'}(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
            printf "\n[%s] ===== Lukoa launcher tavern update =====\n" "${'$'}(timestamp)" >> "${'$'}LOG_FILE"
            printf "[%s] before=%s target=%s repo=%s\n" "${'$'}(timestamp)" "${'$'}before_full" "${'$'}requested_target" "${'$'}OFFICIAL_REPO" >> "${'$'}LOG_FILE"

            git fetch --all --tags --prune >> "${'$'}LOG_FILE" 2>&1
            fetch_code="${'$'}?"
            if [ "${'$'}fetch_code" -ne 0 ]; then
              write_status "error" "git fetch failed; check tavern.log" false "${'$'}fetch_code"
              cat "${'$'}STATUS_FILE"
              printf "\n==== SillyTavern update ====\n"
              printf "directory=%s\n" "${'$'}TAVERN_DIR"
              printf "before=%s\n" "${'$'}before"
              printf "after=%s\n" "${'$'}before"
              printf "exitCode=%s\n" "${'$'}fetch_code"
              printf "\n==== Recent update log ====\n"
              tail -n 100 "${'$'}LOG_FILE" 2>/dev/null || true
              printf "==== end SillyTavern update ====\n"
              exit "${'$'}fetch_code"
            fi

            checkout_update_target "${'$'}requested_target"
            git_code="${'$'}?"
            after="${'$'}(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
            npm_code=0
            if [ "${'$'}git_code" -eq 0 ]; then
              printf "%s\n" "${'$'}before_full" > "${'$'}ROLLBACK_FILE"
              printf "[%s] rollback target saved: %s\n" "${'$'}(timestamp)" "${'$'}before_full" >> "${'$'}LOG_FILE"
              install_node_dependencies
              npm_code="${'$'}?"
            fi

            if [ "${'$'}git_code" -eq 0 ] && [ "${'$'}npm_code" -eq 0 ]; then
              write_status "updated" "SillyTavern source updated successfully" false 0
              code=0
            elif [ "${'$'}git_code" -eq 80 ]; then
              write_status "error" "Could not find a remote branch to update" false 80
              code=80
            elif [ "${'$'}git_code" -ne 0 ]; then
              write_status "error" "git update failed; check tavern.log" false "${'$'}git_code"
              code="${'$'}git_code"
            elif [ "${'$'}npm_code" -eq 69 ]; then
              write_status "error" "npm command not found in Termux" false 69
              code=69
            else
              write_status "error" "npm install failed; check tavern.log" false "${'$'}npm_code"
              code="${'$'}npm_code"
            fi

            cat "${'$'}STATUS_FILE"
            printf "\n==== SillyTavern update ====\n"
            printf "directory=%s\n" "${'$'}TAVERN_DIR"
            printf "target=%s\n" "${'$'}requested_target"
            printf "repo=%s\n" "${'$'}OFFICIAL_REPO"
            printf "before=%s\n" "${'$'}before"
            printf "after=%s\n" "${'$'}after"
            printf "exitCode=%s\n" "${'$'}code"
            printf "npmExitCode=%s\n" "${'$'}npm_code"
            if [ -s "${'$'}ROLLBACK_FILE" ]; then
              printf "rollback.target=%s\n" "${'$'}(cat "${'$'}ROLLBACK_FILE" 2>/dev/null || true)"
            fi
            printf "\n==== Git status ====\n"
            git status --short 2>/dev/null || true
            printf "\n==== Recent update log ====\n"
            tail -n 120 "${'$'}LOG_FILE" 2>/dev/null || true
            printf "\n==== Current SillyTavern version ====\n"
            emit_git_version_info
            printf "==== end SillyTavern update ====\n"
            exit "${'$'}code"
        """.trimIndent()
    }

    private fun buildTavernRollbackCommand(args: TavernVersionCommandArgs): String {
        val quotedTarget = shellSingleQuoted(args.target)
        val quotedRepoUrl = shellSingleQuoted(args.repoUrl)
        return """
            set -u
            ${buildSharedShellPrelude()}
            requested_target=$quotedTarget
            OFFICIAL_REPO=$quotedRepoUrl
            ensure_tavern_mutation_ready "rolling back source files"
            preflight_code="${'$'}?"
            if [ "${'$'}preflight_code" -ne 0 ]; then
              exit "${'$'}preflight_code"
            fi
            if [ -z "${'$'}requested_target" ]; then
              write_status "error" "No SillyTavern rollback target selected" false 64
              cat "${'$'}STATUS_FILE"
              exit 64
            fi

            before_full="${'$'}(git rev-parse HEAD 2>/dev/null || printf unknown)"
            before="${'$'}(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
            printf "\n[%s] ===== Lukoa launcher tavern rollback =====\n" "${'$'}(timestamp)" >> "${'$'}LOG_FILE"
            printf "[%s] before=%s target=%s repo=%s\n" "${'$'}(timestamp)" "${'$'}before_full" "${'$'}requested_target" "${'$'}OFFICIAL_REPO" >> "${'$'}LOG_FILE"

            git fetch --all --tags --prune >> "${'$'}LOG_FILE" 2>&1
            fetch_code="${'$'}?"
            if [ "${'$'}fetch_code" -ne 0 ]; then
              write_status "error" "git fetch failed before rollback; check tavern.log" false "${'$'}fetch_code"
              cat "${'$'}STATUS_FILE"
              printf "\n==== Recent rollback log ====\n"
              tail -n 120 "${'$'}LOG_FILE" 2>/dev/null || true
              printf "==== end SillyTavern rollback ====\n"
              exit "${'$'}fetch_code"
            fi
            checkout_requested_target "${'$'}requested_target"
            git_code="${'$'}?"
            after="${'$'}(git rev-parse --short HEAD 2>/dev/null || printf unknown)"
            npm_code=0
            if [ "${'$'}git_code" -eq 0 ]; then
              printf "%s\n" "${'$'}before_full" > "${'$'}ROLLBACK_FILE"
              install_node_dependencies
              npm_code="${'$'}?"
            fi

            if [ "${'$'}git_code" -eq 0 ] && [ "${'$'}npm_code" -eq 0 ]; then
              write_status "rolled-back" "SillyTavern source rolled back successfully" false 0
              code=0
            elif [ "${'$'}git_code" -ne 0 ]; then
              write_status "error" "git rollback failed; check tavern.log" false "${'$'}git_code"
              code="${'$'}git_code"
            elif [ "${'$'}npm_code" -eq 69 ]; then
              write_status "error" "npm command not found in Termux" false 69
              code=69
            else
              write_status "error" "npm install after rollback failed; check tavern.log" false "${'$'}npm_code"
              code="${'$'}npm_code"
            fi

            cat "${'$'}STATUS_FILE"
            printf "\n==== SillyTavern rollback ====\n"
            printf "directory=%s\n" "${'$'}TAVERN_DIR"
            printf "target=%s\n" "${'$'}requested_target"
            printf "repo=%s\n" "${'$'}OFFICIAL_REPO"
            printf "before=%s\n" "${'$'}before"
            printf "after=%s\n" "${'$'}after"
            printf "exitCode=%s\n" "${'$'}code"
            printf "npmExitCode=%s\n" "${'$'}npm_code"
            if [ -s "${'$'}ROLLBACK_FILE" ]; then
              printf "rollback.target=%s\n" "${'$'}(cat "${'$'}ROLLBACK_FILE" 2>/dev/null || true)"
            fi
            printf "\n==== Recent rollback log ====\n"
            tail -n 120 "${'$'}LOG_FILE" 2>/dev/null || true
            printf "\n==== Current SillyTavern version ====\n"
            emit_git_version_info
            printf "==== end SillyTavern rollback ====\n"
            exit "${'$'}code"
        """.trimIndent()
    }

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"
        const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        @SuppressLint("SdCardPath")
        const val TERMUX_HOME = "/data/data/com.termux/files/home"
        @SuppressLint("SdCardPath")
        const val TERMUX_SH_PATH = "/data/data/com.termux/files/usr/bin/sh"
        const val TERMUX_SCRIPT_PATH = "$TERMUX_HOME/.local/bin/lukoa-tavern.sh"

        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

        const val EXTRA_EXECUTION_ID = "execution_id"
        const val EXTRA_LUKOA_COMMAND = "lukoa_command"
        const val EXTRA_LUKOA_NONCE = "lukoa_nonce"

        private val executionCounter = AtomicInteger(1000)
        private const val INSTALL_EOF_MARKER = "LUKOA_LAUNCHER_SCRIPT_EOF"

        fun nextExecutionId(): Int = executionCounter.incrementAndGet()
    }
}
