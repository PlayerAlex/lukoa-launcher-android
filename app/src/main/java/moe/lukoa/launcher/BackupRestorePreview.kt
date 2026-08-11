package moe.lukoa.launcher

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.InputStream
import kotlin.math.ln
import kotlin.math.pow
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

enum class BackupArchiveContentKind(
    val title: String,
    val detail: String = "",
) {
    CharacterCards("角色卡"),
    Presets("预设"),
    Beautification("美化"),
    RegexScripts("正则"),
    TavernHelperScripts("酒馆助手脚本"),
    Chats("聊天记录"),
    WorldBooks("世界书"),
    Extensions("扩展");

    companion object {
        val displayOrder: List<BackupArchiveContentKind> = listOf(
            Beautification,
            Presets,
            TavernHelperScripts,
            RegexScripts,
            Extensions,
            CharacterCards,
            WorldBooks,
            Chats,
        )

        private val displayRanks = displayOrder.withIndex().associate { (index, kind) ->
            kind to index
        }
    }

    val displayRank: Int
        get() = displayRanks.getValue(this)
}

data class BackupArchiveContentGroup(
    val kind: BackupArchiveContentKind,
    val entryCount: Int,
    val names: List<String> = emptyList(),
    val namesTruncated: Boolean = false,
    val children: List<BackupArchiveContentNode> = emptyList(),
)

data class BackupArchiveContentNode(
    val title: String,
    val entryCount: Int,
    val names: List<String> = emptyList(),
    val children: List<BackupArchiveContentNode> = emptyList(),
)

data class BackupArchiveContentSummary(
    val entryCount: Int,
    val hasUserData: Boolean,
    val hasExtensions: Boolean,
    val hasConfiguration: Boolean,
    val hasLukoaManifest: Boolean,
    val truncated: Boolean,
    val groups: List<BackupArchiveContentGroup> = emptyList(),
) {
    fun group(kind: BackupArchiveContentKind): BackupArchiveContentGroup? {
        return groups.firstOrNull { it.kind == kind }
    }
}

val BackupArchiveContentSummary.displayGroups: List<BackupArchiveContentGroup>
    get() = groups.sortedBy { it.kind.displayRank }

object BackupArchiveContentScanner {
    const val MAX_PREVIEW_ENTRIES = 2_000
    const val MAX_INSPECTABLE_JSON_BYTES = 2 * 1024 * 1024
    private const val MAX_ARCHIVE_SCAN_ENTRIES = 100_000
    private const val MAX_INSPECTABLE_SETTINGS_JSON_BYTES = 16 * 1024 * 1024
    private const val MAX_ENTRY_SIZE = 100L * 1024L * 1024L * 1024L
    private const val MAX_TOTAL_DECLARED_SIZE = 1_000L * 1024L * 1024L * 1024L

