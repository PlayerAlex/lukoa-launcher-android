package moe.lukoa.launcher

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.util.Locale

data class BackupExportDestinationResult(
    val ok: Boolean,
    val message: String,
    val destinationPath: String = "",
    val exportedByApp: Boolean = false,
)

object BackupExportDestinationResolver {
    private const val FALLBACK_FILE_NAME = "sillytavern-backup.tar.gz"

    fun defaultFileName(sourceFileName: String): String {
        val trimmed = sourceFileName.trim().substringAfterLast('/').substringAfterLast('\\')
        return normalizedExportFileName(trimmed).takeIf { it.length <= 120 }
            ?: FALLBACK_FILE_NAME
    }

    fun prepareDestination(
        context: Context,
        uri: Uri,
        sourcePath: String,
    ): BackupExportDestinationResult {
        val requestedFileName = BackupLibraryFiles.displayName(context, uri)
        val fileName = normalizedExportFileName(requestedFileName)
        val destinationUri = runCatching {
            ensureDestinationFileName(context, uri, fileName)
        }.getOrElse { error ->
            deletePlaceholder(context, uri)
            return BackupExportDestinationResult(
                ok = false,
                message = "导出失败：${error.message ?: "文件管理器没有保留 .tar.gz 后缀"}",
            )
        }

        val destinationPath = resolveSharedStoragePath(context, destinationUri, fileName)
        if (!destinationPath.isNullOrBlank() && sameSharedFile(sourcePath, destinationPath)) {
            return BackupExportDestinationResult(
                ok = false,
                message = "导出位置不能选原备份本身，请换个文件名或文件夹。",
            )
        }

        val appExportResult = runCatching {
            copyBackupToUri(
                context = context,
                sourcePath = sourcePath,
                destinationUri = destinationUri,
                destinationFileName = fileName,
                destinationPath = destinationPath,
            )
        }
        return if (appExportResult.isSuccess) {
            appExportResult.getOrThrow()
        } else {
            deletePlaceholder(context, destinationUri)
            BackupExportDestinationResult(
                ok = false,
                message = "导出失败：${appExportResult.exceptionOrNull()?.message ?: "没有复制成功"}",
            )
        }
    }

    private fun copyBackupToUri(
        context: Context,
        sourcePath: String,
        destinationUri: Uri,
        destinationFileName: String,
        destinationPath: String?,
    ): BackupExportDestinationResult {
        val source = BackupLibraryFiles.openLibrarySource(context, sourcePath)
        val tempFile = File.createTempFile("lukoa-backup-export-", ".tar.gz", context.cacheDir)
        try {
            val copiedBytes = source.openInput().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (copiedBytes <= 0L || tempFile.length() <= 0L) {
                error("源备份是空文件")
            }
            if (source.size > 0L && source.size != copiedBytes) {
                error("源备份读取不完整")
            }

            val writtenBytes = tempFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(destinationUri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("无法写入选择的位置")
            }
            if (writtenBytes <= 0L || writtenBytes != tempFile.length()) {
                deletePlaceholder(context, destinationUri)
                error("导出写入不完整")
            }

            return BackupExportDestinationResult(
                ok = true,
                message = "备份已导出：$destinationFileName",
                destinationPath = destinationPath ?: destinationFileName,
                exportedByApp = true,
            )
        } catch (error: Exception) {
            deletePlaceholder(context, destinationUri)
            throw error
        } finally {
            tempFile.delete()
        }
    }

    private fun normalizedExportFileName(rawFileName: String): String {
        val trimmed = rawFileName.trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { FALLBACK_FILE_NAME }
        val normalized = when {
            Regex("(?i)\\.tar\\.gz").containsMatchIn(trimmed) -> {
                val match = Regex("(?i)\\.tar\\.gz").find(trimmed)!!
                trimmed.substring(0, match.range.last + 1)
            }
            trimmed.endsWith(".t..gz", ignoreCase = true) -> {
                trimmed.dropLast(".t..gz".length) + ".tar.gz"
            }
            trimmed.endsWith(".tgz", ignoreCase = true) -> {
                trimmed.dropLast(".tgz".length) + ".tar.gz"
            }
            trimmed.endsWith(".gz", ignoreCase = true) -> {
                trimmed.dropLast(".gz".length).removeSuffix(".tar") + ".tar.gz"
            }
            else -> "$trimmed.tar.gz"
        }
        val label = normalized.replace(Regex("(?i)\\.tar\\.gz$"), "")
        return LauncherInputGuards.backupFileNameForLabel(label)
            ?: FALLBACK_FILE_NAME
    }

