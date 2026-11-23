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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showDetails by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val selectedKb = snapshot?.let { snap ->
        selectedCategories.sumOf { snap.kilobytesFor(it) }
    } ?: 0L

    SettingsFeedbackActionButton(
        text = if (state.loading) "扫描中..." else "扫描当前空间占用",
        enabled = !actionsLocked && !state.loading,
        accentColor = LukoaColors.Primary,
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = if (state.loading) "正在扫描空间占用，请稍等。" else lockedHint,
        onShowHint = onShowHint,
        onClick = onScan,
    )

    if (snapshot == null) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例：$instanceLabel",
                detail = "点击上方“扫描当前空间占用”查看酒馆体积与可清理的缓存大小。",
                detailMaxLines = 2,
            )
        }
        return
    }

    if (snapshot.lastFreedKb != null) {
        NoticeCard(
            title = "清理完成",
            detail = "本次安全释放约 ${formatStorageKilobytes(snapshot.lastFreedKb)} 空间。",
            accentColor = LukoaColors.Primary,
        )
    }

    SettingsEntryGroup {
        SettingsEntryRow(
            title = "酒馆总占用（$instanceLabel）",
            detail = "聊天 ${formatStorageKilobytes(snapshot.chatsKb)} · 角色卡 ${formatStorageKilobytes(snapshot.charactersKb)}",
            detailMaxLines = 2,
            value = formatStorageKilobytes(snapshot.tavernTotalKb),
            valueColor = LukoaColors.TextPrimary,
            valueAsPill = true,
        )
        SettingsEntryDivider()
        SettingsEntryRow(
            title = "可安全清理的缓存与旧档",
            detail = "已默认勾选最安全的 ${selectedCategories.size} 项，点下方按钮即可清理。",
            detailMaxLines = 2,
            value = formatStorageKilobytes(snapshot.totalCleanableKb),
            valueColor = LukoaColors.Primary,
            valueAsPill = true,
        )
    }

    TimedDangerFeedbackActionButton(
        text = if (selectedCategories.isEmpty()) {
            "请先在下方勾选清理项"
        } else {
            "清理已选安全项（约 ${formatStorageKilobytes(selectedKb)}）"
        },
        enabled = !actionsLocked && selectedCategories.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        unavailableHint = lockedHint ?: "请至少勾选一项要清理的缓存。",
        onShowHint = onShowHint,
        onConfirmed = { onClean(selectedCategories) },
    )

    LukoaCollapsibleCard(
        title = "自定义清理项与空间明细",
        subtitle = "自选要清理的项目，或查看程序与数据分别占了多少空间",
        expanded = showDetails,
        onToggle = { showDetails = !showDetails },
    ) {
        Text(
            text = "选择清理项",
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        TavernStorageCleanupCategory.entries.forEachIndexed { index, category ->
            if (index > 0) SettingsEntryDivider()
            val kb = snapshot.kilobytesFor(category)
            val checked = category in selectedCategories
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
                    .padding(vertical = 4.dp),
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
                        checkmarkColor = LukoaColors.OnPrimary,
                        uncheckedColor = LukoaColors.TextSecondary,
                    ),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = category.label,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = category.description,
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = formatStorageKilobytes(kb),
                    color = if (kb > 0L) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        SettingsEntryDivider()
        Text(
            text = "空间分布明细",
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = listOf(
                "用户数据总计：${formatStorageKilobytes(snapshot.dataTotalKb)}",
                "酒馆程序依赖：${formatStorageKilobytes(snapshot.nodeModulesKb)}",
                "第三方网页扩展：${formatStorageKilobytes(snapshot.extensionsKb)}",
                "酒馆运行日志：${formatStorageKilobytes(snapshot.logsKb)}",
            ).joinToString("\n"),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
