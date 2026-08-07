package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    onShowHint: (String) -> Unit = {},
) {
    var pendingDelete by remember { mutableStateOf<TavernExtensionRecord?>(null) }
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

    SectionPanel(
        title = "扩展管理",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = when {
                        actionsLocked -> "当前忙碌"
                        state.loading -> "读取中"
                        state.extensions.isNotEmpty() -> "${state.extensions.size} 个扩展"
                        state.rootDirectory.isNotBlank() -> "暂无扩展"
                        else -> "未读取"
                    },
                    active = actionsLocked || state.loading || state.rootDirectory.isNotBlank(),
                    toneColor = if (actionsLocked) LukoaColors.Accent else LukoaColors.Primary,
                    activeBackground = if (actionsLocked) LukoaColors.AccentSoft else LukoaColors.PrimarySoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看扩展管理说明",
                    title = "扩展管理",
                    body = "这里只管理当前酒馆的第三方网页扩展，不会处理服务器插件。\n读取扩展不会修改文件，酒馆运行时也可以使用。删除扩展前必须先停止酒馆，并会再次显示目标目录供你确认。",
                )
            }
        },
    ) {
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

        if (state.extensions.isNotEmpty()) {
            SettingsEntryGroup {
                state.extensions.forEachIndexed { index, extension ->
                    TavernExtensionRow(
                        extension = extension,
                        deleteEnabled = !actionsLocked && !tavernRunning,
                        deleteUnavailableHint = deleteUnavailableHint,
                        onShowHint = onShowHint,
                        onDelete = { pendingDelete = extension },
                    )
                    if (index < state.extensions.lastIndex) SettingsEntryDivider()
                }
            }
        }
    }
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
                    append(" · 目录：")
                    append(extension.directoryName)
                },
                color = LukoaColors.TextSecondary,
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
