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
import androidx.compose.material3.OutlinedTextField
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
fun TavernUserManagementSection(
    state: TavernUserManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onCreate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onShowHint: (String) -> Unit = {},
) {
    var createDialog by remember { mutableStateOf(false) }
    var deleteUser by remember { mutableStateOf<TavernUserRecord?>(null) }
    val userActionsUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再试。"
        tavernRunning -> "酒馆正在运行，请先停止酒馆再管理用户。"
        else -> null
    }
    if (createDialog) {
        UserInputDialog("新增酒馆用户", "登录标识", "显示名称", onDismiss = { createDialog = false }) { handle, name ->
            createDialog = false
            onCreate(handle, name)
        }
    }
    deleteUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteUser = null },
            title = { Text("删除用户账户") },
            text = { Text("将删除账户“${user.name}（${user.handle}）”，但保留数据目录，避免聊天和角色丢失。默认用户和最后一个管理员不能删除。") },
            confirmButton = { Button(onClick = { deleteUser = null; onDelete(user.handle) }) { Text("删除账户") } },
            dismissButton = { OutlinedButton(onClick = { deleteUser = null }) { Text("取消") } },
        )
    }

    SectionPanel(
        title = "用户管理",
        accentColor = LukoaColors.Info,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = when {
                        actionsLocked -> "当前忙碌"
                        tavernRunning -> "运行中锁定"
                        state.loading -> "读取中"
                        state.users.isEmpty() -> "未读取"
                        else -> "${state.users.size} 位用户"
                    },
                    active = actionsLocked || state.loading || state.users.isNotEmpty(),
                    toneColor = if (actionsLocked || tavernRunning) LukoaColors.Amber else LukoaColors.Info,
                    activeBackground = if (actionsLocked || tavernRunning) LukoaColors.AmberSoft else LukoaColors.InfoSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看用户管理说明",
                    title = "用户管理",
                    body = "这里管理的是当前这套酒馆里的登录账号，不是启动器的实例。\n“显示名称”是页面里看到的昵称；“登录标识”是登录时使用的英文短名，也是这个用户的数据文件夹名。\n为了避免用户文件被同时写入，读取、新增或删除前都要先停止酒馆。",
                )
            }
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例",
                detail = when {
                    actionsLocked -> "当前有其他任务正在处理，用户操作暂时不可用。"
                    tavernRunning -> "酒馆正在运行，请先停止后再读取或修改用户。"
                    state.message == "尚未读取当前酒馆的用户。" -> null
                    state.message.startsWith("已读取") -> null
                    else -> state.message
                },
                value = instanceLabel,
                valueColor = LukoaColors.Info,
                valueAsPill = true,
                highlightColor = LukoaColors.Info,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "读取用户",
                modifier = Modifier.weight(1f),
                enabled = !actionsLocked && !state.loading && !tavernRunning,
                accentColor = LukoaColors.Info,
                unavailableHint = when {
                    state.loading -> "正在读取用户，请稍等。"
                    else -> userActionsUnavailableHint
                },
                onShowHint = onShowHint,
                onClick = onRefresh,
            )
            SettingsFeedbackActionButton(
                text = "新增用户",
                modifier = Modifier.weight(1f),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Accent,
                unavailableHint = userActionsUnavailableHint,
                onShowHint = onShowHint,
                onClick = { createDialog = true },
            )
        }
        if (state.users.isNotEmpty()) {
            SettingsEntryGroup {
                state.users.forEachIndexed { index, user ->
                    TavernUserRow(
                        user = user,
                        deleteEnabled = !actionsLocked && !tavernRunning && user.handle != "default-user",
                        deleteUnavailableHint = when {
                            actionsLocked || tavernRunning -> userActionsUnavailableHint
                            user.handle == "default-user" -> "默认用户不能删除。"
                            else -> null
                        },
                        onShowHint = onShowHint,
                        onDelete = { deleteUser = user },
                    )
                    if (index < state.users.lastIndex) {
                        SettingsEntryDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TavernUserRow(
    user: TavernUserRecord,
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
                text = user.name,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "登录标识：${user.handle}${if (user.admin) " · 管理员" else ""}",
                color = if (user.admin) LukoaColors.Info else LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "目录：${if (user.directoryExists) formatUserDirectorySize(user.directoryKilobytes) else "缺失"} · ${if (user.enabled) "已启用" else "已禁用"}",
                color = when {
                    !user.directoryExists -> LukoaColors.Amber
                    !user.enabled -> LukoaColors.Dim
                    else -> LukoaColors.Muted
                },
                style = MaterialTheme.typography.bodySmall,
            )
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

@Composable
private fun UserInputDialog(
    title: String,
    handleLabel: String,
    nameLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val handleError = TavernUserCommandCodec.validateHandle(handle.trim())
    val nameError = TavernUserCommandCodec.validateName(name.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            SettingsDialogTitle(
                title = title,
                infoText = "登录标识是登录时输入的英文短名，也会成为这个用户的数据文件夹名。创建后不能在启动器里改名。\n显示名称只是页面里看到的昵称，以后可以在酒馆中调整。\n不知道怎么填时，登录标识可用简单的小写英文，显示名称填写你想看到的名字。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = handle, onValueChange = { handle = it.lowercase() }, label = { Text(handleLabel) }, isError = handleError != null, supportingText = { handleError?.let { e -> Text(e) } })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(nameLabel) }, isError = nameError != null, supportingText = { nameError?.let { e -> Text(e) } })
            }
        },
        confirmButton = { Button(enabled = handleError == null && nameError == null, onClick = { onConfirm(handle.trim(), name.trim()) }) { Text("确认") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatUserDirectorySize(kilobytes: Long): String = when {
    kilobytes >= 1024 * 1024 -> "%.1fGB".format(kilobytes / 1024.0 / 1024.0)
    kilobytes >= 1024 -> "%.1fMB".format(kilobytes / 1024.0)
    else -> "${kilobytes}KB"
}
