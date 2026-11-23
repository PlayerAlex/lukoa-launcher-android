package moe.lukoa.launcher

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

enum class TavernQuickImportKind(
    val wireValue: String,
    val label: String,
    val subtitle: String,
    val targetFolderName: String,
    val acceptedExtensions: Set<String>,
    val mimeTypes: Array<String>,
) {
    Character(
        wireValue = "character",
        label = "导入角色卡",
        subtitle = "PNG / JSON / CHARX → characters/",
        targetFolderName = "characters",
        acceptedExtensions = setOf("png", "json", "charx", "webp"),
        mimeTypes = arrayOf("image/png", "image/webp", "application/json", "application/octet-stream", "*/*"),
    ),
    World(
        wireValue = "world",
        label = "导入世界书",
        subtitle = "JSON → worlds/",
        targetFolderName = "worlds",
        acceptedExtensions = setOf("json"),
        mimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"),
    ),
    Preset(
        wireValue = "preset",
        label = "导入对话预设",
        subtitle = "JSON → OpenAI Settings/",
        targetFolderName = "OpenAI Settings",
        acceptedExtensions = setOf("json"),
        mimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"),
    ),
    Theme(
        wireValue = "theme",
        label = "导入网页主题",
        subtitle = "JSON → themes/",
        targetFolderName = "themes",
        acceptedExtensions = setOf("json"),
        mimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"),
    ),
    Chat(
        wireValue = "chat",
        label = "导入聊天记录",
        subtitle = "JSONL → chats/<角色目录>/",
        targetFolderName = "chats",
        acceptedExtensions = setOf("jsonl"),
        mimeTypes = arrayOf("application/octet-stream", "text/plain", "application/json", "*/*"),
    ),
    ;

    companion object {
        fun fromWireValue(value: String?): TavernQuickImportKind? =
            entries.firstOrNull { it.wireValue == value?.trim()?.lowercase() }
    }
}

data class TavernQuickImportPaths(
    val dataRoot: String = "",
    val userRoot: String = "",
    val characterDir: String = "",
    val worldDir: String = "",
    val presetDir: String = "",
    val themeDir: String = "",
    val chatsDir: String = "",
    val chatCharacterDirs: List<String> = emptyList(),
)

data class TavernQuickImportStagedFile(
    val ok: Boolean,
    val kind: TavernQuickImportKind,
    val fileName: String = "",
    val termuxReadablePath: String = "",
    val message: String = "",
)

data class TavernQuickImportStagedBatch(
    val ok: Boolean,
    val kind: TavernQuickImportKind,
    val stagedFiles: List<TavernQuickImportStagedFile> = emptyList(),
    val skippedFiles: List<String> = emptyList(),
    val message: String = "",
)

data class TavernQuickImportBatchArgs(
    val kind: TavernQuickImportKind,
    val userHandle: String,
    val stagedPaths: List<String>,
    val chatCharacterDir: String = "",
)

data class TavernQuickImportState(
    val paths: TavernQuickImportPaths? = null,
    val userHandle: String = "default-user",
    val chatCharacterDir: String = "",
    val loading: Boolean = false,
    val lastImportedFile: String = "",
    val lastImportedFiles: List<String> = emptyList(),
    val message: String = "支持单选或多选批量导入角色卡、世界书、预设、主题和聊天记录到当前酒馆。",
)

object TavernQuickImportCommandCodec {
    private val USER_HANDLE_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")

    fun normalizeUserHandle(value: String): String {
        val trimmed = value.trim()
        return if (USER_HANDLE_REGEX.matches(trimmed)) trimmed else "default-user"
    }

    fun validateChatCharacterDir(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "请先选择或填写聊天记录所属的角色目录名。"
        if (trimmed == "." || trimmed == ".." || trimmed.length > 120) {
            return "角色目录名无效或过长（最多 120 字符）。"
        }
        if (trimmed.any { it == '/' || it == '\\' || it == '\u0000' || it == '\r' || it == '\n' }) {
            return "角色目录名不能包含斜杠或换行符。"
        }
        return null
    }

