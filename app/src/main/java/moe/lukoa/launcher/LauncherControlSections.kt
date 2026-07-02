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

enum class LauncherTab(
    val label: String,
    val shortLabel: String,
) {
    Docs("文档", "文"),
    Version("版本", "版"),
    Launch("启动", "启"),
    Backup("备份", "备"),
    Settings("设置", "设"),
}

private enum class VersionPageView {
    Current,
    Target,
}

private enum class SettingsPageView {
    Path,
    Mirror,
    Permissions,
    Update,
    Diagnostic,
    Wake,
}

@Composable
fun LauncherBottomBar(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
    ) {
        Column {
            HorizontalDivider(color = LukoaColors.Line.copy(alpha = 0.3f))
            NavigationBar(
                containerColor = LukoaColors.Surface,
                contentColor = LukoaColors.Text,
                tonalElevation = 0.dp,
            ) {
                LauncherTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Text(
                                text = tab.shortLabel,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) LukoaColors.Text else LukoaColors.Muted,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LukoaColors.Accent,
                            selectedTextColor = LukoaColors.Text,
                            indicatorColor = LukoaColors.AccentSoft.copy(alpha = 0.6f),
                            unselectedIconColor = LukoaColors.Muted,
                            unselectedTextColor = LukoaColors.Muted,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ExportLogDialog(
    onExportTermux: () -> Unit,
    onExportApp: () -> Unit,
    onExportBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("导出运行日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "导出包含清除后累计内容。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只导出 Termux 调用",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTermux,
                )
                SecondaryActionButton(
                    text = "只导出 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportApp,
                )
                SecondaryActionButton(
                    text = "两个都导出",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun ClearLogScopeDialog(
    onClearTermux: () -> Unit,
    onClearApp: () -> Unit,
    onClearBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("选择清除范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "只清空这里的显示，不删酒馆文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只清除 Termux 调用",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearTermux,
                )
                SecondaryActionButton(
                    text = "只清除 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearApp,
                )
                SecondaryActionButton(
                    text = "两个都清除",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun ClearLogDangerDialog(
    mode: ExportLogMode,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val target = when (mode) {
        ExportLogMode.TermuxOnly -> "Termux 调用返回"
        ExportLogMode.AppOnly -> "App 操作反馈"
        ExportLogMode.Both -> "Termux 调用返回和 App 操作反馈"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认清除日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "将清除：$target。",
                    color = LukoaColors.Text,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "清除后这里看不到旧记录，但不删酒馆文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = onConfirmTextChange,
                    singleLine = true,
                    label = { Text("输入“清除”继续") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Danger,
                    ),
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认清除",
                enabled = confirmText.trim() == "清除",
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton("返回", tone = ActionTone.Danger, onClick = onBack)
                DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
            }
        },
    )
}

@Composable
fun BusyPanel(label: String, startedAtMillis: Long) {
    var nowMillis by remember(label, startedAtMillis) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(label, startedAtMillis) {
        while (true) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val elapsedSeconds = if (startedAtMillis > 0L) {
        ((nowMillis - startedAtMillis).coerceAtLeast(0L) / 1000L).toInt()
    } else {
        0
    }
    val elapsedText = formatBusyElapsed(elapsedSeconds)
    val detail = busyDetailFor(label, elapsedSeconds)
    SectionPanel(title = "正在处理", accentColor = LukoaColors.Amber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = LukoaColors.Text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusPill(
                text = elapsedText,
                active = true,
                toneColor = LukoaColors.Amber,
                activeBackground = LukoaColors.AmberSoft,
            )
        }
        Text(
            text = detail,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "按钮已锁定，别重复点。完成后会显示 Termux 完整返回。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatBusyElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) {
        "%d:%02d".format(minutes, rest)
    } else {
        "${rest}s"
    }
}

private fun busyDetailFor(label: String, seconds: Int): String {
    if (!label.contains("准备 Termux 环境")) {
        return "Termux 正在处理这个操作。"
    }
    return when {
        seconds < 20 -> "已发送命令，正在连接 Termux 包源。"
        seconds < 90 -> "可能正在执行 apt update 或升级基础包。"
        seconds < 240 -> "可能正在安装 git、node、npm，首次安装会比较久。"
        else -> "仍在等待 Termux 回传。只要按钮还锁着，就说明启动器还在等结果。"
    }
}

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
private fun SetupStepLine(number: String, text: String) {
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
    stopConfirmActive: Boolean,
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
        tavernRunning && stopConfirmActive -> "再次点击确认停止"
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
            statusTone = if (tavernRunning && stopConfirmActive) LukoaColors.Danger else LukoaColors.Accent,
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

@Composable
fun VersionManagementSection(
    actionsLocked: Boolean,
    tavernVersionInfo: TavernVersionInfo,
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    rollbackConfirmActive: Boolean,
    updateConfirmActive: Boolean,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onTavernVersion: () -> Unit,
    onTavernUpdate: () -> Unit,
    onTavernRollback: () -> Unit,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    val actionState = TavernVersionActionGuards.evaluate(tavernVersionInfo, selectedVersion)
    val versionManagementChoices = TavernVersionSelection.versionManagementChoices(
        officialVersions = officialVersions,
        current = tavernVersionInfo,
    )
    val updateEnabled = !actionsLocked && actionState.updateAvailable
    val rollbackEnabled = !actionsLocked && actionState.rollbackAvailable
    val disabledReasons = listOfNotNull(
        actionState.updateDisabledReason?.let { "更新：$it" },
        actionState.rollbackDisabledReason?.let { "回退：$it" },
    ).distinct()
    var selectedView by remember {
        mutableStateOf(
            if (tavernVersionInfo.hasData) VersionPageView.Current else VersionPageView.Target,
        )
    }
    val viewOptions = listOf(
        SectionSwitchOption(
            value = VersionPageView.Current,
            label = "当前安装",
            description = "看当前酒馆版本、分支、提交、本地改动和检测目录。",
        ),
        SectionSwitchOption(
            value = VersionPageView.Target,
            label = "目标切换",
            description = "读取官方版本、选择目标版本，再决定更新还是回退。",
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VersionOverviewCard(
            tavernVersionInfo = tavernVersionInfo,
            selectedVersion = selectedVersion,
            officialVersions = officialVersions,
            actionsLocked = actionsLocked,
            onRefreshCurrentVersion = onTavernVersion,
        )
        SectionSwitcherCard(
            title = "版本分区",
            options = viewOptions,
            selected = selectedView,
            onPagerLockChange = onPagerLockChange,
            onSelect = { selectedView = it },
        )

        when (selectedView) {
            VersionPageView.Current -> SectionPanel(title = "当前安装信息", accentColor = LukoaColors.Accent) {
                Text(
                    text = tavernVersionInfo.displayVersion,
                    color = when {
                        tavernVersionInfo.hasData -> LukoaColors.Text
                        tavernVersionInfo.notInstalled -> LukoaColors.Amber
                        else -> LukoaColors.Muted
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (tavernVersionInfo.hasData) {
                    VersionInfoLine("分支", tavernVersionInfo.branch.ifBlank { "未读取" })
                    VersionInfoLine("提交", tavernVersionInfo.commit.ifBlank { "未读取" })
                    VersionInfoLine("Git 描述", tavernVersionInfo.describe.ifBlank { "未读取" })
                    VersionInfoLine("回退点", tavernVersionInfo.rollbackDisplay)
                    VersionInfoLine("目录", tavernVersionInfo.directory.ifBlank { "未读取" })
                    if (tavernVersionInfo.hasLocalChanges) {
                        Text(
                            text = "酒馆源码有改动，先处理后再更新或回退。",
                            color = LukoaColors.Danger,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = tavernVersionInfo.changedFilesPreview,
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (tavernVersionInfo.notInstalled) {
                    Text(
                        text = "没有找到酒馆目录。先读取目标版本，再回启动页安装酒馆。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (tavernVersionInfo.directory.isNotBlank()) {
                        VersionInfoLine("检测目录", tavernVersionInfo.directory)
                    }
                } else {
                    Text(
                        text = "点上面的重新检测酒馆版本，就能读取当前安装信息。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            VersionPageView.Target -> SectionPanel(title = "目标版本与切换", accentColor = LukoaColors.Accent) {
                Text(
                    text = when {
                        tavernVersionInfo.hasData -> "先读取列表，再选目标版本。"
                        tavernVersionInfo.notInstalled -> "酒馆未安装，也可以先读取官方版本给安装使用。"
                        else -> "可以先读取官方版本；更新或回退前再检测当前版本。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )

                OfficialVersionChooser(
                    title = "官方版本下拉框",
                    officialVersions = versionManagementChoices,
                    selectedVersion = selectedVersion,
                    actionsLocked = actionsLocked,
                    refreshEnabled = !actionsLocked,
                    emptyStateText = if (officialVersions.hasData) {
                        "当前版本已经隐藏，暂时没有别的官方版本。"
                    } else {
                        "先读取官方版本"
                    },
                    onRefreshOfficialVersions = onRefreshOfficialVersions,
                    onSelectVersion = onSelectVersion,
                )

                VersionOperationStatusCard(
                    currentVersionInfo = tavernVersionInfo,
                    selectedVersion = selectedVersion,
                    actionState = actionState,
                    disabledReasons = disabledReasons,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryActionButton(
                        text = if (updateConfirmActive) "再次确认" else "更新",
                        enabled = updateEnabled,
                        accentColor = LukoaColors.Amber,
                        modifier = Modifier
                            .weight(1f),
                        onClick = onTavernUpdate,
                    )
                    SecondaryActionButton(
                        text = if (rollbackConfirmActive) "再次确认" else "回退",
                        enabled = rollbackEnabled,
                        accentColor = LukoaColors.Amber,
                        modifier = Modifier
                            .weight(1f),
                        onClick = onTavernRollback,
                    )
                }

                Text(
                    text = if (tavernVersionInfo.notInstalled) {
                        "未安装时不能更新或回退，先安装酒馆。"
                    } else {
                        "只切换程序版本，不删聊天、角色、世界书和插件。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun VersionOverviewCard(
    tavernVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    officialVersions: TavernOfficialVersions,
    actionsLocked: Boolean,
    onRefreshCurrentVersion: () -> Unit,
) {
    val statusText: String
    val statusColor: Color
    when {
        tavernVersionInfo.hasLocalChanges -> {
            statusText = "本地已修改"
            statusColor = LukoaColors.Danger
        }
        tavernVersionInfo.hasData -> {
            statusText = "就绪"
            statusColor = LukoaColors.Accent
        }
        tavernVersionInfo.notInstalled -> {
            statusText = "未安装"
            statusColor = LukoaColors.Amber
        }
        else -> {
            statusText = "检测中"
            statusColor = LukoaColors.Muted
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, LukoaColors.Line.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "版本部署",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(
                    text = statusText,
                    active = statusColor != LukoaColors.Muted,
                    toneColor = statusColor,
                    activeBackground = statusColor.copy(alpha = 0.15f),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.SurfaceAlt.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    VersionStatBlock(
                        label = "当前版本",
                        value = if (tavernVersionInfo.hasData) tavernVersionInfo.displayVersion else "未知",
                        accentColor = if (tavernVersionInfo.hasData) LukoaColors.Text else LukoaColors.Dim,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = LukoaColors.Line,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    VersionStatBlock(
                        label = "目标版本",
                        value = selectedVersion?.label ?: "未选择",
                        accentColor = if (selectedVersion != null) LukoaColors.Accent else LukoaColors.Dim,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            SecondaryActionButton(
                text = "重新检测酒馆版本",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefreshCurrentVersion,
            )
        }
    }
}

@Composable
private fun VersionOperationStatusCard(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    disabledReasons: List<String>,
) {
    val statusText: String
    val statusColor: Color
    when {
        currentVersionInfo.notInstalled -> {
            statusText = "先安装酒馆"
            statusColor = LukoaColors.Amber
        }
        !currentVersionInfo.hasData -> {
            statusText = "先读取当前版本"
            statusColor = LukoaColors.Amber
        }
        currentVersionInfo.hasLocalChanges -> {
            statusText = "源码有本地改动"
            statusColor = LukoaColors.Danger
        }
        selectedVersion == null -> {
            statusText = "先选目标版本"
            statusColor = LukoaColors.Amber
        }
        actionState.updateAvailable -> {
            statusText = "可以更新"
            statusColor = LukoaColors.Accent
        }
        actionState.rollbackAvailable -> {
            statusText = "可以回退"
            statusColor = LukoaColors.Accent
        }
        actionState.relation == TavernTargetRelation.Same -> {
            statusText = "已经是这个版本"
            statusColor = LukoaColors.Muted
        }
        else -> {
            statusText = "暂时不能执行"
            statusColor = LukoaColors.Muted
        }
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "操作状态",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionStatusValueCard(
                    label = "当前版本",
                    value = currentVersionInfo.displayVersion,
                    accentColor = when {
                        currentVersionInfo.hasData -> LukoaColors.Text
                        currentVersionInfo.notInstalled -> LukoaColors.Amber
                        else -> LukoaColors.Muted
                    },
                    modifier = Modifier.weight(1f),
                )
                VersionStatusValueCard(
                    label = "目标版本",
                    value = selectedVersion?.label ?: "未选择",
                    accentColor = if (selectedVersion == null) LukoaColors.Muted else LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                )
            }

            selectedVersion?.let { choice ->
                Text(
                    text = when (choice.kind) {
                        TavernVersionKind.Stable -> "稳定版，适合大多数人。"
                        TavernVersionKind.Test -> "测试版，可能有新功能，也可能不稳定。"
                        TavernVersionKind.Custom -> "自定义目标，请确认版本名、分支名或 commit 没填错。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            TavernVersionActionGuards.relationHint(actionState, selectedVersion)
                ?.takeIf { it.isNotBlank() }
                ?.let { hint ->
                    Text(
                        text = hint,
                        color = if (statusColor == LukoaColors.Accent) LukoaColors.Accent else LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

            if (disabledReasons.isNotEmpty()) {
                HorizontalDivider(color = LukoaColors.Line)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前限制",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    disabledReasons.forEach { reason ->
                        Text(
                            text = reason,
                            color = LukoaColors.Text,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionStatBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun VersionStatusValueCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LukoaColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                color = accentColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VersionSelectionNotice(
    selectedVersion: TavernVersionChoice?,
    relationHint: String?,
) {
    if (selectedVersion == null && relationHint.isNullOrBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            selectedVersion?.let { choice ->
                Text(
                    text = "已选：${choice.label}",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (choice.kind) {
                        TavernVersionKind.Stable -> "稳定版，适合大多数人。"
                        TavernVersionKind.Test -> "测试版，可能有新功能，也可能不稳定。"
                        TavernVersionKind.Custom -> "自定义目标，请确认版本名、分支名或 commit 没填错。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "源：${repoLabelFor(choice.repoUrl.ifBlank { TavernMirrorDefaults.OFFICIAL_REPO })}",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            relationHint?.takeIf { it.isNotBlank() }?.let { hint ->
                Text(
                    text = hint,
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun InstallTavernSection(
    actionsLocked: Boolean,
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onInstallTavern: () -> Unit,
) {
    SectionPanel(title = "安装酒馆", accentColor = LukoaColors.Amber) {
        Text(
            text = "第一次用直接安装 release 分支。想换版本再打开下面的选择。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        SetupStepLine("1", "自动补 git、nodejs")
        SetupStepLine("2", "克隆 release 分支")
        SetupStepLine("3", "执行 npm install")
        Text(
            text = "release 是当前的最新稳定版。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "安装通常会持续 5-10 分钟，这是正常的，等待即可。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )

        OfficialVersionChooser(
            title = "安装版本",
            officialVersions = officialVersions,
            selectedVersion = selectedVersion,
            actionsLocked = actionsLocked,
            onRefreshOfficialVersions = onRefreshOfficialVersions,
            onSelectVersion = onSelectVersion,
        )

        VersionSelectionNotice(
            selectedVersion = selectedVersion ?: TavernInstallDefaults.Release,
            relationHint = null,
        )

        SecondaryActionButton(
            text = "安装 ${selectedVersion?.label ?: TavernInstallDefaults.Release.label}",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Amber,
            modifier = Modifier.fillMaxWidth(),
            onClick = onInstallTavern,
        )
    }
}

@Composable
private fun OfficialVersionChooser(
    title: String,
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    refreshEnabled: Boolean = !actionsLocked,
    refreshDisabledMessage: String? = null,
    emptyStateText: String = "先读取官方版本",
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    val allChoices = officialVersions.all

    if (showCustomDialog) {
        CustomVersionDialog(
            value = customInput,
            onValueChange = { customInput = it },
            onConfirm = {
                val normalized = customInput.trim()
                if (LauncherInputGuards.validateVersionTarget(normalized) == null) {
                    onSelectVersion(
                        TavernVersionChoice(
                            kind = TavernVersionKind.Custom,
                            name = normalized,
                            target = normalized,
                        ),
                    )
                    showCustomDialog = false
                }
            },
            onDismiss = { showCustomDialog = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = selectedVersion?.label ?: "未选择",
                color = if (selectedVersion == null) LukoaColors.Muted else LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !actionsLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.46f)),
                    shape = LukoaCapsuleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = LukoaColors.Accent,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = LukoaColors.Dim,
                    ),
                ) {
                    Text(
                        text = selectedVersion?.label ?: "选择版本",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = LukoaColors.Surface,
                ) {
                    if (allChoices.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(emptyStateText) },
                            enabled = false,
                            onClick = {},
                        )
                    }
                    if (officialVersions.stable.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("稳定版") },
                            enabled = false,
                            onClick = {},
                        )
                        officialVersions.stable.forEach { choice ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = choice.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onSelectVersion(choice)
                                },
                            )
                        }
                    }
                    if (officialVersions.test.isNotEmpty()) {
                        HorizontalDivider(color = LukoaColors.Line)
                        DropdownMenuItem(
                            text = { Text("测试版") },
                            enabled = false,
                            onClick = {},
                        )
                        officialVersions.test.forEach { choice ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = choice.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onSelectVersion(choice)
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = LukoaColors.Line)
                    DropdownMenuItem(
                        text = { Text("自定义版本 / 分支 / commit") },
                        onClick = {
                            expanded = false
                            customInput = selectedVersion?.takeIf { it.kind == TavernVersionKind.Custom }?.target.orEmpty()
                            showCustomDialog = true
                        },
                    )
                }
            }

            SecondaryActionButton(
                text = if (!refreshEnabled && refreshDisabledMessage != null) refreshDisabledMessage else if (officialVersions.hasData) "刷新" else "读取",
                enabled = refreshEnabled,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(0.55f),
                onClick = onRefreshOfficialVersions,
            )
        }

        if (!officialVersions.hasData) {
            Text(
                text = "读取官方版本，也可以自定义输入。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (allChoices.isEmpty()) {
            Text(
                text = "当前同版本已经从列表里隐藏。想切换别的目标，也可以手动输入。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CustomVersionDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = value.trim()
    val validationMessage = LauncherInputGuards.validateVersionTarget(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("自定义酒馆版本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写 tag、分支名或 commit。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text("版本 / 分支 / commit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = LukoaCapsuleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
                    ),
                )
                if (!valid && value.isNotBlank()) {
                    Text(
                        text = validationMessage ?: "版本格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "使用这个版本",
                enabled = valid,
                accentColor = LukoaColors.Amber,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Amber, onClick = onDismiss)
        },
    )
}

@Composable
private fun VersionInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InstallationCheckSection(
    actionsLocked: Boolean,
    checking: Boolean,
    onCheckTavern: () -> Unit,
    onShowInstall: () -> Unit,
) {
    SectionPanel(title = "酒馆安装检测", accentColor = LukoaColors.Amber) {
        Text(
            text = if (checking) "正在检测酒馆。" else "还不知道手机里有没有酒馆。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "老用户先点检测；第一次用就点安装。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = if (checking) "检测中..." else "检测酒馆",
                enabled = !actionsLocked && !checking,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onCheckTavern,
            )
            SecondaryActionButton(
                text = "安装酒馆",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Amber,
                modifier = Modifier.weight(1f),
                onClick = onShowInstall,
            )
        }
    }
}

@Composable
fun ManualBackupConfirmDialog(
    backupName: String,
    onBackupNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val validationMessage = LauncherInputGuards.validateManualBackupName(backupName)
    val nameOk = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = { Text("生成备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "会生成到 Download/lukoa/backups/sd，包含酒馆、聊天、角色、插件、配置和密钥。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "填了名字就按这个名字保存；不填会生成 sd-时间.tar.gz。自动备份会进 zd 文件夹。",
                    color = LukoaColors.Amber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = backupName,
                    onValueChange = onBackupNameChange,
                    singleLine = true,
                    label = { Text("备份名称，可留空") },
                    placeholder = { Text("例如：更新前、插件测试前") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
                    ),
                )
                if (!nameOk) {
                    Text(
                        text = validationMessage ?: "名称格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "开始备份",
                enabled = nameOk,
                accentColor = LukoaColors.Accent,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun AutoBackupSettingsDialog(
    enabled: Boolean,
    intervalMinutes: Int,
    keepCount: Int,
    actionsLocked: Boolean,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onDecreaseIntervalLarge: () -> Unit,
    onIncreaseIntervalLarge: () -> Unit,
    onDecreaseKeep: () -> Unit,
    onIncreaseKeep: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("自动备份设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (enabled) "自动备份已开启。这里调间隔和保留数量。" else "自动备份未开启。回到备份页点开启。",
                    color = if (enabled) LukoaColors.Accent else LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AutoBackupIntervalPanel(
                    intervalMinutes = intervalMinutes,
                    enabled = !actionsLocked,
                    onDecrease = onDecreaseInterval,
                    onIncrease = onIncreaseInterval,
                    onDecreaseLarge = onDecreaseIntervalLarge,
                    onIncreaseLarge = onIncreaseIntervalLarge,
                )
                AutoBackupKeepPanel(
                    keepCount = keepCount,
                    enabled = !actionsLocked,
                    onDecrease = onDecreaseKeep,
                    onIncrease = onIncreaseKeep,
                )
                Text(
                    text = "间隔范围 10 分钟到 12 小时。只清理 Download/lukoa/backups/zd 里最旧的自动备份。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "完成",
                enabled = true,
                accentColor = LukoaColors.Amber,
                onClick = onDismiss,
            )
        },
        dismissButton = null,
    )
}

@Composable
private fun AutoBackupIntervalPanel(
    intervalMinutes: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncreaseLarge: () -> Unit,
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
            Text(
                text = "备份间隔",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "每 ${formatBackupInterval(intervalMinutes)} 一次",
                color = LukoaColors.Text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoBackupAdjustButton("--", enabled, Modifier.weight(1f), onDecreaseLarge)
                AutoBackupAdjustButton("-", enabled, Modifier.weight(1f), onDecrease)
                AutoBackupAdjustButton("+", enabled, Modifier.weight(1f), onIncrease)
                AutoBackupAdjustButton("++", enabled, Modifier.weight(1f), onIncreaseLarge)
            }
            Text(
                text = "- / + 调 10 分钟，-- / ++ 调 1 小时。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AutoBackupKeepPanel(
    keepCount: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "保留数量",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$keepCount 个",
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AutoBackupAdjustButton("-", enabled, Modifier.weight(0.5f), onDecrease)
                AutoBackupAdjustButton("+", enabled, Modifier.weight(0.5f), onIncrease)
            }
            Text(
                text = "超过这个数量后，从最旧的自动备份开始删除。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AutoBackupAdjustButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SecondaryActionButton(
        text = text,
        enabled = enabled,
        accentColor = LukoaColors.Accent,
        modifier = modifier.height(38.dp),
        onClick = onClick,
    )
}

@Composable
fun ApplyBackupPathDialog(
    path: String,
    onPathChange: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = path.trim()
    val validationMessage = LauncherInputGuards.validateBackupArchivePath(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("选择要应用的备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "建议在备份列表里点“应用”。这里也可以手动填路径。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("备份文件完整路径") },
                    placeholder = { Text("/storage/emulated/0/Download/lukoa/backups/sd/xxx.tar.gz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Danger,
                    ),
                )
                if (!valid && path.isNotBlank()) {
                    Text(
                        text = validationMessage ?: "路径格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "下一步",
                enabled = valid,
                tone = ActionTone.Danger,
                onClick = onNext,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}

@Composable
fun ApplyBackupPreviewDialog(
    archivePath: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认应用备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "会把选中的备份直接恢复到酒馆目录，并覆盖当前酒馆数据。",
                    color = LukoaColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "启动器不会自动复制一份当前酒馆。需要保留当前数据时，请先手动备份。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "应用前请确认酒馆已经停止。若 Termux 没有存储权限，启动器会提示你授权。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = archivePath,
                        modifier = Modifier.padding(10.dp),
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认应用",
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}

@Composable
fun TermuxStoragePermissionDialog(
    archivePath: String,
    actionsLocked: Boolean,
    onGrantPermission: () -> Unit,
    onRetryApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("需要 Termux 存储权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Termux 现在读不到 Downloads 里的备份。请先授权，否则不能应用备份。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开 Termux。看到权限弹窗时点允许，再回启动器继续。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (archivePath.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LukoaColors.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = archivePath,
                            modifier = Modifier.padding(10.dp),
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton(
                    text = "去授权",
                    enabled = !actionsLocked,
                    tone = ActionTone.Warning,
                    onClick = onGrantPermission,
                )
                DialogActionButton(
                    text = "我已授权，继续应用",
                    enabled = !actionsLocked && archivePath.isNotBlank(),
                    tone = ActionTone.Safe,
                    onClick = onRetryApply,
                )
            }
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}

@Composable
fun BackgroundRunPermissionDialog(
    granted: Boolean,
    onOpenPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text(if (granted) "后台运行已允许" else "需要后台运行权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (granted) {
                        "系统已经放行后台运行。若自动备份还是卡住，可以再进一次权限页检查省电策略。"
                    } else {
                        "自动备份想在你离开软件后也准时运行，需要把露科亚启动器加入后台运行白名单。"
                    },
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开系统页面。部分手机还要额外允许后台运行、自启动或取消省电限制。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = if (granted) "重新打开权限页" else "去授权",
                tone = ActionTone.Warning,
                onClick = onOpenPermission,
            )
        },
        dismissButton = {
            DialogActionButton("稍后", tone = ActionTone.Safe, onClick = onDismiss)
        },
    )
}

@Composable
fun DeleteBackupConfirmDialog(
    archivePath: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认删除备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "删除后不能从这里恢复，但不会删除酒馆本体。",
                    color = LukoaColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = archivePath,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认删除",
                enabled = true,
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}

@Composable
fun ImportBackupDialog(
    path: String,
    onPathChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = path.trim()
    val validationMessage = LauncherInputGuards.validateBackupArchivePath(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("导入备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写 .tar.gz 备份路径。导入前会先检查。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("备份文件完整路径") },
                    placeholder = { Text("\$HOME/storage/downloads/xxx.tar.gz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
                    ),
                )
                if (!valid && path.isNotBlank()) {
                    Text(
                        text = validationMessage ?: "路径格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "导入",
                enabled = valid,
                tone = ActionTone.Warning,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}

@Composable
fun SettingsSection(
    termuxReturnDelayMs: Long,
    termuxInstalled: Boolean,
    runCommandPermissionGranted: Boolean,
    backgroundRunPermissionGranted: Boolean,
    termuxExternalAppsBlocked: Boolean,
    termuxStoragePermissionBlocked: Boolean,
    allFilesAccessGranted: Boolean,
    installUnknownAppsGranted: Boolean,
    tavernMirrorConfig: TavernMirrorConfig,
    tavernPathConfig: TavernPathConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    tavernPathInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    repositoryInput: String,
    githubUpdateState: GithubUpdateUiState,
    actionsLocked: Boolean,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onTavernPathInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernPath: () -> Unit,
    onRestoreDefaultTavernPath: () -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onRequestBackgroundRunPermission: () -> Unit,
    onRequestRunCommandPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCopyExternalAppsCommand: () -> Unit,
    onOpenTermuxOnly: () -> Unit,
    onOpenAllFilesAccessSettings: () -> Unit,
    onOpenUnknownAppSourcesSettings: () -> Unit,
    onShowTermuxStoragePermissionGuide: () -> Unit,
    onRepositoryInputChange: (String) -> Unit,
    onSaveRepository: () -> Unit,
    onRestoreDefaultRepository: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    val tavernPathError = TavernPathValidator.validate(tavernPathInput.trim())
    val termuxExternalAppsReady = termuxInstalled && !termuxExternalAppsBlocked
    val permissionReadyCount = listOf(
        runCommandPermissionGranted,
        termuxExternalAppsReady,
        backgroundRunPermissionGranted,
        allFilesAccessGranted,
        installUnknownAppsGranted,
    ).count { it }
    val permissionSummaryText = "$permissionReadyCount/5 已就绪"
    val permissionSummaryColor = if (permissionReadyCount >= 4) LukoaColors.Accent else LukoaColors.Amber
    val pathIsDefault = tavernPathConfig.normalizedTavernDir == TavernPathDefaults.DEFAULT_TAVERN_DIR_NORMALIZED
    var showPathSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionCenterDialog by remember { mutableStateOf(false) }
    var showUpdateSettingsDialog by remember { mutableStateOf(false) }
    var showWakeDelayDialog by remember { mutableStateOf(false) }
    var selectedView by remember {
        mutableStateOf(
            if (permissionReadyCount >= 5) SettingsPageView.Mirror else SettingsPageView.Permissions,
        )
    }
    val sectionOptions = listOf(
        SectionSwitchOption(
            value = SettingsPageView.Path,
            label = "路径",
            description = "管理酒馆目录路径，适合文件夹名不是默认 SillyTavern 的情况。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Mirror,
            label = "网络",
            description = "切 GitHub 源、npm 源和 Termux 包源，主要处理国内网络和镜像问题。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Permissions,
            label = "权限",
            description = "集中看 RUN_COMMAND、外部调用、后台运行、文件和安装权限。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Update,
            label = "更新",
            description = "这里只管启动器 APK 的 GitHub 更新，不是酒馆版本更新。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Diagnostic,
            label = "诊断",
            description = "导出诊断日志，或者清启动器里的显示日志，方便查 bug。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Wake,
            label = "唤醒",
            description = "调整唤醒 Termux 后多久自动切回来。",
        ),
    )

    if (showPathSettingsDialog) {
        TavernPathSettingsDialog(
            tavernPathInput = tavernPathInput,
            tavernPathError = tavernPathError,
            displayPathPreview = TavernPathNormalizer.toDisplayPath(
                TavernPathNormalizer.normalize(tavernPathInput),
            ),
            actionsLocked = actionsLocked,
            onValueChange = onTavernPathInputChange,
            onSave = {
                onSaveTavernPath()
                if (tavernPathError == null) {
                    showPathSettingsDialog = false
                }
            },
            onRestoreDefault = onRestoreDefaultTavernPath,
            onDismiss = { showPathSettingsDialog = false },
        )
    }

    if (showPermissionCenterDialog) {
        PermissionCenterDialog(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsReady = termuxExternalAppsReady,
            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
            onRequestRunCommandPermission = onRequestRunCommandPermission,
            onOpenPermissionSettings = onOpenPermissionSettings,
            onCopyExternalAppsCommand = onCopyExternalAppsCommand,
            onOpenTermuxOnly = onOpenTermuxOnly,
            onRequestBackgroundRunPermission = onRequestBackgroundRunPermission,
            onOpenAllFilesAccessSettings = onOpenAllFilesAccessSettings,
            onOpenUnknownAppSourcesSettings = onOpenUnknownAppSourcesSettings,
            onShowTermuxStoragePermissionGuide = onShowTermuxStoragePermissionGuide,
            onDismiss = { showPermissionCenterDialog = false },
        )
    }

    if (showUpdateSettingsDialog) {
        LauncherUpdateSettingsDialog(
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            actionsLocked = actionsLocked,
            onRepositoryInputChange = onRepositoryInputChange,
            onSaveRepository = onSaveRepository,
            onRestoreDefaultRepository = onRestoreDefaultRepository,
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenRelease = onOpenRelease,
            onDismiss = { showUpdateSettingsDialog = false },
        )
    }

    if (showWakeDelayDialog) {
        TermuxWakeDelayDialog(
            termuxReturnDelayMs = termuxReturnDelayMs,
            actionsLocked = actionsLocked,
            onDecrease = onDecreaseTermuxReturnDelay,
            onIncrease = onIncreaseTermuxReturnDelay,
            onDismiss = { showWakeDelayDialog = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsOverviewCard(
            tavernPathConfig = tavernPathConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            permissionSummaryText = permissionSummaryText,
            permissionSummaryColor = permissionSummaryColor,
            githubUpdateState = githubUpdateState,
        )
        SectionSwitcherCard(
            title = "设置分区",
            options = sectionOptions,
            selected = selectedView,
            onPagerLockChange = onPagerLockChange,
            onSelect = { selectedView = it },
        )

        when (selectedView) {
            SettingsPageView.Path -> SectionPanel(title = "酒馆路径", accentColor = LukoaColors.Accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (pathIsDefault) "默认路径" else "已自定义",
                        active = !pathIsDefault,
                        modifier = Modifier.weight(1f),
                        toneColor = if (pathIsDefault) LukoaColors.Muted else LukoaColors.Accent,
                        activeBackground = LukoaColors.AccentSoft,
                    )
                    StatusPill(
                        text = if (actionsLocked) "当前忙碌中" else "可调整",
                        active = !actionsLocked,
                        modifier = Modifier.weight(1f),
                        toneColor = if (actionsLocked) LukoaColors.Amber else LukoaColors.Accent,
                        activeBackground = if (actionsLocked) LukoaColors.AmberSoft else LukoaColors.AccentSoft,
                    )
                }
                Text(
                    text = tavernPathConfig.displayTavernDir,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "默认目录是 ~/SillyTavern。只有你改过酒馆文件夹名，或者酒馆不在默认目录时，才需要改这里。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (tavernPathInput.isNotBlank() && tavernPathError != null) {
                    Text(
                        text = tavernPathError,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SecondaryActionButton(
                    text = "管理路径",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPathSettingsDialog = true },
                )
            }

            SettingsPageView.Mirror -> MirrorSettingsSection(
                tavernMirrorConfig = tavernMirrorConfig,
                tavernRepoInput = tavernRepoInput,
                npmRegistryInput = npmRegistryInput,
                mirrorProbeStatus = mirrorProbeStatus,
                termuxRepoStatus = termuxRepoStatus,
                customTermuxRepoInput = customTermuxRepoInput,
                actionsLocked = actionsLocked,
                onTavernRepoInputChange = onTavernRepoInputChange,
                onNpmRegistryInputChange = onNpmRegistryInputChange,
                onCustomTermuxRepoInputChange = onCustomTermuxRepoInputChange,
                onSaveTavernMirror = onSaveTavernMirror,
                onUseOfficialMirror = onUseOfficialMirror,
                onUseGithubProxyMirror = onUseGithubProxyMirror,
                onUseNpmMirror = onUseNpmMirror,
                onCheckTavernMirror = onCheckTavernMirror,
                onReadTermuxRepoStatus = onReadTermuxRepoStatus,
                onApplyCustomTermuxMirror = onApplyCustomTermuxMirror,
            )

            SettingsPageView.Permissions -> SectionPanel(title = "权限与授权", accentColor = LukoaColors.Accent) {
                Text(
                    text = "把启动器会用到的权限和授权都集中在这里。看不准时，先把没准备好的项目逐个补齐。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (runCommandPermissionGranted) "RUN_COMMAND 已允许" else "RUN_COMMAND 未允许",
                        active = runCommandPermissionGranted,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = if (termuxExternalAppsReady) "外部调用已开启" else "外部调用未开启",
                        active = termuxExternalAppsReady,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (backgroundRunPermissionGranted) "后台运行已允许" else "后台运行未允许",
                        active = backgroundRunPermissionGranted,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = if (allFilesAccessGranted) "文件权限已允许" else "文件权限未允许",
                        active = allFilesAccessGranted,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (installUnknownAppsGranted) "安装权限已允许" else "安装权限未允许",
                        active = installUnknownAppsGranted,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = if (termuxStoragePermissionBlocked) "Termux 存储待处理" else "Termux 存储按需申请",
                        active = !termuxStoragePermissionBlocked,
                        modifier = Modifier.weight(1f),
                        toneColor = if (termuxStoragePermissionBlocked) LukoaColors.Amber else LukoaColors.Muted,
                        activeBackground = if (termuxStoragePermissionBlocked) LukoaColors.AmberSoft else LukoaColors.SurfaceAlt,
                    )
                }
                SecondaryActionButton(
                    text = "查看权限详情",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPermissionCenterDialog = true },
                )
            }

            SettingsPageView.Update -> SectionPanel(title = "应用更新", accentColor = LukoaColors.Accent) {
                Text(
                    text = "这里管理的是启动器更新，不是酒馆版本更新。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                GithubUpdateStatusCard(githubUpdateState)
                VersionInfoLine("当前仓库", repositoryInput.ifBlank { "未配置" })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryActionButton(
                        text = "管理更新设置",
                        enabled = true,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = { showUpdateSettingsDialog = true },
                    )
                    SecondaryActionButton(
                        text = when {
                            githubUpdateState.checking -> "检查中..."
                            githubUpdateState.downloading -> "下载中..."
                            else -> "检查更新"
                        },
                        enabled = !githubUpdateState.checking && !githubUpdateState.downloading,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = onCheckUpdate,
                    )
                }
                if (githubUpdateState.hasUpdate || githubUpdateState.latest?.releaseUrl?.isNotBlank() == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (githubUpdateState.hasUpdate) {
                            SecondaryActionButton(
                                text = "查看新版",
                                enabled = !updateLocked,
                                accentColor = LukoaColors.Accent,
                                modifier = Modifier.weight(1f),
                                onClick = onInstallUpdate,
                            )
                        }
                        if (githubUpdateState.latest?.releaseUrl?.isNotBlank() == true) {
                            SecondaryActionButton(
                                text = "打开发布页",
                                enabled = !updateLocked,
                                accentColor = LukoaColors.Accent,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenRelease,
                            )
                        }
                    }
                }
            }

            SettingsPageView.Diagnostic -> SectionPanel(title = "诊断与日志", accentColor = LukoaColors.Accent) {
                Text(
                    text = "诊断日志适合发给我查 bug。清除日志只会清启动器里的显示，不会去删你酒馆目录里的文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "清除日志",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearLogs,
                )
                SecondaryActionButton(
                    text = "导出诊断日志",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportDiagnostic,
                )
            }

            SettingsPageView.Wake -> SectionPanel(title = "Termux 唤醒", accentColor = LukoaColors.Accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = "${"%.1f".format(termuxReturnDelayMs / 1000f)} 秒返回",
                        active = true,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = if (actionsLocked) "当前忙碌中" else "可调整",
                        active = !actionsLocked,
                        modifier = Modifier.weight(1f),
                        toneColor = if (actionsLocked) LukoaColors.Amber else LukoaColors.Accent,
                        activeBackground = if (actionsLocked) LukoaColors.AmberSoft else LukoaColors.AccentSoft,
                    )
                }
                Text(
                    text = "这里只管唤醒 Termux 后多久自动跳回来。时间太短时，有些手机可能还没来得及唤醒完成。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "调整返回时间",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showWakeDelayDialog = true },
                )
            }
        }
    }
}

@Composable
private fun SettingsStatBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsOverviewCard(
    tavernPathConfig: TavernPathConfig,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    permissionSummaryText: String,
    permissionSummaryColor: Color,
    githubUpdateState: GithubUpdateUiState,
) {
    val updateStatusText = when {
        githubUpdateState.downloading -> "下载中"
        githubUpdateState.checking -> "检查中"
        githubUpdateState.hasUpdate -> "有新版本"
        githubUpdateState.latest != null -> "已是最新"
        else -> "未检查"
    }
    val updateStatusColor = when {
        githubUpdateState.downloading -> LukoaColors.Accent
        githubUpdateState.checking -> LukoaColors.Amber
        githubUpdateState.hasUpdate -> LukoaColors.Accent
        githubUpdateState.latest != null -> LukoaColors.Muted
        else -> LukoaColors.Muted
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, LukoaColors.Line.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "系统与状态概览",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsStatBlock(
                    label = "酒馆目录",
                    value = tavernPathConfig.displayTavernDir,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                )
                SettingsStatBlock(
                    label = "镜像状态",
                    value = mirrorProbeStatus.overallLevel.label(),
                    accentColor = mirrorProbeStatus.overallLevel.toneColor(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsStatBlock(
                    label = "权限状态",
                    value = permissionSummaryText,
                    accentColor = permissionSummaryColor,
                    modifier = Modifier.weight(1f),
                )
                SettingsStatBlock(
                    label = "启动器更新",
                    value = updateStatusText,
                    accentColor = updateStatusColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(
    updateInfo: GithubUpdateInfo,
    currentVersionName: String,
    downloading: Boolean,
    onInstall: () -> Unit,
    onOpenRelease: () -> Unit,
    onClearBadge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val publishedText = remember(updateInfo.publishedAt) {
        formatGithubPublishedTime(updateInfo.publishedAt)
    }
    val primaryActionText = when {
        downloading -> "下载中..."
        updateInfo.apkDownloadUrl.isBlank() -> "打开发布页"
        else -> "立即更新"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("发现新版本 v${updateInfo.versionName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VersionStatusValueCard(
                                label = "当前版本",
                                value = "v$currentVersionName",
                                accentColor = LukoaColors.Muted,
                                modifier = Modifier.weight(1f),
                            )
                            VersionStatusValueCard(
                                label = "新版本",
                                value = "v${updateInfo.versionName}",
                                accentColor = LukoaColors.Accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (publishedText.isNotBlank()) {
                            VersionInfoLine("发布时间", publishedText)
                        }
                        if (updateInfo.releaseName.isNotBlank() && updateInfo.releaseName != updateInfo.tagName) {
                            VersionInfoLine("版本标题", updateInfo.releaseName)
                        }
                    }
                }

                Text(
                    text = "更新内容",
                    color = LukoaColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = updateInfo.body.ifBlank { "这个版本没有填写更新说明。" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp, max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = "清除红点后，这个版本不会再自动弹出提醒，但你之后仍然可以手动点右上角版本查看。",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ToneActionButton(
                    text = primaryActionText,
                    enabled = !downloading,
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onInstall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToneActionButton(
                        text = "详情",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelease,
                    )
                    ToneActionButton(
                        text = "清除红点",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onClearBadge,
                    )
                }
                ToneActionButton(
                    text = "稍后",
                    enabled = true,
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
private fun GithubUpdateStatusCard(
    githubUpdateState: GithubUpdateUiState,
) {
    val statusText: String
    val statusColor: Color
    when {
        githubUpdateState.downloading -> {
            statusText = "正在下载"
            statusColor = LukoaColors.Accent
        }
        githubUpdateState.checking -> {
            statusText = "正在检查"
            statusColor = LukoaColors.Amber
        }
        githubUpdateState.hasUpdate -> {
            statusText = "发现新版本"
            statusColor = LukoaColors.Accent
        }
        githubUpdateState.latest != null -> {
            statusText = "已是最新"
            statusColor = LukoaColors.Muted
        }
        githubUpdateState.repository.isBlank() -> {
            statusText = "未配置仓库"
            statusColor = LukoaColors.Amber
        }
        else -> {
            statusText = "等待检查"
            statusColor = LukoaColors.Muted
        }
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当前状态",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = githubUpdateState.message,
                color = if (githubUpdateState.hasUpdate) LukoaColors.Text else LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            githubUpdateState.latest?.let { latest ->
                VersionInfoLine("GitHub 最新", "v${latest.versionName}")
            }
            githubUpdateState.lastCheckedText
                .takeIf { it.isNotBlank() }
                ?.let { checkedText ->
                    VersionInfoLine("上次检查", checkedText)
                }
        }
    }
}

private val GITHUB_UPDATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatGithubPublishedTime(text: String): String {
    if (text.isBlank()) return ""
    return runCatching {
        GITHUB_UPDATE_TIME_FORMATTER.format(
            Instant.parse(text).atZone(ZoneId.systemDefault()),
        )
    }.getOrElse {
        text.replace("T", " ").removeSuffix("Z").take(16)
    }
}

@Composable
fun InstallRiskConfirmDialog(
    confirmation: TavernInstallConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text(confirmation.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = confirmation.summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        confirmation.details.forEach { item ->
                            Text(
                                text = "• $item",
                                color = LukoaColors.Text,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "继续安装",
                tone = ActionTone.Safe,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun TavernDirectoryChoiceDialog(
    currentPath: String,
    candidates: List<String>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("选一个酒馆目录")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "检测到多个酒馆目录。点一个，启动器会自动切过去。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "当前路径：$currentPath",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        candidates.forEach { candidate ->
                            OutlinedButton(
                                onClick = { onChoose(candidate) },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.46f)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = LukoaColors.Accent.copy(alpha = 0.08f),
                                    contentColor = LukoaColors.Accent,
                                ),
                            ) {
                                Text(
                                    text = candidate,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = LukoaColors.Text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}
