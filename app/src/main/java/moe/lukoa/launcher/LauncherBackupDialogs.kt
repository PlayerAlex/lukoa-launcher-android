package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "生成备份",
                infoText = "生成后会在手机的手动备份文件夹中新增一份备份，不会删除或替换当前酒馆数据。\n备份包含聊天、角色、世界书、插件、设置和密钥。名称可以留空，启动器会按当前时间自动命名。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = backupName,
                    onValueChange = onBackupNameChange,
                    singleLine = true,
                    label = { Text("备份名称，可留空") },
                    placeholder = { Text("例如：更新前、插件测试前") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.TextPrimary,
                        unfocusedTextColor = LukoaColors.TextPrimary,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.Elevated,
                        unfocusedContainerColor = LukoaColors.Elevated,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Primary,
                        unfocusedBorderColor = LukoaColors.Border,
                        disabledBorderColor = LukoaColors.Border,
                        focusedLabelColor = LukoaColors.Primary,
                        unfocusedLabelColor = LukoaColors.TextSecondary,
                        cursorColor = LukoaColors.Primary,
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
                accentColor = LukoaColors.Primary,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Primary, onClick = onDismiss)
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
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "自动备份规则",
                infoText = "备份间隔决定多久自动保存一次，可设为 10 分钟到 12 小时。\n保留数量决定最多留下几份自动备份；超过后只清理最旧的自动备份，不会删除手动备份。\n不知道怎么选时，保持当前规则即可。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusPill(
                    text = if (enabled) "自动备份已开启" else "自动备份未开启",
                    active = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    toneColor = if (enabled) LukoaColors.Primary else LukoaColors.TextSecondary,
                    activeBackground = LukoaColors.PrimarySoft,
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
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "完成",
                enabled = true,
                accentColor = LukoaColors.Primary,
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
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
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
                Text(
                    text = "备份间隔",
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "每 ${formatBackupInterval(intervalMinutes)} 一次",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoBackupAdjustButton("少 10 分钟", enabled, Modifier.weight(1f), onDecrease)
                AutoBackupAdjustButton("多 10 分钟", enabled, Modifier.weight(1f), onIncrease)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoBackupAdjustButton("少 1 小时", enabled, Modifier.weight(1f), onDecreaseLarge)
                AutoBackupAdjustButton("多 1 小时", enabled, Modifier.weight(1f), onIncreaseLarge)
            }
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
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
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
                Text(
                    text = "保留数量",
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$keepCount 份",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AutoBackupAdjustButton("少 1 份", enabled, Modifier.weight(1f), onDecrease)
                AutoBackupAdjustButton("多 1 份", enabled, Modifier.weight(1f), onIncrease)
            }
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
    val feedbackClick = rememberFeedbackClick(onClick)
    OutlinedButton(
        onClick = feedbackClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = LukoaCapsuleShape,
        border = BorderStroke(
            1.dp,
            if (enabled) LukoaColors.Primary.copy(alpha = 0.3f) else LukoaColors.Border,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) LukoaColors.Primary.copy(alpha = 0.05f) else LukoaColors.Elevated,
            contentColor = if (enabled) LukoaColors.Primary else LukoaColors.Dim,
            disabledContainerColor = LukoaColors.Elevated,
            disabledContentColor = LukoaColors.Dim,
        ),
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
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
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "选择要应用的备份",
                infoText = "通常直接在备份库中找到目标备份，再点“应用”即可。\n只有备份文件不在备份库中时，才需要在这里填写它的完整路径。\n继续后还会显示一次确认；确认应用后，备份中的数据会替换当前酒馆里的对应数据。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("备份文件完整路径") },
                    placeholder = { Text("/storage/emulated/0/Download/LukoaLauncher/backups/sd/xxx.tar.gz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.TextPrimary,
                        unfocusedTextColor = LukoaColors.TextPrimary,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.Elevated,
                        unfocusedContainerColor = LukoaColors.Elevated,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Border,
                        disabledBorderColor = LukoaColors.Border,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.TextSecondary,
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
            DialogActionButton("取消", tone = ActionTone.Neutral, onClick = onDismiss)
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
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text("正在读取备份信息") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "正在后台读取备份名称、时间和大小，请稍候。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = archivePath,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton("取消读取", tone = ActionTone.Neutral, onClick = onDismiss)
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
        confirmText = "确认应用",
        confirmTone = ActionTone.Danger,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "会把选中的备份直接恢复到酒馆目录，并覆盖当前酒馆数据。",
            color = LukoaColors.Danger,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "启动器不会自动复制一份当前酒馆。需要保留当前数据时，请先手动备份。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "应用前请确认酒馆已经停止。若 Termux 没有存储权限，启动器会提示你授权。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Elevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionInfoLine("备份名", preview.backupName)
                VersionInfoLine("备份时间", formatBackupRestorePreviewTime(preview.modifiedAtMillis))
                VersionInfoLine("文件大小", formatBackupRestorePreviewSize(preview.sizeBytes))
                VersionInfoLine("恢复到", preview.restoreTargetDir)
                Text(
                    text = preview.archivePath,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Danger.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Danger.copy(alpha = 0.28f)),
        ) {
            Text(
                text = "确认后会覆盖这个目录里的当前酒馆内容。聊天、角色、配置和插件都会按这个备份恢复。",
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.TextPrimary,
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
                    color = LukoaColors.TextSecondary,
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
            DialogActionButton("取消", tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}
