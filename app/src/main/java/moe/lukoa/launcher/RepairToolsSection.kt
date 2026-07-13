package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class RepairConfirmation(val title: String, val detail: String, val action: () -> Unit)

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
    val uploadTargetReady = uploadLimitStatus.targetCompatible == true &&
        uploadLimitStatus.currentMegabytes != null &&
        !uploadLimitStatus.checking
    confirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(request.title) },
            text = { Text(request.detail) },
            confirmButton = {
                Button(onClick = { confirmation = null; request.action() }) { Text("确认执行") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmation = null }) { Text("取消") }
            },
        )
    }

    SectionPanel(title = "修复工具", accentColor = LukoaColors.Amber) {
        Text(if (tavernRunning) "检测到酒馆可能正在运行，请先回到启动页停止酒馆。" else "修复只作用于当前选中的酒馆实例，执行前会保留恢复副本。")
        SecondaryActionButton(
            text = "修复 npm 依赖",
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !tavernRunning,
            accentColor = LukoaColors.Amber,
            onClick = {
                confirmation = RepairConfirmation(
                    "重新安装依赖",
                    "旧的 node_modules 会先被移到带时间戳的恢复目录。只有 npm install 成功后才会清理旧副本。此操作可能需要数分钟。",
                    onRepairDependencies,
                )
            },
        )
        SecondaryActionButton(
            text = "网页打不开时重置主题",
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !tavernRunning,
            accentColor = LukoaColors.Text,
            onClick = {
                confirmation = RepairConfirmation(
                    "重置网页主题",
                    "将搜索当前用户设置并把主题重置为 Dark Lite。原设置文件会保留带时间戳的副本；找不到兼容设置时不会修改任何文件。",
                    onResetTheme,
                )
            },
        )
        Text("Node.js 内存上限")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2048, 4096, 6144).forEach { memory ->
                SecondaryActionButton(
                    text = "${memory / 1024}GB",
                    modifier = Modifier.weight(1f),
                    enabled = !actionsLocked && !tavernRunning,
                    accentColor = LukoaColors.Text,
                    onClick = {
                        confirmation = RepairConfirmation(
                            "设置 ${memory / 1024}GB 内存上限",
                            "启动器会写入独立环境配置，不直接改动 start.sh。低内存设备设置过高可能导致系统杀掉 Termux。",
                        ) { onSetNodeMemory(memory) }
                    },
                )
            }
        }
    }

    SectionPanel(title = "聊天记录上传限制", accentColor = LukoaColors.Accent) {
        val currentLabel = when {
            uploadLimitStatus.checking -> "检查中…"
            uploadLimitStatus.currentMegabytes != null ->
                TavernUploadLimitPolicy.label(uploadLimitStatus.currentMegabytes)
            else -> "尚未读取"
        }
        Text(
            text = "当前检查结果：$currentLabel",
            color = LukoaColors.Text,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                text = when (uploadLimitStatus.patchState) {
                    TavernUploadLimitPatchState.Active -> "补丁已应用"
                    TavernUploadLimitPatchState.NotManaged -> "补丁未应用"
                    TavernUploadLimitPatchState.ChangedOrOverwritten -> "补丁可能失效"
                    TavernUploadLimitPatchState.Unknown -> "状态待确认"
                },
                active = uploadLimitStatus.patchState == TavernUploadLimitPatchState.Active,
                modifier = Modifier.weight(1f),
            )
            StatusPill(
                text = when (uploadLimitStatus.targetCompatible) {
                    true -> "目标代码已唯一识别"
                    false -> "目标代码不兼容"
                    null -> "等待兼容检查"
                },
                active = uploadLimitStatus.targetCompatible == true,
                modifier = Modifier.weight(1f),
            )
        }
        Text("大文件会明显增加 Termux 内存和处理时间。1GB 以上更容易触发系统杀后台，修改后建议重启酒馆。")
        Text(uploadLimitStatus.message)
        uploadLimitStatus.recordedPreviousMegabytes?.let { previous ->
            val applied = uploadLimitStatus.recordedAppliedMegabytes
            Text(
                text = buildString {
                    append("修改前：${TavernUploadLimitPolicy.label(previous)}")
                    if (applied != null) {
                        append(" · 已记录目标：${TavernUploadLimitPolicy.label(applied)}")
                    }
                },
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TavernUploadLimitPolicy.allowedMegabytes.forEach { limit ->
                SecondaryActionButton(
                    text = TavernUploadLimitPolicy.label(limit),
                    modifier = Modifier.weight(1f),
                    enabled = !actionsLocked && !tavernRunning && uploadTargetReady,
                    accentColor = if (uploadLimitStatus.currentMegabytes == limit) LukoaColors.Accent else LukoaColors.Text,
                    onClick = {
                        val label = TavernUploadLimitPolicy.label(limit)
                        confirmation = RepairConfirmation(
                            "设置上传限制为 $label",
                            "只会修改当前实例中已识别的 SillyTavern 上传中间件。修改前会备份源文件并记录原值；如果版本结构不匹配，将拒绝修改。设置较大限制可能导致内存占用显著增加。",
                        ) { onSetUploadLimit(limit) }
                    },
                )
            }
        }
        if (!uploadTargetReady && !uploadLimitStatus.checking) {
            Text("请先重新检查当前限制。只有唯一识别到兼容目标代码后，修改按钮才会启用。")
        }
        Text("修改前会记录原值并备份目标文件。ST 更新或回退后会重新检查；无法识别兼容代码时会拒绝修改。")
        SecondaryActionButton(
            text = if (uploadLimitStatus.checking) "检查中…" else "重新检查当前限制",
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !uploadLimitStatus.checking,
            accentColor = LukoaColors.Text,
            onClick = onCheckUploadLimit,
        )
    }
}
