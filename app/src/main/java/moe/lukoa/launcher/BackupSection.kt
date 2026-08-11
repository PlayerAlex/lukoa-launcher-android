package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    actionsLocked: Boolean,
    backupListRefreshing: Boolean,
    autoBackupEnabled: Boolean,
    backupHistory: List<String>,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails> = emptyMap(),
    backupContentStates: Map<String, BackupContentCatalogState> = emptyMap(),
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
    val manualBackups = backupHistory.filter { isManualBackupPath(it) }
    val autoBackups = backupHistory.filter { isAutoBackupPath(it) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackupActionsSection(
            manualBackupCount = manualBackups.size,
            autoBackupCount = autoBackups.size,
            actionsLocked = actionsLocked,
            autoBackupEnabled = autoBackupEnabled,
            onCreateManualBackup = onCreateManualBackup,
            onToggleAutoBackup = onToggleAutoBackup,
            onOpenAutoBackupSettings = onOpenAutoBackupSettings,
        )
        BackupLibrarySection(
            manualBackups = manualBackups,
            autoBackups = autoBackups,
            backupArchiveDetails = backupArchiveDetails,
            backupContentStates = backupContentStates,
            actionsLocked = actionsLocked,
            backupListRefreshing = backupListRefreshing,
            onImportBackup = onImportBackup,
            onRefreshBackups = onRefreshBackups,
            onCopyBackupLibraryPath = onCopyBackupLibraryPath,
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
    manualBackupCount: Int,
    autoBackupCount: Int,
    actionsLocked: Boolean,
    autoBackupEnabled: Boolean,
    onCreateManualBackup: () -> Unit,
    onToggleAutoBackup: () -> Unit,
    onOpenAutoBackupSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                BackupStatusLine(
                    label = "当前状态：",
                    value = if (autoBackupEnabled) "自动备份已开启" else "自动备份未开启",
                    accent = autoBackupEnabled,
                )
                BackupStatusLine("手动备份库：", "$manualBackupCount 份", accent = true)
                BackupStatusLine("自动备份库：", "$autoBackupCount 份", accent = true)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = "创建手动备份",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onCreateManualBackup,
            )
            SecondaryActionButton(
                text = "敬请期待",
                enabled = false,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = if (autoBackupEnabled) "关闭自动备份" else "开始自动备份",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onToggleAutoBackup,
            )
            SecondaryActionButton(
                text = "自动备份规则",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1f),
                onClick = onOpenAutoBackupSettings,
            )
        }
    }
}

@Composable
private fun BackupStatusLine(
    label: String,
    value: String,
    accent: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            color = if (accent) LukoaColors.Primary else LukoaColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BackupLibrarySection(
    manualBackups: List<String>,
    autoBackups: List<String>,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails>,
    backupContentStates: Map<String, BackupContentCatalogState>,
    actionsLocked: Boolean,
    backupListRefreshing: Boolean,
    onImportBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onCopyBackupLibraryPath: (BackupLibraryPathTarget) -> Unit,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "备份库",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                InfoPopoverButton(
                    contentDescription = "查看备份库说明",
                    title = "备份库里的操作",
                    body = "启动器会自动读取并缓存每份备份的内容分类。\n导出会把备份另存到你选择的位置；复制会在备份库里再留一份；重命名只改变文件名。\n应用和删除需要连续确认，避免误操作。",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryActionButton(
                    text = "导入备份",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onImportBackup,
                )
                SecondaryActionButton(
                    text = if (backupListRefreshing) "正在刷新..." else "刷新备份库",
                    enabled = !actionsLocked && !backupListRefreshing,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onRefreshBackups,
                )
            }
            BackupLibraryGroup(
                title = "手动备份库",
                pathLabel = "Download/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}",
                emptyText = "还没有手动备份，可以先在上方创建一份。",
                backups = manualBackups,
                manual = true,
                backupArchiveDetails = backupArchiveDetails,
                backupContentStates = backupContentStates,
                actionsLocked = actionsLocked,
                onCopyPath = { onCopyBackupLibraryPath(BackupLibraryPathTarget.Manual) },
                onApplyBackup = onApplyBackup,
                onExportBackup = onExportBackup,
                onCopyBackup = onCopyBackup,
                onRenameBackup = onRenameBackup,
                onDeleteBackup = onDeleteBackup,
            )
            BackupLibraryGroup(
                title = "自动备份库",
                pathLabel = "Download/${BackupLibraryFiles.AUTO_RELATIVE_DIR}",
                emptyText = "还没有自动备份。开启后会按设定时间生成。",
                backups = autoBackups,
                manual = false,
                backupArchiveDetails = backupArchiveDetails,
                backupContentStates = backupContentStates,
                actionsLocked = actionsLocked,
                onCopyPath = { onCopyBackupLibraryPath(BackupLibraryPathTarget.Auto) },
                onApplyBackup = onApplyBackup,
                onExportBackup = onExportBackup,
                onCopyBackup = onCopyBackup,
                onRenameBackup = onRenameBackup,
                onDeleteBackup = onDeleteBackup,
            )
        }
    }
}

