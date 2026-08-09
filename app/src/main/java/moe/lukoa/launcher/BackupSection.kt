package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class BackupLibraryPathTarget {
    Manual,
    Auto,
}

private const val DEFAULT_VISIBLE_BACKUP_COUNT = 4

@Composable
fun BackupSection(
    activeInstanceLabel: String,
    actionsLocked: Boolean,
    backupListRefreshing: Boolean,
    autoBackupEnabled: Boolean,
    autoBackupIntervalMinutes: Int,
    autoBackupKeepCount: Int,
    backupHistory: List<String>,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails> = emptyMap(),
    onCreateManualBackup: () -> Unit,
    onToggleAutoBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onOpenAutoBackupSettings: () -> Unit,
    onApplyBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackup: () -> Unit,
    onCopyBackupLibraryPath: (BackupLibraryPathTarget) -> Unit,
) {
    var showCopyPathDialog by remember { mutableStateOf(false) }
    val manualBackups = backupHistory.filter { isManualBackupPath(it) }
    val autoBackups = backupHistory.filter { isAutoBackupPath(it) }

    if (showCopyPathDialog) {
        CopyBackupPathDialog(
            onCopyManual = {
                showCopyPathDialog = false
                onCopyBackupLibraryPath(BackupLibraryPathTarget.Manual)
            },
            onCopyAuto = {
                showCopyPathDialog = false
                onCopyBackupLibraryPath(BackupLibraryPathTarget.Auto)
            },
            onDismiss = { showCopyPathDialog = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackupActionsSection(
            activeInstanceLabel = activeInstanceLabel,
            actionsLocked = actionsLocked,
            backupListRefreshing = backupListRefreshing,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            onCreateManualBackup = onCreateManualBackup,
            onImportBackup = onImportBackup,
            onRefreshBackups = onRefreshBackups,
            onCopyBackupLibraryPath = { showCopyPathDialog = true },
            onToggleAutoBackup = onToggleAutoBackup,
            onOpenAutoBackupSettings = onOpenAutoBackupSettings,
        )
        BackupLibrarySection(
            manualBackups = manualBackups,
            autoBackups = autoBackups,
            backupArchiveDetails = backupArchiveDetails,
            actionsLocked = actionsLocked,
            onApplyBackup = onApplyBackup,
            onExportBackup = onExportBackup,
            onCopyBackup = onCopyBackup,
            onRenameBackup = onRenameBackup,
            onDeleteBackup = onDeleteBackup,
        )
    }
}

@Composable
private fun BackupActionsSection(
    activeInstanceLabel: String,
    actionsLocked: Boolean,
    backupListRefreshing: Boolean,
    autoBackupEnabled: Boolean,
    autoBackupIntervalMinutes: Int,
    autoBackupKeepCount: Int,
    onCreateManualBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onCopyBackupLibraryPath: () -> Unit,
    onToggleAutoBackup: () -> Unit,
    onOpenAutoBackupSettings: () -> Unit,
) {
    SectionPanel(
        title = "备份操作",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = activeInstanceLabel,
                    active = true,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看备份操作说明",
                    title = "备份操作",
                    body = "这里只管理当前实例，不会影响其他实例。生成手动备份会新增一份当前数据，不会改动正在使用的酒馆。导入备份也只是把文件放进备份库，不会立即覆盖数据。\n自动备份会按设定时间保存，并只清理超过保留数量的旧自动备份。重要操作前仍建议手动备份一次。",
                )
            }
        },
    ) {
        SettingsGroupLabel("手动与文件")
        SecondaryActionButton(
            text = "生成手动备份",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateManualBackup,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "导入备份",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onImportBackup,
            )
            SecondaryActionButton(
                text = if (backupListRefreshing) "正在刷新..." else "刷新备份列表",
                enabled = !actionsLocked && !backupListRefreshing,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onRefreshBackups,
            )
        }
        SecondaryActionButton(
            text = "复制备份文件夹地址",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCopyBackupLibraryPath,
        )
        SettingsSectionDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "自动备份",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (autoBackupEnabled) {
                        "每 ${formatBackupInterval(autoBackupIntervalMinutes)} 保存一次，最多保留 $autoBackupKeepCount 份。"
                    } else {
                        "目前只在你手动操作时生成备份。"
                    },
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            StatusPill(
                text = if (autoBackupEnabled) "已开启" else "未开启",
                active = autoBackupEnabled,
                toneColor = if (autoBackupEnabled) LukoaColors.Primary else LukoaColors.TextSecondary,
                activeBackground = LukoaColors.PrimarySoft,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = if (autoBackupEnabled) "关闭自动备份" else "开启自动备份",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onToggleAutoBackup,
            )
            SecondaryActionButton(
                text = "修改自动规则",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onOpenAutoBackupSettings,
            )
        }
    }
}

