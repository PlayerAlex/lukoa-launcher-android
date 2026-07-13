package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class RepairConfirmation(val title: String, val detail: String, val action: () -> Unit)

@Composable
fun RepairToolsSection(
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    uploadLimitStatus: TavernUploadLimitStatus,
    onRepairDependencies: () -> Unit,
    onResetTheme: () -> Unit,
    onSetNodeMemory: (Int) -> Unit,
    onCheckUploadLimit: () -> Unit,
    onSetUploadLimit: (Int) -> Unit,
    leadingContent: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    var confirmation by remember { mutableStateOf<RepairConfirmation?>(null) }
    val uploadStatusText = uploadLimitStatus.currentMegabytes?.let(TavernUploadLimitPolicy::label)
        ?: if (uploadLimitStatus.checking) "检查中…" else "尚未读取"
    val uploadStatusTone = when {
        uploadLimitStatus.checking -> LukoaColors.Amber
        uploadLimitStatus.currentMegabytes != null -> LukoaColors.Accent
        else -> LukoaColors.Muted
    }
    confirmation?.let { request ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(request.title) },
            text = { Text(request.detail) },
            confirmButton = {
                Button(onClick = { confirmation = null; request.action() }) { Text("确认执行") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmation = null }) { Text("取消") }
            },
        )
    }

    SectionPanel(
        title = "修复工具",
        accentColor = LukoaColors.Amber,
        headerAction = {
            StatusPill(
                text = when {
                    actionsLocked -> "当前忙碌"
                    tavernRunning -> "运行中锁定"
                    else -> "可使用"
                },
                active = true,
                toneColor = if (actionsLocked || tavernRunning) LukoaColors.Amber else LukoaColors.Accent,
                activeBackground = if (actionsLocked || tavernRunning) LukoaColors.AmberSoft else LukoaColors.AccentSoft,
            )
        },
    ) {
        SettingsSectionIntro("集中处理环境体检、常用修复、内存、上传限制和诊断日志。")
        leadingContent?.invoke()
        Text(
            text = when {
                actionsLocked -> "当前有其他任务正在处理，设置会在任务结束后自动恢复。"
                tavernRunning -> "检测到酒馆正在运行。体检、检查上传限制和诊断仍可使用；修改类操作需要先停止酒馆。"
                else -> "所有修改只作用于当前实例，并会继续保留原有确认与恢复副本。"
            },
            color = if (actionsLocked || tavernRunning) LukoaColors.Amber else LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        SettingsSectionDivider()
        SettingsSubsection(
            title = "常用修复",
            detail = "适合依赖缺失或错误主题导致网页打不开的情况；执行前会再次确认。",
        ) {
            SecondaryActionButton(
                text = "修复 npm 依赖",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Amber,
                onClick = {
                    confirmation = RepairConfirmation(
                        "重新安装依赖",
                        "旧的 node_modules 会先被移到带时间戳的恢复目录。只有 npm install 成功后才会清理旧副本。此操作可能需要数分钟。",
                        onRepairDependencies,
                    )
                },
            )
            SecondaryActionButton(
                text = "网页打不开时重置主题",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Info,
                onClick = {
                    confirmation = RepairConfirmation(
                        "重置网页主题",
                        "将搜索当前用户设置并把主题重置为 Dark Lite。原设置文件会保留带时间戳的副本；找不到兼容设置时不会修改任何文件。",
                        onResetTheme,
                    )
                },
            )
        }
        SettingsSectionDivider()
        SettingsSubsection(
            title = "Node.js 内存上限",
            detail = "按当前实例保存，不直接改动 start.sh；低内存设备不建议选择过高。",
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2048, 4096, 6144).forEach { memory ->
                    SecondaryActionButton(
                        text = "${memory / 1024}GB",
                        modifier = Modifier.weight(1f),
                        enabled = !actionsLocked && !tavernRunning,
                        accentColor = if (memory >= 6144) LukoaColors.Amber else LukoaColors.Accent,
                        onClick = {
                            confirmation = RepairConfirmation(
                                "设置 ${memory / 1024}GB 内存上限",
                                "启动器会写入独立环境配置，不直接改动 start.sh。低内存设备设置过高可能导致系统杀掉 Termux。",
                            ) { onSetNodeMemory(memory) }
                        },
                    )
                }
            }
        }
        SettingsSectionDivider()
        SettingsSubsection(
            title = "聊天记录上传限制",
            detail = "大文件会增加 Termux 内存和处理时间；1GB 以上更容易被系统杀后台。",
            statusText = uploadStatusText,
            statusTone = uploadStatusTone,
            statusActive = uploadLimitStatus.currentMegabytes != null || uploadLimitStatus.checking,
        ) {
            Text(
                text = uploadLimitStatus.message,
                color = if (uploadLimitStatus.patchState == TavernUploadLimitPatchState.ChangedOrOverwritten) {
                    LukoaColors.Amber
                } else {
                    LukoaColors.Muted
                },
                style = MaterialTheme.typography.bodySmall,
            )
            SecondaryActionButton(
                text = if (uploadLimitStatus.checking) "检查中..." else "重新检查当前限制",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !uploadLimitStatus.checking,
                accentColor = LukoaColors.Accent,
                onClick = onCheckUploadLimit,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TavernUploadLimitPolicy.allowedMegabytes.forEach { limit ->
                    val current = uploadLimitStatus.currentMegabytes == limit
                    SecondaryActionButton(
                        text = TavernUploadLimitPolicy.label(limit),
                        modifier = Modifier.weight(1f),
                        enabled = !actionsLocked && !tavernRunning,
                        accentColor = when {
                            current -> LukoaColors.Accent
                            limit >= 2048 -> LukoaColors.Amber
                            else -> LukoaColors.Info
                        },
                        onClick = {
                            val label = TavernUploadLimitPolicy.label(limit)
                            confirmation = RepairConfirmation(
                                "设置上传限制为 $label",
                                "只会修改当前实例中已识别的 SillyTavern 上传中间件。修改前会备份源文件并记录原值；如果版本结构不匹配，将拒绝修改。设置较大限制可能导致内存占用显著增加。",
                            ) { onSetUploadLimit(limit) }
                        },
                    )
                }
            }
        }
        extraContent?.invoke()
    }
}
