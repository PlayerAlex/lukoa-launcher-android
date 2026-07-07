package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun ExportLogDialog(
    onExportTermux: () -> Unit,
    onExportApp: () -> Unit,
    onExportBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("导出运行日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "导出包含清除后累计内容。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只导出 Termux 调用",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTermux,
                )
                SecondaryActionButton(
                    text = "只导出 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportApp,
                )
                SecondaryActionButton(
                    text = "两个都导出",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun ClearLogScopeDialog(
    onClearTermux: () -> Unit,
    onClearApp: () -> Unit,
    onClearBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("选择清除范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "只清空这里的显示，不删酒馆文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只清除 Termux 调用",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearTermux,
                )
                SecondaryActionButton(
                    text = "只清除 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearApp,
                )
                SecondaryActionButton(
                    text = "两个都清除",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun ClearLogDangerDialog(
    mode: ExportLogMode,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val target = when (mode) {
        ExportLogMode.TermuxOnly -> "Termux 调用返回"
        ExportLogMode.AppOnly -> "App 操作反馈"
        ExportLogMode.Both -> "Termux 调用返回和 App 操作反馈"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认清除日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "将清除：$target。",
                    color = LukoaColors.Text,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "清除后这里看不到旧记录，但不删酒馆文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = onConfirmTextChange,
                    singleLine = true,
                    label = { Text("输入“清除”继续") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Danger,
                    ),
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认清除",
                enabled = confirmText.trim() == "清除",
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton("返回", tone = ActionTone.Danger, onClick = onBack)
                DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
            }
        },
    )
}


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
                    text = "会生成到 Download/lukoa/backups/sd，包含酒馆、聊天、角色、插件、配置和密钥。",
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
                    text = "间隔范围 10 分钟到 12 小时。只清理 Download/lukoa/backups/zd 里最旧的自动备份。",
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
                    placeholder = { Text("/storage/emulated/0/Download/lukoa/backups/sd/xxx.tar.gz") },
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
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "应用前请确认酒馆已经停止。若 Termux 没有存储权限，启动器会提示你授权。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
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
                    color = LukoaColors.Muted,
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
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RiskyActionDialogScaffold(
    title: String,
    titleTone: ActionTone,
    confirmText: String,
    confirmTone: ActionTone,
    confirmEnabled: Boolean = true,
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = titleTone.color(),
        textContentColor = LukoaColors.Text,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } },
        confirmButton = {
            DialogActionButton(
                text = confirmText,
                enabled = confirmEnabled,
                tone = confirmTone,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(dismissText, tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}

@Composable
fun TermuxStoragePermissionDialog(
    archivePath: String,
    actionsLocked: Boolean,
    onGrantPermission: () -> Unit,
    onRetryApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("需要 Termux 存储权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Termux 现在读不到 Downloads 里的备份。请先授权，否则不能应用备份。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开 Termux。看到权限弹窗时点允许，再回启动器继续。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (archivePath.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LukoaColors.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = archivePath,
                            modifier = Modifier.padding(10.dp),
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton(
                    text = "去授权",
                    enabled = !actionsLocked,
                    tone = ActionTone.Warning,
                    onClick = onGrantPermission,
                )
                DialogActionButton(
                    text = "我已授权，继续应用",
                    enabled = !actionsLocked && archivePath.isNotBlank(),
                    tone = ActionTone.Safe,
                    onClick = onRetryApply,
                )
            }
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}

@Composable
fun BackgroundRunPermissionDialog(
    granted: Boolean,
    onOpenPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text(if (granted) "后台运行已允许" else "需要后台运行权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (granted) {
                        "系统已经放行后台运行。若自动备份还是卡住，可以再进一次权限页检查省电策略。"
                    } else {
                        "自动备份想在你离开软件后也准时运行，需要把露科亚启动器加入后台运行白名单。"
                    },
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开系统页面。部分手机还要额外允许后台运行、自启动或取消省电限制。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = if (granted) "重新打开权限页" else "去授权",
                tone = ActionTone.Warning,
                onClick = onOpenPermission,
            )
        },
        dismissButton = {
            DialogActionButton("稍后", tone = ActionTone.Safe, onClick = onDismiss)
        },
    )
}