@Composable
private fun BackupLibrarySection(
    manualBackups: List<String>,
    autoBackups: List<String>,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails>,
    actionsLocked: Boolean,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    SectionPanel(
        title = "备份库",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = "${manualBackups.size + autoBackups.size} 份",
                    active = manualBackups.isNotEmpty() || autoBackups.isNotEmpty(),
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看备份库说明",
                    title = "备份库里的操作",
                    body = "导出会把备份另存到你选择的位置；复制会在备份库里再留一份。\n重命名只改变文件名，不会改变备份内容。\n“应用并覆盖”会用备份替换当前实例的数据；“删除”会永久移除这份备份。这两个红色操作都会再次要求确认。",
                )
            }
        },
    ) {
        BackupLibraryGroup(
            title = "手动备份",
            emptyText = "还没有手动备份，可以先在上方生成一份。",
            backups = manualBackups,
            backupArchiveDetails = backupArchiveDetails,
            actionsLocked = actionsLocked,
            onApplyBackup = onApplyBackup,
            onExportBackup = onExportBackup,
            onCopyBackup = onCopyBackup,
            onRenameBackup = onRenameBackup,
            onDeleteBackup = onDeleteBackup,
        )
        BackupLibraryGroup(
            title = "自动备份",
            emptyText = "还没有自动备份。开启后会按设定时间生成。",
            backups = autoBackups,
            backupArchiveDetails = backupArchiveDetails,
            actionsLocked = actionsLocked,
            onApplyBackup = onApplyBackup,
            onExportBackup = onExportBackup,
            onCopyBackup = onCopyBackup,
            onRenameBackup = onRenameBackup,
            onDeleteBackup = onDeleteBackup,
        )
    }
}

@Composable
private fun CopyBackupPathDialog(
    onCopyManual: () -> Unit,
    onCopyAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text("复制备份文件夹地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryActionButton(
                    text = "手动备份文件夹",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCopyManual,
                )
                SecondaryActionButton(
                    text = "自动备份文件夹",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCopyAuto,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}

@Composable
private fun BackupLibraryGroup(
    title: String,
    emptyText: String,
    backups: List<String>,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails>,
    actionsLocked: Boolean,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    var expanded by remember(title, backups.size) { mutableStateOf(false) }
    val visibleBackups = if (expanded) backups else backups.take(DEFAULT_VISIBLE_BACKUP_COUNT)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            StatusPill(
                text = "${backups.size} 份",
                active = backups.isNotEmpty(),
                toneColor = LukoaColors.Primary,
                activeBackground = LukoaColors.PrimarySoft,
            )
        }
        if (backups.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.Elevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LukoaColors.Border),
            ) {
                Text(
                    text = emptyText,
                    modifier = Modifier.padding(12.dp),
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            visibleBackups.forEach { path ->
                BackupRecordLine(
                    path = path,
                    sizeBytes = findBackupArchiveDetails(backupArchiveDetails, path)?.size,
                    backupType = title,
                    actionsLocked = actionsLocked,
                    onApply = { onApplyBackup(path) },
                    onExport = { onExportBackup(path) },
                    onCopy = { onCopyBackup(path) },
                    onRename = { onRenameBackup(path) },
                    onDelete = { onDeleteBackup(path) },
                )
            }
            if (backups.size > DEFAULT_VISIBLE_BACKUP_COUNT) {
                SecondaryActionButton(
                    text = if (expanded) {
                        "收起其余备份"
                    } else {
                        "查看其余 ${backups.size - DEFAULT_VISIBLE_BACKUP_COUNT} 份备份"
                    },
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expanded = !expanded },
                )
            }
        }
    }
}

