package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernServerPluginSection(
    state: TavernServerPluginState,
    instanceLabel: String,
    actionsLocked: Boolean,
    onRefresh: () -> Unit,
    onInstall: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenTavernConfig: () -> Unit,
    onShowHint: (String) -> Unit,
) {
    val snapshot = state.snapshot
    var showInstallDialog by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    var repoInput by rememberSaveable(instanceLabel) { mutableStateOf("") }
    var pendingDelete by remember(instanceLabel) { mutableStateOf<TavernServerPluginRecord?>(null) }
    var pendingUpdate by remember(instanceLabel) { mutableStateOf<TavernServerPluginRecord?>(null) }
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val repoError = repoInput.takeIf { it.isNotBlank() }?.let(TavernServerPluginCommandCodec::validateRepositoryUrl)

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("安装后端插件") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "粘贴插件作者提供的公开网址（以 https:// 开头）。启动器会自动下载并装好所需文件，不会覆盖已有同名插件。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LukoaTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = "插件网址",
                        placeholder = "https://github.com/owner/plugin-repo",
                    )
                    if (repoError != null) {
                        Text(
                            text = repoError,
                            color = LukoaColors.Danger,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "开始安装",
                    enabled = repoInput.isNotBlank() && repoError == null,
                    tone = ActionTone.Safe,
                    onClick = {
                        val url = repoInput.trim()
                        showInstallDialog = false
                        repoInput = ""
                        onInstall(url)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { showInstallDialog = false },
                )
            },
        )
    }

    pendingUpdate?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pendingUpdate = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("更新后端插件") },
            text = {
                Text(
                    text = "会拉取「${plugin.displayName}」的最新版本并同步所需文件，更新后重启酒馆生效。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认更新",
                    tone = ActionTone.Safe,
                    onClick = {
                        pendingUpdate = null
                        onUpdate(plugin.directoryName)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { pendingUpdate = null },
                )
            },
        )
    }

    pendingDelete?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("删除后端插件") },
            text = {
                Text(
                    text = "将移除插件「${plugin.displayName}」，不会影响聊天记录、角色卡或备份。删除后重启酒馆生效。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认删除",
                    tone = ActionTone.Danger,
                    onClick = {
                        pendingDelete = null
                        onDelete(plugin.directoryName)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { pendingDelete = null },
                )
            },
        )
    }

    SectionPanel(
        title = "服务器插件",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = when {
                        state.loading -> "读取中"
                        snapshot == null -> "未读取"
                        else -> "${snapshot.plugins.size} 个"
                    },
                    active = state.loading || snapshot != null,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看服务器插件说明",
                    title = "什么时候需要用到这里？",
                    body = "平时一般用不到这里，普通酒馆网页扩展请去「扩展管理」安装。\n只有少数插件（如需要调用本机程序或额外后端接口）明确提醒要装「后端 / 服务器插件」时，才来这里粘贴网址安装，并顺手在酒馆设置里打开后端插件开关。",
                )
            }
        },
    ) {
        if (state.loading && snapshot == null) {
            LukoaInlineLoadingCard("正在自动读取当前实例的服务器插件…")
        }
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例",
                detail = state.message,
                detailMaxLines = Int.MAX_VALUE,
                value = instanceLabel,
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
            )
            if (snapshot != null) {
                SettingsEntryDivider()
                SettingsEntryRow(
                    title = "后端插件总开关",
                    detail = if (snapshot.enabledInConfig) {
                        "已开启，重启酒馆后会自动加载下方插件。"
                    } else {
                        "当前未开启，点此前往「酒馆设置」开启后插件才会生效。"
                    },
                    detailMaxLines = 3,
                    value = if (snapshot.enabledInConfig) "已开启" else "未开启",
                    valueColor = if (snapshot.enabledInConfig) LukoaColors.Primary else LukoaColors.TextSecondary,
                    valueAsPill = true,
                    onClick = onOpenTavernConfig,
                )
            }
        }

        if (snapshot != null && !snapshot.enabledInConfig && snapshot.plugins.isNotEmpty()) {
            NoticeCard(
                title = "还差一步：打开后端插件开关",
                detail = "你已经装了后端插件，但酒馆总开关还没开。点下面按钮去「酒馆设置」的更多进阶设置里打开开关，再重启酒馆就能用。",
                accentColor = LukoaColors.Primary,
                actionLabel = "去酒馆设置开启",
                actionTone = ActionTone.Safe,
                onAction = onOpenTavernConfig,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "刷新列表",
                enabled = !actionsLocked && !state.loading,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                unavailableHint = if (state.loading) "正在读取插件列表，请稍等。" else lockedHint,
                onShowHint = onShowHint,
                onClick = onRefresh,
            )
            SettingsFeedbackActionButton(
                text = "安装新插件",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onClick = { showInstallDialog = true },
            )
        }

        val plugins = snapshot?.plugins.orEmpty()
        if (plugins.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                plugins.forEach { plugin ->
                    SettingsEntryGroup {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = plugin.displayName,
                                    modifier = Modifier.weight(1f),
                                    color = LukoaColors.TextPrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = formatStorageKilobytes(plugin.directoryKilobytes),
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                text = buildList {
                                    add("文件夹：${plugin.directoryName}")
                                    if (plugin.version.isNotBlank()) add("版本 ${plugin.version}")
                                }.joinToString(" · "),
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (plugin.description.isNotBlank()) {
                                Text(
                                    text = plugin.description,
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SettingsFeedbackActionButton(
                                    text = "更新",
                                    enabled = !actionsLocked && plugin.isGitRepository,
                                    accentColor = LukoaColors.Primary,
                                    modifier = Modifier.weight(1f),
                                    unavailableHint = lockedHint ?: "这个插件是手动放进去的，暂不支持一键更新。",
                                    onShowHint = onShowHint,
                                    onClick = { pendingUpdate = plugin },
                                )
                                SettingsFeedbackActionButton(
                                    text = "删除",
                                    enabled = !actionsLocked,
                                    accentColor = LukoaColors.Danger,
                                    modifier = Modifier.weight(1f),
                                    unavailableHint = lockedHint,
                                    onShowHint = onShowHint,
                                    onClick = { pendingDelete = plugin },
                                )
                            }
                        }
                    }
                }
            }
        } else if (snapshot != null && !state.loading) {
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "还没有安装后端插件",
                    detail = "不需要的话保持空着即可；如果某个扩展提示需要后端插件，点上方「安装新插件」。",
                    detailMaxLines = 3,
                )
            }
        }
    }
}
