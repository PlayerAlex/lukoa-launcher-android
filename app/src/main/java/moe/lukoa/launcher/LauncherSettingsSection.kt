package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSection(
    termuxReturnDelayMs: Long,
    termuxInstalled: Boolean,
    runCommandPermissionGranted: Boolean,
    backgroundRunPermissionGranted: Boolean,
    termuxBackgroundRunPermissionGranted: Boolean,
    termuxExternalAppsBlocked: Boolean,
    termuxStoragePermissionBlocked: Boolean,
    allFilesAccessGranted: Boolean,
    installUnknownAppsGranted: Boolean,
    tavernMirrorConfig: TavernMirrorConfig,
    tavernPathConfig: TavernPathConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    tavernPathInput: String,
    tavernPortInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    repositoryInput: String,
    githubUpdateState: GithubUpdateUiState,
    currentLauncherVersion: String,
    healthCheckReport: LauncherHealthReport?,
    healthCheckInFlight: Boolean,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    uploadLimitStatus: TavernUploadLimitStatus,
    tavernUserState: TavernUserManagementState,
    forceCleanupSuggestion: TavernForceCleanupSuggestion?,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onTavernPathInputChange: (String) -> Unit,
    onTavernPortInputChange: (String) -> Unit,
    onSelectTavernProfile: (String) -> Unit,
    onAddTavernProfile: () -> Unit,
    onRemoveCurrentTavernProfile: () -> Unit,
    onMigrateToManagedTavernPath: () -> Unit,
    onMigrateToTraditionalTavernPath: () -> Unit,
    onMigrateToCustomTavernPath: () -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernDirectory: () -> Unit,
    onRestoreDefaultTavernDirectory: () -> Unit,
    onSaveTavernPort: () -> Unit,
    onRestoreDefaultTavernPort: () -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onRequestBackgroundRunPermission: () -> Unit,
    onRequestTermuxBackgroundRunPermission: () -> Unit,
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
    onSaveUpdateChannel: (GithubReleaseChannel) -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
    onRunHealthCheck: () -> Unit,
    onRunHealthCheckPrimaryAction: () -> Unit,
    onForceCleanup: () -> Unit,
    onRepairDependencies: () -> Unit,
    onResetTavernTheme: () -> Unit,
    onSetNodeMemory: (Int) -> Unit,
    onCheckUploadLimit: () -> Unit,
    onSetUploadLimit: (Int) -> Unit,
    onRefreshTavernUsers: () -> Unit,
    onCreateTavernUser: (String, String) -> Unit,
    onDeleteTavernUser: (String) -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
) {
    val tavernPathError = TavernPathValidator.validate(tavernPathInput.trim())
    val tavernPortError = LauncherInputGuards.validateTavernPort(tavernPortInput.trim())
    val termuxExternalAppsReady = termuxInstalled && !termuxExternalAppsBlocked
    val permissionNotice = PermissionStatusSummary.settingsNotice(
        termuxInstalled = termuxInstalled,
        runCommandPermissionGranted = runCommandPermissionGranted,
        termuxExternalAppsReady = termuxExternalAppsReady,
        launcherBackgroundRunPermissionGranted = backgroundRunPermissionGranted,
        termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
        allFilesAccessGranted = allFilesAccessGranted,
        installUnknownAppsGranted = installUnknownAppsGranted,
        termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
    )
    val activePathInfo = TavernProfilePathPolicy.describe(tavernPathConfig.activeProfile)
    var showProfileManagementDialog by remember { mutableStateOf(false) }
    var showDirectorySettingsDialog by remember { mutableStateOf(false) }
    var showPortSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionCenterDialog by remember { mutableStateOf(false) }
    var showRepositorySettingsDialog by remember { mutableStateOf(false) }
    var showUpdateChannelDialog by remember { mutableStateOf(false) }
    var showWakeDelayDialog by remember { mutableStateOf(false) }
    var showMirrorSettingsDialog by remember { mutableStateOf(false) }
    var showHealthDialog by remember { mutableStateOf(false) }

    if (showProfileManagementDialog) {
        key(
            tavernPathConfig.activeProfile.id,
            tavernPathConfig.availableProfiles.size,
        ) {
            TavernProfileManagementDialog(
                tavernPathConfig = tavernPathConfig,
                currentPathInfo = activePathInfo,
                actionsLocked = actionsLocked,
                onSelectProfile = { profileId ->
                    onSelectTavernProfile(profileId)
                    showProfileManagementDialog = true
                },
                onAddProfile = onAddTavernProfile,
                onRemoveCurrentProfile = onRemoveCurrentTavernProfile,
                onDismiss = { showProfileManagementDialog = false },
            )
        }
    }

    if (showDirectorySettingsDialog) {
        key(
            tavernPathConfig.activeProfile.id,
            activePathInfo.currentPath,
        ) {
            TavernDirectorySettingsDialog(
                tavernPathConfig = tavernPathConfig,
                currentPathInfo = activePathInfo,
                tavernPathInput = tavernPathInput,
                tavernPathError = tavernPathError,
                displayPathPreview = TavernPathNormalizer.toDisplayPath(
                    TavernPathNormalizer.normalize(tavernPathInput),
                ),
                actionsLocked = actionsLocked,
                onPathChange = onTavernPathInputChange,
                onMigrateToManagedPath = onMigrateToManagedTavernPath,
                onMigrateToTraditionalPath = onMigrateToTraditionalTavernPath,
                onMigrateToCustomPath = onMigrateToCustomTavernPath,
                onSave = {
                    onSaveTavernDirectory()
                    if (tavernPathError == null) {
                        showDirectorySettingsDialog = false
                    }
                },
                onRestoreDefault = onRestoreDefaultTavernDirectory,
                onDismiss = { showDirectorySettingsDialog = false },
            )
        }
    }

    if (showPortSettingsDialog) {
        key(
            tavernPathConfig.activeProfile.id,
            tavernPathConfig.activeProfile.normalizedPort,
        ) {
            TavernPortSettingsDialog(
                tavernPathConfig = tavernPathConfig,
                tavernPortInput = tavernPortInput,
                tavernPortError = tavernPortError,
                actionsLocked = actionsLocked,
                onPortChange = onTavernPortInputChange,
                onSave = {
                    onSaveTavernPort()
                    if (tavernPortError == null) {
                        showPortSettingsDialog = false
                    }
                },
                onRestoreDefault = onRestoreDefaultTavernPort,
                onDismiss = { showPortSettingsDialog = false },
            )
        }
    }

    if (showPermissionCenterDialog) {
        PermissionCenterDialog(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsReady = termuxExternalAppsReady,
            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
            onRequestRunCommandPermission = onRequestRunCommandPermission,
            onOpenPermissionSettings = onOpenPermissionSettings,
            onCopyExternalAppsCommand = onCopyExternalAppsCommand,
            onOpenTermuxOnly = onOpenTermuxOnly,
            onRequestBackgroundRunPermission = onRequestBackgroundRunPermission,
            onRequestTermuxBackgroundRunPermission = onRequestTermuxBackgroundRunPermission,
            onOpenAllFilesAccessSettings = onOpenAllFilesAccessSettings,
            onOpenUnknownAppSourcesSettings = onOpenUnknownAppSourcesSettings,
            onShowTermuxStoragePermissionGuide = onShowTermuxStoragePermissionGuide,
            onDismiss = { showPermissionCenterDialog = false },
        )
    }

    if (showRepositorySettingsDialog) {
        LauncherRepositorySettingsDialog(
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onRepositoryInputChange = onRepositoryInputChange,
            onSaveRepository = onSaveRepository,
            onRestoreDefaultRepository = onRestoreDefaultRepository,
            onDismiss = { showRepositorySettingsDialog = false },
        )
    }

    if (showUpdateChannelDialog) {
        LauncherUpdateChannelDialog(
            githubUpdateState = githubUpdateState,
            onSaveUpdateChannel = onSaveUpdateChannel,
            onDismiss = { showUpdateChannelDialog = false },
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

    if (showMirrorSettingsDialog) {
        MirrorSettingsDialog(
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
            onDismiss = { showMirrorSettingsDialog = false },
        )
    }

    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            containerColor = LukoaColors.Surface,
            titleContentColor = LukoaColors.Accent,
            textContentColor = LukoaColors.Text,
            title = { Text("一键体检") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HealthCheckContent(
                        report = healthCheckReport,
                        checking = healthCheckInFlight,
                        actionsLocked = actionsLocked,
                        onRunHealthCheck = onRunHealthCheck,
                        onPrimaryAction = onRunHealthCheckPrimaryAction,
                    )
                }
            },
            confirmButton = {
                SecondaryActionButton(
                    text = "关闭",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    onClick = { showHealthDialog = false },
                )
            },
            dismissButton = null,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LauncherUpdateSettingsPanel(
            currentLauncherVersion = currentLauncherVersion,
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onOpenRepositorySettings = { showRepositorySettingsDialog = true },
            onOpenUpdateChannelSettings = { showUpdateChannelDialog = true },
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenRelease = onOpenRelease,
        )
        InstanceManagementPanel(
            termuxReturnDelayMs = termuxReturnDelayMs,
            tavernPathConfig = tavernPathConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            permissionNotice = permissionNotice,
            onOpenProfileManagement = { showProfileManagementDialog = true },
            onOpenDirectorySettings = { showDirectorySettingsDialog = true },
            onOpenPortSettings = { showPortSettingsDialog = true },
            onOpenMirrorSettings = { showMirrorSettingsDialog = true },
            onOpenWakeDelaySettings = { showWakeDelayDialog = true },
            onOpenPermissionCenter = { showPermissionCenterDialog = true },
        )
        TavernUserManagementSection(
            state = tavernUserState,
            instanceLabel = tavernPathConfig.activeProfileLabel,
            actionsLocked = actionsLocked,
            tavernRunning = tavernRunning,
            onRefresh = onRefreshTavernUsers,
            onCreate = onCreateTavernUser,
            onDelete = onDeleteTavernUser,
        )
        RepairToolsSection(
            actionsLocked = actionsLocked,
            tavernRunning = tavernRunning,
            uploadLimitStatus = uploadLimitStatus,
            onRepairDependencies = onRepairDependencies,
            onResetTheme = onResetTavernTheme,
            onSetNodeMemory = onSetNodeMemory,
            onCheckUploadLimit = onCheckUploadLimit,
            onSetUploadLimit = onSetUploadLimit,
            leadingContent = {
                SettingsEntryGroup {
                    SettingsEntryRow(
                        title = "一键体检",
                        value = settingsHealthSummaryText(healthCheckReport),
                        valueColor = settingsHealthSummaryTone(healthCheckReport),
                        valueAsPill = true,
                        onClick = { showHealthDialog = true },
                    )
                }
            },
            extraContent = {
                RepairDiagnosticsContent(
                    actionsLocked = actionsLocked,
                    forceCleanupSuggestion = forceCleanupSuggestion,
                    onForceCleanup = onForceCleanup,
                    onClearLogs = onClearLogs,
                    onExportDiagnostic = onExportDiagnostic,
                )
            },
        )
    }
}

