package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        BackupOverviewCard(
            activeInstanceLabel = activeInstanceLabel,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            manualBackupCount = manualBackups.size,
            autoBackupCount = autoBackups.size,
        )
        BackupQuickActionsSection(
            actionsLocked = actionsLocked,
            backupListRefreshing = backupListRefreshing,
            onCreateManualBackup = onCreateManualBackup,
            onImportBackup = onImportBackup,
            onRefreshBackups = onRefreshBackups,
            onCopyBackupLibraryPath = { showCopyPathDialog = true },
        )
        BackupAutomaticSection(
            actionsLocked = actionsLocked,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            onToggleAutoBackup = onToggleAutoBackup,
            onOpenAutoBackupSettings = onOpenAutoBackupSettings,
        )
        BackupLibrarySection(
            manualBackups = manualBackups,
            autoBackups = autoBackups,
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
private fun BackupQuickActionsSection(
    actionsLocked: Boolean,
    backupListRefreshing: Boolean,
    onCreateManualBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRefreshBackups: () -> Unit,
    onCopyBackupLibraryPath: () -> Unit,
) {
    SectionPanel(
        title = "快速操作",
        accentColor = LukoaColors.Accent,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看备份操作说明",
                title = "这些操作会做什么",
                body = "生成手动备份会新增一份当前实例的数据，不会改动正在使用的酒馆。\n导入备份只是把你选的文件放进备份库，也不会立即恢复。\n准备更新、回退、换手机或应用其他备份前，建议先生成一份手动备份。",
            )
        },
    ) {
        SecondaryActionButton(
            text = "生成手动备份",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Accent,
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
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onImportBackup,
            )
            SecondaryActionButton(
                text = if (backupListRefreshing) "正在刷新..." else "刷新备份列表",
                enabled = !actionsLocked && !backupListRefreshing,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onRefreshBackups,
            )
        }
        SecondaryActionButton(
            text = "复制备份文件夹地址",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Accent,
            modifier = Modifier.fillMaxWidth(),
            onClick = onCopyBackupLibraryPath,
        )
    }
}

