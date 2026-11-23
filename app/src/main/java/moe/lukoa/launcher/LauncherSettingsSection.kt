package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    BrowserPreference,
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
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    tavernUserState: TavernUserManagementState,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onTavernPathInputChange: (String) -> Unit,
    onTavernPortInputChange: (String) -> Unit,
    onSelectTavernProfile: (String) -> Unit,
    onAddTavernProfile: () -> Unit,
    onCloneCurrentTavernProfile: () -> Unit,
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
    onRefreshTavernUsers: () -> Unit,
    onCreateTavernUser: (String, String) -> Unit,
    onDeleteTavernUser: (String) -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
    runningNotificationEnabled: Boolean = true,
    onToggleRunningNotification: () -> Unit = {},
    onPinQuickStartShortcut: () -> Unit = {},
    launchMode: TavernLaunchMode = TavernLaunchMode.ForegroundConsole,
    onToggleLaunchMode: () -> Unit = {},
    themeStyle: LukoaThemeStyle = LukoaColors.currentStyle,
    onToggleThemeStyle: () -> Unit = {},
    basicPrefs: LauncherBasicPrefs = LauncherBasicPrefs(),
    installedBrowsers: List<InstalledBrowserOption> = emptyList(),
    onSelectPreferredBrowser: (String) -> Unit = {},
    onToggleKeepScreenOn: () -> Unit = {},
    onToggleAutoStartTavern: () -> Unit = {},
    onOpenUserManagementPage: (() -> Unit)? = null,
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
                onCloneCurrentProfile = onCloneCurrentTavernProfile,
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

    if (activeDialog == SettingsDialogDestination.BrowserPreference) {
        TavernBrowserPreferenceDialog(
            selectedPackage = basicPrefs.preferredBrowserPackage,
            installedBrowsers = installedBrowsers,
            onSelectPackage = { pkg ->
                onSelectPreferredBrowser(pkg)
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        InstanceManagementPanel(
            termuxReturnDelayMs = termuxReturnDelayMs,
            tavernPathConfig = tavernPathConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            permissionNotice = permissionNotice,
            runningNotificationEnabled = runningNotificationEnabled,
            launchMode = launchMode,
            themeStyle = themeStyle,
            preferredBrowserLabel = LauncherBrowserResolver.displayLabelFor(
                basicPrefs.preferredBrowserPackage,
                installedBrowsers,
            ),
            keepScreenOn = basicPrefs.keepScreenOn,
            autoStartTavernOnLaunch = basicPrefs.autoStartTavernOnLaunch,
            onOpenProfileManagement = { activeDialog = SettingsDialogDestination.ProfileManagement },
            onOpenDirectorySettings = { activeDialog = SettingsDialogDestination.Directory },
            onOpenPortSettings = { activeDialog = SettingsDialogDestination.Port },
            onOpenMirrorSettings = { activeDialog = SettingsDialogDestination.Mirror },
            onOpenWakeDelaySettings = { activeDialog = SettingsDialogDestination.WakeDelay },
            onOpenPermissionCenter = { activeDialog = SettingsDialogDestination.PermissionCenter },
            onOpenBrowserPreference = { activeDialog = SettingsDialogDestination.BrowserPreference },
            onToggleRunningNotification = onToggleRunningNotification,
            onPinQuickStartShortcut = onPinQuickStartShortcut,
            onToggleLaunchMode = onToggleLaunchMode,
            onToggleThemeStyle = onToggleThemeStyle,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
            onToggleAutoStartTavern = onToggleAutoStartTavern,
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
            onOpenPage = onOpenUserManagementPage,
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
        containerColor = LukoaColors.Elevated,
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
    runningNotificationEnabled: Boolean = true,
    launchMode: TavernLaunchMode = TavernLaunchMode.ForegroundConsole,
    themeStyle: LukoaThemeStyle = LukoaColors.currentStyle,
    preferredBrowserLabel: String = "系统默认",
    keepScreenOn: Boolean = true,
    autoStartTavernOnLaunch: Boolean = false,
    onOpenProfileManagement: () -> Unit,
    onOpenDirectorySettings: () -> Unit,
    onOpenPortSettings: () -> Unit,
    onOpenMirrorSettings: () -> Unit,
    onOpenWakeDelaySettings: () -> Unit,
    onOpenPermissionCenter: () -> Unit,
    onOpenBrowserPreference: () -> Unit = {},
    onToggleRunningNotification: () -> Unit = {},
    onPinQuickStartShortcut: () -> Unit = {},
    onToggleLaunchMode: () -> Unit = {},
    onToggleThemeStyle: () -> Unit = {},
    onToggleKeepScreenOn: () -> Unit = {},
    onToggleAutoStartTavern: () -> Unit = {},
) {
    val mirrorTone = mirrorProbeStatus.overallLevel.toneColor()
    SectionPanel(
        title = "实例与运行环境",
        accentColor = LukoaColors.Primary,
        containerColor = LukoaColors.Elevated,
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
                    body = "“实例”是一套独立的酒馆（单独目录和端口），普通用户只有一套主实例，保持默认即可。\n“启动与外观”可切换前台/后台启动方式、极简黑白/原版冷青配色、通知栏快捷控制和桌面快捷图标。\n“网络与权限”用于换国内加速镜像、检查 Termux 权限和调整唤醒延迟。",
                )
            }
        },
    ) {
        SettingsGroupLabel("当前酒馆实例")
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
        SettingsGroupLabel("启动与外观")
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "启动方式",
                detail = launchMode.summary,
                value = "${launchMode.shortLabel} · 切换",
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                onClick = onToggleLaunchMode,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "打开酒馆的浏览器",
                detail = "指定浏览器直接打开酒馆，不再每次弹出系统选择框。",
                value = preferredBrowserLabel,
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                onClick = onOpenBrowserPreference,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "打开启动器自动启动酒馆",
                detail = "进入启动器且环境就绪时自动启动当前实例。",
                value = if (autoStartTavernOnLaunch) "已开启" else "已关闭",
                valueColor = if (autoStartTavernOnLaunch) LukoaColors.Primary else LukoaColors.TextSecondary,
                valueAsPill = true,
                onClick = onToggleAutoStartTavern,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "前台保持屏幕常亮",
                detail = "安装、备份或查看日志时防止手机自动息屏。",
                value = if (keepScreenOn) "已开启" else "已关闭",
                valueColor = if (keepScreenOn) LukoaColors.Primary else LukoaColors.TextSecondary,
                valueAsPill = true,
                onClick = onToggleKeepScreenOn,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "界面配色风格",
                detail = themeStyle.summary,
                value = "${themeStyle.label} · 切换",
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                onClick = onToggleThemeStyle,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "运行状态通知栏控制",
                detail = "酒馆运行时可从通知栏直接打开、重启或停止。",
                value = if (runningNotificationEnabled) "已开启" else "已关闭",
                valueColor = if (runningNotificationEnabled) LukoaColors.Primary else LukoaColors.TextSecondary,
                valueAsPill = true,
                onClick = onToggleRunningNotification,
            )
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "添加到桌面一键启动",
                detail = "在手机桌面放一个「启动酒馆」快捷图标。",
                value = "添加",
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                onClick = onPinQuickStartShortcut,
            )
        }
        SettingsGroupLabel("网络与权限")
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
            SettingsEntryDivider()
            SettingsEntryRow(
                title = "唤醒延迟",
                value = "${"%.1f".format(termuxReturnDelayMs / 1000f)} 秒",
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                onClick = onOpenWakeDelaySettings,
            )
        }
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

