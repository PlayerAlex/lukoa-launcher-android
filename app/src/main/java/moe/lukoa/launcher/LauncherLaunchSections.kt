package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    val wakeClick = rememberFeedbackClick(onWakeTermux)
    val primaryClick = rememberFeedbackClick(
        onClick = onPrimaryAction,
        minIntervalMs = if (tavernRunning) 0L else 260L,
    )
    val openTavernClick = rememberFeedbackClick(onOpenTavern)
    val exportClick = rememberFeedbackClick(onExportLog)
    val statusText = when {
        actionInProgress -> busyLabel ?: "处理中"
        tavernStarting -> "启动中"
        tavernRunning -> "运行中"
        else -> "未运行"
    }
    val statusDetail = when {
        actionInProgress -> "正在执行操作，完成后按钮会恢复。"
        tavernStarting -> "正在等待酒馆打开网页。"
        tavernRunning -> "酒馆已运行，主按钮会切换为停止。"
        primaryEnabled -> "酒馆未运行，可以直接启动。"
        primaryDisabledReason != null -> primaryDisabledReason
        else -> "等待检测结果。"
    }
    val primaryText = when {
        actionInProgress -> "${busyLabel ?: "处理中"}..."
        tavernStarting -> "启动中..."
        !primaryEnabled && primaryDisabledReason?.contains("权限") == true -> "先修权限"
        !primaryEnabled && primaryDisabledReason?.contains("Termux") == true -> "先安装 Termux"
        !primaryEnabled -> "先安装酒馆"
        tavernRunning -> "停止酒馆"
        else -> "启动酒馆"
    }
    val primaryColor = when {
        tavernRunning -> LukoaColors.Danger
        else -> LukoaColors.Accent
    }
    SectionPanel(title = "酒馆控制", accentColor = LukoaColors.Accent) {
        TavernControlStatusCard(
            statusText = statusText,
            statusDetail = statusDetail,
            statusActive = tavernRunning || tavernStarting || actionInProgress,
            statusTone = if (tavernRunning) LukoaColors.Danger else LukoaColors.Accent,
            wakeEnabled = !actionInProgress && wakeEnabled,
            onWake = wakeClick,
        )
        Button(
            onClick = primaryClick,
            enabled = !actionInProgress && primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (actionInProgress) LukoaColors.SurfaceAlt else primaryColor,
                contentColor = if (actionInProgress) LukoaColors.Muted else LukoaColors.Background,
                disabledContainerColor = LukoaColors.SurfaceAlt,
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
                color = LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
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
private fun TavernControlStatusCard(
    statusText: String,
    statusDetail: String,
    statusActive: Boolean,
    statusTone: androidx.compose.ui.graphics.Color,
    wakeEnabled: Boolean,
    onWake: () -> Unit,
) {
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "当前控制状态",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusDetail,
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    text = statusText,
                    active = statusActive,
                    toneColor = if (statusActive) statusTone else LukoaColors.Muted,
                    activeBackground = if (statusTone == LukoaColors.Danger) LukoaColors.DangerSoft else LukoaColors.AccentSoft,
                )
            }
            SecondaryActionButton(
                text = "唤醒 Termux 并返回",
                enabled = wakeEnabled,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.fillMaxWidth(),
                onClick = onWake,
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
