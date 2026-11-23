package moe.lukoa.launcher

enum class TavernStorageCleanupCategory(
    val wireKey: String,
    val label: String,
    val description: String,
    val defaultSelected: Boolean = true,
) {
    OldLogs(
        wireKey = "old_logs",
        label = "旧运行日志副本（tavern.log.old）",
        description = "日志轮转留下的历史 1MB 副本，清理不影响当前日志。",
        defaultSelected = true,
    ),
    NpmCache(
        wireKey = "npm_cache",
        label = "npm 下载缓存（~/.npm）",
        description = "安装或更新依赖时留下的下载缓存，可安全释放，后续需要会自动重下。",
        defaultSelected = true,
    ),
    Thumbnails(
        wireKey = "thumbnails",
        label = "角色卡缩略图缓存（thumbnails）",
        description = "酒馆生成的预览缩略图缓存，清理不影响原角色卡图片，打开网页会自动重新生成。",
        defaultSelected = false,
    ),
    ExtensionRollbacks(
        wireKey = "extension_rollbacks",
        label = "扩展更新回退快照",
        description = "更新网页扩展时自动留下的旧版本快照；确认扩展运行正常后可释放。",
        defaultSelected = false,
    ),
    LeftoverDirs(
        wireKey = "leftover_dirs",
        label = "修复 / 恢复历史临时目录",
        description = "历史修复依赖或恢复备份时留下的 .lukoa-before-* / staging 临时目录。",
        defaultSelected = true,
    ),
    ConfigBackups(
        wireKey = "config_backups",
        label = "config.yaml 自动备份副本",
        description = "修改酒馆设置前自动生成的 config.yaml.lukoa-backup-* 副本，不影响当前 config.yaml。",
        defaultSelected = false,
    ),
    ;

    companion object {
        fun fromWireKey(key: String): TavernStorageCleanupCategory? =
            entries.firstOrNull { it.wireKey == key.trim() }
    }
}

data class TavernStorageSnapshot(
    val tavernTotalKb: Long = 0L,
    val dataTotalKb: Long = 0L,
    val chatsKb: Long = 0L,
    val charactersKb: Long = 0L,
    val thumbnailsKb: Long = 0L,
    val chatBackupsKb: Long = 0L,
    val nodeModulesKb: Long = 0L,
    val extensionsKb: Long = 0L,
    val extensionRollbacksKb: Long = 0L,
    val logsKb: Long = 0L,
    val oldLogKb: Long = 0L,
    val npmCacheKb: Long = 0L,
    val leftoverDirsKb: Long = 0L,
    val leftoverDirsCount: Int = 0,
    val configBackupsKb: Long = 0L,
    val configBackupsCount: Int = 0,
    val lastFreedKb: Long? = null,
) {
    fun kilobytesFor(category: TavernStorageCleanupCategory): Long = when (category) {
        TavernStorageCleanupCategory.OldLogs -> oldLogKb
        TavernStorageCleanupCategory.NpmCache -> npmCacheKb
        TavernStorageCleanupCategory.Thumbnails -> thumbnailsKb
        TavernStorageCleanupCategory.ExtensionRollbacks -> extensionRollbacksKb
        TavernStorageCleanupCategory.LeftoverDirs -> leftoverDirsKb
        TavernStorageCleanupCategory.ConfigBackups -> configBackupsKb
    }

    val totalCleanableKb: Long
        get() = TavernStorageCleanupCategory.entries.sumOf(::kilobytesFor)
}

data class TavernStorageState(
    val snapshot: TavernStorageSnapshot? = null,
    val loading: Boolean = false,
    val message: String = "尚未扫描当前实例的磁盘占用。",
)

object TavernStorageCommandCodec {
    fun encodeCategories(categories: Set<TavernStorageCleanupCategory>): String {
        return TavernStorageCleanupCategory.entries
            .filter { it in categories }
            .joinToString(",") { it.wireKey }
    }

    fun decodeCategories(raw: String?): List<TavernStorageCleanupCategory>? {
        val parts = raw.orEmpty().trim().split(',').map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        val mapped = parts.map { TavernStorageCleanupCategory.fromWireKey(it) ?: return null }
        return mapped.distinct()
    }
}

object TavernStorageOutputParser {
    private const val HEADER = "==== Lukoa storage ===="
    private const val FOOTER = "==== end Lukoa storage ===="

    fun parse(output: String): TavernStorageSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var tavernTotalKb = 0L
        var dataTotalKb = 0L
        var chatsKb = 0L
        var charactersKb = 0L
        var thumbnailsKb = 0L
        var chatBackupsKb = 0L
        var nodeModulesKb = 0L
        var extensionsKb = 0L
        var extensionRollbacksKb = 0L
        var logsKb = 0L
        var oldLogKb = 0L
        var npmCacheKb = 0L
        var leftoverDirsKb = 0L
        var leftoverDirsCount = 0
        var configBackupsKb = 0L
        var configBackupsCount = 0
        var lastFreedKb: Long? = null

        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val value = line.substringAfter('=', "")
            val longVal = value.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            when {
                line.startsWith("storage.freedKb=") -> lastFreedKb = longVal
                line.startsWith("storage.tavernTotalKb=") -> tavernTotalKb = longVal
                line.startsWith("storage.dataTotalKb=") -> dataTotalKb = longVal
                line.startsWith("storage.chatsKb=") -> chatsKb = longVal
                line.startsWith("storage.charactersKb=") -> charactersKb = longVal
                line.startsWith("storage.thumbnailsKb=") -> thumbnailsKb = longVal
                line.startsWith("storage.chatBackupsKb=") -> chatBackupsKb = longVal
                line.startsWith("storage.nodeModulesKb=") -> nodeModulesKb = longVal
                line.startsWith("storage.extensionsKb=") -> extensionsKb = longVal
                line.startsWith("storage.extensionRollbacksKb=") -> extensionRollbacksKb = longVal
                line.startsWith("storage.logsKb=") -> logsKb = longVal
                line.startsWith("storage.oldLogKb=") -> oldLogKb = longVal
                line.startsWith("storage.npmCacheKb=") -> npmCacheKb = longVal
                line.startsWith("storage.leftoverDirsKb=") -> leftoverDirsKb = longVal
                line.startsWith("storage.leftoverDirsCount=") -> leftoverDirsCount = longVal.toInt()
                line.startsWith("storage.configBackupsKb=") -> configBackupsKb = longVal
                line.startsWith("storage.configBackupsCount=") -> configBackupsCount = longVal.toInt()
            }
        }
        return TavernStorageSnapshot(
            tavernTotalKb = tavernTotalKb,
            dataTotalKb = dataTotalKb,
            chatsKb = chatsKb,
            charactersKb = charactersKb,
            thumbnailsKb = thumbnailsKb,
            chatBackupsKb = chatBackupsKb,
            nodeModulesKb = nodeModulesKb,
            extensionsKb = extensionsKb,
            extensionRollbacksKb = extensionRollbacksKb,
            logsKb = logsKb,
            oldLogKb = oldLogKb,
            npmCacheKb = npmCacheKb,
            leftoverDirsKb = leftoverDirsKb,
            leftoverDirsCount = leftoverDirsCount,
            configBackupsKb = configBackupsKb,
            configBackupsCount = configBackupsCount,
            lastFreedKb = lastFreedKb,
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
