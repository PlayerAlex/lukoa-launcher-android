package moe.lukoa.launcher

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

data class BackupRestorePreview(
    val archivePath: String,
    val backupName: String,
    val modifiedAtMillis: Long? = null,
    val sizeBytes: Long? = null,
    val restoreTargetDir: String,
    val targetProfileId: String,
    val targetInstanceLabel: String,
    val targetPort: Int,
    val targetWasRunning: Boolean,
)

object BackupRestorePreviewResolver {
    fun resolve(
        context: Context,
        archivePath: String,
        restoreTargetDir: String,
        targetProfileId: String,
        targetInstanceLabel: String,
        targetPort: Int,
        targetWasRunning: Boolean,
    ): BackupRestorePreview {
        val normalizedPath = archivePath.trim()
        val details = BackupLibraryFiles.describeLibraryArchive(context, normalizedPath)
            ?: error("启动器读不到这个备份。请先刷新备份库，或重新导入。")
        return BackupRestorePreview(
            archivePath = normalizedPath,
            backupName = details.fileName,
            modifiedAtMillis = details.modifiedAtMillis.takeIf { it > 0L },
            sizeBytes = details.size.takeIf { it >= 0L },
            restoreTargetDir = restoreTargetDir,
            targetProfileId = targetProfileId,
            targetInstanceLabel = targetInstanceLabel,
            targetPort = targetPort,
            targetWasRunning = targetWasRunning,
        )
    }
}

object BackupRestorePreviewGuard {
    fun rejectionReason(
        preview: BackupRestorePreview,
        activeProfileId: String,
        activeTargetDir: String,
        tavernRunning: Boolean,
    ): String? {
        if (
            preview.targetProfileId != activeProfileId ||
            preview.restoreTargetDir != activeTargetDir
        ) {
            return "当前实例已经切换。请关闭此预览，在目标实例中重新选择备份。"
        }
        if (preview.targetWasRunning || tavernRunning) {
            return "酒馆正在运行或启动。请先停止当前实例，再重新打开恢复预览。"
        }
        return null
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
