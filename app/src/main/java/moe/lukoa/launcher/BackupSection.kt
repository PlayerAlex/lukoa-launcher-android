package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class BackupLibraryPathTarget {
    Manual,
    Auto,
}

private enum class BackupSectionView {
    Quick,
    Auto,
    Library,
}

@Composable
fun BackupSection(
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
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    var showCopyPathDialog by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf(BackupSectionView.Quick) }
    val manualBackups = backupHistory.filter { isManualBackupPath(it) }
    val autoBackups = backupHistory.filter { isAutoBackupPath(it) }
    val sectionOptions = listOf(
        SectionSwitchOption(
            value = BackupSectionView.Quick,
            label = "快捷操作",
            description = "生成、导入、刷新和复制路径。",
        ),
        SectionSwitchOption(
            value = BackupSectionView.Auto,
            label = "自动备份",
            description = "开关、间隔和保留数量。",
        ),
        SectionSwitchOption(
            value = BackupSectionView.Library,
            label = "备份库",
            description = "查看和管理手动、自动备份。",
        ),
    )

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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackupOverviewCard(
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            manualBackupCount = manualBackups.size,
            autoBackupCount = autoBackups.size,
        )
        SectionSwitcherCard(
            title = "备份分区",
            options = sectionOptions,
            selected = selectedView,
            onPagerLockChange = onPagerLockChange,
            onSelect = { selectedView = it },
        )

        when (selectedView) {
            BackupSectionView.Quick -> SectionPanel(
                title = "快速操作",
                accentColor = LukoaColors.Accent,
                headerAction = {
                    InfoPopoverButton(
                        contentDescription = "查看备份内容说明",
                        title = "备份内容",
                        body = "备份会保存聊天、角色、世界书、插件、设置和密钥。可以重新下载的程序文件和缓存不会保存。\n准备更新、回退、换手机或应用其他备份前，建议先生成一份手动备份。\n“生成备份”只会新增备份文件；只有点“应用”并确认后，才会替换当前酒馆里的对应数据。",
                    )
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryActionButton(
                        text = "生成备份",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = onCreateManualBackup,
                    )
                    SecondaryActionButton(
                        text = "导入到备份库",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = onImportBackup,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryActionButton(
                        text = if (backupListRefreshing) "刷新中..." else "刷新列表",
                        enabled = !actionsLocked && !backupListRefreshing,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = onRefreshBackups,
                    )
                    SecondaryActionButton(
                        text = "复制文件地址",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = { showCopyPathDialog = true },
                    )
                }
            }

            BackupSectionView.Auto -> SectionPanel(
                title = "自动备份",
                accentColor = LukoaColors.Accent,
                headerAction = {
                    InfoPopoverButton(
                        contentDescription = "查看自动备份说明",
                        title = "自动备份",
                        body = "开启后，启动器会按设定间隔把当前酒馆数据保存到手机的自动备份文件夹。\n达到保留数量后，只会删除最旧的自动备份，不会删除手动备份。\n自动备份适合日常防护；更新、换手机或应用外部备份前，仍建议再做一次手动备份。",
                    )
                },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (autoBackupEnabled) LukoaColors.AccentSoft else LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (autoBackupEnabled) LukoaColors.Accent.copy(alpha = 0.45f) else LukoaColors.Line),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (autoBackupEnabled) "自动备份已开启" else "自动备份未开启",
                            color = if (autoBackupEnabled) LukoaColors.Text else LukoaColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (autoBackupEnabled) {
                                "每 ${formatBackupInterval(autoBackupIntervalMinutes)} 一次，最多保留 ${autoBackupKeepCount} 个，只清理最旧的自动备份。"
                            } else {
                                "开启后会把备份放进手机的自动备份文件夹。"
                            },
                            color = if (autoBackupEnabled) LukoaColors.Text else LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryActionButton(
                        text = if (autoBackupEnabled) "关闭自动备份" else "开启自动备份",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier.weight(1f),
                        onClick = onToggleAutoBackup,
                    )
                    SecondaryActionButton(
                        text = "自动备份设置",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.Info,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAutoBackupSettings,
                    )
                }
            }

            BackupSectionView.Library -> SectionPanel(
                title = "备份库",
                accentColor = LukoaColors.Accent,
                headerAction = {
                    InfoPopoverButton(
                        contentDescription = "查看备份库说明",
                        title = "备份库",
                        body = "“导入”只是把外部备份复制到启动器的备份库，不会马上改动酒馆。\n“导出”会让你选择一个位置，再把备份复制出去，适合换手机或额外保存。\n只有点某个备份的“应用”并确认后，才会用它替换当前酒馆里的对应数据。",
                    )
                },
            ) {
                BackupLibraryGroup(
                    title = "手动备份库 (${manualBackups.size})",
                    subtitle = "Download/LukoaLauncher/backups/sd",
                    emptyText = "手动备份库还没有备份。",
                    backups = manualBackups,
                    actionsLocked = actionsLocked,
                    onApplyBackup = onApplyBackup,
                    onExportBackup = onExportBackup,
                    onCopyBackup = onCopyBackup,
                    onRenameBackup = onRenameBackup,
                    onDeleteBackup = onDeleteBackup,
                )
                BackupLibraryGroup(
                    title = "自动备份库 (${autoBackups.size})",
                    subtitle = "Download/LukoaLauncher/backups/zd",
                    emptyText = "自动备份库还没有备份。",
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
    }
}