@Composable
fun FirstTavernStartGuideDialog(
    guide: FirstTavernStartGuide,
    onPrimaryAction: () -> Unit,
    onContinueStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission -> "第一次启动前先开 Termux 后台"
        FirstTavernStartGuideKind.KeepTermuxInSmallWindow -> "第一次启动前先把 Termux 挂小窗"
    }
    val summary = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission ->
            "这台手机看起来是 iQOO。第一次启动酒馆前，建议先给 Termux 打开后台运行或省电白名单。"

        FirstTavernStartGuideKind.KeepTermuxInSmallWindow ->
            "这台手机不是 iQOO。第一次启动酒馆时，更稳的做法是先把 Termux 挂到小窗或分屏，不要直接完全退到后台。"
    }
    val detail = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission ->
            "不然系统更容易在后台把 Termux 停掉，安装、启动和日志同步都可能中断。点下面按钮可直接去给 Termux 授权。"

        FirstTavernStartGuideKind.KeepTermuxInSmallWindow ->
            "启动器会先打开 Termux，再自动回到启动器。如果系统没有保住 Termux，请手动把 Termux 挂到小窗或分屏后再继续启动。"
    }
    val primaryLabel = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission -> "给 Termux 开后台"
        FirstTavernStartGuideKind.KeepTermuxInSmallWindow -> "唤醒 Termux 并返回"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LukoaColors.Amber.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
                ) {
                    Text(
                        text = detail,
                        modifier = Modifier.padding(12.dp),
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                DialogActionButton(
                    text = primaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrimaryAction,
                )
                DialogActionButton(
                    text = "我已了解，继续启动",
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinueStart,
                )
                DialogActionButton(
                    text = "稍后再看",
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
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

@Composable
fun ImportBackupDialog(
    path: String,
    onPathChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = path.trim()
    val validationMessage = LauncherInputGuards.validateBackupArchivePath(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("导入备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写 .tar.gz 备份路径。导入前会先检查。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = onPathChange,
                    label = { Text("备份文件完整路径") },
                    placeholder = { Text("\$HOME/storage/downloads/xxx.tar.gz") },
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
                text = "导入",
                enabled = valid,
                tone = ActionTone.Warning,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}


@Composable
fun UpdateAvailableDialog(
    updateInfo: GithubUpdateInfo,
    currentVersionName: String,
    downloading: Boolean,
    onInstall: () -> Unit,
    onOpenRelease: () -> Unit,
    onClearBadge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val publishedText = remember(updateInfo.publishedAt) {
        formatGithubPublishedTime(updateInfo.publishedAt)
    }
    val formattedReleaseNotes = remember(updateInfo.versionName, updateInfo.body) {
        GithubReleaseNotesFormatter.format(updateInfo.versionName, updateInfo.body)
    }
    val primaryActionText = when {
        downloading -> "下载中..."
        updateInfo.apkDownloadUrl.isBlank() -> "打开发布页"
        else -> "立即更新"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("发现${updateInfo.releaseTypeLabel} v${updateInfo.versionName}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VersionStatusValueCard(
                                label = "当前版本",
                                value = "v$currentVersionName",
                                accentColor = LukoaColors.Muted,
                                modifier = Modifier.weight(1f),
                            )
                            VersionStatusValueCard(
                                label = "新版本",
                                value = "v${updateInfo.versionName}",
                                accentColor = LukoaColors.Accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        VersionInfoLine("版本类型", updateInfo.releaseTypeLabel)
                        if (publishedText.isNotBlank()) {
                            VersionInfoLine("发布时间", publishedText)
                        }
                        if (updateInfo.releaseName.isNotBlank() && updateInfo.releaseName != updateInfo.tagName) {
                            VersionInfoLine("版本标题", updateInfo.releaseName)
                        }
                    }
                }
                if (updateInfo.prerelease) {
                    Surface(
                        color = LukoaColors.AmberSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "这是测试版更新，功能会更早到，但稳定性可能不如正式版。",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = LukoaColors.Amber,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text(
                    text = "更新内容",
                    color = LukoaColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = formattedReleaseNotes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp, max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = "清除红点后，这个版本不会再自动弹出提醒，但你之后仍然可以手动点右上角版本查看。",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ToneActionButton(
                    text = primaryActionText,
                    enabled = !downloading,
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onInstall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToneActionButton(
                        text = "详情",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelease,
                    )
                    ToneActionButton(
                        text = "清除红点",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onClearBadge,
                    )
                }
                ToneActionButton(
                    text = "稍后",
                    enabled = true,
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
internal fun GithubUpdateStatusCard(
    githubUpdateState: GithubUpdateUiState,
) {
    val statusText: String
    val statusColor: Color
    when {
        githubUpdateState.downloading -> {
            statusText = "正在下载"
            statusColor = LukoaColors.Accent
        }
        githubUpdateState.checking -> {
            statusText = "正在检查"
            statusColor = LukoaColors.Amber
        }
        githubUpdateState.hasUpdate -> {
            statusText = "发现新版本"
            statusColor = LukoaColors.Accent
        }
        githubUpdateState.latest != null -> {
            statusText = "已是最新"
            statusColor = LukoaColors.Muted
        }
        githubUpdateState.repository.isBlank() -> {
            statusText = "未配置仓库"
            statusColor = LukoaColors.Amber
        }
        else -> {
            statusText = "等待检查"
            statusColor = LukoaColors.Muted
        }
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当前状态",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = githubUpdateState.message,
                color = if (githubUpdateState.hasUpdate) LukoaColors.Text else LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            VersionInfoLine("更新通道", githubUpdateState.channel.label)
            githubUpdateState.latest?.let { latest ->
                VersionInfoLine("GitHub 最新", "v${latest.versionName}")
                VersionInfoLine("版本类型", latest.releaseTypeLabel)
                if (latest.prerelease) {
                    Text(
                        text = "当前读到的是测试版发布，可能比稳定版更早，但不保证和正式版一样稳定。",
                        color = LukoaColors.Amber,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            githubUpdateState.lastCheckedText
                .takeIf { it.isNotBlank() }
                ?.let { checkedText ->
                    VersionInfoLine("上次检查", checkedText)
                }
        }
    }
}

private val GITHUB_UPDATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatGithubPublishedTime(text: String): String {
    if (text.isBlank()) return ""
    return runCatching {
        GITHUB_UPDATE_TIME_FORMATTER.format(
            Instant.parse(text).atZone(ZoneId.systemDefault()),
        )
    }.getOrElse {
        text.replace("T", " ").removeSuffix("Z").take(16)
    }
}

@Composable
fun InstallRiskConfirmDialog(
    confirmation: TavernInstallConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text(confirmation.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = confirmation.summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        confirmation.details.forEach { item ->
                            Text(
                                text = "• $item",
                                color = LukoaColors.Text,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "继续安装",
                tone = ActionTone.Safe,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun StartPreflightConfirmDialog(
    result: TavernStartPreflightResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = {
            Text(result.title.ifBlank { "启动前发现问题" })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = result.summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                result.details.takeIf { it.isNotEmpty() }?.let { details ->
                    Surface(
                        color = LukoaColors.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.forEach { item ->
                                Text(
                                    text = "• $item",
                                    color = LukoaColors.Text,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            result.action?.let { action ->
                DialogActionButton(
                    text = action.label,
                    tone = when (action.type) {
                        TavernStartPreflightActionType.PrepareTermuxEnvironment,
                        TavernStartPreflightActionType.StopDetectedProcess -> ActionTone.Warning

                        TavernStartPreflightActionType.DownloadTermux,
                        TavernStartPreflightActionType.RequestRunPermission,
                        TavernStartPreflightActionType.CopyExternalAppsCommand,
                        TavernStartPreflightActionType.ChooseDetectedDirectory,
                        TavernStartPreflightActionType.OpenPathSettings,
                        TavernStartPreflightActionType.ReturnToTavern,
                        TavernStartPreflightActionType.Retry -> ActionTone.Safe
                    },
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            DialogActionButton(
                text = if (result.action == null) "知道了" else "稍后",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun StopTavernConfirmDialog(
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = "确认停止酒馆",
        titleTone = ActionTone.Danger,
        confirmText = "确认停止",
        confirmTone = ActionTone.Danger,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "停止后，当前酒馆网页会断开连接。确认没有重要操作在跑，再继续。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "这一步不会删除聊天、角色、世界书或备份文件。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun TavernVersionActionConfirmDialog(
    confirmation: TavernVersionActionConfirmation,
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = confirmation.kind.dialogTitle,
        titleTone = ActionTone.Warning,
        confirmText = confirmation.kind.confirmLabel,
        confirmTone = ActionTone.Warning,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = confirmation.summary,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionInfoLine("当前版本", confirmation.currentVersion)
                VersionInfoLine("目标版本", confirmation.targetVersion)
                VersionInfoLine("当前源", confirmation.sourceLabel)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Amber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
        ) {
            Text(
                text = confirmation.detail,
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = confirmation.riskTip,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun TavernDirectoryChoiceDialog(
    currentPath: String,
    candidates: List<String>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("选一个酒馆目录")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "检测到多个酒馆目录。点一个，启动器会自动切过去。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "当前路径：$currentPath",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        candidates.forEach { candidate ->
                            OutlinedButton(
                                onClick = { onChoose(candidate) },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.46f)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = LukoaColors.Accent.copy(alpha = 0.08f),
                                    contentColor = LukoaColors.Accent,
                                ),
                            ) {
                                Text(
                                    text = candidate,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = LukoaColors.Text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}