    fun scan(input: InputStream): BackupArchiveContentSummary {
        var entryCount = 0
        var scannedEntryCount = 0
        var totalDeclaredSize = 0L
        var hasUserData = false
        var hasExtensions = false
        var hasConfiguration = false
        var hasLukoaManifest = false
        var truncated = false
        val categoryCounts = BackupArchiveContentKind.entries.associateWith { 0 }.toMutableMap()
        val categoryNames = BackupArchiveContentKind.entries
            .associateWith { linkedSetOf<String>() }
            .toMutableMap()
        val extensionNamesByLocation = linkedMapOf<String, String>()
        val hierarchy = BackupArchiveContentHierarchyBuilder()
        GzipCompressorInputStream(input).use { gzip ->
            TarArchiveInputStream(gzip).use { tar ->
                while (true) {
                    val entry = tar.nextEntry as? org.apache.commons.compress.archivers.tar.TarArchiveEntry
                        ?: break
                    if (scannedEntryCount >= MAX_ARCHIVE_SCAN_ENTRIES) {
                        truncated = true
                        break
                    }
                    scannedEntryCount += 1
                    val includeInGeneralPreview = entryCount < MAX_PREVIEW_ENTRIES
                    if (includeInGeneralPreview) {
                        entryCount += 1
                    } else {
                        truncated = true
                    }
                    val normalized = validateEntryName(entry.name)
                    if (entry.isSymbolicLink || entry.isLink) {
                        error("备份内容包含链接，不能用于选择性恢复。")
                    }
                    if (entry.size < 0L || entry.size > MAX_ENTRY_SIZE) {
                        error("备份内单个文件大小异常。")
                    }
                    totalDeclaredSize += entry.size
                    if (totalDeclaredSize > MAX_TOTAL_DECLARED_SIZE) {
                        error("备份内容声明的总大小异常。")
                    }
                    val segments = normalized.lowercase(Locale.ROOT).split('/').filter(String::isNotBlank)
                    val fileName = segments.lastOrNull().orEmpty()
                    hasLukoaManifest = hasLukoaManifest || normalized == "LUKOA_BACKUP_MANIFEST.txt"
                    hasUserData = hasUserData || segments.any { it == "data" || it == "default-user" } ||
                        segments.any { it in setOf("chats", "characters", "groups", "worlds") }
                    hasExtensions = hasExtensions || segments.any { it == "extensions" || it == "plugins" }
                    hasConfiguration = hasConfiguration || fileName in setOf("config.yaml", "config.yml")
                    val originalSegments = normalized.split('/').filter(String::isNotBlank)
                    val extensionLocation = findExtensionArchiveLocation(
                        originalSegments = originalSegments,
                        lowerSegments = segments,
                    )
                    extensionLocation?.let { location ->
                        extensionNamesByLocation.putIfAbsent(location.key, location.displayName())
                    }
                    val pathClassification = classifyContent(
                        originalSegments = originalSegments,
                        lowerSegments = segments,
                        isDirectory = entry.isDirectory,
                        entrySize = entry.size,
                    )
                    val isUserSettings = isUserSettingsFile(segments)
                    val isPriorityClassification = pathClassification?.first == BackupArchiveContentKind.RegexScripts ||
                        pathClassification?.first == BackupArchiveContentKind.Extensions
                    val inspectForScopedRegex = isUserSettings ||
                        pathClassification?.first == BackupArchiveContentKind.Presets ||
                        pathClassification?.first == BackupArchiveContentKind.CharacterCards
                    val includeClassification = includeInGeneralPreview || isPriorityClassification
                    val inspectContent = includeInGeneralPreview || isPriorityClassification || inspectForScopedRegex
                    val jsonInspection = when {
                        inspectContent &&
                            !entry.isDirectory &&
                            fileName.endsWith(".json") &&
                            entry.size in 1..maxInspectableJsonBytes(segments).toLong() &&
                            shouldInspectJson(segments, pathClassification?.first) -> {
                            BackupArchiveJsonInspector.inspect(
                                readCurrentEntry(tar, entry.size.toInt()),
                            )
                        }
                        inspectContent &&
                            !entry.isDirectory &&
                            pathClassification?.first == BackupArchiveContentKind.CharacterCards &&
                            fileName.endsWith(".png") -> {
                            BackupArchivePngCharacterCardInspector.inspect(tar, entry.size)
                        }
                        else -> BackupArchiveJsonInspection()
                    }
                    if (pathClassification?.first == BackupArchiveContentKind.Presets) {
                        hierarchy.recordPresetRegexScripts(
                            pathClassification.second,
                            jsonInspection.globalRegexScriptNames + jsonInspection.presetRegexScriptNames,
                        )
                    } else if (pathClassification?.first == BackupArchiveContentKind.CharacterCards) {
                        hierarchy.recordLocalRegexScripts(
                            pathClassification.second,
                            jsonInspection.globalRegexScriptNames + jsonInspection.localRegexScriptNames,
                        )
                    } else {
                        hierarchy.recordGlobalRegexScripts(jsonInspection.globalRegexScriptNames)
                    }
                    pathClassification?.takeIf { includeClassification }?.let { (kind, fallbackName) ->
                        val name = if (kind == BackupArchiveContentKind.Extensions) {
                            jsonInspection.extensionDisplayName ?: fallbackName
                        } else {
                            fallbackName
                        }
                        categoryCounts[kind] = categoryCounts.getValue(kind) + 1
                        if (kind == BackupArchiveContentKind.Extensions) {
                            extensionLocation?.let { location ->
                                extensionNamesByLocation[location.key] = location.displayName(name)
                            } ?: run {
                                categoryNames.getValue(kind) += name.take(120)
                            }
                        } else if (kind == BackupArchiveContentKind.RegexScripts) {
                            hierarchy.recordGlobalRegexScripts(listOf(name))
                        } else if (kind == BackupArchiveContentKind.Chats) {
                            findChatLocation(originalSegments, segments)?.let { (source, chat) ->
                                hierarchy.recordChat(source, chat)
                            }
                        } else if (name.isNotBlank()) {
                            categoryNames.getValue(kind) += name.take(120)
                        }
                    }
                    if (isUserSettings) {
                        hierarchy.recordGlobalScripts(
                            jsonInspection.globalTavernHelperScriptNames,
                        )
                    }
                    if (pathClassification?.first == BackupArchiveContentKind.Presets) {
                        hierarchy.recordPresetScripts(
                            pathClassification.second,
                            jsonInspection.presetTavernHelperScriptNames,
                        )
                    }
                    if (pathClassification?.first == BackupArchiveContentKind.CharacterCards) {
                        hierarchy.recordLocalScripts(
                            pathClassification.second,
                            jsonInspection.localTavernHelperScriptNames,
                        )
                    }
                }
            }
        }
        if (extensionNamesByLocation.isNotEmpty()) {
            categoryCounts[BackupArchiveContentKind.Extensions] = extensionNamesByLocation.size
            categoryNames.getValue(BackupArchiveContentKind.Extensions).apply {
                clear()
                addAll(extensionNamesByLocation.values.map { it.take(120) })
            }
        }
        categoryCounts[BackupArchiveContentKind.RegexScripts] = hierarchy.regexScriptCount
        categoryCounts[BackupArchiveContentKind.TavernHelperScripts] = hierarchy.scriptCount
        return BackupArchiveContentSummary(
            entryCount = entryCount,
            hasUserData = hasUserData,
            hasExtensions = hasExtensions,
            hasConfiguration = hasConfiguration,
            hasLukoaManifest = hasLukoaManifest,
            truncated = truncated,
            groups = BackupArchiveContentKind.displayOrder.mapNotNull { kind ->
                val count = categoryCounts.getValue(kind)
                if (count <= 0) return@mapNotNull null
                val names = categoryNames.getValue(kind).toList()
                BackupArchiveContentGroup(
                    kind = kind,
                    entryCount = count,
                    names = names,
                    namesTruncated = false,
                    children = hierarchy.childrenFor(kind),
                )
            },
        )
    }

