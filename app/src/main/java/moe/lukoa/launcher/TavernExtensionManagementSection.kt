package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernExtensionManagementSettingsPanel(
    state: TavernExtensionManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onShowHint: (String) -> Unit = {},
) {
    var showDialog by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    val dialogStateHolder = rememberSaveableStateHolder()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = LukoaColors.Elevated,
            titleContentColor = LukoaColors.Primary,
            textContentColor = LukoaColors.TextPrimary,
            title = { Text("扩展管理") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    dialogStateHolder.SaveableStateProvider("extension-management-dialog") {
                        TavernExtensionManagementSection(
                            state = state,
                            instanceLabel = instanceLabel,
                            actionsLocked = actionsLocked,
                            tavernRunning = tavernRunning,
                            onRefresh = onRefresh,
                            onDelete = onDelete,
                            onShowHint = onShowHint,
                            showSectionContainer = false,
                        )
                    }
                }
            },
            confirmButton = {
                SecondaryActionButton(
                    text = "关闭",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    onClick = { showDialog = false },
                )
            },
            dismissButton = null,
        )
    }

    SectionPanel(
        title = "扩展管理",
        accentColor = LukoaColors.Primary,
        headerAction = {
            StatusPill(
                text = extensionManagementStatusText(state, actionsLocked),
                active = actionsLocked || state.loading || state.rootDirectory.isNotBlank(),
                toneColor = if (actionsLocked) LukoaColors.Accent else LukoaColors.Primary,
                activeBackground = if (actionsLocked) LukoaColors.AccentSoft else LukoaColors.PrimarySoft,
            )
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "管理已安装扩展",
                detail = "当前实例：$instanceLabel。进入后可读取、搜索和删除第三方网页扩展。",
                value = "打开",
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                highlightColor = LukoaColors.Primary,
                onClick = { showDialog = true },
            )
        }
    }
}

@Composable
fun TavernExtensionManagementSection(
    state: TavernExtensionManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onShowHint: (String) -> Unit = {},
    showSectionContainer: Boolean = true,
) {
    var pendingDelete by remember(instanceLabel, state.rootDirectory) {
        mutableStateOf<TavernExtensionRecord?>(null)
    }
    var searchQuery by rememberSaveable(instanceLabel, state.rootDirectory) { mutableStateOf("") }
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
            ).any { value -> value.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    val deleteUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再删除扩展。"
        tavernRunning -> "删除扩展前必须先停止酒馆，避免扩展文件仍在使用。"
        else -> null
    }

    pendingDelete?.let { extension ->
        val targetDirectory = extensionTargetDirectory(state.rootDirectory, extension.directoryName)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除酒馆扩展") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("扩展：${extension.displayName}")
                    Text("当前实例：$instanceLabel")
                    Text("目标目录：$targetDirectory")
                    Text("只会删除这个扩展程序目录，不会删除聊天、角色或备份。删除后无法从启动器撤销。")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        onDelete(extension.directoryName)
                    },
                ) { Text("确认删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    val content: @Composable () -> Unit = {
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

        if (state.extensions.isNotEmpty()) {
            if (visibleExtensions.isEmpty()) {
                SettingsEntryGroup {
                    SettingsEntryRow(
                        title = "没有匹配的扩展",
                        detail = "请换一个名称、作者、版本或目录关键词。",
                    )
                }
            } else {
                SettingsEntryGroup {
                    visibleExtensions.forEachIndexed { index, extension ->
                        TavernExtensionRow(
                            extension = extension,
                            deleteEnabled = !actionsLocked && !tavernRunning,
                            deleteUnavailableHint = deleteUnavailableHint,
                            onShowHint = onShowHint,
                            onDelete = { pendingDelete = extension },
                        )
                        if (index < visibleExtensions.lastIndex) SettingsEntryDivider()
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
            body = "这里只管理当前酒馆的第三方网页扩展，不会处理服务器插件。\n读取扩展会显示名称、版本、作者、目录和文件大小，不会修改文件；扩展较多时可以直接搜索。\n删除扩展前必须先停止酒馆，并会再次显示目标目录供你确认。",
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
    deleteEnabled: Boolean,
    deleteUnavailableHint: String?,
    onShowHint: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = extension.displayName,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
            Text(
                text = "目录：${extension.directoryName}",
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
        SettingsFeedbackActionButton(
            text = "删除",
            modifier = Modifier.widthIn(min = 82.dp, max = 96.dp),
            enabled = deleteEnabled,
            accentColor = LukoaColors.Danger,
            unavailableHint = deleteUnavailableHint,
            onShowHint = onShowHint,
            onClick = onDelete,
        )
    }
}

private fun extensionTargetDirectory(rootDirectory: String, directoryName: String): String {
    return rootDirectory.trimEnd('/', '\\') + "/" + directoryName
}
