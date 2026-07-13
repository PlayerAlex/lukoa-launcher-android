package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TavernPathSettingsDialog(
    tavernPathConfig: TavernPathConfig,
    currentPathInfo: TavernProfilePathInfo,
    tavernPathInput: String,
    tavernPortInput: String,
    tavernPathError: String?,
    tavernPortError: String?,
    displayPathPreview: String,
    actionsLocked: Boolean,
    onPathChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    onRemoveCurrentProfile: () -> Unit,
    onMigrateToManagedPath: () -> Unit,
    onMigrateToTraditionalPath: () -> Unit,
    onMigrateToCustomPath: () -> Unit,
    onSave: () -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("酒馆路径设置")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "这里开始改成按实例管理。当前激活实例的路径和端口会直接影响启动、停止、体检、安装和打开网页。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "“保存当前实例”只会改启动器配置，不会搬动现有文件；下面的迁移按钮才会真的移动酒馆目录。",
                    color = LukoaColors.Amber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                MiniInfoLine("当前实例", tavernPathConfig.activeProfileLabel)
                MiniInfoLine("实例数量", "${tavernPathConfig.availableProfiles.size}")
                MiniInfoLine("当前路径类型", currentPathInfo.kind.label)
                MiniInfoLine("托管默认目录", currentPathInfo.launcherManagedDefaultDisplayPath)
                if (currentPathInfo.canMigrateToTraditionalDefault || currentPathInfo.kind == TavernProfilePathKind.TraditionalDefault) {
                    MiniInfoLine("传统默认目录", currentPathInfo.traditionalDefaultDisplayPath)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tavernPathConfig.availableProfiles.forEach { profile ->
                        DialogActionButton(
                            text = buildString {
                                append(profile.normalizedName)
                                append(" · ")
                                append(profile.displayTavernDir)
                                append(" · ")
                                append(profile.normalizedPort)
                                if (profile.id == tavernPathConfig.activeProfile.id) {
                                    append(" · 当前")
                                }
                            },
                            enabled = !actionsLocked && profile.id != tavernPathConfig.activeProfile.id,
                            tone = if (profile.id == tavernPathConfig.activeProfile.id) ActionTone.Safe else ActionTone.Neutral,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelectProfile(profile.id) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogActionButton(
                        text = "新建分身",
                        enabled = !actionsLocked,
                        modifier = Modifier.weight(1f),
                        onClick = onAddProfile,
                    )
                    DialogActionButton(
                        text = "删除当前实例",
                        enabled = !actionsLocked && tavernPathConfig.canRemoveActiveProfile,
                        tone = ActionTone.Warning,
                        modifier = Modifier.weight(1f),
                        onClick = onRemoveCurrentProfile,
                    )
                }
                if (tavernPathConfig.hasMultipleProfiles && tavernPathConfig.isActiveProfileMain) {
                    Text(
                        text = "主实例默认保留，当前只能删除分身实例。要删除分身，请先切换到对应分身后再删。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (tavernPathConfig.canRemoveActiveProfile) {
                    Text(
                        text = if (currentPathInfo.canDeleteDirectoryWithProfile) {
                            "当前分身正在使用它自己的托管默认目录。删除这个实例时，会连同这个目录里的酒馆文件一起删掉；备份库不会删。"
                        } else {
                            "删除实例前会再确认一次。当前只会移除启动器里的实例配置，不会删除酒馆目录和备份。"
                        },
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedTextField(
                    value = tavernPathInput,
                    onValueChange = onPathChange,
                    enabled = !actionsLocked,
                    singleLine = true,
                    label = { Text("酒馆目录路径") },
                    placeholder = { Text(currentPathInfo.launcherManagedDefaultDisplayPath) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lukoaTextFieldColors(),
                )
                OutlinedTextField(
                    value = tavernPortInput,
                    onValueChange = onPortChange,
                    enabled = !actionsLocked,
                    singleLine = true,
                    label = { Text("酒馆端口") },
                    placeholder = { Text("8001") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lukoaTextFieldColors(),
                )
                MiniInfoLine("当前预览", displayPathPreview)
                MiniInfoLine("当前端口", tavernPortInput.ifBlank { "未填写" })
                Text(
                    text = when (currentPathInfo.kind) {
                        TavernProfilePathKind.LauncherManaged -> {
                            "当前实例正在使用启动器托管目录。后续从启动器安装酒馆时，默认也会装到这类目录。"
                        }

                        TavernProfilePathKind.TraditionalDefault -> {
                            "当前实例还在用传统默认目录 ~/SillyTavern。这个位置会继续兼容旧习惯，但新实例更推荐迁到托管目录。"
                        }

                        TavernProfilePathKind.Custom -> {
                            "当前实例正在使用自定义目录。删除实例时不会自动删除这个目录，后续路径识别也会更依赖你自己确认。"
                        }
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                tavernPathError?.let { error ->
                    Text(
                        text = error,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                tavernPortError?.let { error ->
                    Text(
                        text = error,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogActionButton(
                        text = "保存当前实例",
                        enabled = !actionsLocked && tavernPathError == null && tavernPortError == null,
                        modifier = Modifier.weight(1f),
                        onClick = onSave,
                    )
                    DialogActionButton(
                        text = "恢复当前默认",
                        enabled = !actionsLocked,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onRestoreDefault,
                    )
                }
                DialogActionButton(
                    text = if (currentPathInfo.kind == TavernProfilePathKind.LauncherManaged) {
                        "当前已在托管目录"
                    } else {
                        "迁移到托管默认目录"
                    },
                    enabled = !actionsLocked && currentPathInfo.kind != TavernProfilePathKind.LauncherManaged,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMigrateToManagedPath,
                )
                if (currentPathInfo.canMigrateToTraditionalDefault || currentPathInfo.kind == TavernProfilePathKind.TraditionalDefault) {
                    DialogActionButton(
                        text = if (currentPathInfo.kind == TavernProfilePathKind.TraditionalDefault) {
                            "当前已在传统默认目录"
                        } else {
                            "迁移到传统默认目录"
                        },
                        enabled = !actionsLocked && currentPathInfo.kind != TavernProfilePathKind.TraditionalDefault,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onMigrateToTraditionalPath,
                    )
                }
                DialogActionButton(
                    text = "迁移到自定义地址",
                    enabled = !actionsLocked,
                    tone = ActionTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMigrateToCustomPath,
                )
                DialogActionButton(
                    text = "关闭",
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
fun LauncherUpdateSettingsDialog(
    repositoryInput: String,
    githubUpdateState: GithubUpdateUiState,
    onRepositoryInputChange: (String) -> Unit,
    onSaveRepository: () -> Unit,
    onRestoreDefaultRepository: () -> Unit,
    onSaveUpdateChannel: (GithubReleaseChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("启动器更新设置")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "这里设置启动器更新使用的 GitHub 仓库和更新通道。酒馆版本仍在版本页管理。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = repositoryInput,
                    onValueChange = onRepositoryInputChange,
                    enabled = !updateLocked,
                    singleLine = true,
                    label = { Text("GitHub 仓库") },
                    placeholder = { Text("用户名/仓库名") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lukoaTextFieldColors(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DialogActionButton(
                        text = "保存仓库",
                        enabled = !updateLocked,
                        modifier = Modifier.weight(1f),
                        onClick = onSaveRepository,
                    )
                    DialogActionButton(
                        text = "恢复默认",
                        enabled = !updateLocked,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onRestoreDefaultRepository,
                    )
                }
                UpdateChannelSelectorCard(
                    channel = githubUpdateState.channel,
                    enabled = !updateLocked,
                    onSelectChannel = onSaveUpdateChannel,
                )
                DialogActionButton(
                    text = "关闭",
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
private fun UpdateChannelSelectorCard(
    channel: GithubReleaseChannel,
    enabled: Boolean,
    onSelectChannel: (GithubReleaseChannel) -> Unit,
) {
    val channelColor = if (channel == GithubReleaseChannel.Test) LukoaColors.Amber else LukoaColors.Accent
    val channelBackground = if (channel == GithubReleaseChannel.Test) LukoaColors.AmberSoft else LukoaColors.AccentSoft

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "更新通道",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = channel.label,
                    active = true,
                    toneColor = channelColor,
                    activeBackground = channelBackground,
                )
            }
            Text(
                text = channel.description,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = if (channel == GithubReleaseChannel.Test) {
                    "测试版会包含 GitHub pre-release。遇到问题时，随时切回稳定版即可。"
                } else {
                    "稳定版只接收正式 Release，适合大多数人。"
                },
                color = if (channel == GithubReleaseChannel.Test) LukoaColors.Amber else LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogActionButton(
                    text = GithubReleaseChannel.Stable.label,
                    enabled = enabled && channel != GithubReleaseChannel.Stable,
                    tone = if (channel == GithubReleaseChannel.Stable) ActionTone.Neutral else ActionTone.Safe,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectChannel(GithubReleaseChannel.Stable) },
                )
                DialogActionButton(
                    text = GithubReleaseChannel.Test.label,
                    enabled = enabled && channel != GithubReleaseChannel.Test,
                    tone = if (channel == GithubReleaseChannel.Test) ActionTone.Neutral else ActionTone.Warning,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectChannel(GithubReleaseChannel.Test) },
                )
            }
            Text(
                text = "切换后会立刻重新检查，并清掉旧通道里已忽略的版本红点。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun TermuxWakeDelayDialog(
    termuxReturnDelayMs: Long,
    actionsLocked: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("Termux 唤醒返回")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "这里只管从启动器跳去 Termux 后，要等多久再自动切回来。时间太短时，某些手机可能还没来得及唤醒完成。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                BackupStepper(
                    label = "返回等待",
                    value = "${"%.1f".format(termuxReturnDelayMs / 1000f)} 秒",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "完成",
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss,
            )
        },
        dismissButton = {},
    )
}

@Composable
fun PermissionCenterDialog(
    termuxInstalled: Boolean,
    runCommandPermissionGranted: Boolean,
    termuxExternalAppsReady: Boolean,
    backgroundRunPermissionGranted: Boolean,
    termuxBackgroundRunPermissionGranted: Boolean,
    allFilesAccessGranted: Boolean,
    installUnknownAppsGranted: Boolean,
    termuxStoragePermissionBlocked: Boolean,
    onRequestRunCommandPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCopyExternalAppsCommand: () -> Unit,
    onOpenTermuxOnly: () -> Unit,
    onRequestBackgroundRunPermission: () -> Unit,
    onRequestTermuxBackgroundRunPermission: () -> Unit,
    onOpenAllFilesAccessSettings: () -> Unit,
    onOpenUnknownAppSourcesSettings: () -> Unit,
    onShowTermuxStoragePermissionGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val readyCount = listOf(
        runCommandPermissionGranted,
        termuxExternalAppsReady,
        backgroundRunPermissionGranted,
        termuxBackgroundRunPermissionGranted,
        allFilesAccessGranted,
        installUnknownAppsGranted,
    ).count { it }
    val readinessText = if (termuxInstalled) {
        "$readyCount/6 已就绪"
    } else {
        "先安装 Termux"
    }
    val readinessActive = termuxInstalled && readyCount >= 5
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("权限与授权")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (termuxInstalled) {
                        "这里把启动器会用到的权限都集中写清楚了。看不懂时，优先把没准备好的项目逐个补齐。"
                    } else {
                        "你还没装 Termux。先装好 Termux，再回来处理下面这些权限。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatusPill(
                        text = readinessText,
                        active = readinessActive,
                        modifier = Modifier.weight(1f),
                        toneColor = if (readinessActive) LukoaColors.Accent else LukoaColors.Amber,
                        activeBackground = if (readinessActive) LukoaColors.AccentSoft else LukoaColors.AmberSoft,
                    )
                    StatusPill(
                        text = if (termuxStoragePermissionBlocked) "Termux 存储待处理" else "Termux 存储按需申请",
                        active = !termuxStoragePermissionBlocked,
                        modifier = Modifier.weight(1f),
                        toneColor = if (termuxStoragePermissionBlocked) LukoaColors.Amber else LukoaColors.Muted,
                        activeBackground = if (termuxStoragePermissionBlocked) LukoaColors.AmberSoft else LukoaColors.SurfaceAlt,
                    )
                }
                PermissionDetailCard(
                    title = "RUN_COMMAND 权限",
                    active = runCommandPermissionGranted,
                    description = "让启动器能把命令发给 Termux。没开这个，启动、停止、安装这些按钮都不会真的跑进 Termux。",
                    detail = if (runCommandPermissionGranted) {
                        "当前已允许。按钮发出的命令可以正常尝试进入 Termux。"
                    } else {
                        "当前还没允许。优先点“请求权限”，如果系统没弹窗，再点“权限设置”。"
                    },
                    primaryLabel = "请求权限",
                    onPrimaryClick = onRequestRunCommandPermission,
                    secondaryLabel = "权限设置",
                    onSecondaryClick = onOpenPermissionSettings,
                )
                PermissionDetailCard(
                    title = "Termux 外部调用",
                    active = termuxExternalAppsReady,
                    description = "让 Termux 接受来自启动器的外部命令。没开这个，Termux 会直接拒绝调用。",
                    detail = if (termuxExternalAppsReady) {
                        "当前已允许外部调用。"
                    } else {
                        "先复制命令，再打开 Termux 粘贴执行一次。执行完回启动器重新检测。"
                    },
                    primaryLabel = "复制命令",
                    onPrimaryClick = onCopyExternalAppsCommand,
                    secondaryLabel = "打开 Termux",
                    onSecondaryClick = onOpenTermuxOnly,
                )
                PermissionDetailCard(
                    title = "后台运行权限",
                    active = backgroundRunPermissionGranted,
                    description = "主要影响自动备份和长任务。没放行时，切后台后可能要你重新回到启动器，它才继续跑。",
                    detail = if (backgroundRunPermissionGranted) {
                        "当前系统已放行后台运行。"
                    } else {
                        "建议允许，尤其是你想让自动备份自己到点执行的时候。"
                    },
                    primaryLabel = if (backgroundRunPermissionGranted) "重新打开权限页" else "去授权",
                    onPrimaryClick = onRequestBackgroundRunPermission,
                )
                PermissionDetailCard(
                    title = "Termux 后台常驻",
                    active = termuxBackgroundRunPermissionGranted,
                    description = "部分手机会单独限制 Termux 的后台存活。没放行时，长任务、前台日志和自动备份更容易被系统打断。",
                    detail = if (termuxBackgroundRunPermissionGranted) {
                        "当前已检测到 Termux 基本不受省电限制。"
                    } else {
                        "建议把 Termux 也加入后台运行、自启动或省电白名单。"
                    },
                    primaryLabel = if (termuxBackgroundRunPermissionGranted) "重新打开权限页" else "给 Termux 授权",
                    onPrimaryClick = onRequestTermuxBackgroundRunPermission,
                )
                PermissionDetailCard(
                    title = "文件管理权限",
                    active = allFilesAccessGranted,
                    description = "导入备份、导出备份、复制备份时会用到。没开这个，文件管理器虽然能弹出来，但真正复制可能失败。",
                    detail = if (allFilesAccessGranted) {
                        "当前已允许。"
                    } else {
                        "去系统里允许“管理所有文件”后，再回来重试导入或导出。"
                    },
                    primaryLabel = "打开文件权限",
                    onPrimaryClick = onOpenAllFilesAccessSettings,
                )
                PermissionDetailCard(
                    title = "安装未知来源应用",
                    active = installUnknownAppsGranted,
                    description = "只在更新启动器 APK 时会用到。没开这个，你能检测到新版本，但安装步骤过不去。",
                    detail = if (installUnknownAppsGranted) {
                        "当前已允许安装启动器新版本。"
                    } else {
                        "更新启动器前先放行一次即可。"
                    },
                    primaryLabel = "打开安装权限",
                    onPrimaryClick = onOpenUnknownAppSourcesSettings,
                )
                PermissionDetailCard(
                    title = "Termux 存储权限",
                    active = !termuxStoragePermissionBlocked,
                    description = "应用备份到酒馆时，Termux 需要能读到 Download 里的备份文件。这个权限不是给启动器，是给 Termux。",
                    detail = if (termuxStoragePermissionBlocked) {
                        "最近一次检测到 Termux 存储权限缺失。点下面的引导去 Termux 授权。"
                    } else {
                        "这项通常只在你第一次应用备份时才会弹出来。"
                    },
                    primaryLabel = "查看引导",
                    onPrimaryClick = onShowTermuxStoragePermissionGuide,
                    tone = if (termuxStoragePermissionBlocked) ActionTone.Warning else ActionTone.Neutral,
                )
                DialogActionButton(
                    text = "关闭",
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
private fun PermissionDetailCard(
    title: String,
    active: Boolean,
    description: String,
    detail: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    tone: ActionTone = if (active) ActionTone.Neutral else ActionTone.Warning,
) {
    val accentColor = when {
        active -> LukoaColors.Accent
        tone == ActionTone.Warning -> LukoaColors.Amber
        tone == ActionTone.Danger -> LukoaColors.Danger
        else -> LukoaColors.Info
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = if (active) "已准备" else "待处理",
                    active = active,
                    toneColor = if (active) LukoaColors.Accent else LukoaColors.Amber,
                    activeBackground = if (active) LukoaColors.AccentSoft else LukoaColors.AmberSoft,
                )
            }
            Text(
                text = description,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = detail,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogActionButton(
                    text = primaryLabel,
                    enabled = true,
                    tone = tone,
                    modifier = Modifier.weight(1f),
                    onClick = onPrimaryClick,
                )
                if (secondaryLabel != null && onSecondaryClick != null) {
                    DialogActionButton(
                        text = secondaryLabel,
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onSecondaryClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniInfoLine(label: String, value: String) {
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
            modifier = Modifier.padding(start = 12.dp),
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun lukoaTextFieldColors(
    accentColor: Color = LukoaColors.Accent,
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LukoaColors.Text,
    unfocusedTextColor = LukoaColors.Text,
    disabledTextColor = LukoaColors.Dim,
    focusedContainerColor = LukoaColors.SurfaceAlt,
    unfocusedContainerColor = LukoaColors.SurfaceAlt,
    disabledContainerColor = LukoaColors.Surface,
    focusedBorderColor = accentColor,
    unfocusedBorderColor = LukoaColors.Line,
    disabledBorderColor = LukoaColors.Line,
    focusedLabelColor = accentColor,
    unfocusedLabelColor = LukoaColors.Muted,
    cursorColor = accentColor,
)
