package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    healthCheckReport: LauncherHealthReport?,
    healthCheckInFlight: Boolean,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    issues: List<TavernIssue>,
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
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
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
    onExportLog: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
) {
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
    val tavernPathError = TavernPathValidator.validate(tavernPathInput.trim())
    val tavernPortError = LauncherInputGuards.validateTavernPort(tavernPortInput.trim())
    val activePathInfo = TavernProfilePathPolicy.describe(tavernPathConfig.activeProfile)
    var showPathSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionCenterDialog by remember { mutableStateOf(false) }
    var showUpdateSettingsDialog by remember { mutableStateOf(false) }
    var showWakeDelayDialog by remember { mutableStateOf(false) }
    var showMirrorDialog by remember { mutableStateOf(false) }
    var showHealthDialog by remember { mutableStateOf(false) }
    var showIssuesDialog by remember { mutableStateOf(false) }

    if (showPathSettingsDialog) {
        key(
            tavernPathConfig.activeProfile.id,
            activePathInfo.currentPath,
            tavernPathConfig.activeProfile.normalizedPort,
        ) {
            TavernPathSettingsDialog(
                tavernPathConfig = tavernPathConfig,
                currentPathInfo = activePathInfo,
                tavernPathInput = tavernPathInput,
                tavernPortInput = tavernPortInput,
                tavernPathError = tavernPathError,
                tavernPortError = tavernPortError,
                displayPathPreview = TavernPathNormalizer.toDisplayPath(
                    TavernPathNormalizer.normalize(tavernPathInput),
                ),
                actionsLocked = actionsLocked,
                onPathChange = onTavernPathInputChange,
                onPortChange = onTavernPortInputChange,
                onSelectProfile = { profileId ->
                    onSelectTavernProfile(profileId)
                    showPathSettingsDialog = true
                },
                onAddProfile = onAddTavernProfile,
                onRemoveCurrentProfile = onRemoveCurrentTavernProfile,
                onMigrateToManagedPath = onMigrateToManagedTavernPath,
                onMigrateToTraditionalPath = onMigrateToTraditionalTavernPath,
                onMigrateToCustomPath = onMigrateToCustomTavernPath,
                onSave = {
                    onSaveTavernPath()
                    if (tavernPathError == null && tavernPortError == null) {
                        showPathSettingsDialog = false
                    }
                },
                onRestoreDefault = onRestoreDefaultTavernPath,
                onDismiss = { showPathSettingsDialog = false },
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

    if (showUpdateSettingsDialog) {
        LauncherUpdateSettingsDialog(
            repositoryInput = repositoryInput,
            githubUpdateState = githubUpdateState,
            onRepositoryInputChange = onRepositoryInputChange,
            onSaveRepository = onSaveRepository,
            onRestoreDefaultRepository = onRestoreDefaultRepository,
            onSaveUpdateChannel = onSaveUpdateChannel,
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

    if (showMirrorDialog) {
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
            onDismiss = { showMirrorDialog = false },
        )
    }

    if (showHealthDialog) {
        LauncherDetailDialog(title = "一键体检", onDismiss = { showHealthDialog = false }) {
            HealthCheckSection(
                report = healthCheckReport,
                checking = healthCheckInFlight,
                actionsLocked = actionsLocked,
                onRunHealthCheck = onRunHealthCheck,
                onPrimaryAction = onRunHealthCheckPrimaryAction,
            )
        }
    }

    if (showIssuesDialog) {
        LauncherDetailDialog(title = "问题分析与快捷处理", onDismiss = { showIssuesDialog = false }) {
            if (issues.isEmpty()) {
                StateNote("当前日志里没有识别到可处理的问题。")
            } else {
                IssueAnalysisPanel(
                    issues = issues,
                    actionsLocked = actionsLocked,
                    onQuickFixAction = onQuickFixAction,
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        LauncherUpdateSettingsGroup(
            state = githubUpdateState,
            onOpenSettings = { showUpdateSettingsDialog = true },
            onCheckUpdate = onCheckUpdate,
            onInstallUpdate = onInstallUpdate,
            onOpenRelease = onOpenRelease,
        )

        DashedSection(
            label = "实例管理",
            headerAction = {
                HelpHint("每个实例都有独立目录、端口、用户、版本和备份目标。切换前请确认顶部显示的实例名称。")
            },
        ) {
            LukoaRow(
                title = "当前实例",
                detail = "${tavernPathConfig.activeProfileLabel} · ${tavernPathConfig.displayTavernDir} · 端口 ${tavernPathConfig.normalizedPort}",
                leading = { RowIcon("▦") },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextValue(tavernPathConfig.activeProfileLabel)
                        ChevronRight()
                    }
                },
                onClick = { showPathSettingsDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "酒馆路径",
                detail = tavernPathConfig.displayTavernDir,
                leading = { RowIcon("◇") },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextValue(activePathInfo.kind.label)
                        ChevronRight()
                    }
                },
                onClick = { showPathSettingsDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "实例端口",
                detail = tavernPathConfig.normalizedPort.toString(),
                leading = { RowIcon("⌁") },
                trailing = { ChevronRight() },
                onClick = { showPathSettingsDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "网络与镜像源",
                detail = "酒馆源 · npm 源 · Termux 包源",
                leading = { RowIcon("⌁") },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextValue(repoLabelFor(tavernMirrorConfig.normalizedRepoUrl))
                        ChevronRight()
                    }
                },
                onClick = { showMirrorDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "唤醒延迟",
                detail = "唤醒 Termux 后多久回切",
                leading = { RowIcon("◷") },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextValue("${termuxReturnDelayMs}ms")
                        ChevronRight()
                    }
                },
                onClick = { showWakeDelayDialog = true },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SecondaryActionButton(
                    text = "新建分身",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                    onClick = onAddTavernProfile,
                )
                SecondaryActionButton(
                    text = "删除当前实例",
                    enabled = !actionsLocked && tavernPathConfig.canRemoveActiveProfile,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(1f),
                    onClick = onRemoveCurrentTavernProfile,
                )
            }
            SecondaryActionButton(
                text = "迁移到自定义地址",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Text,
                modifier = Modifier.fillMaxWidth(),
                onClick = onMigrateToCustomTavernPath,
            )
            SecondaryActionButton(
                text = "立即检测镜像源",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Text,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCheckTavernMirror,
            )
        }

        TavernUserManagementSection(
            state = tavernUserState,
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
        )

        DashedSection(
            label = "权限与体检",
            headerAction = {
                HelpHint("权限中心负责系统授权，一键体检会同时检查权限、路径、镜像源和酒馆环境。")
            },
        ) {
            LukoaRow(
                title = "问题分析与快捷处理",
                detail = if (issues.isEmpty()) "当前没有识别到问题" else "发现 ${issues.size} 个问题",
                leading = { RowIcon("!", color = if (issues.isEmpty()) LukoaColors.Text else LukoaColors.Amber) },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (issues.isNotEmpty()) LukoaPill(issues.size.toString(), LukoaPillTone.Warning)
                        ChevronRight()
                    }
                },
                onClick = { showIssuesDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "权限中心",
                detail = if (permissionNotice.pendingItems.isEmpty()) "权限已基本就绪" else "${permissionNotice.pendingItems.size} 项待处理",
                leading = { RowIcon("♢", color = if (permissionNotice.pendingItems.isEmpty()) LukoaColors.Text else LukoaColors.Amber) },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (permissionNotice.pendingItems.isNotEmpty()) LukoaPill("待处理", LukoaPillTone.Warning)
                        ChevronRight()
                    }
                },
                onClick = { showPermissionCenterDialog = true },
            )
            LukoaRowDivider()
            LukoaRow(
                title = "一键体检",
                detail = healthCheckReport?.takeIf { it.hasData }?.summaryDetail ?: "尚未执行完整体检",
                leading = { RowIcon("✓") },
                trailing = { ChevronRight() },
                onClick = { showHealthDialog = true },
            )
        }

        DashedSection(
            label = "诊断与日志",
            headerAction = {
                HelpHint("普通日志适合查看操作过程；诊断日志包含更完整的环境信息，适合排查复杂问题。")
            },
        ) {
            LukoaRow(
                title = TavernForceCleanupButtonUi.labelFor(forceCleanupSuggestion),
                detail = TavernForceCleanupButtonUi.hintFor(forceCleanupSuggestion),
                leading = { RowIcon("!", color = LukoaColors.Danger) },
                trailing = { ChevronRight() },
                enabled = !actionsLocked,
                onClick = onForceCleanup,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SecondaryActionButton(
                    text = "导出日志",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                    onClick = onExportLog,
                )
                SecondaryActionButton(
                    text = "导出诊断日志",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                    onClick = onExportDiagnostic,
                )
            }
            SecondaryActionButton(
                text = "清空页面日志",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Text,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClearLogs,
            )
            StateNote("只清空当前页面显示，后台诊断归档仍会继续记录，不会删除已保存的诊断文件。")
        }
    }
}

