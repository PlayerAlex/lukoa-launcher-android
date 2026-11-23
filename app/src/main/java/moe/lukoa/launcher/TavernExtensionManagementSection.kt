package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun TavernExtensionManagementSection(
    state: TavernExtensionManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onInstall: (String) -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    onUpdate: (String) -> Unit = {},
    onUpdateAll: () -> Unit = {},
    onRollback: (String) -> Unit = {},
    onCopyPath: (String) -> Boolean = { false },
    onShowHint: (String) -> Unit = {},
    showSectionContainer: Boolean = true,
) {
    var pendingDelete by remember(instanceLabel, state.rootDirectory) {
        mutableStateOf<TavernExtensionRecord?>(null)
    }
    var pendingToggle by remember(instanceLabel, state.rootDirectory, state.disabledRootDirectory) {
        mutableStateOf<TavernExtensionRecord?>(null)
    }
    var pendingUpdate by remember(instanceLabel, state.rootDirectory, state.disabledRootDirectory) {
        mutableStateOf<TavernExtensionRecord?>(null)
    }
    var pendingRollback by remember(instanceLabel, state.rootDirectory, state.disabledRootDirectory) {
        mutableStateOf<TavernExtensionRecord?>(null)
    }
    var showInstallDialog by rememberSaveable(instanceLabel, state.rootDirectory) { mutableStateOf(false) }
    var showUpdateAllDialog by rememberSaveable(instanceLabel, state.rootDirectory) { mutableStateOf(false) }
    var repositoryInput by rememberSaveable(instanceLabel, state.rootDirectory) { mutableStateOf("") }
    var searchQuery by rememberSaveable(instanceLabel, state.rootDirectory) { mutableStateOf("") }
    val updatableCount = state.extensions.count { it.updateStatus == TavernExtensionUpdateStatus.UpdateAvailable }
    val localChangesCount = state.extensions.count { it.updateStatus == TavernExtensionUpdateStatus.LocalChanges }
    val normalizedQuery = searchQuery.trim()
    val visibleExtensions = if (normalizedQuery.isBlank()) {
        state.extensions
    } else {
        state.extensions.filter { extension ->
            sequenceOf(
                extension.displayName,
                extension.directoryName,
                extension.version,
                extension.author,
                extension.repositoryUrl,
            ).any { value -> value.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    val deleteUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再删除扩展。"
        else -> null
    }
    val toggleUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再启用或停用扩展。"
        else -> null
    }
    val repositoryError = repositoryInput.takeIf { it.isNotBlank() }
        ?.let(TavernExtensionCommandCodec::validateRepositoryUrl)
    val normalizedRepository = TavernExtensionCommandCodec.normalizeRepositoryUrl(repositoryInput)

    pendingDelete?.let { extension ->
        val targetDirectory = extensionTargetDirectory(state, extension)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("删除酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("扩展：${extension.displayName}")
                    Text("当前实例：$instanceLabel")
                    Text("目标目录：$targetDirectory")
                    Text(
                        text = "只会删除这个扩展程序目录，不会删除聊天、角色或备份。删除后无法从启动器撤销。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认删除",
                    tone = ActionTone.Danger,
                    onClick = {
                        pendingDelete = null
                        onDelete(extension.directoryName)
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

    pendingToggle?.let { extension ->
        val desiredEnabled = !extension.enabled
        val actionText = if (desiredEnabled) "启用" else "停用"
        val currentDirectory = extensionTargetDirectory(state, extension)
        val destinationDirectory = extensionTargetDirectory(
            state,
            extension.copy(enabled = desiredEnabled),
        )
        AlertDialog(
            onDismissRequest = { pendingToggle = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("${actionText}酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("扩展：${extension.displayName}")
                    Text("当前实例：$instanceLabel")
                    Text("当前位置：$currentDirectory")
                    Text("${actionText}后位置：$destinationDirectory")
                    Text(
                        text = if (desiredEnabled) {
                            "启用会把扩展原样移回酒馆扩展目录，下次启动酒馆时生效。"
                        } else {
                            "停用只会移动扩展目录，不会删除扩展文件；之后可以随时重新启用。"
                        },
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认$actionText",
                    tone = ActionTone.Safe,
                    onClick = {
                        pendingToggle = null
                        onToggleEnabled(extension.directoryName, desiredEnabled)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { pendingToggle = null },
                )
            },
        )
    }

    pendingUpdate?.let { extension ->
        AlertDialog(
            onDismissRequest = { pendingUpdate = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("更新酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("扩展：${extension.displayName}")
                    Text("当前实例：$instanceLabel")
                    Text("版本：${extension.currentRevision.take(7)} → ${extension.latestRevision.take(7)}")
                    if (extension.updateStatus == TavernExtensionUpdateStatus.LocalChanges) {
                        Text(
                            text = "这个扩展里有你改过的文件。更新会用远端版本替换整个目录，你的改动连同旧版本一起保存为回退快照，需要时点「回退」就能找回。",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "启动器会重新下载已检查的版本，校验清单后再替换原目录，并保留最近一次快照；失败时自动恢复。刷新酒馆网页后生效。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = if (extension.updateStatus == TavernExtensionUpdateStatus.LocalChanges) "保存改动并更新" else "确认更新",
                    tone = ActionTone.Safe,
                    onClick = {
                        pendingUpdate = null
                        onUpdate(extension.directoryName)
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

    pendingRollback?.let { extension ->
        AlertDialog(
            onDismissRequest = { pendingRollback = null },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("回退酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("扩展：${extension.displayName}")
                    Text("当前实例：$instanceLabel")
                    Text("回到快照：${extension.rollbackRevision.take(7)}")
                    Text(
                        text = "启动器会交换当前目录与最近一次更新前快照。回退后仍会保留现在的版本，必要时可以再次切换回来。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认回退",
                    tone = ActionTone.Safe,
                    onClick = {
                        pendingRollback = null
                        onRollback(extension.directoryName)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { pendingRollback = null },
                )
            },
        )
    }

    if (showInstallDialog) {
        val directoryName = normalizedRepository
            ?.let(TavernExtensionCommandCodec::repositoryDirectoryName)
            .orEmpty()
        val targetDirectory = directoryName.takeIf { it.isNotBlank() }
            ?.let { extensionTargetDirectory(state.rootDirectory, it) }
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("安装酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前实例：$instanceLabel")
                    OutlinedTextField(
                        value = repositoryInput,
                        onValueChange = { repositoryInput = it.take(240) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("扩展仓库地址") },
                        placeholder = { Text("https://github.com/作者/扩展名") },
                        supportingText = {
                            Text(repositoryError ?: "支持 GitHub、Gitee、GitLab 等 Git 仓库地址，也可以加 gh-proxy 加速前缀。")
                        },
                        isError = repositoryError != null,
                        singleLine = true,
                        colors = lukoaTextFieldColors(LukoaColors.Primary),
                    )
                    normalizedRepository?.let { Text("来源：$it", style = MaterialTheme.typography.bodySmall) }
                    targetDirectory?.let { Text("目标目录：$it", style = MaterialTheme.typography.bodySmall) }
                    Text(
                        text = "安装完成后扩展默认启用，刷新酒馆网页即可生效，不会覆盖已有同名扩展。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认安装",
                    enabled = !actionsLocked && normalizedRepository != null,
                    tone = ActionTone.Safe,
                    onClick = {
                        val repositoryUrl = normalizedRepository ?: return@DialogActionButton
                        showInstallDialog = false
                        onInstall(repositoryUrl)
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

    if (showUpdateAllDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateAllDialog = false },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.TextPrimary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("一键更新全部扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将依次更新 $updatableCount 个已检测到新版本的扩展，并为每个扩展自动保留最近一次回退快照。")
                    if (localChangesCount > 0) {
                        Text(
                            text = "有 $localChangesCount 个存在本地改动的扩展会自动跳过，不会覆盖你的修改。",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "更新完成后刷新酒馆网页或重启酒馆即可生效。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "开始批量更新",
                    tone = ActionTone.Safe,
                    onClick = {
                        showUpdateAllDialog = false
                        onUpdateAll()
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { showUpdateAllDialog = false },
                )
            },
        )
    }

    val content: @Composable () -> Unit = {
        if (!showSectionContainer) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsGroupLabel(
                            title = "当前实例",
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = extensionManagementStatusText(state, actionsLocked),
                            color = if (actionsLocked) LukoaColors.Accent else LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    SettingsEntryGroup {
                        SettingsEntryRow(
                            title = "当前实例",
                            detail = state.message.takeUnless {
                                it == "尚未读取当前酒馆的扩展。" || it.startsWith("已读取")
                            },
                            value = instanceLabel,
                            valueColor = LukoaColors.TextPrimary,
                        )
                        if (state.rootDirectory.isNotBlank()) {
                            SettingsEntryDivider()
                            SettingsEntryRow(
                                title = "扩展目录",
                                value = state.rootDirectory,
                                valueLayout = SettingsValueLayout.Supporting,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsGroupLabel("操作")
                    SettingsEntryGroup {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SettingsFeedbackActionButton(
                                    text = "安装扩展",
                                    modifier = Modifier.weight(1f),
                                    enabled = !actionsLocked && state.rootDirectory.isNotBlank(),
                                    accentColor = LukoaColors.Primary,
                                    unavailableHint = when {
                                        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再安装扩展。"
                                        state.rootDirectory.isBlank() -> "请先读取扩展，确认当前实例的扩展目录。"
                                        else -> null
                                    },
                                    onShowHint = onShowHint,
                                    onClick = { showInstallDialog = true },
                                )
                                SettingsFeedbackActionButton(
                                    text = if (state.loading) "读取中..." else "读取扩展",
                                    modifier = Modifier.weight(1f),
                                    enabled = !actionsLocked && !state.loading,
                                    accentColor = LukoaColors.Primary,
                                    unavailableHint = when {
                                        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再读取扩展。"
                                        state.loading -> "正在读取扩展，请稍等。"
                                        else -> null
                                    },
                                    onShowHint = onShowHint,
                                    onClick = onRefresh,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                SettingsFeedbackActionButton(
                                    text = "检查更新",
                                    modifier = Modifier.weight(1f),
                                    enabled = !actionsLocked && !state.loading && state.extensions.isNotEmpty(),
                                    accentColor = LukoaColors.Primary,
                                    unavailableHint = when {
                                        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再检查更新。"
                                        state.loading -> "正在读取扩展，请稍等。"
                                        state.extensions.isEmpty() -> "请先读取扩展列表。"
                                        else -> null
                                    },
                                    onShowHint = onShowHint,
                                    onClick = onCheckUpdates,
                                )
                                if (state.rootDirectory.isNotBlank()) {
                                    SecondaryActionButton(
                                        text = "复制目录",
                                        enabled = true,
                                        accentColor = LukoaColors.TextSecondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            val copied = onCopyPath(state.rootDirectory)
                                            onShowHint(if (copied) "扩展根目录已复制。" else "复制扩展根目录失败。")
                                        },
                                    )
                                }
                            }
                            if (updatableCount > 0) {
                                SettingsFeedbackActionButton(
                                    text = "一键更新全部（$updatableCount 个）",
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !actionsLocked && !state.loading,
                                    accentColor = LukoaColors.Accent,
                                    unavailableHint = when {
                                        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再批量更新。"
                                        state.loading -> "正在读取扩展，请稍等。"
                                        else -> null
                                    },
                                    onShowHint = onShowHint,
                                    onClick = { showUpdateAllDialog = true },
                                )
                            }
                            if (state.extensions.size > 1) {
                                LukoaSearchField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it.take(80) },
                                    placeholder = "搜索扩展（名称、作者、版本或目录）",
                                )
                                Text(
                                    text = "显示 ${visibleExtensions.size} / ${state.extensions.size} 个扩展",
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsGroupLabel(
                        if (state.extensions.isEmpty()) "已安装扩展" else "已安装扩展（${state.extensions.size} 个）",
                    )
                    if (state.extensions.isNotEmpty()) {
                        if (visibleExtensions.isEmpty()) {
                            SettingsEntryGroup {
                                SettingsEntryRow(
                                    title = "没有匹配的扩展",
                                    detail = "请换一个名称、作者、版本或目录关键词。",
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                visibleExtensions.forEach { extension ->
                                    SettingsEntryGroup {
                                        val updateDisabledReason = TavernExtensionActionPolicy.updateDisabledReason(
                                            extension = extension,
                                            actionsLocked = actionsLocked,
                                            tavernRunning = tavernRunning,
                                        )
                                        val rollbackDisabledReason = TavernExtensionActionPolicy.rollbackDisabledReason(
                                            extension = extension,
                                            actionsLocked = actionsLocked,
                                            tavernRunning = tavernRunning,
                                        )
                                        TavernExtensionRow(
                                            extension = extension,
                                            fullPath = extensionTargetDirectory(state, extension),
                                            toggleEnabled = !actionsLocked,
                                            toggleUnavailableHint = toggleUnavailableHint,
                                            deleteEnabled = !actionsLocked,
                                            deleteUnavailableHint = deleteUnavailableHint,
                                            updateEnabled = updateDisabledReason == null,
                                            updateUnavailableHint = updateDisabledReason,
                                            rollbackEnabled = rollbackDisabledReason == null,
                                            rollbackUnavailableHint = rollbackDisabledReason,
                                            onShowHint = onShowHint,
                                            onCopyPath = onCopyPath,
                                            onToggleEnabled = { pendingToggle = extension },
                                            onUpdate = { pendingUpdate = extension },
                                            onRollback = { pendingRollback = extension },
                                            onDelete = { pendingDelete = extension },
                                        )
                                    }
                                }
                            }
                        }
                    } else if (state.rootDirectory.isNotBlank() && !state.loading) {
                        SettingsEntryGroup {
                            SettingsEntryRow(
                                title = "当前没有第三方扩展",
                                detail = "这个目录中暂时没有可管理的网页扩展。",
                            )
                        }
                    }
                }
            }
        } else {
        ManagementDialogSectionTitle("当前实例与状态")
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例",
                detail = state.message.takeUnless {
                    it == "尚未读取当前酒馆的扩展。" || it.startsWith("已读取")
                },
                value = instanceLabel,
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                highlightColor = LukoaColors.Primary,
            )
            if (state.rootDirectory.isNotBlank()) {
                SettingsEntryDivider()
                SettingsEntryRow(
                    title = "扩展目录",
                    value = state.rootDirectory,
                    valueLayout = SettingsValueLayout.Supporting,
                )
            }
        }

        ManagementDialogActionCard("可用操作") {
            SettingsFeedbackActionButton(
                text = "安装扩展",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && state.rootDirectory.isNotBlank(),
                accentColor = LukoaColors.Primary,
                unavailableHint = when {
                    actionsLocked -> "当前有其他任务正在处理，请等任务完成后再安装扩展。"
                    state.rootDirectory.isBlank() -> "请先读取扩展，确认当前实例的扩展目录。"
                    else -> null
                },
                onShowHint = onShowHint,
                onClick = { showInstallDialog = true },
            )
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "读取扩展",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !state.loading,
                accentColor = LukoaColors.Primary,
                unavailableHint = when {
                    actionsLocked -> "当前有其他任务正在处理，请等任务完成后再读取扩展。"
                    state.loading -> "正在读取扩展，请稍等。"
                    else -> null
                },
                onShowHint = onShowHint,
                onClick = onRefresh,
            )
            SettingsFeedbackActionButton(
                text = "检查更新",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !state.loading && state.extensions.isNotEmpty(),
                accentColor = LukoaColors.Primary,
                unavailableHint = when {
                    actionsLocked -> "当前有其他任务正在处理，请等任务完成后再检查更新。"
                    state.loading -> "正在读取扩展，请稍等。"
                    state.extensions.isEmpty() -> "请先读取扩展列表。"
                    else -> null
                },
                onShowHint = onShowHint,
                onClick = onCheckUpdates,
            )

            if (updatableCount > 0) {
                SettingsFeedbackActionButton(
                    text = "一键更新全部（$updatableCount 个）",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionsLocked && !state.loading,
                    accentColor = LukoaColors.Accent,
                    unavailableHint = when {
                        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再批量更新。"
                        state.loading -> "正在读取扩展，请稍等。"
                        else -> null
                    },
                    onShowHint = onShowHint,
                    onClick = { showUpdateAllDialog = true },
                )
            }

            if (state.rootDirectory.isNotBlank()) {
                SecondaryActionButton(
                    text = "复制扩展根目录",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val copied = onCopyPath(state.rootDirectory)
                        onShowHint(if (copied) "扩展根目录已复制。" else "复制扩展根目录失败。")
                    },
                )
            }

            if (state.extensions.size > 1) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索扩展") },
                    placeholder = { Text("名称、作者、版本或目录") },
                    supportingText = {
                        Text("显示 ${visibleExtensions.size} / ${state.extensions.size} 个扩展")
                    },
                    singleLine = true,
                    colors = lukoaTextFieldColors(LukoaColors.Primary),
                )
            }
        }

        ManagementDialogSectionTitle("已安装扩展")
        if (state.extensions.isNotEmpty()) {
            if (visibleExtensions.isEmpty()) {
                SettingsEntryGroup {
                    SettingsEntryRow(
                        title = "没有匹配的扩展",
                        detail = "请换一个名称、作者、版本或目录关键词。",
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleExtensions.forEach { extension ->
                        SettingsEntryGroup {
                        val updateDisabledReason = TavernExtensionActionPolicy.updateDisabledReason(
                            extension = extension,
                            actionsLocked = actionsLocked,
                            tavernRunning = tavernRunning,
                        )
                        val rollbackDisabledReason = TavernExtensionActionPolicy.rollbackDisabledReason(
                            extension = extension,
                            actionsLocked = actionsLocked,
                            tavernRunning = tavernRunning,
                        )
                        TavernExtensionRow(
                            extension = extension,
                            fullPath = extensionTargetDirectory(state, extension),
                            toggleEnabled = !actionsLocked,
                            toggleUnavailableHint = toggleUnavailableHint,
                            deleteEnabled = !actionsLocked,
                            deleteUnavailableHint = deleteUnavailableHint,
                            updateEnabled = updateDisabledReason == null,
                            updateUnavailableHint = updateDisabledReason,
                            rollbackEnabled = rollbackDisabledReason == null,
                            rollbackUnavailableHint = rollbackDisabledReason,
                            onShowHint = onShowHint,
                            onCopyPath = onCopyPath,
                            onToggleEnabled = { pendingToggle = extension },
                            onUpdate = { pendingUpdate = extension },
                            onRollback = { pendingRollback = extension },
                            onDelete = { pendingDelete = extension },
                        )
                        }
                    }
                }
            }
        } else if (state.rootDirectory.isNotBlank() && !state.loading) {
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前没有第三方扩展",
                    detail = "这个目录中暂时没有可管理的网页扩展。",
                )
            }
        }
        }
    }

    if (showSectionContainer) {
        SectionPanel(
            title = "扩展管理",
            accentColor = LukoaColors.Primary,
            headerAction = { TavernExtensionManagementHeader(state, actionsLocked) },
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
private fun TavernExtensionManagementHeader(
    state: TavernExtensionManagementState,
    actionsLocked: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPill(
            text = extensionManagementStatusText(state, actionsLocked),
            active = actionsLocked || state.loading || state.rootDirectory.isNotBlank(),
            toneColor = if (actionsLocked) LukoaColors.Accent else LukoaColors.Primary,
            activeBackground = if (actionsLocked) LukoaColors.AccentSoft else LukoaColors.PrimarySoft,
        )
        InfoPopoverButton(
            contentDescription = "查看扩展管理说明",
            title = "扩展管理",
            body = "这里只管理当前酒馆的第三方网页扩展，不会处理服务器插件。\n可以粘贴公开 GitHub 仓库地址安装扩展；启动器会先下载到暂存区并校验清单，不会覆盖同名目录。\n检查到公开 GitHub 更新后，可以安全替换并保留最近一次快照；酒馆运行中也可以操作，变更后刷新网页或重启酒馆即可生效。",
        )
    }
}

private fun extensionManagementStatusText(
    state: TavernExtensionManagementState,
    actionsLocked: Boolean,
): String = when {
    actionsLocked -> "当前忙碌"
    state.loading -> "读取中"
    state.extensions.isNotEmpty() -> "${state.extensions.size} 个扩展"
    state.rootDirectory.isNotBlank() -> "暂无扩展"
    else -> "未读取"
}

@Composable
private fun TavernExtensionRow(
    extension: TavernExtensionRecord,
    fullPath: String,
    toggleEnabled: Boolean,
    toggleUnavailableHint: String?,
    deleteEnabled: Boolean,
    deleteUnavailableHint: String?,
    updateEnabled: Boolean,
    updateUnavailableHint: String?,
    rollbackEnabled: Boolean,
    rollbackUnavailableHint: String?,
    onShowHint: (String) -> Unit,
    onCopyPath: (String) -> Boolean,
    onToggleEnabled: () -> Unit,
    onUpdate: () -> Unit,
    onRollback: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = extension.displayName,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = if (extension.enabled) "已启用" else "已停用",
                    active = extension.enabled,
                    toneColor = if (extension.enabled) LukoaColors.Primary else LukoaColors.TextSecondary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
            }
            Text(
                text = buildString {
                    append("版本：")
                    append(extension.version.ifBlank { "未标注" })
                    append(" · 大小：")
                    append(
                        when (val kilobytes = extension.directoryKilobytes) {
                            null -> "未知"
                            else -> formatStorageKilobytes(kilobytes)
                        },
                    )
                },
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            if (extension.author.isNotBlank()) {
                Text(
                    text = "作者：${extension.author}",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (
                extension.updateStatus != TavernExtensionUpdateStatus.NotChecked ||
                extension.repositoryUrl.isNotBlank()
            ) {
                Text(
                    text = extensionUpdateStatusText(extension.updateStatus),
                    color = extensionUpdateStatusColor(extension.updateStatus),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (extension.repositoryUrl.isNotBlank()) {
                Text(
                    text = "来源：${extension.repositoryUrl}",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (extension.currentRevision.isNotBlank()) {
                Text(
                    text = buildString {
                        append("当前：")
                        append(extension.currentRevision.take(7))
                        if (extension.latestRevision.isNotBlank()) {
                            append(" · 远端：")
                            append(extension.latestRevision.take(7))
                        }
                    },
                    color = LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (extension.rollbackRevision.isNotBlank()) {
                Text(
                    text = "可回退快照：${extension.rollbackRevision.take(7)}",
                    color = LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "完整路径：$fullPath",
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!extension.hasManifest) {
                Text(
                    text = "未读取到 manifest.json",
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsFeedbackActionButton(
                text = "更新",
                modifier = Modifier.weight(1f),
                enabled = updateEnabled,
                accentColor = LukoaColors.Primary,
                unavailableHint = updateUnavailableHint,
                onShowHint = onShowHint,
                onClick = onUpdate,
            )
            SettingsFeedbackActionButton(
                text = if (extension.enabled) "停用" else "启用",
                modifier = Modifier.weight(1f),
                enabled = toggleEnabled,
                accentColor = LukoaColors.Primary,
                unavailableHint = toggleUnavailableHint,
                onShowHint = onShowHint,
                onClick = onToggleEnabled,
            )
            SecondaryActionButton(
                text = "复制路径",
                modifier = Modifier.weight(1f),
                enabled = true,
                accentColor = LukoaColors.TextSecondary,
                onClick = {
                    val copied = onCopyPath(fullPath)
                    onShowHint(if (copied) "扩展路径已复制。" else "复制扩展路径失败。")
                },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsFeedbackActionButton(
                text = "回退",
                modifier = Modifier.weight(1f),
                enabled = rollbackEnabled,
                accentColor = LukoaColors.TextSecondary,
                unavailableHint = rollbackUnavailableHint,
                onShowHint = onShowHint,
                onClick = onRollback,
            )
            SettingsFeedbackActionButton(
                text = "删除",
                modifier = Modifier.weight(1f),
                enabled = deleteEnabled,
                accentColor = LukoaColors.Danger,
                unavailableHint = deleteUnavailableHint,
                onShowHint = onShowHint,
                onClick = onDelete,
            )
        }
    }
}

internal fun extensionTargetDirectory(rootDirectory: String, directoryName: String): String {
    val normalizedRoot = rootDirectory.trim().trimEnd('/', '\\')
    return if (normalizedRoot.isBlank()) directoryName else "$normalizedRoot/$directoryName"
}

internal fun extensionTargetDirectory(
    state: TavernExtensionManagementState,
    extension: TavernExtensionRecord,
): String = extensionTargetDirectory(
    rootDirectory = if (extension.enabled) state.rootDirectory else state.disabledRootDirectory,
    directoryName = extension.directoryName,
)

private fun extensionUpdateStatusText(status: TavernExtensionUpdateStatus): String = when (status) {
    TavernExtensionUpdateStatus.NotGitRepository -> "非 Git 安装，无法自动检查"
    TavernExtensionUpdateStatus.UnsupportedSource -> "来源不是 http(s) 地址，无法检查"
    TavernExtensionUpdateStatus.NotChecked -> "尚未检查更新"
    TavernExtensionUpdateStatus.UpToDate -> "已是最新"
    TavernExtensionUpdateStatus.UpdateAvailable -> "发现更新"
    TavernExtensionUpdateStatus.LocalChanges -> "检测到本地改动"
    TavernExtensionUpdateStatus.CheckFailed -> "更新检查失败"
}

private fun extensionUpdateStatusColor(status: TavernExtensionUpdateStatus) = when (status) {
    TavernExtensionUpdateStatus.UpToDate -> LukoaColors.Primary
    TavernExtensionUpdateStatus.UpdateAvailable,
    TavernExtensionUpdateStatus.LocalChanges,
    TavernExtensionUpdateStatus.CheckFailed,
    -> LukoaColors.Accent
    TavernExtensionUpdateStatus.NotGitRepository,
    TavernExtensionUpdateStatus.UnsupportedSource,
    TavernExtensionUpdateStatus.NotChecked,
    -> LukoaColors.TextSecondary
}
