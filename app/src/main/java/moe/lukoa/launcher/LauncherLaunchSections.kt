package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp



@Composable
fun TavernControlSection(
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    actionInProgress: Boolean,
    busyLabel: String?,
    wakeEnabled: Boolean,
    primaryEnabled: Boolean,
    primaryDisabledReason: String?,
    onWakeTermux: () -> Unit,
    onPrimaryAction: () -> Unit,
    onOpenTavern: () -> Unit,
    onExportLog: () -> Unit,
) {
    val offerStop = shouldOfferStopTavern(tavernRunning, tavernStarting)
    val wakeClick = rememberFeedbackClick(onWakeTermux)
    val primaryClick = rememberFeedbackClick(
        onClick = onPrimaryAction,
        minIntervalMs = if (offerStop) 0L else 260L,
    )
    val openTavernClick = rememberFeedbackClick(onOpenTavern)
    val primaryText = when {
        tavernStarting -> "停止酒馆"
        tavernRunning -> "停止酒馆"
        actionInProgress -> "${busyLabel ?: "处理中"}..."
        !primaryEnabled && primaryDisabledReason?.contains("权限") == true -> "先修权限"
        !primaryEnabled && primaryDisabledReason?.contains("Termux") == true -> "先安装 Termux"
        !primaryEnabled -> "先安装酒馆"
        else -> "启动酒馆"
    }
    SectionPanel(title = "操作", accentColor = LukoaColors.Accent) {
        PrimaryActionButton(
            text = primaryText,
            enabled = primaryEnabled && (!actionInProgress || offerStop),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            danger = offerStop,
            onClick = primaryClick,
        )
        if (!primaryEnabled && primaryDisabledReason != null) {
            Text(
                text = primaryDisabledReason,
                color = LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TavernToolButton(
                text = "唤醒 Termux",
                enabled = !actionInProgress && wakeEnabled,
                modifier = Modifier.weight(1f),
                onClick = wakeClick,
            )
            TavernToolButton(
                text = "打开酒馆",
                enabled = !actionInProgress,
                modifier = Modifier.weight(1f),
                onClick = openTavernClick,
            )
        }
    }
}

@Composable
fun LaunchBlockerActionSection(
    termuxInstalled: Boolean,
    runCommandPermissionGranted: Boolean,
    externalAppsBlocked: Boolean,
    actionsLocked: Boolean,
    onOpenTermuxDownload: () -> Unit,
    onOpenTermuxGithub: () -> Unit,
    onRecheckTermux: () -> Unit,
    onRequestPermission: () -> Unit,
    onCopyPermissionCommand: () -> Unit,
    onOpenTermux: () -> Unit,
    onRecheckPermission: () -> Unit,
) {
    DashedSection(label = "下一步") {
        when {
            !termuxInstalled -> {
                StateNote("为什么需要：Termux 负责真正执行安装、启动和备份命令。启动器不会在后台偷偷安装它。")
                PrimaryActionButton(
                    text = "从 F-Droid 安装 Termux",
                    enabled = !actionsLocked,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenTermuxDownload,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SecondaryActionButton(
                        text = "GitHub 下载",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Text,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTermuxGithub,
                    )
                    SecondaryActionButton(
                        text = "重新检测",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Text,
                        modifier = Modifier.weight(1f),
                        onClick = onRecheckTermux,
                    )
                }
            }

            !runCommandPermissionGranted || externalAppsBlocked -> {
                StateNote(
                    if (!runCommandPermissionGranted) {
                        "为什么需要：RUN_COMMAND 是 Android 允许启动器向 Termux 发送命令的权限，不会读取聊天内容。"
                    } else {
                        "为什么需要：Termux 还没有允许外部应用调用。复制命令到 Termux 执行一次即可。"
                    },
                )
                PrimaryActionButton(
                    text = if (!runCommandPermissionGranted) "请求权限" else "复制权限命令",
                    enabled = !actionsLocked,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (!runCommandPermissionGranted) onRequestPermission else onCopyPermissionCommand,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SecondaryActionButton(
                        text = "复制权限命令",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Text,
                        modifier = Modifier.weight(1f),
                        onClick = onCopyPermissionCommand,
                    )
                    SecondaryActionButton(
                        text = "打开 Termux",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Text,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTermux,
                    )
                }
                SecondaryActionButton(
                    text = "重新检测",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRecheckPermission,
                )
            }
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
