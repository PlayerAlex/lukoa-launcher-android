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
internal fun UpdateChannelSelectorCard(
    channel: GithubReleaseChannel,
    enabled: Boolean,
    onSelectChannel: (GithubReleaseChannel) -> Unit,
) {
    val channelColor = LukoaColors.Primary
    val channelBackground = LukoaColors.PrimarySoft

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border.copy(alpha = 0.4f)),
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
                    color = LukoaColors.TextSecondary,
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
                    tone = if (channel == GithubReleaseChannel.Test) ActionTone.Neutral else ActionTone.Safe,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectChannel(GithubReleaseChannel.Test) },
                )
            }
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
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "Termux 唤醒返回",
                infoText = "启动器打开 Termux 执行操作后，会自动切回启动器。这里决定切回前要等多久。\n大多数手机保持默认值即可。如果经常打开 Termux 后没有开始执行命令，可以把等待时间调长一点。\n它只影响切回速度，不会让安装或备份本身变快。",
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BackupStepper(
                    label = "返回等待",
                    value = "${"%.1f".format(termuxReturnDelayMs / 1000f)} 秒",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
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
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "权限与授权",
                infoText = "这里检查启动器控制 Termux、后台运行和读写备份所需的系统权限。\n显示“已准备”的项目不用处理；显示“待处理”时，点对应按钮，再按手机页面的提示完成授权。\n启动器只会带你前往系统设置，是否允许仍由你确认。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!termuxInstalled) {
                    Text(
                        text = "你还没装 Termux。先装好 Termux，再回来处理下面这些权限。",
                        color = LukoaColors.Accent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatusPill(
                        text = readinessText,
                        active = readinessActive,
                        modifier = Modifier.weight(1f),
                        toneColor = if (readinessActive) LukoaColors.Primary else LukoaColors.Accent,
                        activeBackground = if (readinessActive) LukoaColors.PrimarySoft else LukoaColors.AccentSoft,
                    )
                    StatusPill(
                        text = if (termuxStoragePermissionBlocked) "Termux 存储待处理" else "Termux 存储按需申请",
                        active = !termuxStoragePermissionBlocked,
                        modifier = Modifier.weight(1f),
                        toneColor = if (termuxStoragePermissionBlocked) LukoaColors.Accent else LukoaColors.TextSecondary,
                        activeBackground = if (termuxStoragePermissionBlocked) LukoaColors.AccentSoft else LukoaColors.Elevated,
                    )
                }
                PermissionDetailCard(
                    title = "RUN_COMMAND 权限",
                    active = runCommandPermissionGranted,
                    description = "允许启动器把启动、停止、安装等操作交给 Termux 执行。没有它，这些按钮会被系统拦住。",
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
                    description = "允许 Termux 接收启动器发来的操作。没有开启时，Termux 会拒绝启动器的请求。",
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
                    description = "让启动器切到后台后仍能完成自动备份和耗时操作。未允许时，任务可能暂停，需要回到启动器才能继续。",
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
                    description = "让 Termux 在屏幕关闭或切到其他应用后继续工作。未允许时，长任务和自动备份可能被手机中断。",
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
                    description = "让启动器读取和复制你选择的备份文件。未允许时，文件管理器可以打开，但导入或导出可能失败。",
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
                    description = "只在安装启动器更新包时使用。未允许时，可以检查到新版本，但系统不会让你安装。",
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
                    description = "让 Termux 读取手机 Download 文件夹中的备份。它只在应用备份时需要，并且要在 Termux 里授权。",
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
        active -> LukoaColors.Primary
        tone == ActionTone.Warning -> LukoaColors.Accent
        tone == ActionTone.Danger -> LukoaColors.Danger
        else -> LukoaColors.Primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = if (active) "已准备" else "待处理",
                    active = active,
                    toneColor = if (active) LukoaColors.Primary else LukoaColors.Accent,
                    activeBackground = if (active) LukoaColors.PrimarySoft else LukoaColors.AccentSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看$title 说明",
                    title = title,
                    body = description,
                )
            }
            if (!active) {
                Text(
                    text = detail,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
internal fun MiniInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 12.dp),
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun lukoaTextFieldColors(
    accentColor: Color = LukoaColors.Primary,
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LukoaColors.TextPrimary,
    unfocusedTextColor = LukoaColors.TextPrimary,
    disabledTextColor = LukoaColors.Dim,
    focusedContainerColor = LukoaColors.Elevated,
    unfocusedContainerColor = LukoaColors.Elevated,
    disabledContainerColor = LukoaColors.Surface,
    focusedBorderColor = accentColor,
    unfocusedBorderColor = LukoaColors.Border,
    disabledBorderColor = LukoaColors.Border,
    focusedLabelColor = accentColor,
    unfocusedLabelColor = LukoaColors.TextSecondary,
    cursorColor = accentColor,
)
