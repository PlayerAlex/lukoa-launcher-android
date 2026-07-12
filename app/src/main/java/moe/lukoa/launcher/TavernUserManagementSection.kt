package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    SectionPanel(title = "酒馆用户管理", accentColor = LukoaColors.Info) {
        Text(if (tavernRunning) "请先停止酒馆再修改用户。读取列表也建议在停止状态进行。" else state.message)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(modifier = Modifier.weight(1f), enabled = !actionsLocked && !state.loading && !tavernRunning, onClick = onRefresh) { Text("读取用户") }
            Button(modifier = Modifier.weight(1f), enabled = !actionsLocked && !tavernRunning, onClick = { createDialog = true }) { Text("新增用户") }
        }
        state.users.forEach { user ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${user.name} · ${user.handle}${if (user.admin) " · 管理员" else ""}")
                Text("目录：${if (user.directoryExists) formatUserDirectorySize(user.directoryKilobytes) else "缺失"} · ${if (user.enabled) "已启用" else "已禁用"}")
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !actionsLocked && !tavernRunning && user.handle != "default-user", onClick = { deleteUser = user }) { Text("删除账户") }
            }
        }
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("登录标识是登录 SillyTavern 时使用的英文短名，例如 xiaoming；它也会成为 data/登录标识 数据目录名。它不是页面显示昵称。SillyTavern 官方没有提供修改登录标识的接口，创建后不能在启动器里重命名，请确认无误。")
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
