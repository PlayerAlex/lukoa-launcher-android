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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private enum class SettingsDialogDestination {
    ProfileManagement,
    Directory,
    Port,
    PermissionCenter,
    Repository,
    UpdateChannel,
    WakeDelay,
    Mirror,
    HealthCheck,
}

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
    tavernExtensionState: TavernExtensionManagementState,
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
    onSaveTavernDirectory: () -> Boolean,
    onRestoreDefaultTavernDirectory: () -> Unit,
    onSaveTavernPort: () -> Boolean,
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
    onRefreshTavernExtensions: () -> Unit,
    onDeleteTavernExtension: (String) -> Unit,
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
    var activeDialog by rememberSaveable { mutableStateOf<SettingsDialogDestination?>(null) }

    if (activeDialog == SettingsDialogDestination.ProfileManagement) {
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
                },
                onAddProfile = onAddTavernProfile,
                onRemoveCurrentProfile = onRemoveCurrentTavernProfile,
                onDismiss = { activeDialog = null },
            )
        }
    }

    if (activeDialog == SettingsDialogDestination.Directory) {
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
                    if (onSaveTavernDirectory()) {
                        activeDialog = null
                    }
                },
                onRestoreDefault = onRestoreDefaultTavernDirectory,
                onDismiss = { activeDialog = null },
            )
        }
    }

    if (activeDialog == SettingsDialogDestination.Port) {
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
                    if (onSaveTavernPort()) {
                        activeDialog = null
                    }
                },
                onRestoreDefault = onRestoreDefaultTavernPort,
                onDismiss = { activeDialog = null },
            )
        }
    }

    if (activeDialog == SettingsDialogDestination.PermissionCenter) {
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
            onDismiss = { activeDialog = null },
        )
    }

    if (activeDialog == SettingsDialogDestination.Repository) {
        LauncherRepositorySettingsDialog(
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onRepositoryInputChange = onRepositoryInputChange,
            onSaveRepository = onSaveRepository,
            onRestoreDefaultRepository = onRestoreDefaultRepository,
            onDismiss = { activeDialog = null },
        )
    }

    if (activeDialog == SettingsDialogDestination.UpdateChannel) {
        LauncherUpdateChannelDialog(
            githubUpdateState = githubUpdateState,
            onSaveUpdateChannel = onSaveUpdateChannel,
            onDismiss = { activeDialog = null },
        )
    }

    if (activeDialog == SettingsDialogDestination.WakeDelay) {
        TermuxWakeDelayDialog(
            termuxReturnDelayMs = termuxReturnDelayMs,
            actionsLocked = actionsLocked,
            onDecrease = onDecreaseTermuxReturnDelay,
            onIncrease = onIncreaseTermuxReturnDelay,
            onDismiss = { activeDialog = null },
        )
    }

    if (activeDialog == SettingsDialogDestination.Mirror) {
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
            onDismiss = { activeDialog = null },
        )
    }

    if (activeDialog == SettingsDialogDestination.HealthCheck) {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.Primary,
            textContentColor = LukoaColors.TextPrimary,
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
                    accentColor = LukoaColors.Primary,
                    onClick = { activeDialog = null },
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
            onOpenProfileManagement = { activeDialog = SettingsDialogDestination.ProfileManagement },
            onOpenDirectorySettings = { activeDialog = SettingsDialogDestination.Directory },
            onOpenPortSettings = { activeDialog = SettingsDialogDestination.Port },
            onOpenMirrorSettings = { activeDialog = SettingsDialogDestination.Mirror },
            onOpenWakeDelaySettings = { activeDialog = SettingsDialogDestination.WakeDelay },
            onOpenPermissionCenter = { activeDialog = SettingsDialogDestination.PermissionCenter },
        )
        TavernUserManagementSettingsPanel(
            state = tavernUserState,
            instanceLabel = tavernPathConfig.activeProfileLabel,
            actionsLocked = actionsLocked,
            tavernRunning = tavernRunning,
            onRefresh = onRefreshTavernUsers,
            onCreate = onCreateTavernUser,
            onDelete = onDeleteTavernUser,
            onShowHint = showHint,
        )
        TavernExtensionManagementSettingsPanel(
            state = tavernExtensionState,
            instanceLabel = tavernPathConfig.activeProfileLabel,
            actionsLocked = actionsLocked,
            tavernRunning = tavernRunning,
            onRefresh = onRefreshTavernExtensions,
            onDelete = onDeleteTavernExtension,
            onShowHint = showHint,
        )
        RepairToolsSettingsPanel(
            instanceLabel = tavernPathConfig.activeProfileLabel,
            summaryText = settingsHealthSummaryText(healthCheckReport),
            summaryColor = settingsHealthSummaryTone(healthCheckReport),
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
                    onClick = { activeDialog = SettingsDialogDestination.HealthCheck },
                )
            },
        )
        DiagnosticsSettingsPanel(
            actionsLocked = actionsLocked,
            forceCleanupSuggestion = forceCleanupSuggestion,
            onForceCleanup = onForceCleanup,
            onClearLogs = onClearLogs,
            onExportDiagnostic = onExportDiagnostic,
            onShowHint = showHint,
        )
        LauncherUpdateSettingsPanel(
            currentLauncherVersion = currentLauncherVersion,
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onOpenRepositorySettings = { activeDialog = SettingsDialogDestination.Repository },
            onOpenUpdateChannelSettings = { activeDialog = SettingsDialogDestination.UpdateChannel },
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
    var showCurrentReleaseNotes by remember { mutableStateOf(false) }
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    val currentRelease = githubUpdateState.currentRelease
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
        githubUpdateState.checking -> LukoaColors.Primary
        githubUpdateState.hasUpdate || githubUpdateState.downloading -> LukoaColors.Primary
        else -> LukoaColors.TextSecondary
    }

    if (showCurrentReleaseNotes && currentRelease != null) {
        val releaseNotesDocument = remember(currentRelease.versionName, currentRelease.body) {
            GithubReleaseNotesFormatter.parse(currentRelease.versionName, currentRelease.body)
        }
        AlertDialog(
            onDismissRequest = { showCurrentReleaseNotes = false },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.Primary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("v${currentRelease.versionName} 更新内容") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GithubReleaseNotesContent(document = releaseNotesDocument)
                }
            },
            confirmButton = {
                SecondaryActionButton(
                    text = "关闭",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    onClick = { showCurrentReleaseNotes = false },
                )
            },
            dismissButton = null,
        )
    }

    SectionPanel(
        title = "启动器更新",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = updateStatusText,
                    active = githubUpdateState.hasUpdate || githubUpdateState.checking || githubUpdateState.downloading,
                    toneColor = updateStatusTone,
                    activeBackground = LukoaColors.PrimarySoft,
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
                valueColor = if (githubUpdateState.hasUpdate) LukoaColors.Primary else LukoaColors.TextPrimary,
                valueLayout = SettingsValueLayout.Supporting,
                highlightColor = if (githubUpdateState.hasUpdate) LukoaColors.Primary else null,
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
                title = "当前版本更新内容",
                value = currentRelease?.let { "查看 v${it.versionName} 的更新说明" }
                    ?: "检查更新后读取",
                valueLayout = SettingsValueLayout.Supporting,
                enabled = !updateLocked && currentRelease != null,
                onClick = if (currentRelease != null) {
                    { showCurrentReleaseNotes = true }
                } else {
                    null
                },
                unavailableHint = when {
                    githubUpdateState.downloading -> "正在下载启动器更新，请稍等。"
                    githubUpdateState.checking -> "正在检查启动器更新，请稍等。"
                    currentRelease == null -> "点“检查更新”后，启动器会同时读取当前已安装版本的更新内容。"
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
                valueColor = LukoaColors.Primary,
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
                accentColor = LukoaColors.Primary,
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
                accentColor = LukoaColors.Primary,
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
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = tavernPathConfig.activeProfileLabel,
                    active = true,
                    modifier = Modifier.widthIn(max = 112.dp),
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
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
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                highlightColor = LukoaColors.Primary,
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
                valueColor = LukoaColors.Primary,
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
                valueColor = LukoaColors.Primary,
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
                    PermissionNoticeTone.Info -> LukoaColors.Primary
                    PermissionNoticeTone.Warning -> LukoaColors.Accent
                },
                valueAsPill = true,
                onClick = onOpenPermissionCenter,
            )
        }
    }
}

@Composable
private fun DiagnosticsSettingsPanel(
    actionsLocked: Boolean,
    forceCleanupSuggestion: TavernForceCleanupSuggestion?,
    onForceCleanup: () -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    SectionPanel(
        title = "诊断与日志",
        accentColor = LukoaColors.Primary,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看诊断与日志说明",
                title = "诊断与日志",
                body = "“导出诊断日志”会生成一份排错文件，不会修改酒馆数据。“清除页面日志”只清空启动器里当前显示的记录。\n“强制清理”会结束当前实例可能残留的进程，普通停止无效或端口被占用时才使用。${TavernForceCleanupButtonUi.hintFor(forceCleanupSuggestion)}",
            )
        },
    ) {
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
        effectiveReport == null -> LukoaColors.TextSecondary
        effectiveReport.errorCount > 0 -> LukoaColors.Danger
        effectiveReport.warningCount > 0 -> LukoaColors.Accent
        effectiveReport.unknownCount > 0 -> LukoaColors.TextSecondary
        else -> LukoaColors.Primary
    }
}
