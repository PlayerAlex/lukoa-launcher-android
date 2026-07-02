package moe.lukoa.launcher

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SessionLogExporter {
    fun export(
        context: Context,
        summary: String,
        status: String,
        termuxLog: String,
        appLog: String,
        mode: ExportLogMode,
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-session-log-$timestamp", "txt")
        val fullTermuxLog = RuntimeLogArchive.readTermux(context, termuxLog)
        val fullAppLog = RuntimeLogArchive.readApp(context, appLog)
        file.writeText(
            buildString {
                appendLine("露科亚启动器运行日志")
                appendLine("导出时间：${LocalTime.now().withNano(0)}")
                appendLine("范围：从上次清除对应日志后开始累计")
                appendLine("状态摘要：$summary")
                appendLine("当前状态：$status")
                if (mode.includeTermux) {
                    appendLine()
                    appendLine("==== Termux 调用返回 ====")
                    appendLine(fullTermuxLog)
                }
                if (mode.includeApp) {
                    appendLine()
                    appendLine("==== App 操作反馈 ====")
                    appendLine(fullAppLog)
                }
            },
            Charsets.UTF_8,
        )
        return file
    }
}
