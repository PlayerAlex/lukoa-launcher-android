package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TavernQuickImportSection(
    state: TavernQuickImportState,
    instanceLabel: String,
    actionsLocked: Boolean,
    onUserHandleChange: (String) -> Unit,
    onPickAndImport: (TavernQuickImportKind) -> Unit,
    onRefreshPaths: () -> Unit,
    onCopyPath: (String, String) -> Boolean,
    onShowHint: (String) -> Unit,
) {
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null

    SectionPanel(
        title = "常用数据快捷导入",
        accentColor = LukoaColors.Primary,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看快捷导入说明",
                title = "快捷导入",
                body = "不用再到处找 Termux 内部目录：直接从手机文件管理器选择角色卡（PNG/JSON/CHARX）、世界书、预设或主题文件，启动器会自动复制到当前酒馆对应目录。若存在同名文件会自动加时间戳后缀，绝不覆盖旧文件。",
            )
        },
    ) {
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

        if (state.lastImportedFile.isNotBlank()) {
            NoticeCard(
                title = "最近导入成功",
                detail = "已写入：${state.lastImportedFile}。在酒馆页面刷新后即可看到。",
                accentColor = LukoaColors.Primary,
            )
        }

        ManagementDialogSectionTitle("目标酒馆用户")
        SettingsEntryGroup {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = state.userHandle,
                    onValueChange = onUserHandleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("用户目录标识（单用户默认 default-user）") },
                    enabled = !actionsLocked,
                )
                Text(
                    text = "没有开启多用户时保持 default-user 即可；多用户模式下填写对应登录标识。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        ManagementDialogSectionTitle("一键选择文件并导入")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TavernQuickImportKind.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        Text(
            text = TavernQuickImportKind.entries.joinToString("\n") { "• ${it.label}：${it.subtitle}" },
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        ManagementDialogSectionTitle("常用数据目录（点按复制路径）")
        SettingsFeedbackActionButton(
            text = if (state.loading) "读取目录中..." else "刷新数据目录路径",
            enabled = !actionsLocked && !state.loading,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = if (state.loading) "正在解析数据目录，请稍等。" else lockedHint,
            onShowHint = onShowHint,
            onClick = onRefreshPaths,
        )

        val paths = state.paths
        if (paths != null) {
            SettingsEntryGroup {
                val items = listOf(
                    "角色卡目录（characters）" to paths.characterDir,
                    "世界书目录（worlds）" to paths.worldDir,
                    "对话预设目录（OpenAI Settings）" to paths.presetDir,
                    "聊天记录目录（chats）" to paths.chatsDir,
                    "网页主题目录（themes）" to paths.themeDir,
                ).filter { it.second.isNotBlank() }
                items.forEachIndexed { index, (label, path) ->
                    if (index > 0) SettingsEntryDivider()
                    SettingsEntryRow(
                        title = label,
                        value = path,
                        valueLayout = SettingsValueLayout.Supporting,
                        onClick = {
                            val copied = onCopyPath(label, path)
                            onShowHint(if (copied) "已复制 $label 路径。" else "复制路径失败。")
                        },
                    )
                }
            }
        }
    }
}