@Composable
private fun BackupOverviewCard(
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "数据备份",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(
                    text = if (autoBackupEnabled) "自动备份 · ${formatBackupInterval(autoBackupIntervalMinutes)}" else "手动模式",
                    active = autoBackupEnabled,
                    toneColor = if (autoBackupEnabled) LukoaColors.Accent else LukoaColors.Muted,
                    activeBackground = if (autoBackupEnabled) LukoaColors.AccentSoft else LukoaColors.SurfaceAlt,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BackupStatBlock(
                    value = manualBackupCount.toString(),
                    label = "手动备份 (sd)",
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LukoaColors.Line.copy(alpha = 0.4f)).align(Alignment.CenterVertically))
                BackupStatBlock(
                    value = autoBackupCount.toString(),
                    label = "自动备份 (zd)",
                    accentColor = if (autoBackupEnabled) LukoaColors.Accent else LukoaColors.Dim,
                    modifier = Modifier.weight(1f)
                )
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(LukoaColors.Line.copy(alpha = 0.4f)).align(Alignment.CenterVertically))
                BackupStatBlock(
                    value = autoBackupKeepCount.toString(),
                    label = "自动保留上限",
                    accentColor = if (autoBackupEnabled) LukoaColors.Info else LukoaColors.Dim,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BackupStatBlock(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
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
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("复制文件地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryActionButton(
                    text = "手动备份库",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCopyManual,
                )
                SecondaryActionButton(
                    text = "自动备份库",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCopyAuto,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Safe, onClick = onDismiss)
        },
    )
}

@Composable
private fun BackupLibraryGroup(
    title: String,
    subtitle: String,
    emptyText: String,
    backups: List<String>,
    actionsLocked: Boolean,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
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
            backups.take(8).forEach { path ->
                BackupRecordLine(
                    path = path,
                    actionsLocked = actionsLocked,
                    onApply = { onApplyBackup(path) },
                    onExport = { onExportBackup(path) },
                    onCopy = { onCopyBackup(path) },
                    onRename = { onRenameBackup(path) },
                    onDelete = { onDeleteBackup(path) },
                )
            }
            if (backups.size > 8) {
                Text(
                    text = "只显示最新 8 个，更多还在这个备份库里。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BackupRecordLine(
    path: String,
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
            Text(
                text = fileName,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = backupLocationLabel(path),
                color = LukoaColors.Accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = path,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BackupActionRow {
                BackupActionButton(
                    text = "应用",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(1f),
                    onClick = onApply,
                )
                BackupActionButton(
                    text = "导出",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Info,
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
            }
            BackupActionRow {
                BackupActionButton(
                    text = "重命名",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                    onClick = onRename,
                )
                BackupActionButton(
                    text = "删除",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Danger,
                    modifier = Modifier.weight(1f),
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