@Composable
internal fun LauncherUpdateSettingsPanel(
    currentLauncherVersion: String,
    repositoryInput: String,
    githubUpdateState: GithubUpdateUiState,
    onOpenRepositorySettings: () -> Unit,
    onOpenUpdateChannelSettings: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    val repository = githubUpdateState.repository.ifBlank {
        repositoryInput.ifBlank { "未配置" }
    }
    val releasePageAvailable = githubUpdateState.latest?.releaseUrl?.isNotBlank() == true ||
        GithubRepositoryParser.normalize(githubUpdateState.repository)?.isNotBlank() == true
    val versionSummary = launcherVersionSummary(
        currentVersion = currentLauncherVersion,
        latest = githubUpdateState.latest,
    )
    val updateStatusText = when {
        githubUpdateState.downloading -> "下载中"
        githubUpdateState.checking -> "检查中"
        githubUpdateState.hasUpdate -> "有新版"
        githubUpdateState.latest != null -> "已是最新"
        else -> "未检查"
    }
    val updateStatusTone = when {
        githubUpdateState.checking -> LukoaColors.Accent
        githubUpdateState.hasUpdate || githubUpdateState.downloading -> LukoaColors.Accent
        else -> LukoaColors.Muted
    }

    SectionPanel(
        title = "启动器更新",
        accentColor = LukoaColors.Accent,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = updateStatusText,
                    active = githubUpdateState.hasUpdate || githubUpdateState.checking || githubUpdateState.downloading,
                    toneColor = updateStatusTone,
                    activeBackground = updateStatusTone.copy(alpha = 0.16f),
                )
                InfoPopoverButton(
                    contentDescription = "查看启动器更新说明",
                    title = "启动器更新",
                    body = "这里更新的是露科亚启动器本身。要更新或回退 SillyTavern，请前往“版本”页。稳定版更稳，测试版会更早收到新功能。",
                )
            }
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前版本",
                value = versionSummary,
                valueColor = if (githubUpdateState.hasUpdate) LukoaColors.Accent else LukoaColors.Text,
                valueLayout = SettingsValueLayout.Supporting,
                highlightColor = if (githubUpdateState.hasUpdate) LukoaColors.Accent else null,
                enabled = !updateLocked,
                onClick = if (githubUpdateState.hasUpdate) onInstallUpdate else null,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "修改仓库地址",
                value = repository,
                valueLayout = SettingsValueLayout.Supporting,
                enabled = !updateLocked,
                onClick = onOpenRepositorySettings,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "更新通道",
                value = githubUpdateState.channel.label,
                valueColor = LukoaColors.Accent,
                valueAsPill = true,
                enabled = !updateLocked,
                onClick = onOpenUpdateChannelSettings,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = when {
                    githubUpdateState.checking -> "检查中..."
                    githubUpdateState.downloading -> "下载中..."
                    else -> "检查更新"
                },
                enabled = !updateLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onCheckUpdate,
            )
            SecondaryActionButton(
                text = "打开发布页",
                enabled = !updateLocked && releasePageAvailable,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onOpenRelease,
            )
        }
    }
}

