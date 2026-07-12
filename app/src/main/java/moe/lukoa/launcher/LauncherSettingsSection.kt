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
import androidx.compose.runtime.key
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


private enum class SettingsPageView {
    Health,
    Repair,
    Path,
    Mirror,
    Permissions,
    Update,
    Diagnostic,
    Wake,
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
    healthCheckReport: LauncherHealthReport?,
    healthCheckInFlight: Boolean,
    actionsLocked: Boolean,
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
    onForceCleanup: () -> Unit,
    onRepairDependencies: () -> Unit,
    onResetTavernTheme: () -> Unit,
    onSetNodeMemory: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onExportDiagnostic: () -> Unit,
    onDecreaseTermuxReturnDelay: () -> Unit,
    onIncreaseTermuxReturnDelay: () -> Unit,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    val tavernPathError = TavernPathValidator.validate(tavernPathInput.trim())
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
    val healthSummaryText = settingsHealthSummaryText(healthCheckReport)
    val healthSummaryColor = settingsHealthSummaryColor(healthCheckReport)
    val tavernPortError = LauncherInputGuards.validateTavernPort(tavernPortInput.trim())
    val pathIsDefault = tavernPathConfig.isActiveProfileDefault
    val activePathInfo = TavernProfilePathPolicy.describe(tavernPathConfig.activeProfile)
    var showPathSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionCenterDialog by remember { mutableStateOf(false) }
    var showUpdateSettingsDialog by remember { mutableStateOf(false) }
    var showWakeDelayDialog by remember { mutableStateOf(false) }
    var selectedView by remember {
        mutableStateOf(
            if (healthCheckReport?.hasData == true) {
                if (termuxInstalled && permissionNotice.pendingItems.isEmpty()) {
                    SettingsPageView.Mirror
                } else {
                    SettingsPageView.Permissions
                }
            } else {
                SettingsPageView.Health
            },
        )
    }
    val sectionOptions = listOf(
        SectionSwitchOption(
            value = SettingsPageView.Repair,
            label = "修复",
            description = "修复依赖、重置网页主题，或调整 Node.js 内存上限。所有修改都会先保留可恢复副本。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Health,
            label = "体检",
            description = "先做一键体检，集中看权限、路径、镜像源和酒馆环境哪里卡住了。",
        ),
        SectionSwitchOption(
            value = SettingsPageView.Path,
            label = "路径",
            description = "管理实例目录、迁移位置和端口，也可以处理旧酒馆目录迁移到启动器托管目录。",
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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsOverviewCard(
            tavernPathConfig = tavernPathConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            healthSummaryText = healthSummaryText,
            healthSummaryColor = healthSummaryColor,
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
            SettingsPageView.Health -> HealthCheckSection(
                report = healthCheckReport,
                checking = healthCheckInFlight,
                actionsLocked = actionsLocked,
                onRunHealthCheck = onRunHealthCheck,
                onPrimaryAction = onRunHealthCheckPrimaryAction,
            )

            SettingsPageView.Repair -> RepairToolsSection(
                actionsLocked = actionsLocked,
                tavernRunning = healthCheckReport?.doctorReport?.let { it.httpOk == true || it.processDetected == true } == true,
                onRepairDependencies = onRepairDependencies,
                onResetTheme = onResetTavernTheme,
                onSetNodeMemory = onSetNodeMemory,
            )

            SettingsPageView.Path -> SectionPanel(title = "酒馆路径", accentColor = LukoaColors.Accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = if (pathIsDefault) "实例默认路径" else "实例已改路径",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(
                        text = tavernPathConfig.activeProfileLabel,
                        active = true,
                        modifier = Modifier.weight(1f),
                        toneColor = LukoaColors.Accent,
                        activeBackground = LukoaColors.AccentSoft,
                    )
                    StatusPill(
                        text = "端口 ${tavernPathConfig.normalizedPort}",
                        active = true,
                        modifier = Modifier.weight(1f),
                        toneColor = LukoaColors.Info,
                        activeBackground = LukoaColors.InfoSoft,
                    )
                }
                StatusPill(
                    text = activePathInfo.kind.label,
                    active = activePathInfo.kind != TavernProfilePathKind.TraditionalDefault,
                    modifier = Modifier.fillMaxWidth(),
                    toneColor = when (activePathInfo.kind) {
                        TavernProfilePathKind.LauncherManaged -> LukoaColors.Accent
                        TavernProfilePathKind.TraditionalDefault -> LukoaColors.Amber
                        TavernProfilePathKind.Custom -> LukoaColors.Info
                    },
                    activeBackground = when (activePathInfo.kind) {
                        TavernProfilePathKind.LauncherManaged -> LukoaColors.AccentSoft
                        TavernProfilePathKind.TraditionalDefault -> LukoaColors.AmberSoft
                        TavernProfilePathKind.Custom -> LukoaColors.InfoSoft
                    },
                )
                Text(
                    text = tavernPathConfig.displayTavernDir,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("主实例推荐默认目录是 ${TavernPathDefaults.DEFAULT_TAVERN_DIR}。")
                        append(" 分身实例会自动分配到各自的托管目录；传统 ~/SillyTavern 只作为旧目录兼容保留。")
                        if (activePathInfo.canDeleteDirectoryWithProfile) {
                            append(" 当前分身如果直接删除实例，会连同这个托管目录一起删掉。")
                        }
                    },
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
                if (tavernPortInput.isNotBlank() && tavernPortError != null) {
                    Text(
                        text = tavernPortError,
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
                    text = "启动页负责显示当前运行状态；这里主要放权限处理入口和必要提醒，避免把状态到处重复塞满。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                NoticeCard(
                    title = permissionNotice.title,
                    detail = permissionNotice.detail,
                    accentColor = when (permissionNotice.tone) {
                        PermissionNoticeTone.Info -> LukoaColors.Info
                        PermissionNoticeTone.Warning -> LukoaColors.Amber
                    },
                )
                SecondaryActionButton(
                    text = "查看并处理权限",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPermissionCenterDialog = true },
                )
            }

            SettingsPageView.Update -> SectionPanel(title = "应用更新", accentColor = LukoaColors.Accent) {
                Text(
                    text = "这里管理的是启动器更新，走 App 自己的 GitHub 更新流程，不是酒馆版本更新，也不是 Termux 里的更新。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                GithubUpdateStatusCard(githubUpdateState)
                githubReleaseNotesEntries(githubUpdateState).forEach { (title, updateInfo) ->
                    GithubReleaseNotesCard(
                        title = title,
                        updateInfo = updateInfo,
                    )
                }
                VersionInfoLine("当前仓库", githubUpdateState.repository.ifBlank { "未配置" })
                VersionInfoLine("更新通道", githubUpdateState.channel.label)
                githubUpdateState.latest?.let { latest ->
                    VersionInfoLine("最新类型", latest.releaseTypeLabel)
                }
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
                    text = "诊断日志适合用来排查问题。清除日志只会清空页面显示，后台归档会继续记录，不影响后续导出日志和诊断。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
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
    healthSummaryText: String,
    healthSummaryColor: Color,
    githubUpdateState: GithubUpdateUiState,
) {
    val updateStatusText = when {
        githubUpdateState.downloading -> "下载中"
        githubUpdateState.checking -> "检查中"
        githubUpdateState.hasUpdate -> "有新版本"
        githubUpdateState.latest != null -> "已是最新"
        else -> "未检查"
    }
    val updateSummaryText = "$updateStatusText · ${githubUpdateState.channel.label}"
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
                    label = "一键体检",
                    value = healthSummaryText,
                    accentColor = healthSummaryColor,
                    modifier = Modifier.weight(1f),
                )
                SettingsStatBlock(
                    label = "启动器更新",
                    value = updateSummaryText,
                    accentColor = updateStatusColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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

private fun settingsHealthSummaryColor(report: LauncherHealthReport?): Color {
    val effectiveReport = report?.takeIf { it.hasData }
    return when {
        effectiveReport == null -> LukoaColors.Muted
        effectiveReport.errorCount > 0 -> LukoaColors.Danger
        effectiveReport.warningCount > 0 -> LukoaColors.Amber
        else -> LukoaColors.Accent
    }
}
