package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RepairConfirmation(
    val title: String,
    val detail: String,
    val action: () -> Unit,
)

@Composable
fun RepairToolsSection(
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    uploadLimitStatus: TavernUploadLimitStatus,
    onRepairDependencies: () -> Unit,
    onResetTheme: () -> Unit,
    onSetNodeMemory: (Int) -> Unit,
    onCheckUploadLimit: () -> Unit,
    onSetUploadLimit: (Int) -> Unit,
) {
    var confirmation by remember { mutableStateOf<RepairConfirmation?>(null) }
    var showNodeMemoryDialog by remember { mutableStateOf(false) }
    val operationsEnabled = !actionsLocked && !tavernRunning

    confirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            containerColor = LukoaColors.DialogSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            title = { Text(request.title, fontWeight = FontWeight.ExtraBold) },
            text = { Text(request.detail, color = LukoaColors.Muted) },
            confirmButton = {
                DialogActionButton(
                    text = "确认执行",
                    tone = ActionTone.Warning,
                    onClick = {
                        confirmation = null
                        request.action()
                    },
                )
            },
            dismissButton = {
                DialogActionButton(
                    text = "取消",
                    tone = ActionTone.Neutral,
                    onClick = { confirmation = null },
                )
            },
        )
    }

    if (showNodeMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showNodeMemoryDialog = false },
            containerColor = LukoaColors.DialogSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            title = { Text("Node.js 内存上限", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        text = "修改只作用于当前实例。设置过高会增加 Termux 被系统杀后台的概率。",
                        color = LukoaColors.Muted,
                    )
                    listOf(2048, 4096, 6144).forEach { memory ->
                        SecondaryActionButton(
                            text = "${memory / 1024}GB",
                            enabled = operationsEnabled,
                            accentColor = LukoaColors.Text,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showNodeMemoryDialog = false
                                confirmation = RepairConfirmation(
                                    title = "设置 ${memory / 1024}GB 内存上限",
                                    detail = "启动器会写入当前实例的独立环境配置，不会直接修改 start.sh。",
                                ) { onSetNodeMemory(memory) }
                            },
                        )
                    }
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = "关闭",
                    tone = ActionTone.Neutral,
                    onClick = { showNodeMemoryDialog = false },
                )
            },
        )
    }

    DashedSection(
        label = "修复工具",
        headerAction = {
            HelpHint("修复只作用于当前实例。酒馆运行或启动时不会开放修改，执行前会保留恢复副本。")
        },
    ) {
        if (tavernRunning) {
            StateNote("检测到酒馆正在运行或启动，请先回到启动页停止酒馆。", tone = LukoaPillTone.Warning)
        }
        LukoaRow(
            title = "修复 npm 依赖",
            detail = "备份后重建 node_modules",
            leading = { RowIcon("⌁", color = LukoaColors.Amber) },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LukoaPill("修复", LukoaPillTone.Warning)
                    ChevronRight()
                }
            },
            enabled = operationsEnabled,
            onClick = {
                confirmation = RepairConfirmation(
                    title = "重新安装依赖",
                    detail = "旧 node_modules 会先移到恢复目录。只有 npm install 成功后才会清理旧副本，此操作可能持续数分钟。",
                    action = onRepairDependencies,
                )
            },
        )
        LukoaRowDivider()
        LukoaRow(
            title = "重置网页主题",
            detail = "网页打不开或卡住时使用",
            leading = { RowIcon("✓") },
            trailing = { ChevronRight() },
            enabled = operationsEnabled,
            onClick = {
                confirmation = RepairConfirmation(
                    title = "重置网页主题",
                    detail = "将兼容的用户设置恢复为 Dark Lite。原文件会保留副本；无法唯一识别设置文件时不会修改。",
                    action = onResetTheme,
                )
            },
        )
        LukoaRowDivider()
        LukoaRow(
            title = "Node.js 内存上限",
            detail = "当前支持 2GB / 4GB / 6GB",
            leading = { RowIcon("▣") },
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("选择", color = LukoaColors.Muted)
                    ChevronRight()
                }
            },
            enabled = operationsEnabled,
            onClick = { showNodeMemoryDialog = true },
        )
    }

    UploadLimitSettingsSection(
        actionsLocked = actionsLocked,
        tavernRunning = tavernRunning,
        status = uploadLimitStatus,
        onCheck = onCheckUploadLimit,
        onSetLimit = { limit ->
            confirmation = RepairConfirmation(
                title = "设置上传限制为 ${TavernUploadLimitPolicy.label(limit)}",
                detail = "只修改当前实例里唯一识别到的兼容上传中间件。修改前会备份源文件并记录原值；版本结构不匹配时会拒绝修改。",
            ) { onSetUploadLimit(limit) }
        },
    )
}

