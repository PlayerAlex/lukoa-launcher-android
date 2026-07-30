package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TavernProfileManagementDialog(
    tavernPathConfig: TavernPathConfig,
    currentPathInfo: TavernProfilePathInfo,
    actionsLocked: Boolean,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    onRemoveCurrentProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "实例管理",
                infoText = "一个实例就是一套单独的酒馆，它有自己的文件夹、端口和设置。\n切换实例后，启动器里的启动、版本、备份和用户管理都会操作你选中的那一套。\n如果你只有一套酒馆，保持当前实例即可。目录和端口可以回到设置页单独调整。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniInfoLine("当前实例", tavernPathConfig.activeProfileLabel)
                MiniInfoLine("实例数量", "${tavernPathConfig.availableProfiles.size}")
                MiniInfoLine("当前路径", tavernPathConfig.displayTavernDir)
                MiniInfoLine("当前端口", tavernPathConfig.normalizedPort.toString())
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
                            tone = if (profile.id == tavernPathConfig.activeProfile.id) {
                                ActionTone.Safe
                            } else {
                                ActionTone.Neutral
                            },
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
                        tone = ActionTone.Danger,
                        modifier = Modifier.weight(1f),
                        onClick = onRemoveCurrentProfile,
                    )
                }
                if (tavernPathConfig.hasMultipleProfiles && tavernPathConfig.isActiveProfileMain) {
                    Text(
                        text = "主实例默认保留。要删除分身，请先切换到对应分身。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (tavernPathConfig.canRemoveActiveProfile) {
                    Text(
                        text = if (currentPathInfo.canDeleteDirectoryWithProfile) {
                            "当前分身使用自己的托管默认目录。删除实例时会再次确认是否同时删除酒馆文件；备份库不会删除。"
                        } else {
                            "删除前会再次确认。当前只会移除启动器里的实例配置，不会删除酒馆目录和备份。"
                        },
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
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
internal fun TavernDirectorySettingsDialog(
    tavernPathConfig: TavernPathConfig,
    currentPathInfo: TavernProfilePathInfo,
    tavernPathInput: String,
    tavernPathError: String?,
    displayPathPreview: String,
    actionsLocked: Boolean,
    onPathChange: (String) -> Unit,
    onMigrateToManagedPath: () -> Unit,
    onMigrateToTraditionalPath: () -> Unit,
    onMigrateToCustomPath: () -> Unit,
    onSave: () -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "酒馆路径",
                infoText = "酒馆路径就是这套酒馆文件所在的文件夹。大多数人保持默认的 ~/SillyTavern 即可。\n只保存新路径不会自动搬动原来的文件。需要搬家时，请使用页面里的“迁移”按钮。\n“托管目录”由启动器安排位置；“自定义目录”只适合已经知道文件放在哪里的用户。",
            )
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
                    text = "“保存路径”只会修改启动器配置，不会搬动文件；迁移按钮才会真的移动酒馆目录。",
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                MiniInfoLine("当前路径类型", currentPathInfo.kind.label)
                MiniInfoLine("托管默认目录", currentPathInfo.launcherManagedDefaultDisplayPath)
                if (
                    currentPathInfo.canMigrateToTraditionalDefault ||
                    currentPathInfo.kind == TavernProfilePathKind.TraditionalDefault
                ) {
                    MiniInfoLine("传统默认目录", currentPathInfo.traditionalDefaultDisplayPath)
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
                MiniInfoLine("路径预览", displayPathPreview)
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
                        text = "恢复默认路径",
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
                if (
                    currentPathInfo.canMigrateToTraditionalDefault ||
                    currentPathInfo.kind == TavernProfilePathKind.TraditionalDefault
                ) {
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
internal fun TavernPortSettingsDialog(
    tavernPathConfig: TavernPathConfig,
    tavernPortInput: String,
    tavernPortError: String?,
    actionsLocked: Boolean,
    onPortChange: (String) -> Unit,
    onSave: () -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultPort = TavernProfileDefaults.profileForId(tavernPathConfig.activeProfile.id).normalizedPort
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "实例端口",
                infoText = "端口是浏览器连接这套酒馆时使用的编号。只有提示端口冲突，或你有多套酒馆时才需要修改。\n每套实例必须使用不同端口；不知道填什么时使用“恢复默认”。修改端口不会移动酒馆文件。",
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniInfoLine("当前已保存", tavernPathConfig.normalizedPort.toString())
                MiniInfoLine("此实例默认端口", defaultPort.toString())
                OutlinedTextField(
                    value = tavernPortInput,
                    onValueChange = onPortChange,
                    enabled = !actionsLocked,
                    singleLine = true,
                    label = { Text("酒馆端口") },
                    placeholder = { Text(defaultPort.toString()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = lukoaTextFieldColors(LukoaColors.Primary),
                )
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
                        text = "保存端口",
                        enabled = !actionsLocked && tavernPortError == null,
                        modifier = Modifier.weight(1f),
                        onClick = onSave,
                    )
                    DialogActionButton(
                        text = "恢复默认端口",
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
internal fun LauncherRepositorySettingsDialog(
    repositoryInput: String,
    githubUpdateState: GithubUpdateUiState,
    onRepositoryInputChange: (String) -> Unit,
    onSaveRepository: () -> Unit,
    onRestoreDefaultRepository: () -> Unit,
    onDismiss: () -> Unit,
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "更新仓库",
                infoText = "这里决定露科亚启动器从哪个 GitHub 项目检查和下载自己的更新，不是酒馆的下载地址。\n大多数用户不需要修改。只有你明确要使用其他人发布的启动器版本时，才填写对方的“用户名/仓库名”。\n地址填错时只会检查不到启动器更新，不会改动 SillyTavern。",
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
internal fun LauncherUpdateChannelDialog(
    githubUpdateState: GithubUpdateUiState,
    onSaveUpdateChannel: (GithubReleaseChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    val updateLocked = githubUpdateState.checking || githubUpdateState.downloading
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "更新通道",
                infoText = "稳定版只接收正式更新，适合大多数用户。\n测试版会更早收到新功能，也可能遇到还没修好的问题。只有愿意帮忙体验和反馈时再选择。\n切换后只会重新检查启动器更新，不会修改酒馆版本或更新仓库。",
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