@Composable
private fun BackupRecordLine(
    path: String,
    sizeBytes: Long?,
    backupType: String,
    actionsLocked: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val fileName = path.substringAfterLast('/')
    Surface(
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fileName,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = backupType,
                    active = true,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.Surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LukoaColors.Border),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "文件大小",
                            modifier = Modifier.weight(1f),
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = sizeBytes?.let(::formatBackupRestorePreviewSize) ?: "正在读取…",
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "文件地址",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = path,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            BackupActionRow {
                SecondaryActionButton(
                    text = "导出",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onExport,
                )
                SecondaryActionButton(
                    text = "复制",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onCopy,
                )
            }
            SecondaryActionButton(
                text = "重命名",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRename,
            )
            BackupActionRow {
                SecondaryActionButton(
                    text = "应用并覆盖",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(1.4f),
                    onClick = onApply,
                )
                SecondaryActionButton(
                    text = "删除",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(0.8f),
                    onClick = onDelete,
                )
            }
        }
    }
}

private fun findBackupArchiveDetails(
    archiveDetails: Map<String, BackupLibraryArchiveDetails>,
    path: String,
): BackupLibraryArchiveDetails? {
    archiveDetails[path]?.let { return it }
    val normalizedPath = path.trim().replace('\\', '/')
    return archiveDetails.values.firstOrNull { details ->
        details.termuxReadablePath.trim().replace('\\', '/').equals(normalizedPath, ignoreCase = true)
    }
}

@Composable
private fun BackupActionRow(content: RowScopeContent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private typealias RowScopeContent = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit

private fun backupLocationLabel(path: String): String {
    return when {
        path.contains("/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}/", ignoreCase = true) -> "手动备份 / Download/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}"
        path.contains("/${BackupLibraryFiles.AUTO_RELATIVE_DIR}/", ignoreCase = true) -> "自动备份 / Download/${BackupLibraryFiles.AUTO_RELATIVE_DIR}"
        path.contains("/${BackupLibraryFiles.LEGACY_ROOT_RELATIVE_DIR}/", ignoreCase = true) -> "不支持的旧位置 / Download/${BackupLibraryFiles.LEGACY_ROOT_RELATIVE_DIR}"
        path.contains("/storage/downloads/", ignoreCase = true) -> "Downloads 备份库"
        else -> "露科亚备份库"
    }
}

private fun isManualBackupPath(path: String): Boolean {
    val normalized = path.trim().replace('\\', '/')
    return normalized.contains("/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}/", ignoreCase = true)
}

private fun isAutoBackupPath(path: String): Boolean {
    val normalized = path.trim().replace('\\', '/')
    return normalized.contains("/${BackupLibraryFiles.AUTO_RELATIVE_DIR}/", ignoreCase = true)
}

@Composable
fun CopyBackupConfirmDialog(
    archivePath: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "复制备份",
                infoText = "会在备份库中生成一份新副本，不会覆盖原文件。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                text = "复制",
                enabled = true,
                tone = ActionTone.Safe,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Safe, onClick = onDismiss)
        },
    )
}

@Composable
fun RenameBackupDialog(
    archivePath: String,
    newName: String,
    backupHistory: List<String>,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nameValidationMessage = LauncherInputGuards.validateBackupRequiredName(newName)
    val targetFileName = LauncherInputGuards.backupFileNameForLabel(newName)
    val duplicatePath = if (nameValidationMessage == null && targetFileName != null) {
        backupHistory.firstOrNull { existingPath ->
            existingPath.trim() != archivePath.trim() &&
                existingPath.substringAfterLast('/') == targetFileName
        }
    } else {
        null
    }
    val validationMessage = nameValidationMessage ?: duplicatePath?.let {
        "已经有同名备份：${backupLocationLabel(it)}。请换个名字。"
    }
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "重命名备份",
                infoText = "只修改备份文件名，不改变备份内容；已有同名文件时会阻止保存。",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = archivePath.substringAfterLast('/'),
                    color = LukoaColors.Primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text("新名称，不需要写 .tar.gz") },
                    placeholder = { Text("例如：更新前-稳定版") },
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
                if (!valid) {
                    Text(
                        text = validationMessage ?: "名称格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "重命名",
                enabled = valid,
                tone = ActionTone.Safe,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}