    fun sanitizeFileName(rawName: String, fallbackExt: String): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\r\\n\\u0000:*?\"<>|]"), "_")
            .trim()
            .trimStart('.')
        return if (base.isBlank()) "imported.$fallbackExt" else base.take(120)
    }

    fun encode(kind: TavernQuickImportKind, stagedPath: String, userHandle: String): String {
        return "${kind.wireValue}|${normalizeUserHandle(userHandle)}|$stagedPath"
    }

    fun decode(raw: String?): Triple<TavernQuickImportKind, String, String>? {
        val trimmed = raw.orEmpty().trim()
        val firstSep = trimmed.indexOf('|')
        if (firstSep <= 0) return null
        val secondSep = trimmed.indexOf('|', startIndex = firstSep + 1)
        if (secondSep <= firstSep + 1) return null
        val kind = TavernQuickImportKind.fromWireValue(trimmed.substring(0, firstSep)) ?: return null
        val userHandle = normalizeUserHandle(trimmed.substring(firstSep + 1, secondSep))
        val path = trimmed.substring(secondSep + 1).trim()
        if (path.isBlank() || path.contains('\n') || path.contains('\r') || path.contains('\u0000')) return null
        return Triple(kind, userHandle, path)
    }

    fun encodeBatch(
        kind: TavernQuickImportKind,
        stagedPaths: List<String>,
        userHandle: String,
        chatCharacterDir: String = "",
    ): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val pathsB64 = encoder.encodeToString(
            stagedPaths.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n")
                .toByteArray(StandardCharsets.UTF_8),
        )
        val charB64 = if (kind == TavernQuickImportKind.Chat) {
            encoder.encodeToString(chatCharacterDir.trim().toByteArray(StandardCharsets.UTF_8))
        } else {
            ""
        }
        return "${kind.wireValue}|${normalizeUserHandle(userHandle)}|$pathsB64|$charB64"
    }

    fun decodeBatch(raw: String?): TavernQuickImportBatchArgs? {
        val parts = raw.orEmpty().trim().split('|', limit = 4)
        if (parts.size < 3) return null
        val kind = TavernQuickImportKind.fromWireValue(parts[0]) ?: return null
        val userHandle = normalizeUserHandle(parts[1])
        val pathsRaw = decodeBase64Url(parts[2]) ?: return null
        val paths = pathsRaw.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (paths.isEmpty() || paths.size > 50 || paths.any { it.contains('\u0000') }) return null
        val chatChar = if (parts.size >= 4 && parts[3].isNotBlank()) {
            decodeBase64Url(parts[3])?.trim() ?: return null
        } else {
            ""
        }
        if (kind == TavernQuickImportKind.Chat && validateChatCharacterDir(chatChar) != null) {
            return null
        }
        return TavernQuickImportBatchArgs(
            kind = kind,
            userHandle = userHandle,
            stagedPaths = paths,
            chatCharacterDir = chatChar,
        )
    }

    internal fun encodeBase64Url(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    internal fun decodeBase64Url(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value.trim()), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

object TavernQuickImportOutputParser {
    private const val PATHS_HEADER = "==== SillyTavern quick import paths ===="
    private const val PATHS_FOOTER = "==== end SillyTavern quick import paths ===="
    private const val RESULT_HEADER = "==== SillyTavern quick import ===="
    private const val RESULT_FOOTER = "==== end SillyTavern quick import ===="

    fun parsePaths(output: String): TavernQuickImportPaths? {
        val block = extractLastCompleteBlock(output, PATHS_HEADER, PATHS_FOOTER) ?: return null
        var dataRoot = ""
        var userRoot = ""
        var characterDir = ""
        var worldDir = ""
        var presetDir = ""
        var themeDir = ""
        var chatsDir = ""
        var chatChars = emptyList<String>()
        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("import.dataRoot=") -> dataRoot = line.substringAfter('=')
                line.startsWith("import.userRoot=") -> userRoot = line.substringAfter('=')
                line.startsWith("import.dir.character=") -> characterDir = line.substringAfter('=')
                line.startsWith("import.dir.world=") -> worldDir = line.substringAfter('=')
                line.startsWith("import.dir.preset=") -> presetDir = line.substringAfter('=')
                line.startsWith("import.dir.theme=") -> themeDir = line.substringAfter('=')
                line.startsWith("import.dir.chats=") -> chatsDir = line.substringAfter('=')
                line.startsWith("import.chatChars=") -> {
                    val decoded = TavernQuickImportCommandCodec.decodeBase64Url(line.substringAfter('='))
                    chatChars = decoded.orEmpty().lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                }
            }
        }
        return TavernQuickImportPaths(
            dataRoot = dataRoot,
            userRoot = userRoot,
            characterDir = characterDir,
            worldDir = worldDir,
            presetDir = presetDir,
            themeDir = themeDir,
            chatsDir = chatsDir,
            chatCharacterDirs = chatChars,
        )
    }

    fun parseImportedTargetFile(output: String): String? {
        val block = extractLastCompleteBlock(output, RESULT_HEADER, RESULT_FOOTER) ?: return null
        return block.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("import.targetFile=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
    }

    fun parseImportedTargetFiles(output: String): List<String> {
        val block = extractLastCompleteBlock(output, RESULT_HEADER, RESULT_FOOTER) ?: return emptyList()
        val encodedList = block.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("import.targetFiles=") }
            ?.substringAfter('=')
            ?.takeIf { it.isNotBlank() }
        if (encodedList != null) {
            val decoded = TavernQuickImportCommandCodec.decodeBase64Url(encodedList).orEmpty()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()
            if (decoded.isNotEmpty()) return decoded
        }
        return listOfNotNull(parseImportedTargetFile(output))
    }

    private fun extractLastCompleteBlock(output: String, header: String, footer: String): String? {
        val headerIndex = output.lastIndexOf(header)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(footer, startIndex = headerIndex + header.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + footer.length)
    }
}

