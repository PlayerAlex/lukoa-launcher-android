package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class ToolboxDialogDestination {
    HealthCheck,
    RepairTools,
    Debug,
    ChatGuard,
    StorageCleanup,
    GitStash,
}

@Composable
fun ToolboxSection(
    healthCheckReport: LauncherHealthReport? = null,
    healthCheckInFlight: Boolean = false,
    actionsLocked: Boolean = false,
    tavernRunning: Boolean = false,
    instanceLabel: String = "主实例",
    uploadLimitStatus: TavernUploadLimitStatus = TavernUploadLimitStatus(),
    chatGuardState: TavernChatGuardState = TavernChatGuardState(),
    storageState: TavernStorageState = TavernStorageState(),
    gitStashState: TavernGitStashState = TavernGitStashState(),
    forceCleanupSuggestion: TavernForceCleanupSuggestion? = null,
    backgroundTaskStatus: String = "当前空闲",
    backgroundTaskNeedsAttention: Boolean = false,
    repairToolsOpenSignal: Int = 0,
    onRepairToolsOpenSignalConsumed: () -> Unit = {},
    chatGuardOpenSignal: Int = 0,
    onChatGuardOpenSignalConsumed: () -> Unit = {},
    onRunHealthCheck: () -> Unit = {},
    onRunHealthCheckPrimaryAction: () -> Unit = {},
    onRepairDependencies: () -> Unit = {},
    onResetTavernTheme: () -> Unit = {},
    onSetNodeMemory: (Int) -> Unit = {},
    onCheckUploadLimit: () -> Unit = {},
    onSetUploadLimit: (Int) -> Unit = {},
    onResetUploadLimit: () -> Unit = {},
    onRefreshChatGuard: () -> Unit = {},
    onSaveChatGuardConfig: (TavernChatGuardConfig) -> Unit = {},
    onClearChatGuardHistory: () -> Unit = {},
    onScanStorage: () -> Unit = {},
    onCleanStorage: (Set<TavernStorageCleanupCategory>) -> Unit = {},
    onRefreshGitStash: () -> Unit = {},
    onApplyGitStash: (Int) -> Unit = {},
    onDropGitStash: (Int) -> Unit = {},
    onCopyText: (String, String) -> Boolean = { _, _ -> false },
    onForceCleanup: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    onExportDiagnostic: () -> Unit = {},
    onOpenBackgroundTaskCenter: () -> Unit = {},
) {
    var activeDialog by rememberSaveable { mutableStateOf<ToolboxDialogDestination?>(null) }
    val showHint = rememberTransientHint()

    LaunchedEffect(repairToolsOpenSignal) {
        if (repairToolsOpenSignal > 0) {
            activeDialog = ToolboxDialogDestination.RepairTools
            onRepairToolsOpenSignalConsumed()
        }
    }

    LaunchedEffect(chatGuardOpenSignal) {
        if (chatGuardOpenSignal > 0) {
            activeDialog = ToolboxDialogDestination.ChatGuard
            onChatGuardOpenSignalConsumed()
            if (chatGuardState.snapshot == null && !actionsLocked && !chatGuardState.loading) {
                onRefreshChatGuard()
            }
        }
    }

    when (activeDialog) {
        ToolboxDialogDestination.HealthCheck -> ToolboxContentDialog(
            title = "一键体检",
            onDismiss = { activeDialog = null },
        ) {
            HealthCheckContent(
                report = healthCheckReport,
                checking = healthCheckInFlight,
                actionsLocked = actionsLocked,
                onRunHealthCheck = onRunHealthCheck,
                onPrimaryAction = onRunHealthCheckPrimaryAction,
                showRunHealthCheckAction = false,
            )
        }

        ToolboxDialogDestination.RepairTools -> ToolboxContentDialog(
            title = "修复工具",
            onDismiss = { activeDialog = null },
        ) {
            RepairToolsSection(
                actionsLocked = actionsLocked,
                tavernRunning = tavernRunning,
                uploadLimitStatus = uploadLimitStatus,
                onRepairDependencies = onRepairDependencies,
                onResetTheme = onResetTavernTheme,
                onSetNodeMemory = onSetNodeMemory,
                onCheckUploadLimit = onCheckUploadLimit,
                onSetUploadLimit = onSetUploadLimit,
                onResetUploadLimit = onResetUploadLimit,
                onShowHint = showHint,
                showSectionContainer = false,
            )
        }

        ToolboxDialogDestination.Debug -> ToolboxContentDialog(
            title = "Debug 区",
            onDismiss = { activeDialog = null },
        ) {
            ToolboxDebugContent(
                actionsLocked = actionsLocked,
                forceCleanupSuggestion = forceCleanupSuggestion,
                onForceCleanup = onForceCleanup,
                onClearLogs = onClearLogs,
                onExportDiagnostic = onExportDiagnostic,
                onShowHint = showHint,
            )
        }

        ToolboxDialogDestination.ChatGuard -> ToolboxContentDialog(
            title = "聊天守护",
            onDismiss = { activeDialog = null },
        ) {
            TavernChatGuardContent(
                state = chatGuardState,
                instanceLabel = instanceLabel,
                actionsLocked = actionsLocked,
                tavernRunning = tavernRunning,
                onRefresh = onRefreshChatGuard,
                onSaveConfig = onSaveChatGuardConfig,
                onClearHistory = onClearChatGuardHistory,
                onCopyPath = { path -> onCopyText("救回聊天路径", path) },
                onShowHint = showHint,
            )
        }

        ToolboxDialogDestination.StorageCleanup -> ToolboxContentDialog(
            title = "空间清理",
            onDismiss = { activeDialog = null },
        ) {
            TavernStorageContent(
                state = storageState,
                instanceLabel = instanceLabel,
                actionsLocked = actionsLocked,
                onScan = onScanStorage,
                onClean = onCleanStorage,
                onShowHint = showHint,
            )
        }

        ToolboxDialogDestination.GitStash -> ToolboxContentDialog(
            title = "暂存管理",
            onDismiss = { activeDialog = null },
        ) {
            TavernGitStashContent(
                state = gitStashState,
                instanceLabel = instanceLabel,
                actionsLocked = actionsLocked,
                tavernRunning = tavernRunning,
                onRefresh = onRefreshGitStash,
                onApply = onApplyGitStash,
                onDrop = onDropGitStash,
                onShowHint = showHint,
            )
        }

        null -> Unit
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp)
            .testTag("toolbox-section"),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column {
            Text(
                text = "工具箱",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .semantics { heading() },
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            HorizontalDivider(color = LukoaColors.Border)
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ToolboxHealthCard(
                    report = healthCheckReport,
                    checking = healthCheckInFlight,
                    actionsLocked = actionsLocked,
                    onRunHealthCheck = onRunHealthCheck,
                    onViewDetails = { activeDialog = ToolboxDialogDestination.HealthCheck },
                )
                ToolboxGrid(
                    backgroundTaskStatus = backgroundTaskStatus,
                    backgroundTaskNeedsAttention = backgroundTaskNeedsAttention,
                    chatGuardState = chatGuardState,
                    storageState = storageState,
                    gitStashState = gitStashState,
                    onOpenRepairTools = { activeDialog = ToolboxDialogDestination.RepairTools },
                    onOpenDebug = { activeDialog = ToolboxDialogDestination.Debug },
                    onOpenTaskCenter = onOpenBackgroundTaskCenter,
                    onOpenChatGuard = {
                        activeDialog = ToolboxDialogDestination.ChatGuard
                        if (chatGuardState.snapshot == null && !actionsLocked && !chatGuardState.loading) {
                            onRefreshChatGuard()
                        }
                    },
                    onOpenStorageCleanup = {
                        activeDialog = ToolboxDialogDestination.StorageCleanup
                        if (storageState.snapshot == null && !actionsLocked && !storageState.loading) {
                            onScanStorage()
                        }
                    },
                    onOpenGitStash = {
                        activeDialog = ToolboxDialogDestination.GitStash
                        if (gitStashState.gitAvailable == null && !actionsLocked && !gitStashState.loading) {
                            onRefreshGitStash()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolboxHealthCard(
    report: LauncherHealthReport?,
    checking: Boolean,
    actionsLocked: Boolean,
    onRunHealthCheck: () -> Unit,
    onViewDetails: () -> Unit,
) {
    val effectiveReport = report?.takeIf { it.hasData }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("toolbox-health-card"),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "一键体检工具",
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "当前状态：",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = toolboxHealthStatusText(effectiveReport, checking),
                            modifier = Modifier.testTag("toolbox-health-status-plain"),
                            color = toolboxHealthStatusTone(effectiveReport, checking),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "上次体检时间：${toolboxHealthCheckedAt(effectiveReport)}",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(
                    modifier = Modifier.width(112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryActionButton(
                        text = if (checking) "体检中..." else "一键体检",
                        enabled = !actionsLocked && !checking,
                        accentColor = LukoaColors.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRunHealthCheck,
                    )
                    SecondaryActionButton(
                        text = "查看详情",
                        enabled = true,
                        accentColor = LukoaColors.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onViewDetails,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolboxGrid(
    backgroundTaskStatus: String,
    backgroundTaskNeedsAttention: Boolean,
    chatGuardState: TavernChatGuardState,
    storageState: TavernStorageState,
    gitStashState: TavernGitStashState,
    onOpenRepairTools: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenTaskCenter: () -> Unit,
    onOpenChatGuard: () -> Unit,
    onOpenStorageCleanup: () -> Unit,
    onOpenGitStash: () -> Unit,
) {
    val chatGuardDescription = when {
        chatGuardState.effectiveRescueCount > 0 -> "已救回 ${chatGuardState.effectiveRescueCount} 条聊天"
        chatGuardState.snapshot?.running == true -> "后台守护中"
        else -> "防写坏救回与防休眠"
    }
    val storageDescription = storageState.snapshot?.let {
        "可清约 ${formatStorageKilobytes(it.totalCleanableKb)}"
    } ?: "空间体检与安全清理"
    val gitStashDescription = when {
        gitStashState.entries.isNotEmpty() -> "${gitStashState.entries.size} 条暂存改动"
        gitStashState.gitAvailable == true -> "暂无暂存改动"
        else -> "恢复更新暂存改动"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column {
            HubGridRow(
                left = HubGridEntry("修复工具", "处理常见环境问题", onOpenRepairTools),
                right = HubGridEntry("Debug 区", "诊断、日志与强制清理", onOpenDebug),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry(
                    label = "任务中心",
                    description = backgroundTaskStatus,
                    onClick = onOpenTaskCenter,
                    emphasized = backgroundTaskNeedsAttention,
                ),
                right = HubGridEntry(
                    label = "聊天守护",
                    description = chatGuardDescription,
                    onClick = onOpenChatGuard,
                    emphasized = chatGuardState.effectiveRescueCount > 0,
                ),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("空间清理", storageDescription, onOpenStorageCleanup),
                right = HubGridEntry("暂存管理", gitStashDescription, onOpenGitStash),
            )
        }
    }
}

@Composable
private fun ToolboxContentDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "关闭",
                enabled = true,
                accentColor = LukoaColors.Primary,
                onClick = onDismiss,
            )
        },
        dismissButton = null,
    )
}

@Composable
private fun ToolboxDebugContent(
    actionsLocked: Boolean,
    forceCleanupSuggestion: TavernForceCleanupSuggestion?,
    onForceCleanup: () -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    ManagementDialogIntroCard(
        "这里集中放诊断记录和最后手段。遇到问题时先导出诊断日志；只有普通停止无效或确认存在端口冲突时，才使用强制处理。",
    )
    ManagementDialogActionCard(title = "诊断与日志") {
        Text(
            text = "导出内容适合用于排查问题；清除只会整理启动器页面里的日志，不会删除酒馆聊天或备份。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        SettingsFeedbackActionButton(
            text = "导出诊断日志",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onClick = onExportDiagnostic,
        )
        SettingsFeedbackActionButton(
            text = "清除页面日志",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onClick = onClearLogs,
        )
    }
    ManagementDialogActionCard(title = "强制处理") {
        Text(
            text = TavernForceCleanupButtonUi.hintFor(forceCleanupSuggestion),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        TimedDangerFeedbackActionButton(
            text = TavernForceCleanupButtonUi.labelFor(forceCleanupSuggestion),
            enabled = !actionsLocked,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onConfirmed = onForceCleanup,
        )
    }
}

private fun toolboxHealthStatusText(report: LauncherHealthReport?, checking: Boolean): String {
    return when {
        checking -> "体检中"
        report == null -> "未体检"
        report.errorCount > 0 -> "${report.errorCount} 个问题"
        report.warningCount > 0 -> "${report.warningCount} 个提醒"
        report.unknownCount > 0 -> "${report.unknownCount} 项未确认"
        else -> "基本正常"
    }
}

private fun toolboxHealthStatusTone(report: LauncherHealthReport?, checking: Boolean): Color {
    return when {
        checking -> LukoaColors.Primary
        report == null -> LukoaColors.TextSecondary
        report.errorCount > 0 -> LukoaColors.Danger
        report.warningCount > 0 -> LukoaColors.Accent
        report.unknownCount > 0 -> LukoaColors.TextSecondary
        else -> LukoaColors.Primary
    }
}

private fun toolboxHealthCheckedAt(report: LauncherHealthReport?): String {
    val timeMillis = report?.checkedAtMillis ?: return "暂无"
    if (timeMillis <= 0L) return "暂无"
    return TOOLBOX_HEALTH_TIME_FORMATTER.format(
        Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()),
    )
}

private val TOOLBOX_HEALTH_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm")