@Composable
private fun UploadLimitSettingsSection(
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    status: TavernUploadLimitStatus,
    onCheck: () -> Unit,
    onSetLimit: (Int) -> Unit,
) {
    val targetReady = status.targetCompatible == true && status.currentMegabytes != null && !status.checking
    val currentLabel = when {
        status.checking -> "检查中…"
        status.currentMegabytes != null -> TavernUploadLimitPolicy.label(status.currentMegabytes)
        else -> "尚未读取"
    }
    DashedSection(
        label = "聊天记录上传限制",
        headerAction = {
            HelpHint("大文件会明显增加 Termux 内存和处理时间。1GB 以上更容易触发系统杀后台。")
        },
    ) {
        val currentResultText = buildString {
                append("当前检查结果：$currentLabel")
                if (status.targetFile.isNotBlank()) {
                    append("\n目标文件：${status.targetFile}")
                }
            }
        StateNote(
            text = currentResultText,
            emphasizePrefix = "当前检查结果：$currentLabel",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            LukoaPill(
                text = when (status.targetCompatible) {
                    true -> "目标代码已唯一识别"
                    false -> "目标代码不兼容"
                    null -> "等待兼容检查"
                },
                tone = when (status.targetCompatible) {
                    true -> LukoaPillTone.Accent
                    false -> LukoaPillTone.Danger
                    null -> LukoaPillTone.Muted
                },
                modifier = Modifier.weight(1f),
            )
            LukoaPill(
                text = when (status.patchState) {
                    TavernUploadLimitPatchState.Active -> "补丁已应用"
                    TavernUploadLimitPatchState.NotManaged -> "补丁未应用"
                    TavernUploadLimitPatchState.ChangedOrOverwritten -> "补丁可能失效"
                    TavernUploadLimitPatchState.Unknown -> "状态待确认"
                },
                tone = if (status.patchState == TavernUploadLimitPatchState.Active) {
                    LukoaPillTone.Accent
                } else {
                    LukoaPillTone.Muted
                },
                modifier = Modifier.weight(1f),
            )
        }

        TavernUploadLimitPolicy.allowedMegabytes.forEachIndexed { index, limit ->
            if (index > 0) LukoaRowDivider()
            val selected = status.currentMegabytes == limit
            LukoaRow(
                title = TavernUploadLimitPolicy.label(limit),
                detail = when (limit) {
                    500 -> if (selected) "当前原值" else "恢复酒馆默认限制"
                    1024 -> "适合确实需要导入大文件时"
                    else -> "内存压力高，更容易被系统杀后台"
                },
                trailing = {
                    if (selected) {
                        LukoaPill("当前", LukoaPillTone.Accent)
                    } else {
                        ChevronRight()
                    }
                },
                enabled = targetReady && !actionsLocked && !tavernRunning,
                onClick = { onSetLimit(limit) },
            )
        }

        StateNote(
            text = buildString {
                append("修改前：记录原值并备份目标文件。ST 更新或回退后自动重新检查；无法唯一识别代码时拒绝修改。")
                status.recordedPreviousMegabytes?.let { previous ->
                    append("\n已记录原值：${TavernUploadLimitPolicy.label(previous)}")
                }
            },
            emphasizePrefix = "修改前：",
        )
        SecondaryActionButton(
            text = if (status.checking) "检查中…" else "重新检查当前限制",
            enabled = !actionsLocked && !status.checking,
            accentColor = LukoaColors.Text,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCheck,
        )
    }
}