@Composable
private fun TavernBrowserPreferenceDialog(
    selectedPackage: String,
    installedBrowsers: List<InstalledBrowserOption>,
    onSelectPackage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "选择打开酒馆的浏览器",
                infoText = "指定浏览器后，点击“返回酒馆”或通知栏“打开酒馆”都会直接用它打开。若之后卸载了该浏览器，启动器会自动回退为系统默认。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrowserChoiceCard(
                    title = "系统默认浏览器",
                    subtitle = "跟随手机系统默认设置",
                    selected = selectedPackage.isBlank(),
                    onClick = { onSelectPackage("") },
                )
                if (installedBrowsers.isEmpty()) {
                    Text(
                        text = "未扫描到其他浏览器，将使用系统默认方式打开网页。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    installedBrowsers.forEach { option ->
                        BrowserChoiceCard(
                            title = option.label,
                            subtitle = option.packageName,
                            selected = selectedPackage == option.packageName,
                            onClick = { onSelectPackage(option.packageName) },
                        )
                    }
                }
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
private fun BrowserChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        color = if (selected) LukoaColors.PrimarySoft else LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) LukoaColors.Primary else LukoaColors.Border,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = if (selected) LukoaColors.Primary else LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Text(
                    text = "当前",
                    color = LukoaColors.Primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
