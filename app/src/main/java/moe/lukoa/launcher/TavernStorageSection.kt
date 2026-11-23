package moe.lukoa.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernStorageContent(
    state: TavernStorageState,
    instanceLabel: String,
    actionsLocked: Boolean,
    onScan: () -> Unit,
    onClean: (Set<TavernStorageCleanupCategory>) -> Unit,
    onShowHint: (String) -> Unit,
) {
    val snapshot = state.snapshot
    var selectedCategories by remember {
        mutableStateOf(
            TavernStorageCleanupCategory.entries
                .filter { it.defaultSelected }
                .toSet(),
        )
    }
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val selectedKb = snapshot?.let { snap ->
        selectedCategories.sumOf { snap.kilobytesFor(it) }
    } ?: 0L

    ManagementDialogIntroCard(
        "扫描当前酒馆实例的空间分布，并提供只针对缓存与历史临时文件的安全清理。清理绝不会触碰聊天记录、角色卡、世界书、预设或备份库。",
    )

    SettingsEntryGroup {
        SettingsEntryRow(
            title = "当前实例",
            detail = state.message,
            detailMaxLines = Int.MAX_VALUE,
            value = instanceLabel,
            valueColor = LukoaColors.Primary,
            valueAsPill = true,
        )
    }

    SettingsFeedbackActionButton(
        text = if (state.loading) "扫描中..." else "扫描当前空间占用",
        enabled = !actionsLocked && !state.loading,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = if (state.loading) "正在扫描磁盘占用，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onScan,
    )

    if (snapshot == null) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "尚未扫描空间",
                detail = "点击上方“扫描当前空间占用”查看酒馆各目录体积及可安全释放的空间。",
                detailMaxLines = 3,
            )
        }
        return
    }

    if (snapshot.lastFreedKb != null) {
        NoticeCard(
            title = "安全清理完成",
            detail = "本次已安全释放约 ${formatStorageKilobytes(snapshot.lastFreedKb)} 空间。",
            accentColor = LukoaColors.Primary,
        )
    }

    ManagementDialogSectionTitle("空间分布总览")
    SettingsEntryGroup {
        SettingsEntryRow(
            title = "酒馆目录总占用",
            value = formatStorageKilobytes(snapshot.tavernTotalKb),
            valueColor = LukoaColors.Primary,
            valueAsPill = true,
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "用户数据（data）",
            detail = "含聊天 ${formatStorageKilobytes(snapshot.chatsKb)} · 角色卡 ${formatStorageKilobytes(snapshot.charactersKb)} · 滚动备份 ${formatStorageKilobytes(snapshot.chatBackupsKb)}",
            detailMaxLines = 3,
            value = formatStorageKilobytes(snapshot.dataTotalKb),
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "运行依赖（node_modules）",
            value = formatStorageKilobytes(snapshot.nodeModulesKb),
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "第三方网页扩展",
            value = formatStorageKilobytes(snapshot.extensionsKb),
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "酒馆运行日志",
            value = formatStorageKilobytes(snapshot.logsKb),
        )
    }

    ManagementDialogSectionTitle("可安全清理项（共 ${formatStorageKilobytes(snapshot.totalCleanableKb)}）")
    SettingsEntryGroup {
        TavernStorageCleanupCategory.entries.forEachIndexed { index, category ->
            if (index > 0) SettingsEntryDivider()
            val kb = snapshot.kilobytesFor(category)
            val checked = category in selectedCategories
            val extraCount = when (category) {
                TavernStorageCleanupCategory.LeftoverDirs ->
                    if (snapshot.leftoverDirsCount > 0) "（${snapshot.leftoverDirsCount} 项）" else ""
                TavernStorageCleanupCategory.ConfigBackups ->
                    if (snapshot.configBackupsCount > 0) "（${snapshot.configBackupsCount} 份）" else ""
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !actionsLocked) {
                        selectedCategories = if (checked) {
                            selectedCategories - category
                        } else {
                            selectedCategories + category
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        selectedCategories = if (isChecked) {
                            selectedCategories + category
                        } else {
                            selectedCategories - category
                        }
                    },
                    enabled = !actionsLocked,
                    colors = CheckboxDefaults.colors(
                        checkedColor = LukoaColors.Primary,
                        uncheckedColor = LukoaColors.TextSecondary,
                    ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${category.label}$extraCount",
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = category.description,
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = formatStorageKilobytes(kb),
                    color = if (kb > 0L) LukoaColors.Accent else LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    TimedDangerFeedbackActionButton(
        text = if (selectedCategories.isEmpty()) {
            "请先勾选要清理的安全项"
        } else {
            "清理已选安全项（约 ${formatStorageKilobytes(selectedKb)}）"
        },
        enabled = !actionsLocked && selectedCategories.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = lockedHint ?: "请至少勾选一项要清理的缓存或临时文件。",
        onShowHint = onShowHint,
        onConfirmed = { onClean(selectedCategories) },
    )
}