    private fun classifyContent(
        originalSegments: List<String>,
        lowerSegments: List<String>,
        isDirectory: Boolean,
        entrySize: Long,
    ): Pair<BackupArchiveContentKind, String>? {
        if (isDirectory || originalSegments.isEmpty()) return null
        val lowerFileName = lowerSegments.last()
        val displayFileName = originalSegments.last().substringBeforeLast('.', originalSegments.last())

        val extensionLocation = findExtensionArchiveLocation(originalSegments, lowerSegments)
        if (extensionLocation != null && lowerFileName == "manifest.json") {
            return BackupArchiveContentKind.Extensions to extensionLocation.fallbackName
        }

        val charactersIndex = lowerSegments.indexOf("characters")
        if (
            isDirectUserDataFile(lowerSegments, charactersIndex) &&
            lowerFileName.substringAfterLast('.', "") in setOf("png", "json", "webp")
        ) {
            return BackupArchiveContentKind.CharacterCards to displayFileName
        }

        val regexIndex = lowerSegments.indexOf("regex")
        if (
            isDirectRegexFile(lowerSegments, regexIndex) &&
            lowerFileName.endsWith(".json")
        ) {
            return BackupArchiveContentKind.RegexScripts to displayFileName
        }

        val presetIndex = lowerSegments.indexOfFirst { it in USER_PRESET_DIRECTORY_NAMES }
        if (isDirectUserDataFile(lowerSegments, presetIndex) && lowerFileName.endsWith(".json")) {
            return BackupArchiveContentKind.Presets to displayFileName
        }

        val themesIndex = lowerSegments.indexOf("themes")
        if (isDirectUserDataFile(lowerSegments, themesIndex) && lowerFileName.endsWith(".json")) {
            return BackupArchiveContentKind.Beautification to displayFileName
        }
        if (
            lowerFileName == "user.css" &&
            entrySize > 0L &&
            isDirectUserRootFile(lowerSegments)
        ) {
            return BackupArchiveContentKind.Beautification to "自定义 CSS"
        }

        val chatsIndex = lowerSegments.indexOf("chats")
        if (isUserDataDirectory(lowerSegments, chatsIndex)) {
            val chatName = originalSegments.getOrNull(chatsIndex + 1)
                ?.takeIf { chatsIndex + 1 < originalSegments.lastIndex }
                ?: displayFileName
            return BackupArchiveContentKind.Chats to chatName
        }
        val groupChatsIndex = lowerSegments.indexOf("group chats")
        if (isUserDataDirectory(lowerSegments, groupChatsIndex)) {
            return BackupArchiveContentKind.Chats to "群聊"
        }

        val worldsIndex = lowerSegments.indexOfFirst { it == "worlds" || it == "world-info" }
        if (isDirectUserDataFile(lowerSegments, worldsIndex) && lowerFileName.endsWith(".json")) {
            return BackupArchiveContentKind.WorldBooks to displayFileName
        }
        return null
    }

