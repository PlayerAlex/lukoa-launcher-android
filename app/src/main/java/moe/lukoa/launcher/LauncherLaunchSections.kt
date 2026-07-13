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
    val offerStop = shouldOfferStopTavern(tavernRunning, tavernStarting)
    val wakeClick = rememberFeedbackClick(onWakeTermux)
    val primaryClick = rememberFeedbackClick(
        onClick = onPrimaryAction,
        minIntervalMs = if (offerStop) 0L else 260L,
    )
    val openTavernClick = rememberFeedbackClick(onOpenTavern)
    val exportClick = rememberFeedbackClick(onExportLog)
    val primaryText = when {
        tavernStarting -> "停止酒馆"
        tavernRunning -> "停止酒馆"
        actionInProgress -> "${busyLabel ?: "处理中"}..."
        !primaryEnabled && primaryDisabledReason?.contains("权限") == true -> "先修权限"
        !primaryEnabled && primaryDisabledReason?.contains("Termux") == true -> "先安装 Termux"
        !primaryEnabled -> "先安装酒馆"
        else -> "启动酒馆"
    }
    val primaryColor = if (offerStop) LukoaColors.Danger else LukoaColors.Accent
    SectionPanel(title = "操作", accentColor = LukoaColors.Accent) {
        Button(
            onClick = primaryClick,
            enabled = primaryEnabled && (!actionInProgress || offerStop),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (actionInProgress && !offerStop) LukoaColors.SurfaceAlt else primaryColor,
                contentColor = if (actionInProgress && !offerStop) LukoaColors.Muted else LukoaColors.Background,
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
        TavernToolButton(
            text = "导出日志",
            enabled = !actionInProgress,
            modifier = Modifier.fillMaxWidth(),
            onClick = exportClick,
        )
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
