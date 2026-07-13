package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernUserManagementSection(
    state: TavernUserManagementState,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onCreate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var createDialog by remember { mutableStateOf(false) }
    var deleteUser by remember { mutableStateOf<TavernUserRecord?>(null) }
    if (createDialog) {
        UserInputDialog(
            onDismiss = { createDialog = false },
            onConfirm = { handle, name ->
                createDialog = false
                onCreate(handle, name)
            },
        )
    }
    deleteUser?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteUser = null },
            containerColor = LukoaColors.DialogSurface,
            shape = RoundedCornerShape(24.dp),
            title = { Text("删除用户账户", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    KeyValueLine("显示名称", user.name)
                    KeyValueLine("登录标识", user.handle, monospace = true)
                    KeyValueLine(
                        "数据目录",
                        if (user.directoryExists) "存在 · ${formatUserDirectorySize(user.directoryKilobytes)}" else "不存在",
                    )
                    StateNote("只删除账户记录，保留数据目录，避免聊天和角色丢失。默认用户和最后一个启用管理员不能删除。")
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "删除账户",
                    tone = ActionTone.Danger,
                    onClick = {
                        deleteUser = null
                        onDelete(user.handle)
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { deleteUser = null },
                )
            },
        )
    }

    DashedSection(
        label = "用户管理",
        headerAction = {
            HelpHint("这里管理当前实例里的 SillyTavern 登录用户，不是启动器分身实例。修改用户前需要先停止酒馆。")
        },
    ) {
        if (tavernRunning) {
            StateNote("请先停止酒馆再修改用户。", tone = LukoaPillTone.Warning)
        }
        if (state.users.isEmpty()) {
            StateNote(state.message)
        } else {
            state.users.forEachIndexed { index, user ->
                if (index > 0) LukoaRowDivider()
                val protectedReason = TavernUserDeletionPolicy.disabledReason(state.users, user)
                LukoaRow(
                    title = "${user.name} · ${user.handle}",
                    detail = buildString {
                        append(if (user.admin) "管理员" else "普通用户")
                        append(" · ")
                        append(if (user.enabled) "已启用" else "已禁用")
                        append(" · 数据目录 ")
                        append(if (user.directoryExists) formatUserDirectorySize(user.directoryKilobytes) else "缺失")
                    },
                    leading = { RowIcon("♙") },
                    trailing = {
                        when {
                            user.handle == "default-user" -> LukoaPill("默认用户", LukoaPillTone.Idle)
                            protectedReason != null -> LukoaPill("受保护", LukoaPillTone.Muted)
                            else -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("删除账户", color = LukoaColors.Danger)
                                ChevronRight(color = LukoaColors.Danger)
                            }
                        }
                    },
                    enabled = !actionsLocked && !tavernRunning && protectedReason == null,
                    onClick = if (protectedReason == null) {
                        { deleteUser = user }
                    } else {
                        null
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SecondaryActionButton(
                text = if (state.loading) "读取中…" else "读取用户",
                enabled = !actionsLocked && !state.loading && !tavernRunning,
                accentColor = LukoaColors.Text,
                modifier = Modifier.weight(1f),
                onClick = onRefresh,
            )
            SecondaryActionButton(
                text = "新增用户",
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Text,
                modifier = Modifier.weight(1f),
                onClick = { createDialog = true },
            )
        }
        StateNote("登录标识是登录时使用的英文短名，也是数据目录名。SillyTavern 官方没有修改登录标识的接口，创建后启动器不提供重命名。")
    }
}

@Composable
private fun UserInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var handle by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val handleError = TavernUserCommandCodec.validateHandle(handle.trim())
    val nameError = TavernUserCommandCodec.validateName(name.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.DialogSurface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("新增酒馆用户", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StateNote("登录标识是英文短名和 data/登录标识 目录名，不是显示昵称。SillyTavern 官方没有修改接口，创建后不能重命名。")
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.lowercase() },
                    label = { Text("登录标识") },
                    isError = handle.isNotBlank() && handleError != null,
                    supportingText = { handleError?.takeIf { handle.isNotBlank() }?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = userFieldColors(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("显示名称") },
                    isError = name.isNotBlank() && nameError != null,
                    supportingText = { nameError?.takeIf { name.isNotBlank() }?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = userFieldColors(),
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认新增",
                enabled = handleError == null && nameError == null,
                tone = ActionTone.Safe,
                onClick = { onConfirm(handle.trim(), name.trim()) },
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}

@Composable
private fun userFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LukoaColors.Text,
    unfocusedTextColor = LukoaColors.Text,
    focusedContainerColor = LukoaColors.BackgroundAlt,
    unfocusedContainerColor = LukoaColors.BackgroundAlt,
    focusedBorderColor = LukoaColors.Accent,
    unfocusedBorderColor = LukoaColors.Line,
    focusedLabelColor = LukoaColors.Accent,
    unfocusedLabelColor = LukoaColors.Muted,
    cursorColor = LukoaColors.Accent,
)

private fun formatUserDirectorySize(kilobytes: Long): String = when {
    kilobytes >= 1024 * 1024 -> "%.1fGB".format(kilobytes / 1024.0 / 1024.0)
    kilobytes >= 1024 -> "%.1fMB".format(kilobytes / 1024.0)
    else -> "${kilobytes}KB"
}
