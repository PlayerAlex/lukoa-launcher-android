package moe.lukoa.launcher

import android.content.Context
import android.net.Uri

data class ExternalBackupImportResult(
    val ok: Boolean,
    val message: String,
    val termuxReadablePath: String = "",
)

object ExternalBackupImporter {
    fun copyToBackupLibrary(context: Context, sourceUri: Uri): ExternalBackupImportResult {
        return try {
            val copied = BackupLibraryFiles.copyExternalUriToLibrary(context, sourceUri)
            ExternalBackupImportResult(
                ok = true,
                message = "已复制到备份库：${copied.fileName}（${copied.size.coerceAtLeast(0L)} 字节）",
                termuxReadablePath = copied.termuxReadablePath,
            )
        } catch (error: Exception) {
            ExternalBackupImportResult(
                ok = false,
                message = "导入外部备份失败：${error.message ?: error.javaClass.simpleName}",
            )
        }
    }
}
