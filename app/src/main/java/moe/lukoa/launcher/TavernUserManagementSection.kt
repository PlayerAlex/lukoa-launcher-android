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
fun TavernUserManagementSettingsPanel(
    state: TavernUserManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onCreate: (String, String) -> Unit,
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
            title = { Text("用户管理") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "这里管理当前酒馆实例中的登录账户。读取和修改前需要先停止酒馆，删除账户时仍会保留对应的数据目录。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    dialogStateHolder.SaveableStateProvider("user-management-dialog") {
                        TavernUserManagementSection(
                            state = state,
                            instanceLabel = instanceLabel,
                            actionsLocked = actionsLocked,
                            tavernRunning = tavernRunning,
                            onRefresh = onRefresh,
                            onCreate = onCreate,
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
        title = "用户管理",
        accentColor = LukoaColors.Primary,
        containerColor = LukoaColors.Elevated,
        headerAction = {
            TavernUserManagementHeader(
                state = state,
                actionsLocked = actionsLocked,
                tavernRunning = tavernRunning,
            )
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "管理酒馆用户",
                detail = "当前实例：$instanceLabel。进入后可读取、新增和删除酒馆登录账户。",
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
fun TavernUserManagementSection(
    state: TavernUserManagementState,
    instanceLabel: String,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onCreate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onShowHint: (String) -> Unit = {},
    showSectionContainer: Boolean = true,
) {
    var createDialog by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    var deleteUser by remember { mutableStateOf<TavernUserRecord?>(null) }
    val userActionsUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再试。"
        tavernRunning -> "酒馆正在运行，请先停止酒馆再管理用户。"
        else -> null
    }
    val adminCount = state.users.count { it.admin }
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

    val content: @Composable () -> Unit = {
        ManagementDialogSectionTitle("当前实例与状态")
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
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
                highlightColor = LukoaColors.Primary,
            )
        }
        ManagementDialogSectionTitle("可用操作")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "读取用户",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !state.loading && !tavernRunning,
                accentColor = LukoaColors.Primary,
                unavailableHint = when {
                    state.loading -> "正在读取用户，请稍等。"
                    else -> userActionsUnavailableHint
                },
                onShowHint = onShowHint,
                onClick = onRefresh,
            )
            SettingsFeedbackActionButton(
                text = "新增用户",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Primary,
                unavailableHint = userActionsUnavailableHint,
                onShowHint = onShowHint,
                onClick = { createDialog = true },
            )
        }
        ManagementDialogSectionTitle("酒馆用户")
        if (state.users.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.users.forEach { user ->
                    SettingsEntryGroup {
                        val isLastAdmin = user.admin && adminCount <= 1
                        TavernUserRow(
                            user = user,
                            deleteEnabled = !actionsLocked && !tavernRunning && user.handle != "default-user" && !isLastAdmin,
                            deleteUnavailableHint = when {
                                actionsLocked || tavernRunning -> userActionsUnavailableHint
                                user.handle == "default-user" -> "默认用户不能删除。"
                                isLastAdmin -> "最后一个管理员不能删除。"
                                else -> null
                            },
                            onShowHint = onShowHint,
                            onDelete = { deleteUser = user },
                        )
                    }
                }
            }
        } else {
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "尚未读取用户",
                    detail = "点击“读取用户”后，这里会按账户分别显示。",
                )
            }
        }
    }

    if (showSectionContainer) {
        SectionPanel(
            title = "用户管理",
            accentColor = LukoaColors.Primary,
            headerAction = {
                TavernUserManagementHeader(
                    state = state,
                    actionsLocked = actionsLocked,
                    tavernRunning = tavernRunning,
                )
            },
            content = content,
        )
    } else {
        content()
    }
}

@Composable
private fun TavernUserManagementHeader(
    state: TavernUserManagementState,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPill(
            text = when {
                actionsLocked -> "当前忙碌"
                tavernRunning -> "需先停止酒馆"
                state.loading -> "读取中"
                state.users.isEmpty() -> "未读取"
                else -> "${state.users.size} 位用户"
            },
            active = actionsLocked || state.loading || state.users.isNotEmpty(),
            toneColor = if (actionsLocked || tavernRunning) LukoaColors.Accent else LukoaColors.Primary,
            activeBackground = if (actionsLocked || tavernRunning) LukoaColors.AccentSoft else LukoaColors.PrimarySoft,
        )
        InfoPopoverButton(
            contentDescription = "查看用户管理说明",
            title = "用户管理",
            body = "这里管理的是当前这套酒馆里的登录账号，不是启动器的实例。\n“显示名称”是页面里看到的昵称；“登录标识”是登录时使用的英文短名，也是这个用户的数据文件夹名。\n为了避免用户文件被同时写入，读取、新增或删除前都要先停止酒馆。",
        )
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
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "登录标识：${user.handle}${if (user.admin) " · 管理员" else ""}",
                color = if (user.admin) LukoaColors.Primary else LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "目录：${if (user.directoryExists) formatStorageKilobytes(user.directoryKilobytes) else "缺失"} · ${if (user.enabled) "已启用" else "已禁用"}",
                color = when {
                    !user.directoryExists -> LukoaColors.Accent
                    !user.enabled -> LukoaColors.Dim
                    else -> LukoaColors.TextSecondary
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
    var handle by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
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
