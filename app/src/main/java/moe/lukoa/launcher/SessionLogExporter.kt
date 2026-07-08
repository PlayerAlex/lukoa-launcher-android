package moe.lukoa.launcher

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SessionLogExporter {
    private const val MAX_EXPORTED_LOG_CHARS = 120_000

    fun export(
        context: Context,
        state: LauncherUiState,
        mode: ExportLogMode,
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-session-log-$timestamp", "txt")
        val fullTermuxLog = RuntimeLogArchive.readTermuxCommand(context, state.termuxLog)
        val fullTavernRuntimeLog = RuntimeLogArchive.readTavernRuntime(context, state.tavernRuntimeLog)
        val fullAppLog = RuntimeLogArchive.readApp(context, state.appLog)
        val foregroundConsoleLog = TavernLogSignals.latestForegroundSession(fullTavernRuntimeLog)
            .ifBlank { fullTermuxLog }
        file.writeText(
            buildString {
                appendLine("露科亚启动器运行日志")
                appendLine("导出时间：${LocalTime.now().withNano(0)}")
                appendLine("范围：从上次清除对应日志后开始累计")
                appendLine("状态摘要：${state.summary}")
                appendLine("当前状态：${state.status}")
                if (mode.includeTermux) {
                    appendLine()
                    appendLine("==== Termux 前台回传 ====")
                    appendLine(trimForExport(foregroundConsoleLog))
                    appendLine()
                    appendLine("==== 酒馆运行日志 ====")
                    appendLine(trimForExport(fullTavernRuntimeLog))
                }
                if (mode.includeApp) {
                    appendLine()
                    appendLine("==== App 操作反馈 ====")
                    appendLine(trimForExport(fullAppLog))
                }
            },
            Charsets.UTF_8,
        )
        return file
    }

    private fun trimForExport(value: String): String {
        if (value.length <= MAX_EXPORTED_LOG_CHARS) return value
        val omitted = value.length - MAX_EXPORTED_LOG_CHARS
        return "... 前面已截断 $omitted 个字符 ...\n${value.takeLast(MAX_EXPORTED_LOG_CHARS)}"
    }
}
