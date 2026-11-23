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
    nodeMemoryStatus: TavernNodeMemoryStatus = TavernNodeMemoryStatus(),
    issues: List<TavernIssue> = emptyList(),
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
    onQuickFixAction: (LauncherQuickFixAction) -> Unit = {},
    onRepairDependencies: () -> Unit = {},
    onResetTavernTheme: () -> Unit = {},
    onCheckNodeMemory: () -> Unit = {},
    onSetNodeMemory: (Int) -> Unit = {},
    onResetNodeMemory: () -> Unit = {},
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
    onOpenHealthCheckPage: (() -> Unit)? = null,
    onOpenChatGuardPage: (() -> Unit)? = null,
    onOpenStorageCleanupPage: (() -> Unit)? = null,
    onOpenRepairToolsPage: (() -> Unit)? = null,
    onOpenDebugPage: (() -> Unit)? = null,
    onOpenGitStashPage: (() -> Unit)? = null,
) {
    var activeDialog by rememberSaveable { mutableStateOf<ToolboxDialogDestination?>(null) }
    val showHint = rememberTransientHint()

    LaunchedEffect(repairToolsOpenSignal) {
        if (repairToolsOpenSignal > 0) {
            onRepairToolsOpenSignalConsumed()
            if (onOpenRepairToolsPage != null) {
                onOpenRepairToolsPage()
            } else {
                activeDialog = ToolboxDialogDestination.RepairTools
            }
        }
    }

    LaunchedEffect(chatGuardOpenSignal) {
        if (chatGuardOpenSignal > 0) {
            onChatGuardOpenSignalConsumed()
            if (onOpenChatGuardPage != null) {
                onOpenChatGuardPage()
            } else {
                activeDialog = ToolboxDialogDestination.ChatGuard
                if (chatGuardState.snapshot == null && !actionsLocked && !chatGuardState.loading) {
                    onRefreshChatGuard()
                }
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
            infoText = "安装中断或启动缺依赖时可用「修复 npm 依赖」；网页主题损坏打不开时再点「重置主题」。\n内存上限和聊天文件大小一般保持默认或常用值即可，所有修改前都会先备份并确认。",
            onDismiss = { activeDialog = null },
        ) {
            RepairToolsSection(
                actionsLocked = actionsLocked,
                tavernRunning = tavernRunning,
                uploadLimitStatus = uploadLimitStatus,
                nodeMemoryStatus = nodeMemoryStatus,
                onRepairDependencies = onRepairDependencies,
                onResetTheme = onResetTavernTheme,
                onCheckNodeMemory = onCheckNodeMemory,
                onSetNodeMemory = onSetNodeMemory,
                onResetNodeMemory = onResetNodeMemory,
                onCheckUploadLimit = onCheckUploadLimit,
                onSetUploadLimit = onSetUploadLimit,
                onResetUploadLimit = onResetUploadLimit,
                onShowHint = showHint,
                showSectionContainer = false,
            )
        }

        ToolboxDialogDestination.Debug -> ToolboxContentDialog(
            title = "Debug 区",
            infoText = "集中放置故障自动诊断、日志导出与最后手段。\n遇到启动失败先看诊断建议或导出诊断日志；只有普通停止无效或确认端口被占时，才使用强制处理。",
            onDismiss = { activeDialog = null },
        ) {
            ToolboxDebugContent(
                actionsLocked = actionsLocked,
                issues = issues,
                forceCleanupSuggestion = forceCleanupSuggestion,
                onQuickFixAction = { action ->
                    activeDialog = null
                    onQuickFixAction(action)
                },
                onForceCleanup = onForceCleanup,
                onClearLogs = onClearLogs,
                onExportDiagnostic = onExportDiagnostic,
                onShowHint = showHint,
            )
        }

        ToolboxDialogDestination.ChatGuard -> ToolboxContentDialog(
            title = "聊天守护",
            infoText = "为什么需要守护：手机浏览器卡死重刷时，酒馆有时会把没加载完的空白聊天存回去，覆盖原本很长的对话。\n守护如何工作：酒馆运行时会给长聊天建立不额外占空间的硬链接保底；一旦发现聊天体积骤降一半以上，会自动把旧档救回为同目录的「原名 [露科亚救回 时间]」文件。\n安全保障：绝不删除你的原始聊天；每条聊天最多保留 5 份救回，且永远保护体积最大的那份完整旧档不被清理。",
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
            infoText = "只清理可安全重建的缓存、旧日志和升级残留临时文件，绝不会碰聊天记录、角色卡、世界书、预设或备份库。",
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
            infoText = "平时一般不需要打开这里。\n只有当你更新或回退酒馆版本时选择了「先把本地改动收起来」，记录才会出现在这里，方便之后放回或清理。",
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
                    onViewDetails = {
                        if (onOpenHealthCheckPage != null) {
                            onOpenHealthCheckPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.HealthCheck
                        }
                    },
                )
                ToolboxGrid(
                    backgroundTaskStatus = backgroundTaskStatus,
                    backgroundTaskNeedsAttention = backgroundTaskNeedsAttention,
                    chatGuardState = chatGuardState,
                    storageState = storageState,
                    gitStashState = gitStashState,
                    onOpenRepairTools = {
                        if (onOpenRepairToolsPage != null) {
                            onOpenRepairToolsPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.RepairTools
                        }
                    },
                    onOpenDebug = {
                        if (onOpenDebugPage != null) {
                            onOpenDebugPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.Debug
                        }
                    },
                    onOpenTaskCenter = onOpenBackgroundTaskCenter,
                    onOpenChatGuard = {
                        if (onOpenChatGuardPage != null) {
                            onOpenChatGuardPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.ChatGuard
                            if (chatGuardState.snapshot == null && !actionsLocked && !chatGuardState.loading) {
                                onRefreshChatGuard()
                            }
                        }
                    },
                    onOpenStorageCleanup = {
                        if (onOpenStorageCleanupPage != null) {
                            onOpenStorageCleanupPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.StorageCleanup
                            if (storageState.snapshot == null && !actionsLocked && !storageState.loading) {
                                onScanStorage()
                            }
                        }
                    },
                    onOpenGitStash = {
                        if (onOpenGitStashPage != null) {
                            onOpenGitStashPage()
                        } else {
                            activeDialog = ToolboxDialogDestination.GitStash
                            if (gitStashState.gitAvailable == null && !actionsLocked && !gitStashState.loading) {
                                onRefreshGitStash()
                            }
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
        chatGuardState.effectiveRescueCount > 0 -> "已救回 ${chatGuardState.effectiveRescueCount} 条"
        chatGuardState.snapshot?.running == true -> "后台守护中"
        else -> "防丢档与防休眠"
    }
    val storageDescription = storageState.snapshot?.let {
        "可清约 ${formatStorageKilobytes(it.totalCleanableKb)}"
    } ?: "安全清理缓存"
    val gitStashDescription = when {
        gitStashState.entries.isNotEmpty() -> "${gitStashState.entries.size} 条暂存改动"
        gitStashState.gitAvailable == true -> "暂无暂存改动"
        else -> "找回收起的改动"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column {
            HubGridRow(
                left = HubGridEntry(
                    label = "聊天守护",
                    description = chatGuardDescription,
                    onClick = onOpenChatGuard,
                ),
                right = HubGridEntry("空间清理", storageDescription, onOpenStorageCleanup),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("修复工具", "修依赖与调内存", onOpenRepairTools),
                right = HubGridEntry(
                    label = "任务中心",
                    description = backgroundTaskStatus,
                    onClick = onOpenTaskCenter,
                ),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("Debug 区", "报错修复与强停", onOpenDebug),
                right = HubGridEntry("暂存管理", gitStashDescription, onOpenGitStash),
            )
        }
    }
}

@Composable
private fun ToolboxContentDialog(
    title: String,
    infoText: String? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            if (!infoText.isNullOrBlank()) {
                SettingsDialogTitle(title = title, infoText = infoText)
            } else {
                Text(title)
            }
        },
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
internal fun ToolboxDebugContent(
    actionsLocked: Boolean,
    issues: List<TavernIssue>,
    forceCleanupSuggestion: TavernForceCleanupSuggestion?,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
    onForceCleanup: () -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel(
                if (issues.isEmpty()) "报错诊断" else "报错诊断（${issues.size} 条）",
            )
            IssueAnalysisPanel(
                issues = issues,
                actionsLocked = actionsLocked,
                onQuickFixAction = onQuickFixAction,
                showSectionContainer = false,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("诊断与日志")
            SettingsEntryGroup {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "导出内容适合用于排查问题；清除只会整理启动器页面里的日志，不会删除酒馆聊天或备份。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SettingsFeedbackActionButton(
                            text = "导出诊断日志",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onClick = onExportDiagnostic,
                        )
                        SettingsFeedbackActionButton(
                            text = "清除页面日志",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onClick = onClearLogs,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("强制处理")
            SettingsEntryGroup {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
        }
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
