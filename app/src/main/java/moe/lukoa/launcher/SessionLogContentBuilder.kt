package moe.lukoa.launcher

object SessionLogContentBuilder {
    private const val MAX_EXPORTED_LOG_CHARS = 120_000

    fun build(
        state: LauncherUiState,
        mode: ExportLogMode,
        tavernRuntimeLog: String,
        appLog: String,
        exportTime: String,
    ): String {
        return buildString {
            appendLine("露科亚启动器运行日志")
            appendLine("导出时间：$exportTime")
            appendLine("范围：后台诊断归档中的最近记录（页面清空不影响归档）")
            appendLine("状态摘要：${state.summary}")
            appendLine("当前状态：${state.status}")
            if (mode.includeTermux) {
                appendLine()
                appendLine("==== 酒馆运行日志 ====")
                appendLine(trimForExport(tavernRuntimeLog))
            }
            if (mode.includeApp) {
                appendLine()
                appendLine("==== App 操作反馈 ====")
                appendLine(trimForExport(appLog))
            }
        }
    }

    private fun trimForExport(value: String): String {
        if (value.length <= MAX_EXPORTED_LOG_CHARS) return value
        val omitted = value.length - MAX_EXPORTED_LOG_CHARS
        return "... 前面已截断 ${omitted} 个字符 ...\n${value.takeLast(MAX_EXPORTED_LOG_CHARS)}"
    }
}
