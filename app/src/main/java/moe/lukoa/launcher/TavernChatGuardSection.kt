package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernChatGuardContent(
    state: TavernChatGuardState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onSaveConfig: (TavernChatGuardConfig) -> Unit,
    onClearHistory: () -> Unit,
    onCopyPath: (String) -> Boolean,
    onShowHint: (String) -> Unit,
) {
    val snapshot = state.snapshot
    val baseConfig = snapshot?.config ?: TavernChatGuardConfig()
    var enabled by remember(baseConfig) { mutableStateOf(baseConfig.enabled) }
    var wakeLockEnabled by remember(baseConfig) { mutableStateOf(baseConfig.wakeLockEnabled) }
    var minKilobytes by remember(baseConfig) { mutableIntStateOf(baseConfig.minKilobytes) }
    var intervalSeconds by remember(baseConfig) { mutableIntStateOf(baseConfig.intervalSeconds) }

    LaunchedEffect(snapshot) {
        if (snapshot != null) {
            enabled = snapshot.config.enabled
            wakeLockEnabled = snapshot.config.wakeLockEnabled
            minKilobytes = snapshot.config.minKilobytes
            intervalSeconds = snapshot.config.intervalSeconds
        }
    }

    val draftConfig = TavernChatGuardConfig(
        enabled = enabled,
        wakeLockEnabled = wakeLockEnabled,
        minKilobytes = minKilobytes,
        intervalSeconds = intervalSeconds,
    )
    val hasChanges = snapshot == null || draftConfig != snapshot.config
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null

    ManagementDialogIntroCard(
        "聊天守护会用硬链接在后台盯住大体积聊天记录（几乎不占额外空间）。当网页卡死把长聊天写空或截断成不到一半大小时，会自动救回旧版本到同目录。",
    )

    ManagementDialogSectionTitle("当前状态")
    SettingsEntryGroup {
        SettingsEntryRow(
            title = "当前实例",
            detail = state.message,
            detailMaxLines = Int.MAX_VALUE,
            value = instanceLabel,
            valueColor = LukoaColors.Primary,
            valueAsPill = true,
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "守护进程",
            detail = when {
                snapshot == null -> "点击下方“刷新状态”查看实时守护状态。"
                snapshot.running -> "后台守护运行中（PID ${snapshot.pid ?: "-"}），已关联 ${snapshot.linkedChatCount} 个聊天文件。"
                tavernRunning -> "酒馆正在运行，但未检测到守护进程；重启酒馆后会自动拉起。"
                else -> "酒馆未运行时守护会自动退出，下次启动酒馆时一起拉起。"
            },
            detailMaxLines = 3,
            value = when {
                snapshot == null -> "未读取"
                snapshot.running -> "守护中"
                else -> "未运行"
            },
            valueColor = when {
                snapshot?.running == true -> LukoaColors.Primary
                else -> LukoaColors.TextSecondary
            },
            valueAsPill = true,
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "已救回记录",
            detail = if (state.effectiveRescueCount > 0) {
                "检测到 ${state.effectiveRescueCount} 条救回记录。在酒馆打开对应角色 → “管理聊天文件”就能看到带「露科亚救回」的聊天。"
            } else {
                "目前没有触发过异常覆盖救回。"
            },
            detailMaxLines = 3,
            value = "${state.effectiveRescueCount} 条",
            valueColor = if (state.effectiveRescueCount > 0) LukoaColors.Accent else LukoaColors.TextSecondary,
            valueAsPill = true,
            highlightColor = if (state.effectiveRescueCount > 0) LukoaColors.Accent else null,
        )
    }

    SettingsFeedbackActionButton(
        text = if (state.loading) "读取中..." else "刷新守护状态",
        enabled = !actionsLocked && !state.loading,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = if (state.loading) "正在读取守护状态，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onRefresh,
    )

    ManagementDialogSectionTitle("守护策略")
    SettingsEntryGroup {
        ChatGuardSwitchRow(
            title = "开启聊天防写坏守护",
            detail = "推荐保持开启。修改后在酒馆运行中会自动生效。",
            checked = enabled,
            enabled = !actionsLocked,
            onCheckedChange = { enabled = it },
        )
        SettingsEntryDivider()
        ChatGuardSwitchRow(
            title = "启动时开启防休眠锁（Wake Lock）",
            detail = "减少熄屏后系统杀掉 Termux 导致回复中断的概率；下次启动或重启酒馆时生效。",
            checked = wakeLockEnabled,
            enabled = !actionsLocked,
            onCheckedChange = { wakeLockEnabled = it },
        )
    }

    ManagementDialogActionCard(title = "触发救回的最小聊天体积（当前：$minKilobytes KB）") {
        Text(
            text = "只有原聊天大于该体积，且被新文件覆盖到不到一半大小时才会救回，避免误伤短测试聊天。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TavernChatGuardPolicy.MIN_KB_PRESETS.forEach { kb ->
                val selected = minKilobytes == kb
                SettingsCompactChoiceButton(
                    text = if (kb >= 1024) "${kb / 1024}MB" else "${kb}KB",
                    enabled = !actionsLocked,
                    accentColor = if (selected) LukoaColors.Accent else LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = lockedHint,
                    onShowHint = onShowHint,
                    onClick = { minKilobytes = kb },
                )
            }
        }
    }

    ManagementDialogActionCard(title = "后台扫描间隔（当前：$intervalSeconds 秒）") {
        Text(
            text = "默认 10 秒，几乎不耗电；聊天频繁时可设 5 秒，追求更低唤醒频率可设 20–30 秒。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TavernChatGuardPolicy.INTERVAL_PRESETS.forEach { seconds ->
                val selected = intervalSeconds == seconds
                SettingsCompactChoiceButton(
                    text = "${seconds}秒",
                    enabled = !actionsLocked,
                    accentColor = if (selected) LukoaColors.Accent else LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = lockedHint,
                    onShowHint = onShowHint,
                    onClick = { intervalSeconds = seconds },
                )
            }
        }
    }

    SettingsFeedbackActionButton(
        text = "保存守护设置",
        enabled = !actionsLocked && hasChanges,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = lockedHint ?: "当前设置没有改动。",
        onShowHint = onShowHint,
        onClick = { onSaveConfig(draftConfig) },
    )

    ManagementDialogSectionTitle("救回历史")
    val rescues = snapshot?.rescues.orEmpty()
    if (rescues.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rescues.forEach { record ->
                SettingsEntryGroup {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = record.chatDisplayName,
                                modifier = Modifier.weight(1f),
                                color = LukoaColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            StatusPill(
                                text = if (record.exists) "${record.sizeKilobytes} KB" else "已移走",
                                active = record.exists,
                                toneColor = if (record.exists) LukoaColors.Accent else LukoaColors.TextSecondary,
                                activeBackground = LukoaColors.AccentSoft,
                            )
                        }
                        Text(
                            text = "救回时间：${record.timestamp}",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "救回文件：${record.rescuedFileName}",
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SettingsFeedbackActionButton(
                            text = "复制救回文件路径",
                            enabled = true,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = null,
                            onShowHint = onShowHint,
                            onClick = {
                                val copied = onCopyPath(record.rescuedChatPath)
                                onShowHint(if (copied) "已复制救回文件路径。" else "复制路径失败。")
                            },
                        )
                    }
                }
            }
            TimedDangerFeedbackActionButton(
                text = "清空救回记录列表（不删除聊天文件）",
                enabled = !actionsLocked,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onConfirmed = onClearHistory,
            )
        }
    } else {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "暂无救回记录",
                detail = if (snapshot == null) {
                    "点击“刷新守护状态”读取最近救回记录。"
                } else {
                    "聊天文件正常，还没有发生过被截断覆盖的情况。"
                },
                detailMaxLines = 3,
            )
        }
    }
}

@Composable
private fun ChatGuardSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = if (enabled) LukoaColors.TextPrimary else LukoaColors.Dim,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LukoaColors.Background,
                checkedTrackColor = LukoaColors.Primary,
                uncheckedThumbColor = LukoaColors.TextSecondary,
                uncheckedTrackColor = LukoaColors.Elevated,
            ),
        )
    }
}
