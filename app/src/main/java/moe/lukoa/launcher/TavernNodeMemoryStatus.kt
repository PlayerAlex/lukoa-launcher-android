package moe.lukoa.launcher

data class TavernNodeMemoryStatus(
    val configured: Boolean? = null,
    val megabytes: Int? = null,
    val checking: Boolean = false,
    val message: String = "点击下方“当前内存上限”可读取当前实例是否已设置限制。",
) {
    val displayLabel: String
        get() = when {
            checking -> "读取中…"
            configured == true && megabytes != null -> TavernNodeMemoryPolicy.label(megabytes)
            configured == false -> "未限制（默认）"
            else -> "尚未读取"
        }
}

object TavernNodeMemoryOutputParser {
    private const val HEADER = "==== Lukoa node memory ===="
    private const val FOOTER = "==== end Lukoa node memory ===="

    fun parse(output: String): TavernNodeMemoryStatus? {
        val block = extractLastCompleteBlock(output)
        if (block != null) {
            var configured: Boolean? = null
            var megabytes: Int? = null
            block.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                val value = line.substringAfter('=', "").trim()
                when {
                    line.startsWith("nodeMemory.configured=") -> configured = value == "true"
                    line.startsWith("nodeMemory.megabytes=") -> {
                        megabytes = value.toIntOrNull()?.takeIf(TavernNodeMemoryPolicy::isAllowed)
                    }
                }
            }
            val isConfigured = configured ?: (megabytes != null)
            return if (isConfigured && megabytes != null) {
                TavernNodeMemoryStatus(
                    configured = true,
                    megabytes = megabytes,
                    checking = false,
                    message = "当前实例已设置上限 ${TavernNodeMemoryPolicy.label(megabytes!!)}（重启或下次启动酒馆后生效）。",
                )
            } else {
                TavernNodeMemoryStatus(
                    configured = false,
                    megabytes = null,
                    checking = false,
                    message = "当前实例未单独限制 Node.js 内存，将使用 Termux 默认上限。",
                )
            }
        }

        // Backward-compatible fallback for legacy cmd_node_memory_set output.
        val legacyMb = output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("node.memory.mb=") }
            ?.substringAfter('=')
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf(TavernNodeMemoryPolicy::isAllowed)
            ?: return null
        return TavernNodeMemoryStatus(
            configured = true,
            megabytes = legacyMb,
            checking = false,
            message = "当前实例已设置上限 ${TavernNodeMemoryPolicy.label(legacyMb)}（重启或下次启动酒馆后生效）。",
        )
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }
}