    private fun findExtensionArchiveLocation(
        originalSegments: List<String>,
        lowerSegments: List<String>,
    ): ExtensionArchiveLocation? {
        val thirdPartyIndex = lowerSegments.indexOf("third-party")
        if (
            thirdPartyIndex > 0 &&
            lowerSegments.getOrNull(thirdPartyIndex - 1) == "extensions" &&
            thirdPartyIndex + 1 < originalSegments.lastIndex
        ) {
            return ExtensionArchiveLocation(
                key = lowerSegments.take(thirdPartyIndex + 2).joinToString("/"),
                fallbackName = originalSegments[thirdPartyIndex + 1],
            )
        }

        val pluginsIndex = lowerSegments.indexOf("plugins")
        if (pluginsIndex >= 0 && pluginsIndex + 1 < originalSegments.lastIndex) {
            return ExtensionArchiveLocation(
                key = lowerSegments.take(pluginsIndex + 2).joinToString("/"),
                fallbackName = originalSegments[pluginsIndex + 1],
            )
        }

        val disabledThirdPartyIndex = lowerSegments.indexOf(".lukoa-disabled-third-party")
        if (
            disabledThirdPartyIndex > 0 &&
            lowerSegments.getOrNull(disabledThirdPartyIndex - 1) == "extensions" &&
            disabledThirdPartyIndex + 1 < originalSegments.lastIndex
        ) {
            return ExtensionArchiveLocation(
                key = lowerSegments.take(disabledThirdPartyIndex + 2).joinToString("/"),
                fallbackName = originalSegments[disabledThirdPartyIndex + 1],
                suffix = "（已停用）",
            )
        }
        return null
    }

    private fun isDirectUserDataFile(segments: List<String>, directoryIndex: Int): Boolean {
        return isUserDataDirectory(segments, directoryIndex) && directoryIndex == segments.lastIndex - 1
    }

    private fun isDirectRegexFile(segments: List<String>, regexIndex: Int): Boolean {
        if (isDirectUserDataFile(segments, regexIndex)) return true
        if (regexIndex != 2 || regexIndex != segments.lastIndex - 1) return false
        val possibleUserDirectory = segments.getOrNull(regexIndex - 1).orEmpty()
        return possibleUserDirectory !in NON_USER_DATA_DIRECTORY_NAMES
    }

    private fun isUserDataDirectory(segments: List<String>, directoryIndex: Int): Boolean {
        return directoryIndex >= 2 &&
            segments.getOrNull(directoryIndex - 2) == "data" &&
            directoryIndex < segments.lastIndex
    }

    private fun isDirectUserRootFile(segments: List<String>): Boolean {
        val dataIndex = segments.indexOf("data")
        return dataIndex >= 0 && dataIndex + 2 == segments.lastIndex
    }

    private fun isUserSettingsFile(segments: List<String>): Boolean {
        if (segments.lastOrNull() != "settings.json") return false
        if (isDirectUserRootFile(segments)) return true
        if (segments.size != 3) return false
        return segments[1] !in NON_USER_DATA_DIRECTORY_NAMES
    }

    private fun shouldInspectJson(
        lowerSegments: List<String>,
        pathKind: BackupArchiveContentKind?,
    ): Boolean {
        if (
            pathKind == BackupArchiveContentKind.Extensions ||
            pathKind == BackupArchiveContentKind.Presets ||
            pathKind == BackupArchiveContentKind.CharacterCards
        ) {
            return true
        }
        return isUserSettingsFile(lowerSegments)
    }

