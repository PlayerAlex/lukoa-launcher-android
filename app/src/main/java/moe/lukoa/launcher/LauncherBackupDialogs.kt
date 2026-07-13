package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ManualBackupConfirmDialog(
    backupName: String,
    onBackupNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val validationMessage = LauncherInputGuards.validateManualBackupName(backupName)
    val nameOk = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = { Text("生成备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "会生成到 Download/LukoaLauncher/backups/sd，包含酒馆、聊天、角色、插件、配置和密钥。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "填了名字就按这个名字保存；不填会生成 sd-时间.tar.gz。自动备份会进 zd 文件夹。",
                    color = LukoaColors.Amber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = backupName,
                    onValueChange = onBackupNameChange,
                    singleLine = true,
                    label = { Text("备份名称，可留空") },
                    placeholder = { Text("例如：更新前、插件测试前") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
                    ),
                )
                if (!nameOk) {
                    Text(
                        text = validationMessage ?: "名称格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "开始备份",
                enabled = nameOk,
                accentColor = LukoaColors.Accent,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}
@Composable
fun AutoBackupSettingsDialog(
    enabled: Boolean,
    intervalMinutes: Int,
    keepCount: Int,
    actionsLocked: Boolean,
    onDecreaseInterval: () -> Unit,
    onIncreaseInterval: () -> Unit,
    onDecreaseIntervalLarge: () -> Unit,
    onIncreaseIntervalLarge: () -> Unit,
    onDecreaseKeep: () -> Unit,
    onIncreaseKeep: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("自动备份设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (enabled) "自动备份已开启。这里调间隔和保留数量。" else "自动备份未开启。回到备份页点开启。",
                    color = if (enabled) LukoaColors.Accent else LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AutoBackupIntervalPanel(
                    intervalMinutes = intervalMinutes,
                    enabled = !actionsLocked,
                    onDecrease = onDecreaseInterval,
                    onIncrease = onIncreaseInterval,
                    onDecreaseLarge = onDecreaseIntervalLarge,
                    onIncreaseLarge = onIncreaseIntervalLarge,
                )
                AutoBackupKeepPanel(
                    keepCount = keepCount,
                    enabled = !actionsLocked,
                    onDecrease = onDecreaseKeep,
                    onIncrease = onIncreaseKeep,
                )
                Text(
                    text = "间隔范围 10 分钟到 12 小时。只清理 Download/LukoaLauncher/backups/zd 里最旧的自动备份。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "完成",
                enabled = true,
                accentColor = LukoaColors.Amber,
                onClick = onDismiss,
            )
        },
        dismissButton = null,
    )
}

@Composable
private fun AutoBackupIntervalPanel(
    intervalMinutes: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncreaseLarge: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "备份间隔",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "每 ${formatBackupInterval(intervalMinutes)} 一次",
                color = LukoaColors.Text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoBackupAdjustButton("--", enabled, Modifier.weight(1f), onDecreaseLarge)
                AutoBackupAdjustButton("-", enabled, Modifier.weight(1f), onDecrease)
                AutoBackupAdjustButton("+", enabled, Modifier.weight(1f), onIncrease)
                AutoBackupAdjustButton("++", enabled, Modifier.weight(1f), onIncreaseLarge)
            }
            Text(
                text = "- / + 调 10 分钟，-- / ++ 调 1 小时。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AutoBackupKeepPanel(
    keepCount: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "保留数量",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$keepCount 个",
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                AutoBackupAdjustButton("-", enabled, Modifier.weight(0.5f), onDecrease)
                AutoBackupAdjustButton("+", enabled, Modifier.weight(0.5f), onIncrease)
            }
            Text(
                text = "超过这个数量后，从最旧的自动备份开始删除。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AutoBackupAdjustButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SecondaryActionButton(
        text = text,
        enabled = enabled,
        accentColor = LukoaColors.Accent,
        modifier = modifier.height(38.dp),
        onClick = onClick,
    )
}

@Composable
fun ApplyBackupPathDialog(
    path: String,
    onPathChange: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = path.trim()
    val validationMessage = LauncherInputGuards.validateBackupArchivePath(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("选择要应用的备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "建议在备份列表里点“应用”。这里也可以手动填路径。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("备份文件完整路径") },
                    placeholder = { Text("/storage/emulated/0/Download/LukoaLauncher/backups/sd/xxx.tar.gz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Danger,
                    ),
                )
                if (!valid && path.isNotBlank()) {
                    Text(
                        text = validationMessage ?: "路径格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "下一步",
                enabled = valid,
                tone = ActionTone.Danger,
                onClick = onNext,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}

@Composable
fun ApplyBackupPreviewLoadingDialog(
    archivePath: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("正在读取备份信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "正在后台读取备份名称、时间和大小，请稍候。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = archivePath,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton("取消读取", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}

@Composable
fun ApplyBackupPreviewDialog(
    preview: BackupRestorePreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = "确认应用备份",
        titleTone = ActionTone.Danger,
        confirmText = "确认覆盖并恢复",
        confirmTone = ActionTone.Danger,
        confirmEnabled = !preview.targetWasRunning,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        VersionInfoLine(
            "备份",
            "${preview.backupName} · ${formatBackupRestorePreviewSize(preview.sizeBytes)}",
        )
        VersionInfoLine("备份时间", formatBackupRestorePreviewTime(preview.modifiedAtMillis))
        VersionInfoLine(
            "目标实例",
            "${preview.targetInstanceLabel} · 端口 ${preview.targetPort}",
        )
        VersionInfoLine("目标目录", preview.restoreTargetDir)
        VersionInfoLine(
            "酒馆状态",
            if (preview.targetWasRunning) "运行中，请先停止" else "已停止",
        )
        StateNote(
            text = if (preview.targetWasRunning) {
                "检测到目标实例仍在运行。请取消并停止酒馆，再重新打开恢复预览。"
            } else {
                "会覆盖当前数据。启动器不会自动复制一份当前酒馆；需要保留时，请先取消并生成手动备份。"
            },
            tone = if (preview.targetWasRunning) LukoaPillTone.Warning else LukoaPillTone.Danger,
        )
        Text(
            text = preview.archivePath,
            color = LukoaColors.Dim,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DeleteBackupConfirmDialog(
    archivePath: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认删除备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "删除后不能从这里恢复，但不会删除酒馆本体。",
                    color = LukoaColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = archivePath,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认删除",
                enabled = true,
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
        },
    )
}
