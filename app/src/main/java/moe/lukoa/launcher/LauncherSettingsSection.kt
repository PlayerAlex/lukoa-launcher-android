package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
    onResetUploadLimit: () -> Unit,
    onRefreshTavernUsers: () -> Unit,
    onCreateTavernUser: (String, String) -> Unit,
    onDeleteTavernUser: (String) -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
) {
    val showHint = rememberTransientHint()
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
            onShowHint = showHint,
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
            onResetUploadLimit = onResetUploadLimit,
            onShowHint = showHint,
            leadingContent = {
                SettingsEntryRow(
                    title = "一键体检",
                    detail = "检查当前实例的安装、权限和运行环境。",
                    value = settingsHealthSummaryText(healthCheckReport),
                    valueColor = settingsHealthSummaryTone(healthCheckReport),
                    valueAsPill = true,
                    onClick = { showHealthDialog = true },
                )
            },
            extraContent = {
                RepairDiagnosticsContent(
                    actionsLocked = actionsLocked,
                    forceCleanupSuggestion = forceCleanupSuggestion,
                    onForceCleanup = onForceCleanup,
                    onClearLogs = onClearLogs,
                    onExportDiagnostic = onExportDiagnostic,
                    onShowHint = showHint,
                )
            },
        )
        LauncherUpdateSettingsPanel(
            currentLauncherVersion = currentLauncherVersion,
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onOpenRepositorySettings = { showRepositorySettingsDialog = true },
            onOpenUpdateChannelSettings = { showUpdateChannelDialog = true },
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenRelease = onOpenRelease,
            onShowHint = showHint,
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
    onShowHint: (String) -> Unit = {},
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
                    activeBackground = LukoaColors.AccentSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看启动器更新说明",
                    title = "启动器更新",
                    body = "这里更新的是露科亚启动器本身，不是 SillyTavern 酒馆。\n点“检查更新”只会查询新版本；发现新版后，你再决定是否下载安装。\n大多数人选稳定版；想提前体验新功能时再选测试版。酒馆的更新和回退请到“版本”页。",
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
                unavailableHint = when {
                    githubUpdateState.downloading -> "正在下载启动器更新，请稍等。"
                    githubUpdateState.checking -> "正在检查启动器更新，请稍等。"
                    !githubUpdateState.hasUpdate -> "当前没有待安装的新版本，可以点下方“检查更新”重新检查。"
                    else -> null
                },
                onShowHint = onShowHint,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "修改仓库地址",
                value = repository,
                valueLayout = SettingsValueLayout.Supporting,
                enabled = !updateLocked,
                onClick = onOpenRepositorySettings,
                unavailableHint = if (updateLocked) "更新任务正在进行，完成后才能修改仓库地址。" else null,
                onShowHint = onShowHint,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "更新通道",
                value = githubUpdateState.channel.label,
                valueColor = LukoaColors.Accent,
                valueAsPill = true,
                enabled = !updateLocked,
                onClick = onOpenUpdateChannelSettings,
                unavailableHint = if (updateLocked) "更新任务正在进行，完成后才能切换更新通道。" else null,
                onShowHint = onShowHint,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsFeedbackActionButton(
                text = when {
                    githubUpdateState.checking -> "检查中..."
                    githubUpdateState.downloading -> "下载中..."
                    else -> "检查更新"
                },
                enabled = !updateLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                unavailableHint = when {
                    githubUpdateState.downloading -> "正在下载启动器更新，请稍等。"
                    githubUpdateState.checking -> "正在检查启动器更新，请稍等。"
                    else -> null
                },
                onShowHint = onShowHint,
                onClick = onCheckUpdate,
            )
            SettingsFeedbackActionButton(
                text = "打开发布页",
                enabled = !updateLocked && releasePageAvailable,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                unavailableHint = when {
                    updateLocked -> "更新任务正在进行，请稍等。"
                    !releasePageAvailable -> "请先设置有效的启动器仓库地址，再打开发布页。"
                    else -> null
                },
                onShowHint = onShowHint,
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
        title = "实例与运行环境",
        accentColor = LukoaColors.Info,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = tavernPathConfig.activeProfileLabel,
                    active = true,
                    modifier = Modifier.widthIn(max = 112.dp),
                    toneColor = LukoaColors.Info,
                    activeBackground = LukoaColors.InfoSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看实例与运行环境说明",
                    title = "实例与运行环境",
                    body = "“实例”可以理解为一套单独的酒馆：它有自己的文件夹、端口和设置。\n切换实例后，启动、版本、备份和用户管理都会改为操作你刚选中的那一套。\n如果你只有一套酒馆，保持当前实例即可，不需要另外新建。",
                )
            }
        },
    ) {
        SettingsGroupLabel("当前实例")
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "实例名称",
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
                title = "访问端口",
                value = tavernPathConfig.normalizedPort.toString(),
                valueColor = LukoaColors.Info,
                valueAsPill = true,
                onClick = onOpenPortSettings,
            )
        }
        SettingsGroupLabel("运行环境")
        SettingsEntryGroup {
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
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    SettingsSubsection(
        title = "诊断与日志",
        detail = "“导出诊断日志”会生成一份排错文件，不会修改酒馆数据。“清除页面日志”只清空启动器里当前显示的记录。\n“强制清理”会结束当前实例可能残留的进程，普通停止无效或端口被占用时才使用。${TavernForceCleanupButtonUi.hintFor(forceCleanupSuggestion)}",
    ) {
        SettingsFeedbackActionButton(
            text = "导出诊断日志",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Accent,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onClick = onExportDiagnostic,
        )
        SettingsFeedbackActionButton(
            text = "清除页面日志",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Accent,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onClick = onClearLogs,
        )
        SettingsFeedbackActionButton(
            text = TavernForceCleanupButtonUi.labelFor(forceCleanupSuggestion),
            enabled = !actionsLocked,
            accentColor = LukoaColors.Danger,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = lockedHint,
            onShowHint = onShowHint,
            onClick = onForceCleanup,
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
        effectiveReport.unknownCount > 0 -> "${effectiveReport.unknownCount} 项未确认"
        else -> "基本正常"
    }
}

private fun settingsHealthSummaryTone(report: LauncherHealthReport?): Color {
    val effectiveReport = report?.takeIf { it.hasData }
    return when {
        effectiveReport == null -> LukoaColors.Muted
        effectiveReport.errorCount > 0 -> LukoaColors.Danger
        effectiveReport.warningCount > 0 -> LukoaColors.Amber
        effectiveReport.unknownCount > 0 -> LukoaColors.Muted
        else -> LukoaColors.Accent
    }
}
