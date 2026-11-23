package moe.lukoa.launcher

data class LogFilterResult(
    val text: String,
    val totalLines: Int,
    val matchedLines: Int,
    val errorLines: Int,
)

object LogPanelFilter {
    private val ERROR_SIGNAL_REGEX = Regex(
        pattern = "(?:\\berror\\b|\\bexception\\b|\\bfatal\\b|\\bfailed\\b|eaddrinuse|econnrefused|etimedout|" +
            "heap out of memory|reached heap limit|\\boom\\b|syntaxerror|typeerror|referenceerror|" +
            "cannot find module|err_module_not_found|traceback|\\berrno\\b|\\b401\\b|\\b403\\b|\\b429\\b|" +
            "permission denied|报错|失败|异常|崩溃|超时|无法连接|拒绝调用)",
        option = RegexOption.IGNORE_CASE,
    )
    private val BENIGN_LINE_REGEX = Regex(
        pattern = "(?:\"exitcode\"\\s*:\\s*0|\\berrorcount\\s*[:=]\\s*0|\\berror\\s*=\\s*0|" +
            "0\\s*个错误|0\\s*个问题|未发现常见报错|\"errcode\"\\s*:\\s*null)",
        option = RegexOption.IGNORE_CASE,
    )

    fun isErrorLine(line: String): Boolean {
        if (line.isBlank()) return false
        if (BENIGN_LINE_REGEX.containsMatchIn(line)) return false
        return ERROR_SIGNAL_REGEX.containsMatchIn(line)
    }

    fun filter(
        content: String,
        errorsOnly: Boolean,
        query: String,
    ): LogFilterResult {
        if (content.isEmpty()) {
            return LogFilterResult(text = "", totalLines = 0, matchedLines = 0, errorLines = 0)
        }
        val lines = content.lineSequence().toList()
        var errorCount = 0
        val trimmedQuery = query.trim()
        val matched = ArrayList<String>(lines.size)
        lines.forEach { line ->
            val isErr = isErrorLine(line)
            if (isErr) errorCount += 1
            if (errorsOnly && !isErr) return@forEach
            if (trimmedQuery.isNotEmpty() && !line.contains(trimmedQuery, ignoreCase = true)) return@forEach
            matched += line
        }
        val text = if (!errorsOnly && trimmedQuery.isEmpty()) {
            content
        } else {
            matched.joinToString("\n")
        }
        return LogFilterResult(
            text = text,
            totalLines = lines.size,
            matchedLines = matched.size,
            errorLines = errorCount,
        )
    }
}
