package moe.lukoa.launcher

import android.content.Context
import android.os.Build
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DiagnosticSnapshot(
    val state: LauncherUiState,
    val versionInfo: VersionInfo,
    val termuxInstalled: Boolean,
    val runCommandPermissionGranted: Boolean,
    val termuxExternalAppsBlocked: Boolean,
    val tavernRunning: Boolean,
    val tavernStarting: Boolean,
    val tavernInstallDetected: Boolean?,
    val actionInProgress: Boolean,
    val busyLabel: String?,
    val tavernVersionInfo: TavernVersionInfo,
    val officialVersions: TavernOfficialVersions,
    val selectedVersion: TavernVersionChoice?,
    val tavernMirrorConfig: TavernMirrorConfig,
    val tavernPathConfig: TavernPathConfig,
    val githubRepository: String,
    val githubUpdateState: GithubUpdateUiState,
    val issueAnalysis: List<TavernIssue>,
)

object DiagnosticLogExporter {
    private const val MAX_LOG_CHARS = 120_000
    private const val MAX_RESULT_CHARS = 20_000
    private const val MAX_EMBEDDED_TAVERN_LOG_LINES = 50

    fun export(context: Context, snapshot: DiagnosticSnapshot): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-diagnostic-$timestamp", "txt")
        val results = TermuxResultStore.recent(context)
        val fullTermuxLog = RuntimeLogArchive.readTermux(context, snapshot.state.termuxLog)
        val fullAppLog = RuntimeLogArchive.readApp(context, snapshot.state.appLog)
        file.writeText(
            buildReport(
                snapshot = snapshot,
                results = results,
                fullTermuxLog = fullTermuxLog,
                fullAppLog = fullAppLog,
                timestamp = timestamp,
                nextAutoBackupAt = AutoBackupScheduler.nextTriggerAt(context),
            ),
            Charsets.UTF_8,
        )
        return file
    }

    private fun buildReport(
        snapshot: DiagnosticSnapshot,
        results: List<TermuxCommandResult>,
        fullTermuxLog: String,
        fullAppLog: String,
        timestamp: String,
        nextAutoBackupAt: Long,
    ): String {
        return buildString {
            appendLine("露科亚启动器诊断日志")
            appendLine("导出时间：$timestamp")
            appendLine("说明：明显密钥字段会自动打码，但日志里仍可能包含路径或角色名称。")
            appendLine()

            appendLine("==== App ====")
            appendLine("包名=${snapshot.versionInfo.packageName}")
            appendLine("版本=${snapshot.versionInfo.versionName}")
            appendLine("版本代码=${snapshot.versionInfo.versionCode}")
            appendLine("备份格式=${snapshot.versionInfo.backupSchemaVersion}")
            appendLine("Android SDK=${Build.VERSION.SDK_INT}")
            appendLine("设备=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()

            appendLine("==== 当前判断 ====")
            appendLine("状态摘要=${snapshot.state.summary}")
            appendLine("当前状态=${snapshot.state.status}")
            appendLine("已确认=${snapshot.state.verified}")
            appendLine("Termux已安装=${snapshot.termuxInstalled}")
            appendLine("RUN_COMMAND权限=${snapshot.runCommandPermissionGranted}")
            appendLine("Termux外部调用被拦=${snapshot.termuxExternalAppsBlocked}")
            appendLine("酒馆运行中=${snapshot.tavernRunning}")
            appendLine("酒馆启动中=${snapshot.tavernStarting}")
            appendLine("酒馆安装状态=${snapshot.tavernInstallDetected?.toString() ?: "未知"}")
            appendLine("当前忙碌=${snapshot.actionInProgress}")
            appendLine("忙碌任务=${snapshot.busyLabel ?: "无"}")
            appendLine("返回等待=${snapshot.state.termuxReturnDelayMs}ms")
            appendLine()

            appendLine("==== 版本管理 ====")
            appendLine("酒馆版本=${snapshot.tavernVersionInfo.displayVersion}")
            appendLine("已读取版本=${snapshot.tavernVersionInfo.hasData}")
            appendLine("酒馆未安装=${snapshot.tavernVersionInfo.notInstalled}")
            appendLine("目录=${snapshot.tavernVersionInfo.directory}")
            appendLine("分支=${snapshot.tavernVersionInfo.branch}")
            appendLine("提交=${snapshot.tavernVersionInfo.commit}")
            appendLine("描述=${snapshot.tavernVersionInfo.describe}")
            appendLine("远端=${snapshot.tavernVersionInfo.remote}")
            appendLine("上游=${snapshot.tavernVersionInfo.upstream}")
            appendLine("本地改动=${snapshot.tavernVersionInfo.hasLocalChanges}")
            appendLine("本地改动预览=${snapshot.tavernVersionInfo.changedFilesPreview.ifBlank { "无" }}")
            appendLine("回退点=${snapshot.tavernVersionInfo.rollbackDisplay}")
            appendLine("已选目标=${snapshot.selectedVersion?.label ?: "未选择"}")
            appendLine("稳定版列表=${snapshot.officialVersions.stable.joinToString { it.label }}")
            appendLine("测试版列表=${snapshot.officialVersions.test.joinToString { it.label }}")
            appendLine()

            appendLine("==== 网络与镜像源 ====")
            appendLine("酒馆源=${snapshot.tavernMirrorConfig.repoLabel}")
            appendLine("酒馆源地址=${snapshot.tavernMirrorConfig.normalizedRepoUrl}")
            appendLine("npm源=${snapshot.tavernMirrorConfig.npmLabel}")
            appendLine("npm源地址=${snapshot.tavernMirrorConfig.normalizedNpmRegistry}")
            appendLine("酒馆目录=${snapshot.tavernPathConfig.displayTavernDir}")
            appendLine()

            appendLine("==== 备份 ====")
            appendLine("自动备份=${snapshot.state.autoBackupEnabled}")
            appendLine("自动备份间隔=${formatBackupInterval(snapshot.state.autoBackupIntervalMinutes)}")
            appendLine("自动备份间隔分钟=${snapshot.state.autoBackupIntervalMinutes}")
            appendLine("自动备份保留=${snapshot.state.autoBackupKeepCount}个")
            appendLine("下次计划自动备份=${formatScheduledTime(nextAutoBackupAt)}")
            appendLine("备份数量=${snapshot.state.backupHistory.size}")
            snapshot.state.backupHistory.forEachIndexed { index, path ->
                appendLine("备份${index + 1}=$path")
            }
            appendLine()

            appendLine("==== GitHub 更新 ====")
            appendLine("仓库=${snapshot.githubRepository}")
            appendLine("检查中=${snapshot.githubUpdateState.checking}")
            appendLine("下载中=${snapshot.githubUpdateState.downloading}")
            appendLine("提示=${snapshot.githubUpdateState.message}")
            appendLine("最新版本=${snapshot.githubUpdateState.latest?.versionName ?: "未读取"}")
            appendLine("有新版本=${snapshot.githubUpdateState.hasUpdate}")
            appendLine("APK=${snapshot.githubUpdateState.latest?.apkName.orEmpty().ifBlank { "无" }}")
            appendLine()

            appendLine("==== 露科亚问题分析辅助 ====")
            if (snapshot.issueAnalysis.isEmpty()) {
                appendLine("暂未发现常见报错。")
            } else {
                snapshot.issueAnalysis.forEachIndexed { index, issue ->
                    appendLine("${index + 1}. ${issue.title}")
                    appendLine("   原因=${issue.detail}")
                    appendLine("   建议=${issue.action}")
                }
            }
            appendLine()

            appendLine("==== 最近 Termux 命令 ====")
            if (results.isEmpty()) {
                appendLine("暂无记录。")
            } else {
                results.forEachIndexed { index, result ->
                    appendLine("-- #${index + 1} ${result.command.ifBlank { "Termux" }} --")
                    appendLine("executionId=${result.executionId}")
                    appendLine("timeMillis=${result.timeMillis}")
                    appendLine("hasResultBundle=${result.hasResultBundle}")
                    appendLine("exitCode=${result.exitCode?.toString() ?: "无"}")
                    appendLine("errCode=${result.errCode?.toString() ?: "无"}")
                    appendLine("errMessage=${redact(result.errMessage).ifBlank { "无" }}")
                    appendLine("stdoutOriginalLength=${result.stdoutOriginalLength}")
                    appendLine("stderrOriginalLength=${result.stderrOriginalLength}")
                    appendLine("stdout:")
                    appendLine(compactTermuxOutput(result.stdout).trimForReport(MAX_RESULT_CHARS))
                    appendLine("stderr:")
                    appendLine(compactTermuxOutput(result.stderr).trimForReport(MAX_RESULT_CHARS))
                }
            }
            appendLine()

            appendLine("==== Termux 调用返回 ====")
            appendLine(compactTermuxOutput(fullTermuxLog).trimForReport(MAX_LOG_CHARS))
            appendLine()

            appendLine("==== App 操作反馈 ====")
            appendLine(redact(fullAppLog).trimForReport(MAX_LOG_CHARS))
        }
    }

    private fun String.trimForReport(maxChars: Int): String {
        if (length <= maxChars) return this
        val omitted = length - maxChars
        return takeLast(maxChars) + "\n... 前面已截断 $omitted 个字符 ..."
    }

    private fun compactTermuxOutput(value: String): String {
        val redacted = redact(value)
        if (!redacted.contains("==== SillyTavern recent log:")) return redacted

        val output = mutableListOf<String>()
        val logBuffer = mutableListOf<String>()
        var inTavernLog = false
        var compactedSections = 0

        redacted.lineSequence().forEach { line ->
            when {
                line.startsWith("==== SillyTavern recent log:") -> {
                    inTavernLog = true
                    logBuffer.clear()
                    output += line
                }
                line.startsWith("==== end SillyTavern recent log ====") && inTavernLog -> {
                    val omitted = (logBuffer.size - MAX_EMBEDDED_TAVERN_LOG_LINES).coerceAtLeast(0)
                    if (omitted > 0) {
                        output += "... 已省略 $omitted 行重复酒馆日志 ..."
                    }
                    output += logBuffer.takeLast(MAX_EMBEDDED_TAVERN_LOG_LINES)
                    output += line
                    inTavernLog = false
                    compactedSections += 1
                }
                inTavernLog -> {
                    logBuffer += line
                }
                else -> output += line
            }
        }

        if (inTavernLog) {
            val omitted = (logBuffer.size - MAX_EMBEDDED_TAVERN_LOG_LINES).coerceAtLeast(0)
            if (omitted > 0) {
                output += "... 已省略 $omitted 行重复酒馆日志 ..."
            }
            output += logBuffer.takeLast(MAX_EMBEDDED_TAVERN_LOG_LINES)
        }

        if (compactedSections > 0) {
            output += ""
            output += "诊断日志已压缩 $compactedSections 段重复酒馆日志。"
        }
        return output.joinToString("\n")
    }

    private fun redact(value: String): String {
        if (value.isBlank()) return value
        return value
            .replace(Regex("""(?i)(api[_-]?key|authorization|bearer|token|secret|password|passwd|pwd)(\s*[:=]\s*)([^\s"',;]+)""")) {
                "${it.groupValues[1]}${it.groupValues[2]}***"
            }
            .replace(Regex("""(?i)(sk-[A-Za-z0-9_\-]{12,})"""), "sk-***")
            .replace(Regex("""(?i)(xox[baprs]-[A-Za-z0-9\-]{12,})"""), "***")
            .replace(Regex("""(?i)(gh[pousr]_[A-Za-z0-9_]{12,})"""), "***")
    }

    private fun formatScheduledTime(timeMillis: Long): String {
        if (timeMillis <= 0L) return "未安排"
        return runCatching {
            java.time.Instant.ofEpochMilli(timeMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }.getOrElse { timeMillis.toString() }
    }
}
