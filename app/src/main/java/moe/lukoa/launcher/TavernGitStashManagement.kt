package moe.lukoa.launcher

data class TavernGitStashEntry(
    val index: Int,
    val ref: String,
    val message: String,
    val createdAt: String,
) {
    val isLukoaManaged: Boolean
        get() = message.contains("lukoa-before-", ignoreCase = true) ||
            message.contains("lukoa-manual-", ignoreCase = true)
}

data class TavernGitStashState(
    val gitAvailable: Boolean? = null,
    val entries: List<TavernGitStashEntry> = emptyList(),
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的本地暂存记录。",
)

object TavernGitStashCommandCodec {
    fun isValidIndex(index: Int?): Boolean = index != null && index in 0..999
}

object TavernGitStashOutputParser {
    private const val HEADER = "==== SillyTavern git stash ===="
    private const val FOOTER = "==== end SillyTavern git stash ===="
    private val REF_INDEX_REGEX = Regex("^stash@\\{(\\d+)\\}$")

    fun parse(output: String): Pair<Boolean, List<TavernGitStashEntry>>? {
        val block = extractLastCompleteBlock(output) ?: return null
        var available = false
        val entries = mutableListOf<TavernGitStashEntry>()
        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("stash.available=") -> available = line.substringAfter('=') == "true"
                line.startsWith("stash.entry=") -> {
                    val fields = line.substringAfter('=').split('\t')
                    if (fields.size < 2) return@forEach
                    val ref = fields[0].trim()
                    val index = REF_INDEX_REGEX.matchEntire(ref)?.groupValues?.getOrNull(1)?.toIntOrNull()
                        ?: entries.size
                    entries += TavernGitStashEntry(
                        index = index,
                        ref = ref,
                        message = fields[1].trim(),
                        createdAt = fields.getOrNull(2)?.trim().orEmpty(),
                    )
                }
            }
        }
        return available to entries
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }
}