@Composable
internal fun InstanceManagementPanel(
    termuxReturnDelayMs: Long,
    tavernPathConfig: TavernPathConfig,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    permissionNotice: PermissionStatusNotice,
    onOpenProfileManagement: () -> Unit,
    onOpenDirectorySettings: () -> Unit,
    onOpenPortSettings: () -> Unit,
    onOpenMirrorSettings: () -> Unit,
    onOpenWakeDelaySettings: () -> Unit,
    onOpenPermissionCenter: () -> Unit,
) {
    val mirrorTone = mirrorProbeStatus.overallLevel.toneColor()
    SectionPanel(
        title = "实例管理",
        accentColor = LukoaColors.Info,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = "端口 ${tavernPathConfig.normalizedPort}",
                    active = true,
                    toneColor = LukoaColors.Info,
                    activeBackground = LukoaColors.InfoSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看实例管理说明",
                    title = "实例管理",
                    body = "一个实例就是一套独立的酒馆目录和设置。切换实例后，启动、版本、备份和用户管理都会使用你选中的那一套。端口是浏览器连接它时使用的编号。",
                )
            }
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例",
                value = tavernPathConfig.activeProfileLabel,
                valueColor = LukoaColors.Info,
                valueAsPill = true,
                highlightColor = LukoaColors.Info,
                onClick = onOpenProfileManagement,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "酒馆路径",
                value = tavernPathConfig.displayTavernDir,
                valueLayout = SettingsValueLayout.Supporting,
                onClick = onOpenDirectorySettings,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "实例端口",
                value = tavernPathConfig.normalizedPort.toString(),
                valueColor = LukoaColors.Info,
                valueAsPill = true,
                onClick = onOpenPortSettings,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "网络与镜像源",
                value = mirrorProbeStatus.overallLevel.label(),
                valueColor = mirrorTone,
                valueAsPill = true,
                onClick = onOpenMirrorSettings,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "唤醒延迟",
                value = "${"%.1f".format(termuxReturnDelayMs / 1000f)} 秒",
                valueColor = LukoaColors.Accent,
                valueAsPill = true,
                onClick = onOpenWakeDelaySettings,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "权限中心",
                value = if (permissionNotice.pendingItems.isEmpty()) {
                    "已就绪"
                } else {
                    "${permissionNotice.pendingItems.size} 项待处理"
                },
                valueColor = when (permissionNotice.tone) {
                    PermissionNoticeTone.Info -> LukoaColors.Accent
                    PermissionNoticeTone.Warning -> LukoaColors.Amber
                },
                valueAsPill = true,
                onClick = onOpenPermissionCenter,
            )
        }
    }
}

