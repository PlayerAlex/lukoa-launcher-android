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
        tavernRunning -> "恢复源码暂存前请先停止酒馆。"
        else -> null
    }

    ManagementDialogIntroCard(
        "更新或回退酒馆版本时，如果你曾选择“暂存本地改动”，启动器会把改动保存在 Git 暂存区里（带 lukoa-before-* 标记）。在这里可以查看、尝试恢复或清理旧暂存。",
    )

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
        unavailableHint = if (state.loading) "正在读取 Git 暂存列表，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onRefresh,
    )

    if (state.gitAvailable == false) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前目录不是 Git 仓库",
                detail = "只有通过 Git 管理的酒馆目录才支持暂存管理。",
            )
        }
        return
    }

    if (state.entries.isNotEmpty()) {
        ManagementDialogSectionTitle("已暂存的本地改动（${state.entries.size} 条）")
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
                                text = entry.ref,
                                color = LukoaColors.Primary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (entry.isLukoaManaged) {
                                StatusPill(
                                    text = "更新前自动暂存",
                                    active = true,
                                    toneColor = LukoaColors.Accent,
                                    activeBackground = LukoaColors.AccentSoft,
                                )
                            }
                        }
                        Text(
                            text = entry.message,
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (entry.createdAt.isNotBlank()) {
                            Text(
                                text = "暂存时间：${entry.createdAt}",
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        SettingsFeedbackActionButton(
                            text = "恢复到当前目录（保留暂存备份）",
                            enabled = applyUnavailableHint == null,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = applyUnavailableHint,
                            onShowHint = onShowHint,
                            onClick = { onApply(entry.index) },
                        )
                        TimedDangerFeedbackActionButton(
                            text = "删除这条暂存记录",
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
                title = "当前没有暂存记录",
                detail = "Git 暂存区是空的，没有待处理的本地源码改动。",
            )
        }
    }
}