@Composable
private fun BackupAutomaticSection(
    actionsLocked: Boolean,
    autoBackupEnabled: Boolean,
    autoBackupIntervalMinutes: Int,
    autoBackupKeepCount: Int,
    onToggleAutoBackup: () -> Unit,
    onOpenAutoBackupSettings: () -> Unit,
) {
    SectionPanel(
        title = "自动备份",
        accentColor = LukoaColors.Accent,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看自动备份说明",
                title = "自动备份",
                body = "开启后，启动器会按设定时间自动保存当前实例的数据。\n自动备份达到保留数量后，只会清理最旧的自动备份，不会删除手动备份。\n自动备份适合日常保护；重要操作前仍建议再做一份手动备份。",
            )
        },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (autoBackupEnabled) {
                LukoaColors.AccentSoft.copy(alpha = 0.62f)
            } else {
                LukoaColors.SurfaceAlt
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (autoBackupEnabled) {
                    LukoaColors.Accent.copy(alpha = 0.32f)
                } else {
                    LukoaColors.Line.copy(alpha = 0.4f)
                },
            ),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                    StatusPill(
                        text = if (autoBackupEnabled) "已开启" else "未开启",
                        active = autoBackupEnabled,
                        toneColor = if (autoBackupEnabled) LukoaColors.Accent else LukoaColors.Muted,
                        activeBackground = LukoaColors.AccentSoft,
                    )
                }
                Text(
                    text = if (autoBackupEnabled) {
                        "每 ${formatBackupInterval(autoBackupIntervalMinutes)} 保存一次，最多保留 $autoBackupKeepCount 份。"
                    } else {
                        "现有手动备份和自动备份都不会受到影响。"
                    },
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryActionButton(
                text = if (autoBackupEnabled) "关闭自动备份" else "开启自动备份",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(1f),
                onClick = onToggleAutoBackup,
            )
            SecondaryActionButton(
                text = "修改自动规则",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
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
    actionsLocked: Boolean,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    SectionPanel(
        title = "备份库",
        accentColor = LukoaColors.Accent,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = "${manualBackups.size + autoBackups.size} 份",
                    active = manualBackups.isNotEmpty() || autoBackups.isNotEmpty(),
                    toneColor = LukoaColors.Accent,
                    activeBackground = LukoaColors.AccentSoft,
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
private fun BackupOverviewCard(
    activeInstanceLabel: String,
    autoBackupEnabled: Boolean,
    autoBackupIntervalMinutes: Int,
    autoBackupKeepCount: Int,
    manualBackupCount: Int,
    autoBackupCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, LukoaColors.Line.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "备份概览",
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(
                    text = if (autoBackupEnabled) "自动保护已开启" else "仅手动备份",
                    active = autoBackupEnabled,
                    toneColor = if (autoBackupEnabled) LukoaColors.Accent else LukoaColors.Muted,
                    activeBackground = LukoaColors.AccentSoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看备份内容说明",
                    title = "备份会保存什么",
                    body = "这里备份的是当前实例，不会影响其他实例。\n备份会保存聊天、角色、世界书、插件、设置和密钥。可以重新下载的程序文件和缓存不会保存。\n生成和导入备份都不会改动当前酒馆；只有确认“应用并覆盖”后，当前数据才会被替换。",
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.SurfaceAlt,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.36f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "当前实例",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = activeInstanceLabel,
                        modifier = Modifier.weight(1f),
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BackupMetricCard(
                    label = "手动备份",
                    value = "$manualBackupCount 份",
                    modifier = Modifier.weight(1f),
                )
                BackupMetricCard(
                    label = "自动备份",
                    value = "$autoBackupCount 份",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "自动规则",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (autoBackupEnabled) {
                        "每 ${formatBackupInterval(autoBackupIntervalMinutes)} · 保留 $autoBackupKeepCount 份"
                    } else {
                        "未开启"
                    },
                    modifier = Modifier.weight(1f),
                    color = if (autoBackupEnabled) LukoaColors.Text else LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BackupMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
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
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("复制备份文件夹地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryActionButton(
                    text = "手动备份文件夹",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCopyManual,
                )
                SecondaryActionButton(
                    text = "自动备份文件夹",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
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
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            StatusPill(
                text = "${backups.size} 份",
                active = backups.isNotEmpty(),
                toneColor = LukoaColors.Accent,
                activeBackground = LukoaColors.AccentSoft,
            )
        }
        if (backups.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.SurfaceAlt,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = emptyText,
                    modifier = Modifier.padding(12.dp),
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            visibleBackups.forEach { path ->
                BackupRecordLine(
                    path = path,
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
                    accentColor = LukoaColors.Accent,
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
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
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
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = backupType,
                    active = true,
                    toneColor = LukoaColors.Accent,
                    activeBackground = LukoaColors.AccentSoft,
                )
            }
            BackupActionRow {
                BackupActionButton(
                    text = "导出",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                    onClick = onExport,
                )
                BackupActionButton(
                    text = "复制",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                    onClick = onCopy,
                )
                BackupActionButton(
                    text = "重命名",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                    onClick = onRename,
                )
            }
            BackupActionRow {
                BackupActionButton(
                    text = "应用并覆盖",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(1.4f),
                    onClick = onApply,
                )
                BackupActionButton(
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

@Composable
private fun BackupActionRow(content: RowScopeContent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private typealias RowScopeContent = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit

@Composable
private fun BackupActionButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SecondaryActionButton(
        text = text,
        enabled = enabled,
        accentColor = accentColor,
        modifier = modifier,
        onClick = onClick,
    )
}

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
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
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
                    color = LukoaColors.Muted,
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
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
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
                    color = LukoaColors.Accent,
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
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Accent,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Accent,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Accent,
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
