package moe.lukoa.launcher

data class TermuxRepoStatus(
    val label: String = "未读取",
    val uri: String = "",
    val updatedAtMillis: Long = 0L,
) {
    val hasData: Boolean
        get() = uri.isNotBlank()
}

object TermuxRepoStatusParser {
    fun parse(output: String, nowMillis: Long = System.currentTimeMillis()): TermuxRepoStatus? {
        if (output.isBlank()) return null
        val uri = output.lineValue("current.uri")
            ?: output.lineValue("uri")
            ?: return null
        val label = output.lineValue("current.label")
            ?: output.lineValue("source")
            ?: labelForUri(uri)
        return TermuxRepoStatus(
            label = label.ifBlank { labelForUri(uri) },
            uri = uri,
            updatedAtMillis = nowMillis,
        )
    }

    fun labelForUri(uri: String): String {
        return when {
            uri.isBlank() -> "未读取"
            uri.contains("mirrors.tuna.tsinghua.edu.cn", ignoreCase = true) -> "清华源"
            uri.contains("packages.termux.dev", ignoreCase = true) -> "官方源"
            else -> "自定义"
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
}
