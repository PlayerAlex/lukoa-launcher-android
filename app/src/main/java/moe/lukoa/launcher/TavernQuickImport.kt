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
)

data class TavernQuickImportStagedFile(
    val ok: Boolean,
    val kind: TavernQuickImportKind,
    val fileName: String = "",
    val termuxReadablePath: String = "",
    val message: String = "",
)

data class TavernQuickImportState(
    val paths: TavernQuickImportPaths? = null,
    val userHandle: String = "default-user",
    val loading: Boolean = false,
    val lastImportedFile: String = "",
    val message: String = "选择类型即可从手机文件管理器导入角色卡、世界书、预设或主题到当前酒馆。",
)

object TavernQuickImportCommandCodec {
    private val USER_HANDLE_REGEX = Regex("^[A-Za-z0-9._-]{1,64}$")

    fun normalizeUserHandle(value: String): String {
        val trimmed = value.trim()
        return if (USER_HANDLE_REGEX.matches(trimmed)) trimmed else "default-user"
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

    fun stageUri(context: Context, kind: TavernQuickImportKind, sourceUri: Uri): TavernQuickImportStagedFile {
        return try {
            val rawName = queryDisplayName(context, sourceUri).orEmpty()
            val fallbackExt = kind.acceptedExtensions.first()
            val safeName = TavernQuickImportCommandCodec.sanitizeFileName(rawName, fallbackExt)
            val ext = safeName.substringAfterLast('.', "").lowercase()
            if (ext.isNotBlank() && ext !in kind.acceptedExtensions) {
                return TavernQuickImportStagedFile(
                    ok = false,
                    kind = kind,
                    message = "选中的文件格式（.$ext）与「${kind.label}」不匹配，请选 ${kind.acceptedExtensions.joinToString("/")} 文件。",
                )
            }
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