    private fun maxInspectableJsonBytes(lowerSegments: List<String>): Int {
        return if (
            isUserSettingsFile(lowerSegments)
        ) {
            MAX_INSPECTABLE_SETTINGS_JSON_BYTES
        } else {
            MAX_INSPECTABLE_JSON_BYTES
        }
    }

    private fun findChatLocation(
        originalSegments: List<String>,
        lowerSegments: List<String>,
    ): Pair<String, String>? {
        val displayFileName = originalSegments.lastOrNull()
            ?.substringBeforeLast('.', originalSegments.last())
            .orEmpty()
        val groupChatsIndex = lowerSegments.indexOf("group chats")
        if (isUserDataDirectory(lowerSegments, groupChatsIndex)) {
            return "群聊" to displayFileName
        }
        val chatsIndex = lowerSegments.indexOf("chats")
        if (!isUserDataDirectory(lowerSegments, chatsIndex)) return null
        val source = originalSegments.getOrNull(chatsIndex + 1)
            ?.takeIf { chatsIndex + 1 < originalSegments.lastIndex }
            ?: "未分类聊天"
        return source to displayFileName
    }

    private fun readCurrentEntry(tar: TarArchiveInputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = tar.read(bytes, offset, size - offset)
            if (read <= 0) error("备份内文件内容不完整。")
            offset += read
        }
        return bytes
    }

    private fun validateEntryName(value: String): String {
        val normalized = value.replace('\\', '/').trimEnd('/')
        if (normalized.isBlank() || normalized.startsWith('/') || normalized.startsWith("~/")) {
            error("备份内包含不安全路径。")
        }
        val segments = normalized.split('/')
        if (segments.any { it.isBlank() || it == "." || it == ".." }) {
            error("备份内包含不安全路径。")
        }
        return normalized
    }

    private val USER_PRESET_DIRECTORY_NAMES = setOf(
        "presets",
        "openai settings",
    )

    private val NON_USER_DATA_DIRECTORY_NAMES = setOf(
        "public",
        "src",
        "lib",
        "dist",
        "assets",
        "plugins",
        "extensions",
        "node_modules",
    )
}

private data class ExtensionArchiveLocation(
    val key: String,
    val fallbackName: String,
    val suffix: String = "",
) {
    fun displayName(name: String = fallbackName): String = "$name$suffix"
}

private class BackupArchiveContentHierarchyBuilder {
    private data class Bucket(
        var entryCount: Int = 0,
        val names: LinkedHashSet<String> = linkedSetOf(),
    )

    private val chats = linkedMapOf<String, Bucket>()
    private val globalScripts = linkedSetOf<String>()
    private val presetScripts = linkedMapOf<String, Bucket>()
    private val localScripts = linkedMapOf<String, Bucket>()
    private val globalRegexScripts = linkedSetOf<String>()
    private val presetRegexScripts = linkedMapOf<String, Bucket>()
    private val localRegexScripts = linkedMapOf<String, Bucket>()

    val scriptCount: Int
        get() = globalScripts.size +
            presetScripts.values.sumOf { it.entryCount } +
            localScripts.values.sumOf { it.entryCount }

    val regexScriptCount: Int
        get() = globalRegexScripts.size +
            presetRegexScripts.values.sumOf { it.entryCount } +
            localRegexScripts.values.sumOf { it.entryCount }

    fun recordChat(source: String, chatName: String) {
        val bucket = chats.getOrPut(source) { Bucket() }
        bucket.entryCount += 1
        if (chatName.isNotBlank()) bucket.names += chatName.take(120)
    }

    fun recordGlobalScripts(names: List<String>) {
        globalScripts += names
    }

    fun recordPresetScripts(presetName: String, names: List<String>) {
        recordScripts(presetScripts, presetName, names)
    }

    fun recordLocalScripts(characterName: String, names: List<String>) {
        recordScripts(localScripts, characterName, names)
    }

    fun recordGlobalRegexScripts(names: List<String>) {
        globalRegexScripts += names
    }

    fun recordPresetRegexScripts(presetName: String, names: List<String>) {
        recordScripts(presetRegexScripts, presetName, names)
    }

