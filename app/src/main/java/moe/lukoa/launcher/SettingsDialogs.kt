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
    tavernPathInput: String,
    tavernPathError: String?,
    displayPathPreview: String,
    actionsLocked: Boolean,
    onValueChange: (String) -> Unit,
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
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "默认目录是 ~/SillyTavern。只有你自己改过文件夹名，或者酒馆不在默认目录时，才需要改这里。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = tavernPathInput,
                    onValueChange = onValueChange,
                    enabled = !actionsLocked,
                    singleLine = true,
                    label = { Text("酒馆目录路径") },
                    placeholder = { Text("~/SillyTavern2") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lukoaTextFieldColors(),
                )
                MiniInfoLine("当前预览", displayPathPreview)
                tavernPathError?.let { error ->
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
                        text = "保存路径",
                        enabled = !actionsLocked && tavernPathError == null,
                        modifier = Modifier.weight(1f),
                        onClick = onSave,
                    )
                    DialogActionButton(
                        text = "恢复默认",
                        enabled = !actionsLocked,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onRestoreDefault,
                    )
                }
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
    actionsLocked: Boolean,
    onRepositoryInputChange: (String) -> Unit,
    onSaveRepository: () -> Unit,
    onRestoreDefaultRepository: () -> Unit,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: () -> Unit,
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
                    text = "这里只管露科亚启动器 APK 更新。酒馆版本的安装、更新、回退还在版本页处理。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                UpdateStatusSummary(githubUpdateState = githubUpdateState)
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
                DialogActionButton(
                    text = when {
                        githubUpdateState.checking -> "检查中..."
                        githubUpdateState.downloading -> "下载中..."
                        else -> "检查更新"
                    },
                    enabled = !updateLocked,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCheckUpdate,
                )
                if (githubUpdateState.hasUpdate || githubUpdateState.latest?.releaseUrl?.isNotBlank() == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (githubUpdateState.hasUpdate) {
                            DialogActionButton(
                                text = "查看新版",
                                enabled = !updateLocked,
                                modifier = Modifier.weight(1f),
                                onClick = onInstallUpdate,
                            )
                        }
                        if (githubUpdateState.latest?.releaseUrl?.isNotBlank() == true) {
                            DialogActionButton(
                                text = "打开发布页",
                                enabled = !updateLocked,
                                tone = ActionTone.Neutral,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenRelease,
                            )
                        }
                    }
                }
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
    allFilesAccessGranted: Boolean,
    installUnknownAppsGranted: Boolean,
    termuxStoragePermissionBlocked: Boolean,
    onRequestRunCommandPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCopyExternalAppsCommand: () -> Unit,
    onOpenTermuxOnly: () -> Unit,
    onRequestBackgroundRunPermission: () -> Unit,
    onOpenAllFilesAccessSettings: () -> Unit,
    onOpenUnknownAppSourcesSettings: () -> Unit,
    onShowTermuxStoragePermissionGuide: () -> Unit,
    onDismiss: () -> Unit,
) {
    val readyCount = listOf(
        runCommandPermissionGranted,
        termuxExternalAppsReady,
        backgroundRunPermissionGranted,
        allFilesAccessGranted,
        installUnknownAppsGranted,
    ).count { it }
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
                        text = "$readyCount/5 已就绪",
                        active = readyCount >= 4,
                        modifier = Modifier.weight(1f),
                        toneColor = if (readyCount >= 4) LukoaColors.Accent else LukoaColors.Amber,
                        activeBackground = if (readyCount >= 4) LukoaColors.AccentSoft else LukoaColors.AmberSoft,
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
private fun UpdateStatusSummary(
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "当前状态",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = statusText,
                    active = githubUpdateState.hasUpdate || githubUpdateState.downloading || githubUpdateState.checking,
                    toneColor = statusColor,
                    activeBackground = when (statusColor) {
                        LukoaColors.Accent -> LukoaColors.AccentSoft
                        LukoaColors.Amber -> LukoaColors.AmberSoft
                        else -> LukoaColors.SurfaceAlt
                    },
                )
            }
            Text(
                text = githubUpdateState.message,
                color = if (githubUpdateState.hasUpdate) LukoaColors.Text else LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            githubUpdateState.latest?.let { latest ->
                MiniInfoLine("GitHub 最新", "v${latest.versionName}")
            }
            githubUpdateState.lastCheckedText
                .takeIf { it.isNotBlank() }
                ?.let { checkedText ->
                    MiniInfoLine("上次检查", checkedText)
                }
        }
    }
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
