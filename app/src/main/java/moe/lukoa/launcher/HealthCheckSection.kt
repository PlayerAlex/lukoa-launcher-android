package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HealthCheckSection(
    report: LauncherHealthReport?,
    checking: Boolean,
    actionsLocked: Boolean,
    onRunHealthCheck: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    SectionPanel(title = "一键体检", accentColor = LukoaColors.Accent) {
        val effectiveReport = report.takeIf { it?.hasData == true }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = effectiveReport?.summaryTitle ?: "还没体检",
                        modifier = Modifier.weight(1f),
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StatusPill(
                        text = summaryPillText(effectiveReport),
                        active = effectiveReport != null,
                        toneColor = summaryTone(effectiveReport),
                        activeBackground = summaryTone(effectiveReport).copy(alpha = 0.14f),
                    )
                }
                Text(
                    text = effectiveReport?.summaryDetail
                        ?: "点“一键体检”后，启动器会把权限、路径、镜像源和酒馆环境一起看一遍。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                effectiveReport?.let {
                    Text(
                        text = "上次体检：${formatCheckedAt(it.checkedAtMillis)}",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = if (checking) "体检中..." else "一键体检",
                enabled = !actionsLocked && !checking,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onRunHealthCheck,
            )
            effectiveReport?.primaryAction?.let { action ->
                SecondaryActionButton(
                    text = action.label,
                    enabled = !actionsLocked && !checking,
                    accentColor = primaryActionColor(action.type),
                    modifier = Modifier.weight(1f),
                    onClick = onPrimaryAction,
                )
            }
        }

        effectiveReport?.items?.takeIf { it.isNotEmpty() }?.forEach { item ->
            HealthCheckItemRow(item)
        }
    }
}

@Composable
private fun HealthCheckItemRow(item: LauncherHealthItem) {
    val toneColor = item.level.toneColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface.copy(alpha = 0.75f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = item.level.label(),
                    active = item.level != LauncherHealthLevel.Unknown,
                    toneColor = toneColor,
                    activeBackground = toneColor.copy(alpha = 0.14f),
                )
            }
            Text(
                text = item.detail,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun summaryPillText(report: LauncherHealthReport?): String {
    if (report == null) return "未体检"
    return when {
        report.errorCount > 0 -> "${report.errorCount} 个问题"
        report.warningCount > 0 -> "${report.warningCount} 个提醒"
        else -> "正常"
    }
}

private fun summaryTone(report: LauncherHealthReport?): Color {
    return when {
        report == null -> LukoaColors.Muted
        report.errorCount > 0 -> LukoaColors.Danger
        report.warningCount > 0 -> LukoaColors.Amber
        else -> LukoaColors.Accent
    }
}

private fun LauncherHealthLevel.label(): String {
    return when (this) {
        LauncherHealthLevel.Good -> "正常"
        LauncherHealthLevel.Warning -> "提醒"
        LauncherHealthLevel.Error -> "需处理"
        LauncherHealthLevel.Unknown -> "未读到"
    }
}

private fun LauncherHealthLevel.toneColor(): Color {
    return when (this) {
        LauncherHealthLevel.Good -> LukoaColors.Accent
        LauncherHealthLevel.Warning -> LukoaColors.Amber
        LauncherHealthLevel.Error -> LukoaColors.Danger
        LauncherHealthLevel.Unknown -> LukoaColors.Muted
    }
}

private fun primaryActionColor(type: LauncherHealthActionType): Color {
    return when (type) {
        LauncherHealthActionType.DownloadTermux,
        LauncherHealthActionType.RequestRunPermission,
        LauncherHealthActionType.CopyExternalAppsCommand,
        LauncherHealthActionType.PrepareTermuxEnvironment -> LukoaColors.Amber

        LauncherHealthActionType.StopTavern -> LukoaColors.Danger
        else -> LukoaColors.Accent
    }
}

private fun formatCheckedAt(timeMillis: Long): String {
    if (timeMillis <= 0L) return "刚刚"
    return CHECKED_AT_FORMATTER.format(Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()))
}

private val CHECKED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