@Composable
private fun LauncherUpdateSettingsGroup(
    state: GithubUpdateUiState,
    onOpenSettings: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    val latest = state.latest
    DashedSection(
        label = "启动器更新",
        headerAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (state.hasUpdate) LukoaPill("有新版", LukoaPillTone.Danger)
                HelpHint("这里管理启动器 APK 更新，不是 SillyTavern 版本更新。")
            }
        },
    ) {
        LukoaRow(
            title = if (latest != null) {
                "${state.currentRelease?.versionName ?: "当前版本"} → ${latest.versionName}"
            } else {
                "检查启动器版本"
            },
            detail = latest?.let { "来自 GitHub Release · ${it.releaseTypeLabel}" } ?: state.message,
            leading = { RowIcon("↑", color = if (state.hasUpdate) LukoaColors.Danger else LukoaColors.Text) },
            trailing = { ChevronRight() },
            enabled = !state.checking && !state.downloading,
            onClick = if (state.hasUpdate) onInstallUpdate else onCheckUpdate,
        )
        LukoaRowDivider()
        LukoaRow(
            title = "更新源仓库",
            detail = state.repository.ifBlank { "未配置" },
            leading = { RowIcon("◇") },
            trailing = { ChevronRight() },
            onClick = onOpenSettings,
        )
        LukoaRowDivider()
        LukoaRow(
            title = "更新通道",
            detail = state.channel.label,
            leading = { RowIcon("○") },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TextValue(state.channel.label)
                    ChevronRight()
                }
            },
            onClick = onOpenSettings,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SecondaryActionButton(
                text = if (state.checking) "检查中…" else "检查更新",
                enabled = !state.checking && !state.downloading,
                accentColor = LukoaColors.Text,
                modifier = Modifier.weight(1f),
                onClick = onCheckUpdate,
            )
            SecondaryActionButton(
                text = "打开发布页",
                enabled = latest?.releaseUrl?.isNotBlank() == true && !state.downloading,
                accentColor = LukoaColors.Text,
                modifier = Modifier.weight(1f),
                onClick = onOpenRelease,
            )
        }
    }
}

@Composable
private fun TextValue(text: String, color: Color = LukoaColors.Muted) {
    androidx.compose.material3.Text(
        text = text,
        color = color,
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        maxLines = 1,
    )
}
