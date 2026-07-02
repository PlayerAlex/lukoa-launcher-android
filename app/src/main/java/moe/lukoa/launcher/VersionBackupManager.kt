package moe.lukoa.launcher

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class VersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val backupSchemaVersion: Int,
)

object VersionBackupManager {
    const val BACKUP_SCHEMA_VERSION = 1

    fun versionInfo(context: Context): VersionInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return VersionInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName ?: "...",
            versionCode = versionCode,
            backupSchemaVersion = BACKUP_SCHEMA_VERSION,
        )
    }

    fun createBackup(context: Context, state: LauncherUiState): File {
        val versionInfo = versionInfo(context)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = timestamp()
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-backup-$timestamp", "json")
        val json = JSONObject()
            .put("schemaVersion", BACKUP_SCHEMA_VERSION)
            .put("createdAt", timestamp)
            .put(
                "app",
                JSONObject()
                    .put("packageName", versionInfo.packageName)
                    .put("versionName", versionInfo.versionName)
                    .put("versionCode", versionInfo.versionCode),
            )
            .put(
                "state",
                JSONObject()
                    .put("summary", state.summary)
                    .put("status", state.status)
                    .put("verified", state.verified)
                    .put("termuxLog", state.termuxLog)
                    .put("appLog", state.appLog),
            )
        file.writeText(json.toString(2), Charsets.UTF_8)
        return file
    }

    fun createVersionReport(context: Context): File {
        val versionInfo = versionInfo(context)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = timestamp()
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-version-$timestamp", "txt")
        file.writeText(
            buildString {
                appendLine("露科亚启动器版本信息")
                appendLine("导出时间：$timestamp")
                appendLine("包名：${versionInfo.packageName}")
                appendLine("应用版本：v${versionInfo.versionName}")
                appendLine("版本代码：${versionInfo.versionCode}")
                appendLine("备份格式：v${versionInfo.backupSchemaVersion}")
            },
            Charsets.UTF_8,
        )
        return file
    }

    private fun timestamp(): String {
        return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
    }
}