@Composable
private fun BackupLibraryGroup(
    title: String,
    pathLabel: String,
    emptyText: String,
    backups: List<String>,
    manual: Boolean,
    backupArchiveDetails: Map<String, BackupLibraryArchiveDetails>,
    backupContentStates: Map<String, BackupContentCatalogState>,
    actionsLocked: Boolean,
    onCopyPath: () -> Unit,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    var expanded by remember(title, backups.size) { mutableStateOf(false) }
    val visibleBackups = if (expanded) backups else backups.take(DEFAULT_VISIBLE_BACKUP_COUNT)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (manual) LukoaColors.PrimarySoft else LukoaColors.Elevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (manual) LukoaColors.Primary.copy(alpha = 0.38f) else LukoaColors.Border,
        ),
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = pathLabel,
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SecondaryActionButton(
                    text = "复制地址",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    onClick = onCopyPath,
                )
            }
            if (backups.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LukoaColors.Surface,
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
                        contentState = findBackupContentState(backupContentStates, path),
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
                        text = if (expanded) "收起其余备份" else "查看其余 ${backups.size - DEFAULT_VISIBLE_BACKUP_COUNT} 份备份",
                        enabled = true,
                        accentColor = LukoaColors.Primary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { expanded = !expanded },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupRecordLine(
    path: String,
    sizeBytes: Long?,
    contentState: BackupContentCatalogState?,
    actionsLocked: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val fileName = path.substringAfterLast('/')
    var contentExpanded by remember(path) { mutableStateOf(false) }
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
                    SettingsSectionDivider()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { contentExpanded = !contentExpanded },
                        color = LukoaColors.Surface,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = if (contentExpanded) 3.dp else 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "备份内容",
                                modifier = Modifier.weight(1f),
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = if (contentExpanded) "收起" else "展开",
                                color = LukoaColors.Primary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    if (contentExpanded) {
                        when {
                            contentState?.summary != null -> {
                                val summary = contentState.summary
                                if (summary.groups.isEmpty()) {
                                    Text(
                                        text = "没有识别到可单独列出的角色卡、预设或其他用户内容。",
                                        color = LukoaColors.TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else {
                                    summary.displayGroups.forEach { group ->
                                        BackupContentGroupRow(group)
                                    }
                                }
                            }
                            contentState?.errorMessage?.isNotBlank() == true -> {
                                Text(
                                    text = contentState.errorMessage,
                                    color = LukoaColors.Accent,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            else -> {
                                Text(
                                    text = if (contentState?.isLoading == true) {
                                        "正在自动读取并保存内容摘要…"
                                    } else {
                                        "等待自动读取内容摘要…"
                                    },
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            BackupActionRow {
                TimedDangerFeedbackActionButton(
                    text = "应用",
                    enabled = !actionsLocked,
                    modifier = Modifier.weight(1f),
                    onConfirmed = onApply,
                )
                SecondaryActionButton(
                    text = "复制",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onCopy,
                )
                SecondaryActionButton(
                    text = "导出",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onExport,
                )
            }
            BackupActionRow {
                SecondaryActionButton(
                    text = "重命名",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onRename,
                )
                TimedDangerFeedbackActionButton(
                    text = "删除",
                    enabled = !actionsLocked,
                    modifier = Modifier.weight(1f),
                    onConfirmed = onDelete,
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

private fun findBackupContentState(
    contentStates: Map<String, BackupContentCatalogState>,
    path: String,
): BackupContentCatalogState? {
    contentStates[path]?.let { return it }
    val normalizedPath = path.trim().replace('\\', '/')
    return contentStates.entries.firstOrNull { (storedPath, _) ->
        storedPath.trim().replace('\\', '/').equals(normalizedPath, ignoreCase = true)
    }?.value
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
