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
    Safety,
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
    var showBackupContentDialog by remember { mutableStateOf(false) }
    var showCopyPathDialog by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf(BackupSectionView.Quick) }
    val manualBackups = backupHistory.filter { isManualBackupPath(it) }
    val autoBackups = backupHistory.filter { isAutoBackupPath(it) }
    val sectionOptions = listOf(
        SectionSwitchOption(
            value = BackupSectionView.Quick,
            label = "快捷操作",
            description = "先做备份、导入备份、刷新备份库和复制备份路径，都在这里。",
        ),
        SectionSwitchOption(
            value = BackupSectionView.Auto,
            label = "自动备份",
            description = "这里只看自动备份开关、间隔和保留策略。",
        ),
        SectionSwitchOption(
            value = BackupSectionView.Library,
            label = "备份库",
            description = "手动备份库和自动备份库分开看，应用、导出、复制、重命名、删除都在这里。",
        ),
        SectionSwitchOption(
            value = BackupSectionView.Safety,
            label = "数据安全",
            description = "提醒你哪些操作会覆盖数据，哪些只是导入导出文件。",
        ),
    )

    if (showBackupContentDialog) {
        BackupContentInfoDialog(onDismiss = { showBackupContentDialog = false })
    }
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
                    InfoIconButton(
                        contentDescription = "查看备份内容说明",
                        onClick = { showBackupContentDialog = true },
                    )
                },
            ) {
                Text(
                    text = "先做备份，再更新、回退或应用外部备份，会更稳。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
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

            BackupSectionView.Auto -> SectionPanel(title = "自动备份", accentColor = LukoaColors.Accent) {
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
                                "开启后会把备份放进 Download/lukoa/backups/zd。"
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
                        accentColor = if (autoBackupEnabled) LukoaColors.Danger else LukoaColors.Accent,
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

            BackupSectionView.Library -> SectionPanel(title = "备份库", accentColor = LukoaColors.Accent) {
                Text(
                    text = "导入会进手动备份库 sd。导出时会打开文件管理器让你选保存位置。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                BackupLibraryGroup(
                    title = "手动备份库 (${manualBackups.size})",
                    subtitle = "Download/lukoa/backups/sd",
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
                    subtitle = "Download/lukoa/backups/zd",
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

            BackupSectionView.Safety -> SectionPanel(title = "数据安全", accentColor = LukoaColors.Danger) {
                Text(
                    text = "应用备份前先停止酒馆。删除只删选中的备份包。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "导入到备份库只是复制备份文件；应用备份才会真正覆盖当前酒馆目录。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "做更新、回退、装插件前，最好先手动生成一个备份。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
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
                Text(
                    text = "选择要复制的备份库地址。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
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
private fun BackupContentInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("备份内容") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LukoaColors.InfoSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Info.copy(alpha = 0.34f)),
                ) {
                    Text(
                        text = "会备份角色、聊天、世界书、插件/扩展、配置和密钥文件。",
                        modifier = Modifier.padding(10.dp),
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "不会备份 node_modules、Git 历史和缓存；这些可以重新安装，备份包也会更小。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "应用备份会覆盖当前酒馆数据。重要操作前建议先生成一个备份。",
                    color = LukoaColors.Amber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "知道了",
                enabled = true,
                tone = ActionTone.Safe,
                onClick = onDismiss,
            )
        },
    )
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
                color = LukoaColors.Amber,
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
                    accentColor = LukoaColors.Amber,
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
        path.contains("/lukoa/backups/sd/", ignoreCase = true) -> "手动备份 / Download/lukoa/backups/sd"
        path.contains("/lukoa/backups/zd/", ignoreCase = true) -> "自动备份 / Download/lukoa/backups/zd"
        path.contains("/lukoa/backups/", ignoreCase = true) -> "不支持的旧位置 / Download/lukoa/backups"
        path.contains("/storage/downloads/", ignoreCase = true) -> "Downloads 备份库"
        path.contains("/.local/state/", ignoreCase = true) -> "Termux 私有备份库"
        else -> "露科亚备份库"
    }
}

private fun isManualBackupPath(path: String): Boolean {
    val normalized = path.trim().replace('\\', '/')
    return normalized.contains("/lukoa/backups/sd/", ignoreCase = true) ||
        normalized.contains("/.local/state/lukoa-launcher/backups/sd/", ignoreCase = true)
}

private fun isAutoBackupPath(path: String): Boolean {
    val normalized = path.trim().replace('\\', '/')
    return normalized.contains("/lukoa/backups/zd/", ignoreCase = true) ||
        normalized.contains("/.local/state/lukoa-launcher/backups/zd/", ignoreCase = true)
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
        title = { Text("复制备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "会复制一份，不会覆盖原文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
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
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("重命名备份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "只改文件名，不改备份内容。同名会被拦截。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = archivePath.substringAfterLast('/'),
                    color = LukoaColors.Amber,
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
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
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
                tone = ActionTone.Warning,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}
