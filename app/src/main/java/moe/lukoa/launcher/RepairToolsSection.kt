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
    onShowHint: (String) -> Unit = {},
    leadingContent: (@Composable () -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    var confirmation by remember { mutableStateOf<RepairConfirmation?>(null) }
    val mutationUnavailableHint = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再试。"
        tavernRunning -> "酒馆正在运行，请先停止酒馆再修改这项设置。"
        else -> null
    }
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
                    body = "不知道问题在哪里时，先点“一键体检”。它只查看当前状态，不会修改酒馆文件。\n酒馆安装中断或启动时报缺少文件，可以尝试“修复 npm 依赖”；只有网页因为主题设置损坏而打不开时，才重置主题。\n内存和聊天文件大小一般保持常用值即可。所有会修改文件的操作都会再次确认，而且只影响当前实例。",
                )
            }
        },
    ) {
        if (leadingContent != null) {
            SettingsSubsection(
                title = "先检查问题",
                detail = "不知道问题在哪里时，先点“一键体检”。它会检查当前实例的安装、权限、下载源和运行环境，不会修改酒馆文件。检查完成后再按结果处理。",
            ) {
                leadingContent()
            }
        }
        SettingsSubsection(
            title = "常用修复",
            detail = "酒馆安装中断、启动时报缺少文件或依赖错误时，可以重新下载依赖。只有酒馆网页因为主题设置损坏而打不开时，才重置主题。两项都会先保留旧文件并再次确认。",
        ) {
            SettingsFeedbackActionButton(
                text = "修复 npm 依赖",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Accent,
                unavailableHint = mutationUnavailableHint,
                onShowHint = onShowHint,
                onClick = {
                    confirmation = RepairConfirmation(
                        "重新安装依赖",
                        "旧的程序依赖文件会先移到带时间的恢复文件夹。只有重新下载成功后才会清理旧副本。这个过程可能需要几分钟。",
                        onRepairDependencies,
                    )
                },
            )
            SettingsFeedbackActionButton(
                text = "网页打不开时重置主题",
                modifier = Modifier.fillMaxWidth(),
                enabled = !actionsLocked && !tavernRunning,
                accentColor = LukoaColors.Info,
                unavailableHint = mutationUnavailableHint,
                onShowHint = onShowHint,
                onClick = {
                    confirmation = RepairConfirmation(
                        "重置网页主题",
                        "将搜索当前用户设置并把主题重置为 Dark Lite。原设置文件会保留带时间戳的副本；找不到兼容设置时不会修改任何文件。",
                        onResetTheme,
                    )
                },
            )
        }
        SettingsSubsection(
            title = "酒馆运行内存",
            detail = "这里设置酒馆最多可以使用多少运行内存，并不会增加手机本身的内存。一般选 4GB，内存较小的手机选 2GB；只有手机内存充足且酒馆明确提示内存不足时才选 6GB。设置过高反而可能让系统关闭 Termux。",
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2048, 4096, 6144).forEach { memory ->
                    SettingsFeedbackActionButton(
                        text = "${memory / 1024}GB",
                        modifier = Modifier.weight(1f),
                        enabled = !actionsLocked && !tavernRunning,
                        accentColor = LukoaColors.Accent,
                        unavailableHint = mutationUnavailableHint,
                        onShowHint = onShowHint,
                        onClick = {
                            confirmation = RepairConfirmation(
                                "设置 ${memory / 1024}GB 内存上限",
                                "启动器只会保存当前实例的内存设置，不直接修改酒馆启动脚本。手机内存不足时，设置过高可能导致系统关闭 Termux。",
                            ) { onSetNodeMemory(memory) }
                        },
                    )
                }
            }
        }
        SettingsSubsection(
            title = "聊天文件大小",
            detail = "这里限制一次最多能导入多大的聊天记录文件，不是全部聊天记录的总容量。一般保持 500MB 就够用；只有文件确实超过限制时才选 1GB 或 2GB。数值越大，导入时占用的内存越多。",
        ) {
            SettingsEntryRow(
                title = "当前上传限制",
                detail = uploadLimitStatus.message,
                value = uploadStatusText,
                valueColor = if (uploadLimitStatus.patchState == TavernUploadLimitPatchState.ChangedOrOverwritten) {
                    LukoaColors.Amber
                } else {
                    uploadStatusTone
                },
                valueAsPill = true,
                enabled = !actionsLocked && !uploadLimitStatus.checking,
                onClick = onCheckUploadLimit,
                unavailableHint = when {
                    actionsLocked -> mutationUnavailableHint
                    uploadLimitStatus.checking -> "正在检查当前上传限制，请稍等。"
                    else -> null
                },
                onShowHint = onShowHint,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TavernUploadLimitPolicy.allowedMegabytes.forEach { limit ->
                    val current = uploadLimitStatus.currentMegabytes == limit
                    SettingsFeedbackActionButton(
                        text = TavernUploadLimitPolicy.label(limit),
                        modifier = Modifier.weight(1f),
                        enabled = !actionsLocked && !tavernRunning,
                        accentColor = if (current) LukoaColors.Accent else LukoaColors.Info,
                        unavailableHint = mutationUnavailableHint,
                        onShowHint = onShowHint,
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