    fun recordLocalRegexScripts(characterName: String, names: List<String>) {
        recordScripts(localRegexScripts, characterName, names)
    }

    fun childrenFor(kind: BackupArchiveContentKind): List<BackupArchiveContentNode> {
        return when (kind) {
            BackupArchiveContentKind.Chats -> chats.toNodes()
            BackupArchiveContentKind.TavernHelperScripts -> buildList {
                if (globalScripts.isNotEmpty()) {
                    add(
                        BackupArchiveContentNode(
                            title = "全局脚本",
                            entryCount = globalScripts.size,
                            names = globalScripts.toList(),
                        ),
                    )
                }
                addScope("预设脚本", presetScripts)
                addScope("局部脚本", localScripts)
            }
            BackupArchiveContentKind.RegexScripts -> buildList {
                if (globalRegexScripts.isNotEmpty()) {
                    add(
                        BackupArchiveContentNode(
                            title = "全局正则",
                            entryCount = globalRegexScripts.size,
                            names = globalRegexScripts.toList(),
                        ),
                    )
                }
                addScope("预设正则", presetRegexScripts)
                addScope("局部正则", localRegexScripts)
            }
            else -> emptyList()
        }
    }

    private fun recordScripts(
        target: LinkedHashMap<String, Bucket>,
        sourceName: String,
        names: List<String>,
    ) {
        if (names.isEmpty()) return
        val bucket = target.getOrPut(sourceName) { Bucket() }
        names.forEach { name ->
            if (bucket.names.add(name)) bucket.entryCount += 1
        }
    }

    private fun MutableList<BackupArchiveContentNode>.addScope(
        title: String,
        sources: LinkedHashMap<String, Bucket>,
    ) {
        if (sources.isEmpty()) return
        add(
            BackupArchiveContentNode(
                title = title,
                entryCount = sources.values.sumOf { it.entryCount },
                children = sources.toNodes(),
            ),
        )
    }

    private fun LinkedHashMap<String, Bucket>.toNodes(): List<BackupArchiveContentNode> {
        return map { (title, bucket) ->
            BackupArchiveContentNode(
                title = title,
                entryCount = bucket.entryCount,
                names = bucket.names.toList(),
            )
        }
    }
}

data class BackupRestorePreview(
    val archivePath: String,
    val backupName: String,
    val modifiedAtMillis: Long? = null,
    val sizeBytes: Long? = null,
    val restoreTargetDir: String,
    val contentSummary: BackupArchiveContentSummary? = null,
    val contentReadError: String = "",
)

object BackupRestorePreviewResolver {
    fun resolve(
        context: Context,
        archivePath: String,
        restoreTargetDir: String,
    ): BackupRestorePreview {
        val normalizedPath = archivePath.trim()
        val details = BackupLibraryFiles.describeLibraryArchive(context, normalizedPath)
            ?: error("启动器读不到这个备份。请先刷新备份库，或重新导入。")
        val contentResult = runCatching {
            BackupLibraryFiles.openLibrarySource(context, normalizedPath).openInput().use(
                BackupArchiveContentScanner::scan,
            )
        }
        return BackupRestorePreview(
            archivePath = normalizedPath,
            backupName = details.fileName,
            modifiedAtMillis = details.modifiedAtMillis.takeIf { it > 0L },
            sizeBytes = details.size.takeIf { it >= 0L },
            restoreTargetDir = restoreTargetDir,
            contentSummary = contentResult.getOrNull(),
            contentReadError = contentResult.exceptionOrNull()?.message.orEmpty(),
        )
    }
}

fun formatBackupRestorePreviewTime(
    modifiedAtMillis: Long?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (modifiedAtMillis == null || modifiedAtMillis <= 0L) return "未读取"
    return runCatching {
        Instant.ofEpochMilli(modifiedAtMillis)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }.getOrElse { "未读取" }
}

fun formatBackupRestorePreviewSize(sizeBytes: Long?): String {
    val bytes = sizeBytes ?: return "未读取"
    if (bytes < 0L) return "未读取"
    if (bytes < 1024L) return "${bytes} B"
    val units = listOf("KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.size)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    val unit = units[digitGroups - 1]
    return String.format(Locale.ROOT, "%.1f %s", value, unit)
}
