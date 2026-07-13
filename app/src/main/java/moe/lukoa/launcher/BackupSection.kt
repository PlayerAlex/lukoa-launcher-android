package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BackupLibraryPathTarget {
    Manual,
    Auto,
}

@Composable
fun BackupSection(
    instanceLabel: String,
    instanceDirectory: String,
    instancePort: Int,
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
    val context = LocalContext.current.applicationContext
    var showBackupContentDialog by remember { mutableStateOf(false) }
    var showCopyPathDialog by remember { mutableStateOf(false) }
    var expandedBackupPath by remember { mutableStateOf<String?>(null) }
    var archiveDetails by remember(backupHistory) {
        mutableStateOf<Map<String, BackupLibraryArchiveDetails>>(emptyMap())
    }
    val manualBackups = backupHistory.filter { isManualBackupPath(it) }
    val autoBackups = backupHistory.filter { isAutoBackupPath(it) }

    LaunchedEffect(backupHistory) {
        val paths = backupHistory.distinct()
        archiveDetails = withContext(Dispatchers.IO) {
            paths.mapNotNull { path ->
                BackupLibraryFiles.describeLibraryArchive(context, path)?.let { path to it }
            }.toMap()
        }
    }

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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackupOverviewCard(
            instanceLabel = instanceLabel,
            instanceDirectory = instanceDirectory,
            instancePort = instancePort,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            manualBackupCount = manualBackups.size,
            autoBackupCount = autoBackups.size,
            lastBackupDetails = archiveDetails.values.maxByOrNull { it.modifiedAtMillis },
        )
        SectionPanel(title = "快捷操作", accentColor = LukoaColors.Accent) {
            PrimaryActionButton(
                text = "立即备份",
                enabled = !actionsLocked,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateManualBackup,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SecondaryActionButton(
                    text = "导入备份",
                    enabled = !actionsLocked,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                    onClick = onImportBackup,
                )
                SecondaryActionButton(
                    text = if (backupListRefreshing) "刷新中..." else "刷新列表",
                    enabled = !actionsLocked && !backupListRefreshing,
                    accentColor = LukoaColors.Text,
                    modifier = Modifier.weight(1f),
                    onClick = onRefreshBackups,
                )
            }
            SecondaryActionButton(
                text = "复制文件地址",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Text,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCopyPathDialog = true },
            )
        }

        SectionPanel(
            title = "自动备份",
            accentColor = LukoaColors.Accent,
            headerAction = {
                InfoIconButton(
                    contentDescription = "查看自动备份说明",
                    onClick = { showBackupContentDialog = true },
                )
            },
        ) {
            BackupSettingRow(
                title = "启用自动备份",
                detail = "到时间自动帮你备一份",
                enabled = !actionsLocked,
                trailing = {
                    BackupToggle(
                        checked = autoBackupEnabled,
                        enabled = !actionsLocked,
                        onClick = onToggleAutoBackup,
                    )
                },
                onClick = if (actionsLocked) null else onToggleAutoBackup,
            )
            BackupRowDivider()
            BackupSettingRow(
                title = "备份间隔",
                value = "每 ${formatBackupInterval(autoBackupIntervalMinutes)}",
                enabled = !actionsLocked,
                onClick = if (actionsLocked) null else onOpenAutoBackupSettings,
            )
            BackupRowDivider()
            BackupSettingRow(
                title = "保留份数",
                value = "留 $autoBackupKeepCount 份",
                enabled = !actionsLocked,
                onClick = if (actionsLocked) null else onOpenAutoBackupSettings,
            )
        }

        BackupLibraryGroup(
            title = "手动备份库 · ${manualBackups.size}",
            emptyText = "手动备份库还没有备份。",
            backups = manualBackups,
            detailsByPath = archiveDetails,
            expandedPath = expandedBackupPath,
            actionsLocked = actionsLocked,
            showInfo = true,
            onShowInfo = { showBackupContentDialog = true },
            onToggleExpanded = { path -> expandedBackupPath = path.takeUnless { it == expandedBackupPath } },
            onApplyBackup = onApplyBackup,
            onExportBackup = onExportBackup,
            onCopyBackup = onCopyBackup,
            onRenameBackup = onRenameBackup,
            onDeleteBackup = onDeleteBackup,
        )
        BackupLibraryGroup(
            title = "自动备份库 · ${autoBackups.size}",
            emptyText = "自动备份库还没有备份。",
            backups = autoBackups,
            detailsByPath = archiveDetails,
            expandedPath = expandedBackupPath,
            actionsLocked = actionsLocked,
            showInfo = false,
            onShowInfo = {},
            onToggleExpanded = { path -> expandedBackupPath = path.takeUnless { it == expandedBackupPath } },
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
    instanceLabel: String,
    instanceDirectory: String,
    instancePort: Int,
    autoBackupEnabled: Boolean,
    autoBackupIntervalMinutes: Int,
    autoBackupKeepCount: Int,
    manualBackupCount: Int,
    autoBackupCount: Int,
    lastBackupDetails: BackupLibraryArchiveDetails?,
) {
    DashedSection(
        label = "备份概览",
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BackupStatBlock(
                    value = manualBackupCount.toString(),
                    label = "手动备份",
                    modifier = Modifier.weight(1f)
                )
                BackupStatBlock(
                    value = autoBackupCount.toString(),
                    label = "自动备份",
                    modifier = Modifier.weight(1f)
                )
                BackupStatBlock(
                    value = formatLastBackupAge(lastBackupDetails?.modifiedAtMillis),
                    label = "上次备份",
                    modifier = Modifier.weight(1f)
                )
            }
            BackupOverviewPill(
                text = if (autoBackupEnabled) {
                    "自动备份已开 · ${formatBackupInterval(autoBackupIntervalMinutes)} / 留 $autoBackupKeepCount"
                } else {
                    "自动备份未开启"
                },
                active = autoBackupEnabled,
            )
            BackupStateNote(
                text = "当前实例：$instanceLabel · $instanceDirectory · 端口 $instancePort",
                emphasizePrefix = "当前实例：",
            )
        }
    }
}

