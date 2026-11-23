package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showAdvanced by rememberSaveable(instanceLabel) { mutableStateOf(false) }

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

    SettingsEntryGroup {
        SettingsEntryRow(
            title = "保护状态（$instanceLabel）",
            detail = when {
                snapshot == null -> "点下方按钮查看当前保护状态。"
                snapshot.running -> "正在后台保护 ${snapshot.linkedChatCount} 个聊天文件。"
                tavernRunning -> "酒馆已运行，重启酒馆后会自动开启保护。"
                else -> "待机中，下次启动酒馆时会自动一起开启。"
            },
            detailMaxLines = 2,
            value = when {
                snapshot == null -> "未读取"
                snapshot.running -> "保护中"
                else -> "待机中"
            },
            valueColor = if (snapshot?.running == true) LukoaColors.Primary else LukoaColors.TextSecondary,
            valueAsPill = true,
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "已救回聊天",
            detail = if (state.effectiveRescueCount > 0) {
                "打开酒馆对应角色的“管理聊天文件”就能看到救回的旧档。"
            } else {
                "目前没有出现过聊天异常丢失。"
            },
            detailMaxLines = 2,
            value = "${state.effectiveRescueCount} 条",
            valueColor = if (state.effectiveRescueCount > 0) LukoaColors.Primary else LukoaColors.TextSecondary,
            valueAsPill = true,
        )
    }

    SettingsFeedbackActionButton(
        text = if (state.loading) "读取中..." else "刷新守护状态",
        enabled = !actionsLocked && !state.loading,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = if (state.loading) "正在读取状态，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onRefresh,
    )

    ManagementDialogSectionTitle("保护开关")
    SettingsEntryGroup {
        LukoaSwitchRow(
            title = "自动保护长聊天防丢失",
            detail = "建议保持开启，改完后自动生效。",
            checked = enabled,
            enabled = !actionsLocked,
            onCheckedChange = { enabled = it },
        )
        SettingsEntryDivider()
        LukoaSwitchRow(
            title = "熄屏防断连（防系统杀后台）",
            detail = "减少锁屏或切到别的软件时回复中断的概率，下次启动生效。",
            checked = wakeLockEnabled,
            enabled = !actionsLocked,
            onCheckedChange = { wakeLockEnabled = it },
        )
    }

    LukoaCollapsibleCard(
        title = "高级参数（一般保持默认即可）",
        subtitle = "触发体积 ${if (minKilobytes >= 1024) "${minKilobytes / 1024}MB" else "${minKilobytes}KB"} · 每 ${intervalSeconds} 秒检查",
        expanded = showAdvanced,
        onToggle = { showAdvanced = !showAdvanced },
    ) {
        Text(
            text = "多大以上的聊天才需要保护",
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
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
                    selected = selected,
                    accentColor = if (selected) LukoaColors.Primary else LukoaColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = lockedHint,
                    onShowHint = onShowHint,
                    onClick = { minKilobytes = kb },
                )
            }
        }
        Text(
            text = "后台检查频率（默认 10 秒）",
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
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
                    selected = selected,
                    accentColor = if (selected) LukoaColors.Primary else LukoaColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = lockedHint,
                    onShowHint = onShowHint,
                    onClick = { intervalSeconds = seconds },
                )
            }
        }
    }

    SettingsFeedbackActionButton(
        text = "保存保护设置",
        enabled = !actionsLocked && hasChanges,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = lockedHint ?: "当前设置没有改动。",
        onShowHint = onShowHint,
        onClick = { onSaveConfig(draftConfig) },
    )

    val rescues = snapshot?.rescues.orEmpty()
    if (rescues.isNotEmpty()) {
        ManagementDialogSectionTitle("已救回的聊天记录")
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
                                toneColor = if (record.exists) LukoaColors.Primary else LukoaColors.TextSecondary,
                                activeBackground = LukoaColors.PrimarySoft,
                            )
                        }
                        Text(
                            text = "救回文件：${record.rescuedFileName}",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SettingsFeedbackActionButton(
                            text = "复制文件路径",
                            enabled = true,
                            accentColor = LukoaColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = null,
                            onShowHint = onShowHint,
                            onClick = {
                                val copied = onCopyPath(record.rescuedChatPath)
                                onShowHint(if (copied) "已复制文件路径。" else "复制路径失败。")
                            },
                        )
                    }
                }
            }
            TimedDangerFeedbackActionButton(
                text = "清空提示列表（不会删除聊天文件）",
                enabled = !actionsLocked,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onConfirmed = onClearHistory,
            )
        }
    }
}
