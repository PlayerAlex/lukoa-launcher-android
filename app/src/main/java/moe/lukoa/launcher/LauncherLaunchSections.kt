package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun TermuxInstallHelpSection(
    actionsLocked: Boolean,
    onOpenFDroid: () -> Unit,
    onOpenGithub: () -> Unit,
    onRecheck: () -> Unit,
) {
    SectionPanel(title = "先安装 Termux", accentColor = LukoaColors.Amber) {
        Text(
            text = "没检测到 Termux。启动酒馆必须先装它。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        SetupStepLine("1", "点下面下载 Termux")
        SetupStepLine("2", "安装后打开 Termux 一次")
        SetupStepLine("3", "回到启动器点重新检测")
        Text(
            text = "推荐 F-Droid 版；GitHub 是备用下载。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "F-Droid 下载",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onOpenFDroid,
            )
            SecondaryActionButton(
                text = "GitHub 备用",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Info,
                modifier = Modifier.weight(1f),
                onClick = onOpenGithub,
            )
        }
        SecondaryActionButton(
            text = "重新检测 Termux",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Accent,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRecheck,
        )
    }
}

@Composable
internal fun SetupStepLine(number: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = LukoaColors.AmberSoft,
                shape = LukoaCapsuleShape,
                border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.42f)),
            ) {
                Text(
                    text = number,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    color = LukoaColors.Amber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = text,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun TermuxPermissionHelpSection(
    actionsLocked: Boolean,
    commandText: String,
    runCommandPermissionGranted: Boolean,
    externalAppsBlocked: Boolean,
    onRequestPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCopyCommand: () -> Unit,
    onRecheckPermission: () -> Unit,
) {
    SectionPanel(title = "Termux 权限未准备好", accentColor = LukoaColors.Danger) {
        Text(
            text = if (externalAppsBlocked) {
                "Termux 还没允许外部调用。复制下面的命令到 Termux 执行一次。"
            } else {
                "还不能调用 Termux。按顺序做一次就行。"
            },
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        SetupStepLine("1", "点重新请求权限")
        SetupStepLine("2", "复制命令到 Termux 执行")
        SetupStepLine("3", "回到这里重新检测")
        StatusPill(
            text = when {
                externalAppsBlocked -> "Termux 外部调用未开启"
                runCommandPermissionGranted -> "RUN_COMMAND 已允许"
                else -> "RUN_COMMAND 未允许"
            },
            active = runCommandPermissionGranted && !externalAppsBlocked,
            toneColor = if (runCommandPermissionGranted && !externalAppsBlocked) LukoaColors.Accent else LukoaColors.Danger,
            activeBackground = if (runCommandPermissionGranted && !externalAppsBlocked) LukoaColors.AccentSoft else LukoaColors.DangerSoft,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "重新请求权限",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Danger,
                modifier = Modifier.weight(1f),
                onClick = onRequestPermission,
            )
            SecondaryActionButton(
                text = "打开权限设置",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Info,
                modifier = Modifier.weight(1f),
                onClick = onOpenPermissionSettings,
            )
        }
        Text(
            text = "命令只需要执行一次。执行后回启动器点“重新检测”。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Terminal,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Text(
                text = commandText,
                modifier = Modifier.padding(10.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "复制命令",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onCopyCommand,
            )
            SecondaryActionButton(
                text = "重新检测",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onRecheckPermission,
            )
        }
    }
}

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

