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

data class BackupArchiveContentSummary(
    val entryCount: Int,
    val hasUserData: Boolean,
    val hasExtensions: Boolean,
    val hasConfiguration: Boolean,
    val hasLukoaManifest: Boolean,
    val truncated: Boolean,
)

object BackupArchiveContentScanner {
    const val MAX_PREVIEW_ENTRIES = 2_000
    private const val MAX_ENTRY_SIZE = 100L * 1024L * 1024L * 1024L
    private const val MAX_TOTAL_DECLARED_SIZE = 1_000L * 1024L * 1024L * 1024L

    fun scan(input: InputStream): BackupArchiveContentSummary {
        var entryCount = 0
        var totalDeclaredSize = 0L
        var hasUserData = false
        var hasExtensions = false
        var hasConfiguration = false
        var hasLukoaManifest = false
        var truncated = false
        GzipCompressorInputStream(input).use { gzip ->
            TarArchiveInputStream(gzip).use { tar ->
                while (true) {
                    val entry = tar.nextEntry as? org.apache.commons.compress.archivers.tar.TarArchiveEntry
                        ?: break
                    if (entryCount >= MAX_PREVIEW_ENTRIES) {
                        truncated = true
                        break
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
                    entryCount += 1
                    val segments = normalized.lowercase(Locale.ROOT).split('/').filter(String::isNotBlank)
                    val fileName = segments.lastOrNull().orEmpty()
                    hasLukoaManifest = hasLukoaManifest || normalized == "LUKOA_BACKUP_MANIFEST.txt"
                    hasUserData = hasUserData || segments.any { it == "data" || it == "default-user" } ||
                        segments.any { it in setOf("chats", "characters", "groups", "worlds") }
                    hasExtensions = hasExtensions || segments.any { it == "extensions" || it == "plugins" }
                    hasConfiguration = hasConfiguration || fileName in setOf("config.yaml", "config.yml")
                }
            }
        }
        return BackupArchiveContentSummary(
            entryCount = entryCount,
            hasUserData = hasUserData,
            hasExtensions = hasExtensions,
            hasConfiguration = hasConfiguration,
            hasLukoaManifest = hasLukoaManifest,
            truncated = truncated,
        )
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
