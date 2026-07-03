package moe.lukoa.launcher

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ln
import kotlin.math.pow

data class BackupRestorePreview(
    val archivePath: String,
    val backupName: String,
    val modifiedAtMillis: Long? = null,
    val sizeBytes: Long? = null,
    val restoreTargetDir: String,
)

object BackupRestorePreviewResolver {
    fun resolve(
        context: Context,
        archivePath: String,
        restoreTargetDir: String,
    ): BackupRestorePreview {
        val normalizedPath = archivePath.trim()
        val details = runCatching {
            BackupLibraryFiles.describeLibraryArchive(context, normalizedPath)
        }.getOrNull()
        return BackupRestorePreview(
            archivePath = normalizedPath,
            backupName = details?.fileName
                ?: normalizedPath.replace('\\', '/').substringAfterLast('/').ifBlank { "未命名备份" },
            modifiedAtMillis = details?.modifiedAtMillis?.takeIf { it > 0L },
            sizeBytes = details?.size?.takeIf { it >= 0L },
            restoreTargetDir = restoreTargetDir,
        )
    }
}

fun formatBackupRestorePreviewTime(modifiedAtMillis: Long?): String {
    if (modifiedAtMillis == null || modifiedAtMillis <= 0L) return "未读取"
    return runCatching {
        Instant.ofEpochMilli(modifiedAtMillis)
            .atZone(ZoneId.systemDefault())
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
    return String.format("%.1f %s", value, unit)
}