object TavernQuickImportStager {
    private const val IMPORTS_RELATIVE_DIR = "LukoaLauncher/imports"
    private const val ANDROID_IMPORTS_RELATIVE_PATH = "Download/$IMPORTS_RELATIVE_DIR/"
    private const val MAX_BATCH_FILES = 50

    fun stageUris(
        context: Context,
        kind: TavernQuickImportKind,
        sourceUris: List<Uri>,
    ): TavernQuickImportStagedBatch {
        if (sourceUris.isEmpty()) {
            return TavernQuickImportStagedBatch(
                ok = false,
                kind = kind,
                message = "已取消选择要导入的文件。",
            )
        }
        val cappedUris = sourceUris.take(MAX_BATCH_FILES)
        val usedNames = linkedSetOf<String>()
        val staged = mutableListOf<TavernQuickImportStagedFile>()
        val skipped = mutableListOf<String>()
        cappedUris.forEach { uri ->
            val result = stageUri(context, kind, uri, usedNames)
            if (result.ok) {
                staged += result
            } else {
                skipped += result.fileName.ifBlank { result.message }
            }
        }
        if (staged.isEmpty()) {
            val detail = skipped.firstOrNull().orEmpty()
            return TavernQuickImportStagedBatch(
                ok = false,
                kind = kind,
                skippedFiles = skipped,
                message = detail.ifBlank { "所选文件无法导入，请检查格式是否符合 ${kind.acceptedExtensions.joinToString("/")}。" },
            )
        }
        val skippedHint = if (skipped.isNotEmpty()) "（已跳过 ${skipped.size} 个不兼容文件）" else ""
        return TavernQuickImportStagedBatch(
            ok = true,
            kind = kind,
            stagedFiles = staged,
            skippedFiles = skipped,
            message = "已读取 ${staged.size} 个文件$skippedHint，正在写入酒馆目录…",
        )
    }

    fun stageUri(
        context: Context,
        kind: TavernQuickImportKind,
        sourceUri: Uri,
        usedNames: MutableSet<String> = linkedSetOf(),
    ): TavernQuickImportStagedFile {
        return try {
            val rawName = queryDisplayName(context, sourceUri).orEmpty()
            val fallbackExt = kind.acceptedExtensions.first()
            val baseSafeName = TavernQuickImportCommandCodec.sanitizeFileName(rawName, fallbackExt)
            val ext = baseSafeName.substringAfterLast('.', "").lowercase()
            if (ext.isNotBlank() && ext !in kind.acceptedExtensions) {
                return TavernQuickImportStagedFile(
                    ok = false,
                    kind = kind,
                    fileName = baseSafeName,
                    message = "选中的文件格式（.$ext）与「${kind.label}」不匹配，请选 ${kind.acceptedExtensions.joinToString("/")} 文件。",
                )
            }
            val safeName = deduplicateName(baseSafeName, usedNames)
            usedNames += safeName.lowercase()
            val stagedPath = runCatching {
                copyDirectToDownloads(context, sourceUri, safeName)
            }.getOrElse { directError ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    copyViaMediaStore(context, sourceUri, safeName)
                } else {
                    throw directError
                }
            }
            TavernQuickImportStagedFile(
                ok = true,
                kind = kind,
                fileName = safeName,
                termuxReadablePath = stagedPath,
                message = "已读取文件：$safeName，正在写入酒馆目录…",
            )
        } catch (error: Exception) {
            TavernQuickImportStagedFile(
                ok = false,
                kind = kind,
                message = "读取导入文件失败：${error.message ?: error.javaClass.simpleName}",
            )
        }
    }

    private fun deduplicateName(name: String, usedNames: Set<String>): String {
        if (name.lowercase() !in usedNames) return name
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var counter = 1
        while (counter < 1000) {
            val candidate = "$stem ($counter)$ext"
            if (candidate.lowercase() !in usedNames) return candidate
            counter += 1
        }
        return name
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment
    }

    private fun copyDirectToDownloads(context: Context, sourceUri: Uri, fileName: String): String {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, IMPORTS_RELATIVE_DIR)
        if (!dir.exists() && !dir.mkdirs()) {
            error("无法创建临时导入目录")
        }
        val dest = File(dir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("无法打开所选文件")
        return "/storage/emulated/0/Download/$IMPORTS_RELATIVE_DIR/$fileName"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyViaMediaStore(context: Context, sourceUri: Uri, fileName: String): String {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ANDROID_IMPORTS_RELATIVE_PATH)
        }
        val resolver = context.contentResolver
        val inserted = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("系统拒绝写入 Download/LukoaLauncher/imports")
        resolver.openInputStream(sourceUri)?.use { input ->
            resolver.openOutputStream(inserted)?.use { output -> input.copyTo(output) }
                ?: error("无法写入临时导入文件")
        } ?: error("无法打开所选文件")
        return "/storage/emulated/0/Download/$IMPORTS_RELATIVE_DIR/$fileName"
    }
}
