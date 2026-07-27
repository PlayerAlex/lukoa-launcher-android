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
import androidx.compose.ui.Alignment
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
        uploadLimitStatus.checking -> LukoaColors.Accent
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
        accentColor = LukoaColors.Accent,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                InfoPopoverButton(
                    contentDescription = "查看修复工具说明",
                    title = "修复工具",
                    body = "这里可以检查运行环境、重新下载缺失的程序依赖、修复主题、调整内存和上传大小，并导出排错信息。所有操作只影响当前实例；会改文件的操作仍会先让你确认。",
                )
            }
        },
    ) {
        leadingContent?.invoke()
        if (actionsLocked || tavernRunning) {
            Text(
                text = if (actionsLocked) {
                    "当前有其他任务正在处理，设置会在任务结束后恢复。"
                } else {
                    "酒馆正在运行；修改类操作需要先停止酒馆。"
                },
                color = LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SettingsSectionDivider()
        SettingsSubsection(
            title = "常用修复",
            detail = "网页打不开、安装失败或主题损坏时可尝试。重新下载依赖会先保留旧文件，修复主题会保存一份原文件；执行前都会再次确认。",
        ) {
            SecondaryActionButton(
                text = "修复 npm 依赖",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Accent,
                onClick = {
                    confirmation = RepairConfirmation(
                        "重新安装依赖",
                        "旧的程序依赖文件会先移到带时间的恢复文件夹。只有重新下载成功后才会清理旧副本。这个过程可能需要几分钟。",
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
            detail = "决定酒馆最多能使用多少运行内存，只影响当前实例。手机内存较小时建议选 2GB 或 4GB；设置过高可能被系统强制关闭。",
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
                                "启动器只会保存当前实例的内存设置，不直接修改酒馆启动脚本。手机内存不足时，设置过高可能导致系统关闭 Termux。",
                            ) { onSetNodeMemory(memory) }
                        },
                    )
                }
            }
            Text(
                text = "低内存设备不建议选择过高，设置过高可能导致系统结束 Termux。",
                color = LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SettingsSectionDivider()
        SettingsSubsection(
            title = "聊天记录上传限制",
            detail = "决定一次最多能导入多大的聊天记录。文件越大，处理越慢、占用内存越多；超过 1GB 更容易被系统中断。",
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
            Text(
                text = "1GB 以上会明显增加内存压力，更容易被系统结束后台。",
                color = LukoaColors.Amber,
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
                                "只会修改当前实例中负责接收上传文件的程序部分。修改前会保存原文件和原来的数值；如果当前版本不支持，启动器会停止操作。限制越大，占用的内存也会越多。",
                            ) { onSetUploadLimit(limit) }
                        },
                    )
                }
            }
        }
        extraContent?.invoke()
    }
}
