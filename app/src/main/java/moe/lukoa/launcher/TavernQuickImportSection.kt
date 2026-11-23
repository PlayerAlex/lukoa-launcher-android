package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TavernQuickImportSection(
    state: TavernQuickImportState,
    instanceLabel: String,
    actionsLocked: Boolean,
    availableUsers: List<TavernUserRecord> = emptyList(),
    onUserHandleChange: (String) -> Unit,
    onChatCharacterChange: (String) -> Unit = {},
    onPickAndImport: (TavernQuickImportKind) -> Unit,
    onRefreshPaths: () -> Unit,
    onCopyPath: (String, String) -> Boolean,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    var showChatImport by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    val userHandleOptions = buildList {
        add("default-user")
        availableUsers.forEach { user ->
            val h = user.handle.trim()
            if (h.isNotBlank() && h !in this) add(h)
        }
    }
    val chatCharDirs = state.paths?.chatCharacterDirs.orEmpty()
    val chatCharError = TavernQuickImportCommandCodec.validateChatCharacterDir(state.chatCharacterDir)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("当前实例")
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前实例",
                    value = instanceLabel,
                    valueColor = LukoaColors.TextPrimary,
                )
            }
        }

        if (state.lastImportedFiles.isNotEmpty() || state.lastImportedFile.isNotBlank()) {
            val files = state.lastImportedFiles.ifEmpty { listOf(state.lastImportedFile) }
            val summaryText = if (files.size == 1) {
                "已放入：${files.first().substringAfterLast('/')}。回到酒馆刷新网页即可看到。"
            } else {
                val previewNames = files.take(3).joinToString("、") { it.substringAfterLast('/') }
                val more = if (files.size > 3) " 等共 ${files.size} 个文件" else "（共 ${files.size} 个文件）"
                "已批量放入：$previewNames$more。回到酒馆刷新网页即可看到。"
            }
            NoticeCard(
                title = if (files.size > 1) "批量导入完成" else "导入完成",
                detail = summaryText,
                accentColor = LukoaColors.Primary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("选择要导入的类型（可一次多选）")
            val generalKinds = listOf(
                TavernQuickImportKind.Character,
                TavernQuickImportKind.World,
                TavernQuickImportKind.Preset,
                TavernQuickImportKind.Theme,
            )
            SettingsEntryGroup {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    generalKinds.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            pair.forEach { kind ->
                                SettingsFeedbackActionButton(
                                    text = kind.label,
                                    enabled = !actionsLocked,
                                    accentColor = LukoaColors.Primary,
                                    modifier = Modifier.weight(1f),
                                    unavailableHint = lockedHint,
                                    onShowHint = onShowHint,
                                    onClick = { onPickAndImport(kind) },
                                )
                            }
                        }
                    }
                }
            }
        }

        LukoaCollapsibleCard(
            title = "导入聊天记录（.jsonl）",
            subtitle = "把旧聊天记录放回指定角色的聊天文件夹",
            expanded = showChatImport,
            onToggle = { showChatImport = !showChatImport },
        ) {
            if (chatCharDirs.isNotEmpty()) {
                Text(
                    text = "点选已有角色文件夹：",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    chatCharDirs.forEach { dirName ->
                        QuickImportOptionChip(
                            label = dirName,
                            selected = state.chatCharacterDir.trim() == dirName,
                            enabled = !actionsLocked,
                            onClick = { onChatCharacterChange(dirName) },
                        )
                    }
                }
            }
            LukoaTextField(
                value = state.chatCharacterDir,
                onValueChange = onChatCharacterChange,
                label = "目标角色文件夹名",
                placeholder = "输入角色名，或点选上方已有角色",
                enabled = !actionsLocked,
            )
            SettingsFeedbackActionButton(
                text = "选择 .jsonl 聊天文件并导入",
                enabled = !actionsLocked && chatCharError == null,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = lockedHint ?: chatCharError,
                onShowHint = onShowHint,
                onClick = { onPickAndImport(TavernQuickImportKind.Chat) },
            )
        }

        LukoaCollapsibleCard(
            title = "更多：切换多用户与查看文件夹路径",
            subtitle = "单用户无需修改（默认 default-user）",
            expanded = showAdvanced,
            onToggle = { showAdvanced = !showAdvanced },
        ) {
            if (userHandleOptions.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    userHandleOptions.forEach { handle ->
                        QuickImportOptionChip(
                            label = handle,
                            selected = state.userHandle.trim() == handle,
                            enabled = !actionsLocked,
                            onClick = { onUserHandleChange(handle) },
                        )
                    }
                }
            }
            LukoaTextField(
                value = state.userHandle,
                onValueChange = onUserHandleChange,
                label = "酒馆用户目录（默认 default-user）",
                enabled = !actionsLocked,
            )
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "刷新文件夹路径与角色列表",
                enabled = !actionsLocked && !state.loading,
                accentColor = LukoaColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = if (state.loading) "正在读取目录，请稍等。" else lockedHint,
                onShowHint = onShowHint,
                onClick = onRefreshPaths,
            )
            val paths = state.paths
            if (paths != null) {
                val items = listOf(
                    "角色卡文件夹" to paths.characterDir,
                    "世界书文件夹" to paths.worldDir,
                    "对话预设文件夹" to paths.presetDir,
                    "聊天记录文件夹" to paths.chatsDir,
                    "网页主题文件夹" to paths.themeDir,
                ).filter { it.second.isNotBlank() }
                items.forEachIndexed { index, (label, path) ->
                    if (index > 0) SettingsEntryDivider()
                    SettingsEntryRow(
                        title = label,
                        value = path,
                        valueLayout = SettingsValueLayout.Supporting,
                        onClick = {
                            val copied = onCopyPath(label, path)
                            onShowHint(if (copied) "已复制$label 路径。" else "复制路径失败。")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickImportOptionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        color = if (selected) LukoaColors.PrimarySoft else LukoaColors.Elevated,
        shape = LukoaCapsuleShape,
        border = BorderStroke(
            1.dp,
            if (selected) LukoaColors.Primary else LukoaColors.Border,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) LukoaColors.Primary else LukoaColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
