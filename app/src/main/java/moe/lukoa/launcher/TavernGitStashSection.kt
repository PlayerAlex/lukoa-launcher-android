package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernGitStashContent(
    state: TavernGitStashState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onApply: (Int) -> Unit,
    onDrop: (Int) -> Unit,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val applyUnavailableHint = when {
        actionsLocked -> lockedHint
        tavernRunning -> "把改动放回酒馆前，请先停止酒馆。"
        else -> null
    }

    if (state.loading && state.gitAvailable == null) {
        LukoaInlineLoadingCard("正在自动读取当前实例的暂存记录…")
    }

    SettingsEntryGroup {
        SettingsEntryRow(
            title = "当前实例",
            detail = state.message,
            detailMaxLines = Int.MAX_VALUE,
            value = instanceLabel,
            valueColor = LukoaColors.Primary,
            valueAsPill = true,
        )
    }

    SettingsFeedbackActionButton(
        text = if (state.loading) "读取中..." else "读取暂存记录",
        enabled = !actionsLocked && !state.loading,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = if (state.loading) "正在查看暂存记录，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onRefresh,
    )

    if (state.gitAvailable == false) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前酒馆不支持暂存记录",
                detail = "只有通过启动器正常安装的酒馆目录才会有这项记录。",
            )
        }
        return
    }

    if (state.entries.isNotEmpty()) {
        ManagementDialogSectionTitle("收起来的改动（${state.entries.size} 条）")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.entries.forEach { entry ->
                SettingsEntryGroup {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "暂存记录 #${entry.index + 1}",
                                color = LukoaColors.TextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (entry.isLukoaManaged) {
                                StatusPill(
                                    text = "更新前自动收起",
                                    active = true,
                                    toneColor = LukoaColors.Primary,
                                    activeBackground = LukoaColors.PrimarySoft,
                                )
                            }
                        }
                        Text(
                            text = entry.message,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (entry.createdAt.isNotBlank()) {
                            Text(
                                text = "保存时间：${entry.createdAt}",
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        SettingsFeedbackActionButton(
                            text = "把改动放回当前酒馆",
                            enabled = applyUnavailableHint == null,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = applyUnavailableHint,
                            onShowHint = onShowHint,
                            onClick = { onApply(entry.index) },
                        )
                        TimedDangerFeedbackActionButton(
                            text = "清理这条记录",
                            enabled = !actionsLocked,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onConfirmed = { onDrop(entry.index) },
                        )
                    }
                }
            }
        }
    } else if (state.gitAvailable == true && !state.loading) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "很干净，没有暂存记录",
                detail = "你没有待找回或待清理的临时改动。",
            )
        }
    }
}
