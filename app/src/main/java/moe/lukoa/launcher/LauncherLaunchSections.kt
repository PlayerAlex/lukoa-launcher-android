package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val launchTips = listOf(
    "遇到问题？导出日志更方便排查。",
    "点“返回酒馆”可重新打开网页。",
    "更新或恢复前，建议先手动备份。",
    "更新或回退版本前，请先停止酒馆。",
    "手动与自动备份会存入不同备份库。",
    "找不到功能时，可先查看文档页。",
    "版本管理会自动比较当前与目标版本。",
    "开启自动备份后，会按规则定时保存。",
    "网页打不开时，先确认酒馆是否运行。",
    "反馈问题时，请附上运行日志。",
    "切换分身后，请确认当前酒馆位置。",
    "重要操作前，请保留一份手动备份。",
)


@Composable
fun TavernControlSection(
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    tavernVersion: String,
    actionInProgress: Boolean,
    busyLabel: String?,
    wakeEnabled: Boolean,
    primaryEnabled: Boolean,
    primaryDisabledReason: String?,
    activeProfileLabel: String = "",
    activePort: Int = 8000,
    profileCount: Int = 1,
    onOpenQuickSwitchProfile: (() -> Unit)? = null,
    launchMode: TavernLaunchMode = TavernLaunchMode.ForegroundConsole,
    onToggleLaunchMode: () -> Unit = {},
    onRestartTavern: () -> Unit = {},
    onWakeTermux: () -> Unit,
    onPrimaryAction: () -> Unit,
    onOpenTavern: () -> Unit,
    onExportLog: () -> Unit,
    onCopyTavernUrl: (() -> Unit)? = null,
) {
    val wakeClick = rememberFeedbackClick(onWakeTermux)
    val primaryClick = rememberFeedbackClick(
        onClick = onPrimaryAction,
        minIntervalMs = if (shouldOfferStopTavern(tavernRunning, tavernStarting)) 0L else 260L,
    )
    val restartClick = rememberFeedbackClick(onRestartTavern)
    val launchModeClick = rememberFeedbackClick(onToggleLaunchMode)
    val openTavernClick = rememberFeedbackClick(onOpenTavern)
    val exportClick = rememberFeedbackClick(onExportLog)
    var tipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_200L)
            tipIndex = (tipIndex + 1) % launchTips.size
        }
    }
    val statusText = when {
        actionInProgress -> busyLabel ?: "处理中"
        tavernStarting -> "启动中"
        tavernRunning -> "运行中"
        else -> "未运行"
    }
    val primaryText = tavernPrimaryActionLabel(
        tavernRunning = tavernRunning,
        tavernStarting = tavernStarting,
        actionInProgress = actionInProgress,
        busyLabel = busyLabel,
        primaryEnabled = primaryEnabled,
        primaryDisabledReason = primaryDisabledReason,
    )
    val primaryColor = when {
        shouldOfferStopTavern(tavernRunning, tavernStarting) -> LukoaColors.Stop
        else -> LukoaColors.Primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "当前状态",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusText,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (activeProfileLabel.isNotBlank() && profileCount > 1) {
                    val switchEnabled = !actionInProgress && onOpenQuickSwitchProfile != null
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "当前实例",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Surface(
                            modifier = Modifier.clickable(
                                enabled = switchEnabled,
                                role = Role.Button,
                                onClick = { onOpenQuickSwitchProfile?.invoke() },
                            ),
                            color = LukoaColors.Elevated,
                            shape = LukoaCapsuleShape,
                            border = BorderStroke(1.dp, LukoaColors.Border),
                        ) {
                            Text(
                                text = "$activeProfileLabel · $activePort · 切换",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = LukoaColors.TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (tavernRunning && onCopyTavernUrl != null) {
                    val localUrl = "http://127.0.0.1:$activePort"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "本机地址",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Surface(
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClick = rememberFeedbackClick(onCopyTavernUrl),
                            ),
                            color = LukoaColors.Elevated,
                            shape = LukoaCapsuleShape,
                            border = BorderStroke(1.dp, LukoaColors.Border),
                        ) {
                            Text(
                                text = "$localUrl · 复制",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = LukoaColors.TextPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前酒馆版本",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = tavernVersion.ifBlank { "未读取" },
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "tip:",
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = launchTips[tipIndex],
                        modifier = Modifier.weight(1f),
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        SecondaryActionButton(
            text = "唤醒 Termux 并返回",
            enabled = !actionInProgress && wakeEnabled,
            accentColor = LukoaColors.Primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            onClick = wakeClick,
        )
        Button(
            onClick = primaryClick,
            enabled = !actionInProgress && primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (actionInProgress) LukoaColors.Elevated else primaryColor,
                contentColor = if (actionInProgress) LukoaColors.TextSecondary else LukoaColors.Background,
                disabledContainerColor = LukoaColors.Elevated,
                disabledContentColor = LukoaColors.Dim,
            ),
        ) {
            Text(
                primaryText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (!primaryEnabled && primaryDisabledReason != null) {
            Text(
                text = primaryDisabledReason,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (tavernRunning && !tavernStarting) {
            SecondaryActionButton(
                text = "重启酒馆",
                enabled = !actionInProgress,
                accentColor = LukoaColors.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = restartClick,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TavernToolButton(
                text = "返回酒馆",
                enabled = !actionInProgress,
                modifier = Modifier.weight(1f),
                onClick = openTavernClick,
            )
            TavernToolButton(
                text = "导出日志",
                enabled = !actionInProgress,
                modifier = Modifier.weight(1f),
                onClick = exportClick,
            )
        }
    }
}

@Composable
private fun TavernToolButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ToneActionButton(
        text = text,
        enabled = enabled,
        tone = ActionTone.Safe,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun QuickSwitchProfileDialog(
    tavernPathConfig: TavernPathConfig,
    actionsLocked: Boolean,
    onSelectProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text("快速切换酒馆实例") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "切换实例后，启动、停止、版本、备份和设置都会针对选中的实例。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                tavernPathConfig.availableProfiles.forEach { profile ->
                    val isCurrent = profile.id == tavernPathConfig.activeProfile.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !actionsLocked && !isCurrent,
                                role = Role.Button,
                                onClick = {
                                    onSelectProfile(profile.id)
                                    onDismiss()
                                },
                            ),
                        color = if (isCurrent) LukoaColors.PrimarySoft else LukoaColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) LukoaColors.Primary else LukoaColors.Border,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = profile.normalizedName,
                                    color = if (isCurrent) LukoaColors.Primary else LukoaColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (isCurrent) "当前 · 端口 ${profile.normalizedPort}" else "端口 ${profile.normalizedPort}",
                                    color = if (isCurrent) LukoaColors.Primary else LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = TavernPathNormalizer.toDisplayPath(profile.normalizedTavernDir),
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "新建 / 管理分身",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                onClick = {
                    onDismiss()
                    onOpenSettings()
                },
            )
        },
        dismissButton = {
            SecondaryActionButton(
                text = "关闭",
                enabled = true,
                accentColor = LukoaColors.TextSecondary,
                onClick = onDismiss,
            )
        },
    )
}