    private fun ensureDestinationFileName(context: Context, uri: Uri, desiredFileName: String): Uri {
        val currentFileName = BackupLibraryFiles.displayName(context, uri)
        if (currentFileName == desiredFileName) {
            return uri
        }
        return renameDestination(context, uri, desiredFileName)
            ?: error("文件管理器把文件名改成了“$currentFileName”，并且无法自动改回“$desiredFileName”")
    }

    private fun renameDestination(context: Context, uri: Uri, desiredFileName: String): Uri? {
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            val renamed = File(file.parentFile ?: return null, desiredFileName)
            return if (!renamed.exists() && file.renameTo(renamed)) {
                Uri.fromFile(renamed)
            } else {
                null
            }
        }

        val resolver = context.contentResolver
        if (DocumentsContract.isDocumentUri(context, uri)) {
            runCatching {
                DocumentsContract.renameDocument(resolver, uri, desiredFileName)
            }.getOrNull()?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val renamed = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, desiredFileName)
                }
                resolver.update(uri, values, null, null) > 0
            }.getOrDefault(false)
            if (renamed) return uri
        }
        return null
    }

    @SuppressLint("NewApi")
    @Suppress("DEPRECATION")
    private fun resolveSharedStoragePath(context: Context, uri: Uri, fallbackFileName: String): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        val externalRoot = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull().orEmpty()
            when {
                documentId.startsWith("raw:") -> return documentId.removePrefix("raw:")
                documentId.startsWith("primary:") -> {
                    val relative = documentId.substringAfter("primary:").trim('/')
                    return "$externalRoot/$relative"
                }
                documentId.startsWith("home:") -> {
                    val relative = documentId.substringAfter("home:").trim('/')
                    return "$externalRoot/Documents/$relative"
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
            )
            runCatching {
                context.contentResolver.query(uri, projection, null, null, null)
            }.getOrNull()?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val name = if (nameIndex >= 0) {
                        cursor.getString(nameIndex)?.takeIf { it.isNotBlank() } ?: fallbackFileName
                    } else {
                        fallbackFileName
                    }
                    val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex) else ""
                    if (!relative.isNullOrBlank()) {
                        return "$externalRoot/${relative.trim('/')}/$name"
                    }
                }
            }
        }

        return null
    }

    private fun deletePlaceholder(context: Context, uri: Uri): Boolean {
        return try {
            if (uri.scheme == "file") {
                val file = File(uri.path.orEmpty())
                !file.exists() || file.delete()
            } else {
                val resolver = context.contentResolver
                val deletedByDocument = runCatching {
                    DocumentsContract.isDocumentUri(context, uri) && DocumentsContract.deleteDocument(resolver, uri)
                }.getOrDefault(false)
                deletedByDocument || resolver.delete(uri, null, null) > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun sameSharedFile(left: String, right: String): Boolean {
        val normalizedLeft = normalizeSharedPath(left)
        val normalizedRight = normalizeSharedPath(right)
        return normalizedLeft.isNotBlank() &&
            normalizedRight.isNotBlank() &&
            normalizedLeft.equals(normalizedRight, ignoreCase = true)
    }

    private fun normalizeSharedPath(path: String): String {
        var normalized = path.trim().replace('\\', '/')
        normalized = normalized.replace(Regex("^/sdcard/"), "/storage/emulated/0/")
        normalized = normalized.replace(
            Regex("^/data/data/com\\.termux/files/home/storage/downloads/"),
            "/storage/emulated/0/Download/",
        )
        normalized = normalized.replace(
            Regex("^/data/data/com\\.termux/files/home/storage/shared/"),
            "/storage/emulated/0/",
        )
        return normalized.lowercase(Locale.US)
    }
}