@Composable
private fun RepairDiagnosticsContent(
    actionsLocked: Boolean,
    forceCleanupSuggestion: TavernForceCleanupSuggestion?,
    onForceCleanup: () -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
) {
    SettingsSectionDivider()
    SettingsSubsection(
        title = "诊断与日志",
        detail = "导出诊断信息可用于排查问题。清除日志只会清空当前页面显示；“强制清理”用于残留进程，可能中断正在运行的任务。",
    ) {
        SecondaryActionButton(
            text = TavernForceCleanupButtonUi.labelFor(forceCleanupSuggestion),
            enabled = !actionsLocked,
            accentColor = LukoaColors.Danger,
            modifier = Modifier.fillMaxWidth(),
            onClick = onForceCleanup,
        )
        Text(
            text = TavernForceCleanupButtonUi.hintFor(forceCleanupSuggestion),
            color = LukoaColors.Amber,
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
}

internal fun launcherVersionSummary(
    currentVersion: String,
    latest: GithubUpdateInfo?,
): String {
    return if (latest?.isNewer == true) {
        "$currentVersion → ${latest.versionName}"
    } else {
        currentVersion
    }
}

private fun settingsHealthSummaryText(report: LauncherHealthReport?): String {
    val effectiveReport = report?.takeIf { it.hasData }
    return when {
        effectiveReport == null -> "未体检"
        effectiveReport.errorCount > 0 -> "${effectiveReport.errorCount} 个问题"
        effectiveReport.warningCount > 0 -> "${effectiveReport.warningCount} 个提醒"
        else -> "基本正常"
    }
}

private fun settingsHealthSummaryTone(report: LauncherHealthReport?): Color {
    val effectiveReport = report?.takeIf { it.hasData }
    return when {
        effectiveReport == null -> LukoaColors.Muted
        effectiveReport.errorCount > 0 -> LukoaColors.Danger
        effectiveReport.warningCount > 0 -> LukoaColors.Amber
        else -> LukoaColors.Accent
    }
}