@Composable
private fun BackupStatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = LukoaColors.Dim,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BackupOverviewPill(
    text: String,
    active: Boolean,
) {
    Surface(
        color = if (active) LukoaColors.Accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
        shape = LukoaCapsuleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(if (active) LukoaColors.Accent else LukoaColors.Muted, LukoaCapsuleShape),
            )
            Text(
                text = text,
                color = if (active) LukoaColors.Accent else LukoaColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BackupStateNote(
    text: String,
    emphasizePrefix: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
            val prefix = emphasizePrefix?.takeIf(text::startsWith)
            if (prefix != null) {
                Text(
                    text = prefix,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = text.removePrefix(prefix),
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = text,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BackupSettingRow(
    title: String,
    enabled: Boolean,
    detail: String? = null,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val feedbackClick = onClick?.let { rememberFeedbackClick(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.56f)
            .then(
                if (feedbackClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = feedbackClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 2.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackupArchiveIcon(clock = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            detail?.let {
                Text(
                    text = it,
                    color = LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        value?.let {
            Text(
                text = it,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Text(
                text = "›",
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun BackupToggle(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(25.dp)
            .alpha(if (enabled) 1f else 0.46f)
            .background(
                color = if (checked) LukoaColors.Accent else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(13.dp),
            )
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = rememberFeedbackClick(onClick),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = if (checked) 19.dp else 2.5.dp)
                .size(20.dp)
                .background(if (checked) Color.White else LukoaColors.Dim, LukoaCapsuleShape),
        )
    }
}

@Composable
private fun BackupArchiveIcon(clock: Boolean = false) {
    Box(
        modifier = Modifier.size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(1.dp, LukoaColors.Text, if (clock) LukoaCapsuleShape else RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (clock) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(5.dp)
                        .offset(y = (-2).dp)
                        .background(LukoaColors.Text),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = (-3).dp)
                        .background(LukoaColors.Text),
                )
            }
        }
    }
}

@Composable
private fun BackupRowDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
}

private fun formatArchiveSummary(details: BackupLibraryArchiveDetails?): String {
    if (details == null) return "正在读取时间与大小…"
    val time = if (details.modifiedAtMillis > 0L) {
        runCatching {
            Instant.ofEpochMilli(details.modifiedAtMillis)
                .atZone(ZoneId.systemDefault())
                .format(BACKUP_LIST_TIME_FORMATTER)
        }.getOrDefault("时间未读取")
    } else {
        "时间未读取"
    }
    return "$time · ${formatBackupArchiveSize(details.size)}"
}

private fun formatLastBackupAge(modifiedAtMillis: Long?): String {
    val timestamp = modifiedAtMillis?.takeIf { it > 0L } ?: return "暂无"
    val deltaMinutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L)
    return when {
        deltaMinutes < 1L -> "刚刚"
        deltaMinutes < 60L -> "${deltaMinutes}m 前"
        deltaMinutes < 24L * 60L -> "${deltaMinutes / 60L}h 前"
        else -> "${deltaMinutes / (24L * 60L)}d 前"
    }
}

private fun formatBackupArchiveSize(bytes: Long): String {
    if (bytes < 0L) return "大小未读取"
    if (bytes < 1024L) return "${bytes}B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index += 1
    }
    val decimals = if (value >= 10.0 || value % 1.0 == 0.0) 0 else 1
    return String.format(Locale.ROOT, "%.${decimals}f%s", value, units[index.coerceAtLeast(0)])
}

private val BACKUP_LIST_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

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
    emptyText: String,
    backups: List<String>,
    detailsByPath: Map<String, BackupLibraryArchiveDetails>,
    expandedPath: String?,
    actionsLocked: Boolean,
    showInfo: Boolean,
    onShowInfo: () -> Unit,
    onToggleExpanded: (String) -> Unit,
    onApplyBackup: (String) -> Unit,
    onExportBackup: (String) -> Unit,
    onCopyBackup: (String) -> Unit,
    onRenameBackup: (String) -> Unit,
    onDeleteBackup: (String) -> Unit,
) {
    SectionPanel(
        title = title,
        accentColor = LukoaColors.Accent,
        headerAction = if (showInfo) {
            {
                InfoIconButton(
                    contentDescription = "查看备份内容说明",
                    onClick = onShowInfo,
                )
            }
        } else {
            null
        },
    ) {
        if (backups.isEmpty()) {
            BackupStateNote(text = emptyText)
        } else {
            backups.forEachIndexed { index, path ->
                if (index > 0) BackupRowDivider()
                BackupRecordLine(
                    path = path,
                    details = detailsByPath[path],
                    expanded = expandedPath == path,
                    actionsLocked = actionsLocked,
                    onToggleExpanded = { onToggleExpanded(path) },
                    onApply = { onApplyBackup(path) },
                    onExport = { onExportBackup(path) },
                    onCopy = { onCopyBackup(path) },
                    onRename = { onRenameBackup(path) },
                    onDelete = { onDeleteBackup(path) },
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
    details: BackupLibraryArchiveDetails?,
    expanded: Boolean,
    actionsLocked: Boolean,
    onToggleExpanded: () -> Unit,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val fileName = details?.fileName ?: path.substringAfterLast('/')
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = rememberFeedbackClick(onToggleExpanded))
                .padding(horizontal = 2.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackupArchiveIcon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatArchiveSummary(details),
                    color = LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (expanded) "收起" else "›",
                color = if (expanded) LukoaColors.Accent else LukoaColors.Dim,
                style = if (expanded) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleLarge,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Normal,
            )
        }
        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BackupActionButton(
                    text = "应用",
                    enabled = !actionsLocked,
                    dangerFill = true,
                    modifier = Modifier.weight(1f),
                    onClick = onApply,
                )
                BackupActionButton(
                    text = "导出",
                    enabled = !actionsLocked,
                    modifier = Modifier.weight(1f),
                    onClick = onExport,
                )
                BackupActionButton(
                    text = "复制",
                    enabled = !actionsLocked,
                    modifier = Modifier.weight(1f),
                    onClick = onCopy,
                )
                BackupActionButton(
                    text = "重命名",
                    enabled = !actionsLocked,
                    modifier = Modifier.weight(1f),
                    onClick = onRename,
                )
                BackupActionButton(
                    text = "删除",
                    enabled = !actionsLocked,
                    dangerText = true,
                    modifier = Modifier.weight(1f),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun BackupActionButton(
    text: String,
    enabled: Boolean,
    dangerFill: Boolean = false,
    dangerText: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (dangerFill) LukoaColors.Danger else Color.White.copy(alpha = 0.06f)
    val contentColor = when {
        dangerFill -> Color(0xFF2A050D)
        dangerText -> LukoaColors.Danger
        else -> LukoaColors.Text
    }
    Surface(
        modifier = modifier
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.46f)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = rememberFeedbackClick(onClick),
            ),
        color = background,
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
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
