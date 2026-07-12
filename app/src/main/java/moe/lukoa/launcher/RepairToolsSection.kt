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

    SectionPanel(title = "酒馆修复工具", accentColor = LukoaColors.Amber) {
        Text(if (tavernRunning) "检测到酒馆可能正在运行，请先回到启动页停止酒馆。" else "修复只作用于当前选中的酒馆实例，执行前会保留恢复副本。")
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !tavernRunning,
            onClick = {
                confirmation = RepairConfirmation(
                    "重新安装依赖",
                    "旧的 node_modules 会先被移到带时间戳的恢复目录。只有 npm install 成功后才会清理旧副本。此操作可能需要数分钟。",
                    onRepairDependencies,
                )
            },
        ) { Text("修复 npm 依赖") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !tavernRunning,
            onClick = {
                confirmation = RepairConfirmation(
                    "重置网页主题",
                    "将搜索当前用户设置并把主题重置为 Dark Lite。原设置文件会保留带时间戳的副本；找不到兼容设置时不会修改任何文件。",
                    onResetTheme,
                )
            },
        ) { Text("网页打不开时重置主题") }
        Text("Node.js 内存上限")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2048, 4096, 6144).forEach { memory ->
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !actionsLocked && !tavernRunning,
                    onClick = {
                        confirmation = RepairConfirmation(
                            "设置 ${memory / 1024}GB 内存上限",
                            "启动器会写入独立环境配置，不直接改动 start.sh。低内存设备设置过高可能导致系统杀掉 Termux。",
                        ) { onSetNodeMemory(memory) }
                    },
                ) { Text("${memory / 1024}GB") }
            }
        }
        Text("聊天记录上传限制")
        Text("大文件会明显增加 Termux 内存和处理时间。1GB 以上更容易触发系统杀后台，修改后建议重启酒馆。")
        Text(
            uploadLimitStatus.currentMegabytes?.let {
                "当前限制：${TavernUploadLimitPolicy.label(it)}"
            } ?: if (uploadLimitStatus.checking) {
                "当前限制：检查中…"
            } else {
                "当前限制：尚未读取"
            },
        )
        Text(uploadLimitStatus.message)
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !actionsLocked && !uploadLimitStatus.checking,
            onClick = onCheckUploadLimit,
        ) { Text("检查当前上传限制") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TavernUploadLimitPolicy.allowedMegabytes.forEach { limit ->
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !actionsLocked && !tavernRunning,
                    onClick = {
                        val label = TavernUploadLimitPolicy.label(limit)
                        confirmation = RepairConfirmation(
                            "设置上传限制为 $label",
                            "只会修改当前实例中已识别的 SillyTavern 上传中间件。修改前会备份源文件并记录原值；如果版本结构不匹配，将拒绝修改。设置较大限制可能导致内存占用显著增加。",
                        ) { onSetUploadLimit(limit) }
                    },
                ) { Text(TavernUploadLimitPolicy.label(limit)) }
            }
        }
    }
}
