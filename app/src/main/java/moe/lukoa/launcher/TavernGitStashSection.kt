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
    onSaveStash: (() -> Unit)? = null,
    onApply: (Int) -> Unit,
    onDrop: (Int) -> Unit,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val stopRequiredHint = when {
        actionsLocked -> lockedHint
        tavernRunning -> "把改动收起或放回酒馆前，请先停止酒馆。"
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("当前实例")
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前实例",
                    detail = state.message,
                    detailMaxLines = Int.MAX_VALUE,
                    value = instanceLabel,
                    valueColor = LukoaColors.TextPrimary,
                )
            }
            SettingsEntryGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SettingsFeedbackActionButton(
                        text = if (state.loading) "读取中..." else "读取暂存记录",
                        enabled = !actionsLocked && !state.loading,
                        accentColor = LukoaColors.Primary,
                        modifier = Modifier.weight(1f),
                        unavailableHint = if (state.loading) "正在查看暂存记录，请稍等。" else lockedHint,
                        onShowHint = onShowHint,
                        onClick = onRefresh,
                    )
                    if (onSaveStash != null) {
                        SettingsFeedbackActionButton(
                            text = "暂存当前改动",
                            enabled = stopRequiredHint == null && !state.loading,
                            accentColor = LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = if (state.loading) "正在处理，请稍等。" else stopRequiredHint,
                            onShowHint = onShowHint,
                            onClick = onSaveStash,
                        )
                    }
                }
            }
        }

        if (state.gitAvailable == false) {
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前酒馆不支持暂存记录",
                    detail = "只有通过启动器正常安装的酒馆目录才会有这项记录。",
                )
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel(
                if (state.entries.isEmpty()) "收起来的改动" else "收起来的改动（${state.entries.size} 条）",
            )
            if (state.entries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.entries.forEach { entry ->
                        SettingsEntryGroup {
                            Column(
                                modifier = Modifier.padding(14.dp),
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
                                        Text(
                                            text = "启动器自动收起",
                                            color = LukoaColors.Primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
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
                                        color = LukoaColors.Dim,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    SettingsFeedbackActionButton(
                                        text = "放回当前酒馆",
                                        enabled = stopRequiredHint == null,
                                        accentColor = LukoaColors.Primary,
                                        modifier = Modifier.weight(1f),
                                        unavailableHint = stopRequiredHint,
                                        onShowHint = onShowHint,
                                        onClick = { onApply(entry.index) },
                                    )
                                    TimedDangerFeedbackActionButton(
                                        text = "清理这条记录",
                                        enabled = !actionsLocked,
                                        modifier = Modifier.weight(1f),
                                        unavailableHint = lockedHint,
                                        onShowHint = onShowHint,
                                        onConfirmed = { onDrop(entry.index) },
                                    )
                                }
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
            } else {
                SettingsEntryGroup {
                    SettingsEntryRow(
                        title = if (state.loading) "正在查看暂存记录…" else "尚未读取暂存记录",
                        detail = if (state.loading) "请稍等。" else "点击上方「读取暂存记录」查看收起来的本地改动。",
                    )
                }
            }
        }
    }
}
