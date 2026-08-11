package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class LauncherHealthSnapshot(
    val checkedAtMillis: Long,
    val summaryTitle: String,
    val summaryDetail: String,
    val items: List<LauncherHealthItem>,
    val errorCount: Int,
    val warningCount: Int,
    val unknownCount: Int,
) {
    fun toDisplayReport(): LauncherHealthReport {
        return LauncherHealthReport(
            checkedAtMillis = checkedAtMillis,
            summaryTitle = summaryTitle,
            summaryDetail = summaryDetail,
            items = items,
            errorCount = errorCount,
            warningCount = warningCount,
            unknownCount = unknownCount,
            primaryAction = null,
            doctorReport = null,
        )
    }

    companion object {
        fun fromReport(report: LauncherHealthReport?): LauncherHealthSnapshot? {
            val validReport = report?.takeIf { it.hasData } ?: return null
            return LauncherHealthSnapshot(
                checkedAtMillis = validReport.checkedAtMillis,
                summaryTitle = validReport.summaryTitle,
                summaryDetail = validReport.summaryDetail,
                items = validReport.items.take(MAX_SAVED_HEALTH_ITEMS),
                errorCount = validReport.errorCount,
                warningCount = validReport.warningCount,
                unknownCount = validReport.unknownCount,
            )
        }
    }
}

object LauncherHealthSnapshotCodec {
    private const val FORMAT = "health-v1"
    private const val MAX_RAW_LENGTH = 64 * 1024
    private const val MAX_TEXT_LENGTH = 8 * 1024

    fun encode(snapshot: LauncherHealthSnapshot): String {
        return buildList {
            add(FORMAT)
            add(snapshot.checkedAtMillis.toString())
            add("${snapshot.errorCount}|${snapshot.warningCount}|${snapshot.unknownCount}")
            add(encodeText(snapshot.summaryTitle))
            add(encodeText(snapshot.summaryDetail))
            snapshot.items.take(MAX_SAVED_HEALTH_ITEMS).forEach { item ->
                add("${encodeText(item.title)}|${encodeText(item.detail)}|${item.level.name}")
            }
        }.joinToString("\n")
    }

    fun decode(raw: String?): LauncherHealthSnapshot? {
        if (raw.isNullOrBlank() || raw.length > MAX_RAW_LENGTH) return null
        return runCatching {
            val lines = raw.lineSequence().toList()
            if (lines.size < 5 || lines.first() != FORMAT) return null
            val checkedAtMillis = lines[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
            val counts = lines[2].split('|')
            if (counts.size != 3) return null
            val errorCount = counts[0].toSafeCount() ?: return null
            val warningCount = counts[1].toSafeCount() ?: return null
            val unknownCount = counts[2].toSafeCount() ?: return null
            val summaryTitle = decodeText(lines[3]) ?: return null
            val summaryDetail = decodeText(lines[4]) ?: return null
            val items = lines.drop(5)
                .take(MAX_SAVED_HEALTH_ITEMS)
                .map { decodeItem(it) ?: return null }
            LauncherHealthSnapshot(
                checkedAtMillis = checkedAtMillis,
                summaryTitle = summaryTitle,
                summaryDetail = summaryDetail,
                items = items,
                errorCount = errorCount,
                warningCount = warningCount,
                unknownCount = unknownCount,
            )
        }.getOrNull()
    }

    private fun decodeItem(line: String): LauncherHealthItem? {
        val parts = line.split('|')
        if (parts.size != 3) return null
        val title = decodeText(parts[0]) ?: return null
        val detail = decodeText(parts[1]) ?: return null
        val level = LauncherHealthLevel.entries.firstOrNull { it.name == parts[2] } ?: return null
        return LauncherHealthItem(title = title, detail = detail, level = level)
    }

    private fun encodeText(value: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.take(MAX_TEXT_LENGTH).toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeText(value: String): String? {
        return runCatching {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                .takeIf { it.length <= MAX_TEXT_LENGTH }
        }.getOrNull()
    }

    private fun String.toSafeCount(): Int? {
        return toIntOrNull()?.takeIf { it in 0..MAX_SAVED_HEALTH_ITEMS }
    }
}

private const val MAX_SAVED_HEALTH_ITEMS = 32
