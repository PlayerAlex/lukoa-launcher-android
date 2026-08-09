package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal enum class VersionPrimaryAction {
    Update,
    Rollback,
}

internal fun preferredVersionPrimaryAction(
    state: TavernVersionActionState,
): VersionPrimaryAction = when {
    state.updateAvailable -> VersionPrimaryAction.Update
    state.rollbackAvailable -> VersionPrimaryAction.Rollback
    state.relation == TavernTargetRelation.Older -> VersionPrimaryAction.Rollback
    else -> VersionPrimaryAction.Update
}

private data class VersionStatusStyle(
    val text: String,
    val tone: Color,
    val background: Color,
)

@Composable
fun VersionManagementSection(
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    tavernVersionInfo: TavernVersionInfo,
    officialVersions: TavernOfficialVersions,
    currentRepoUrl: String,
    selectedVersion: TavernVersionChoice?,
    lastOperationSummary: TavernVersionOperationSummary? = null,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onTavernVersion: () -> Unit,
    onTavernUpdate: () -> Unit,
    onTavernRollback: () -> Unit,
    onOpenSafetyBackup: () -> Unit = {},
    uploadLimitStatus: TavernUploadLimitStatus = TavernUploadLimitStatus(),
    onResetUploadLimit: () -> Unit = {},
) {
    val actionState = TavernVersionActionGuards.evaluate(
        current = tavernVersionInfo,
        target = selectedVersion,
        officialVersions = officialVersions,
        currentRepoUrl = currentRepoUrl,
        tavernRunning = tavernRunning,
        tavernStarting = tavernStarting,
    )
    val versionChoices = TavernVersionSelection.versionManagementChoices(
        officialVersions = officialVersions,
        current = tavernVersionInfo,
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CurrentVersionSection(
            tavernVersionInfo = tavernVersionInfo,
            actionsLocked = actionsLocked,
            tavernRunning = tavernRunning || tavernStarting,
            uploadLimitStatus = uploadLimitStatus,
            onResetUploadLimit = onResetUploadLimit,
            onRefreshCurrentVersion = onTavernVersion,
        )
        TargetVersionSection(
            officialVersions = officialVersions,
            versionChoices = versionChoices,
            currentRepoUrl = currentRepoUrl,
            selectedVersion = selectedVersion,
            actionsLocked = actionsLocked,
            onRefreshOfficialVersions = onRefreshOfficialVersions,
            onSelectVersion = onSelectVersion,
        )
        VersionExecutionSection(
            currentVersionInfo = tavernVersionInfo,
            selectedVersion = selectedVersion,
            actionState = actionState,
            actionsLocked = actionsLocked,
            onUpdate = onTavernUpdate,
            onRollback = onTavernRollback,
        )
        lastOperationSummary?.let {
            VersionOperationResultSection(
                summary = it,
                onOpenSafetyBackup = onOpenSafetyBackup,
            )
        }
    }
}

@Composable
private fun VersionOperationResultSection(
    summary: TavernVersionOperationSummary,
    onOpenSafetyBackup: () -> Unit,
) {
    val actionText = when (summary.kind) {
        TavernVersionActionKind.Update -> "更新"
        TavernVersionActionKind.Rollback -> "回退"
    }
    val resultText = when {
        !summary.succeeded -> "${actionText}未完成"
        summary.revisionChanged -> "${actionText}已完成"
        else -> "任务已完成，版本未变化"
    }
    val tone = if (summary.succeeded) LukoaColors.Primary else LukoaColors.Accent
    SectionPanel(
        title = "上次执行结果",
        accentColor = tone,
        headerAction = {
            StatusPill(
                text = resultText,
                active = true,
                toneColor = tone,
                activeBackground = if (summary.succeeded) LukoaColors.PrimarySoft else LukoaColors.AccentSoft,
            )
        },
    ) {
        VersionInfoLine("执行目标", summary.target.ifBlank { "未读取" })
        VersionInfoLine("执行前提交", summary.beforeRevision.ifBlank { "未读取" })
        VersionInfoLine("执行后提交", summary.afterRevision.ifBlank { "未读取" })
        VersionInfoLine(
            "依赖安装",
            when (summary.npmExitCode) {
                null -> "未报告"
                0 -> "已完成"
                else -> "失败（代码 ${summary.npmExitCode}）"
            },
        )
        Text(
            text = if (summary.succeeded) {
                "程序版本切换结果已经返回；安全备份仍保留，可在备份页查看。"
            } else {
                "操作没有完整成功。安全备份仍保留，请先查看日志或诊断，再决定是否恢复。"
            },
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        if (summary.safetyBackupPath.isNotBlank()) {
            VersionInfoLine("安全备份", summary.safetyBackupPath)
            SecondaryActionButton(
                text = "到备份页查看安全备份",
                enabled = true,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSafetyBackup,
            )
        }
    }
}

@Composable
private fun CurrentVersionSection(
    tavernVersionInfo: TavernVersionInfo,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    uploadLimitStatus: TavernUploadLimitStatus,
    onResetUploadLimit: () -> Unit,
    onRefreshCurrentVersion: () -> Unit,
) {
    var showTechnicalDetails by remember { mutableStateOf(false) }
    val status = currentVersionStatus(tavernVersionInfo)

    SectionPanel(
        title = "当前安装",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = status.text,
                    active = true,
                    toneColor = status.tone,
                    activeBackground = status.background,
                )
                InfoPopoverButton(
                    contentDescription = "查看当前安装说明",
                    title = "当前安装",
                    body = "这里显示当前这套 SillyTavern 酒馆安装的版本和位置。\n“本地已修改”表示酒馆目录里的程序文件被改过。启动器会在提示中写出修改所在目录和恢复后的检查步骤。\n提交号、Git 描述和回退点属于排错信息，普通使用时不用查看。",
                )
            }
        },
    ) {
        when {
            tavernVersionInfo.hasData -> {
                Text(
                    text = tavernVersionInfo.displayVersion,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                VersionInfoLine("分支", tavernVersionInfo.branch.ifBlank { "未读取" })
                VersionInfoLine("酒馆位置", tavernVersionInfo.directory.ifBlank { "未读取" })

                if (tavernVersionInfo.hasLocalChanges) {
                    LocalChangesNotice(
                        directory = tavernVersionInfo.directory,
                        changedFilesPreview = tavernVersionInfo.changedFilesPreview,
                        likelyUploadLimitChange = TavernLocalChangesGuidance.isLikelyUploadLimitChange(
                            versionInfo = tavernVersionInfo,
                            uploadLimitStatus = uploadLimitStatus,
                        ),
                        actionsLocked = actionsLocked,
                        tavernRunning = tavernRunning,
                        onResetUploadLimit = onResetUploadLimit,
                    )
                }

                SecondaryActionButton(
                    text = if (showTechnicalDetails) "收起技术信息" else "查看技术信息",
                    enabled = true,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTechnicalDetails = !showTechnicalDetails },
                )
                if (showTechnicalDetails) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LukoaColors.Elevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Border),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VersionInfoLine("提交", tavernVersionInfo.commit.ifBlank { "未读取" })
                            VersionInfoLine("Git 描述", tavernVersionInfo.describe.ifBlank { "未读取" })
                            VersionInfoLine("回退点", tavernVersionInfo.rollbackDisplay)
                        }
                    }
                }
            }

            tavernVersionInfo.notInstalled -> {
                Text(
                    text = "当前路径里没有找到酒馆。",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                tavernVersionInfo.directory.takeIf(String::isNotBlank)?.let { directory ->
                    VersionInfoLine("检测位置", directory)
                }
                Text(
                    text = "你仍然可以在下方选择目标版本，之后回到启动页完成首次安装。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            else -> {
                Text(
                    text = "尚未读取到当前版本。启动器进入版本页时会自动读取，也可以手动重新检测。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        SecondaryActionButton(
            text = "重新检测当前版本",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefreshCurrentVersion,
        )
    }
}

@Composable
private fun LocalChangesNotice(
    directory: String,
    changedFilesPreview: String,
    likelyUploadLimitChange: Boolean,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onResetUploadLimit: () -> Unit,
) {
    var confirmUploadLimitReset by remember { mutableStateOf(false) }
    val location = directory.ifBlank { "上方“酒馆位置”显示的目录" }
    if (confirmUploadLimitReset) {
        AlertDialog(
            onDismissRequest = { confirmUploadLimitReset = false },
            containerColor = LukoaColors.Elevated,
            title = { Text("恢复聊天文件大小默认值") },
            text = {
                Text(
                    text = "启动器会从当前 SillyTavern 版本读取原本的默认大小，只恢复聊天文件大小这一个数值，不会覆盖同一文件里的其他修改。操作前会保存原文件。",
                    color = LukoaColors.TextPrimary,
                )
            },
            confirmButton = {
                DialogActionButton(
                    text = "确认恢复默认值",
                    tone = ActionTone.Safe,
                    onClick = {
                        confirmUploadLimitReset = false
                        onResetUploadLimit()
                    },
                )
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmUploadLimitReset = false }) {
                    Text("取消")
                }
            },
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.AccentSoft,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "检测到本地修改",
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                InfoPopoverButton(
                    contentDescription = "查看恢复本地修改的方法",
                    title = "怎样恢复原文件",
                    body = if (likelyUploadLimitChange) {
                        "启动器检测到聊天文件大小相关的程序文件被修改。这通常是你在设置里选择过 500MB、1GB 或 2GB。\n准备更新或回退时，先停止酒馆，再点击这里的“恢复聊天文件大小默认值”。启动器只恢复这一个数值，不会覆盖同一文件里的其他修改。\n如果恢复后仍显示本地修改，说明还有其他文件被改过，再按文件列表处理。"
                    } else {
                        "先到备份页生成一份手动备份，再打开 Termux，进入提示里的酒馆目录，用 Git 恢复你改过的程序文件。\n启动器不会自动还原不认识的文件，因为自动处理可能删除你想保留的修改。不会使用 Git 时，不要直接删除文件，可以先导出诊断日志寻求帮助。\n恢复完成后回到版本页，点击“重新检测当前版本”。"
                    },
                )
            }
            Text(
                text = "修改位置：$location",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (likelyUploadLimitChange) {
                Text(
                    text = "这很可能是你在“设置 → 修复工具 → 聊天文件大小”中修改过数值。要更新或回退，请先恢复当前酒馆版本的默认值。",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "恢复聊天文件大小默认值",
                    enabled = !actionsLocked && !tavernRunning,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmUploadLimitReset = true },
                )
                if (actionsLocked || tavernRunning) {
                    Text(
                        text = if (tavernRunning) {
                            "酒馆正在运行，请先停止酒馆再恢复默认值。"
                        } else {
                            "当前有其他任务正在处理，完成后再恢复默认值。"
                        },
                        color = LukoaColors.Accent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Text(
                    text = "要改回原文件：先到备份页生成手动备份，再打开 Termux 进入这个目录，用 Git 恢复改动；完成后回到这里重新检测。",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (changedFilesPreview.isNotBlank()) {
                Text(
                    text = "检测到的文件",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = changedFilesPreview,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TargetVersionSection(
    officialVersions: TavernOfficialVersions,
    versionChoices: TavernOfficialVersions,
    currentRepoUrl: String,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    val listMatchesCurrentSource = TavernVersionCatalog.listMatchesCurrentMirror(
        officialVersions,
        currentRepoUrl,
    )
    val sourceText = when {
        !officialVersions.hasData -> "还没有读取官方版本列表。"
        !listMatchesCurrentSource -> "当前列表来自旧下载源，请重新读取。"
        else -> "版本来源：${repoLabelFor(officialVersions.repoUrl.ifBlank { currentRepoUrl })}"
    }
    val sourceColor = if (officialVersions.hasData && !listMatchesCurrentSource) {
        LukoaColors.Accent
    } else {
        LukoaColors.TextSecondary
    }

    SectionPanel(
        title = "选择目标版本",
        accentColor = LukoaColors.Primary,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看目标版本说明",
                title = "选择目标版本",
                body = "先读取官方版本，再选择你想使用的目标。\n目标比当前新时会执行更新，比当前旧时会执行回退；无法判断新旧的自定义目标只允许按更新方式处理。\n酒馆尚未安装时，这个选择会保留给首次安装使用。大多数用户优先选择稳定版。",
            )
        },
    ) {
        OfficialVersionChooser(
            officialVersions = versionChoices,
            officialListLoaded = officialVersions.hasData,
            selectedVersion = selectedVersion,
            actionsLocked = actionsLocked,
            refreshEnabled = !actionsLocked,
            emptyStateText = if (officialVersions.hasData) {
                "当前版本已从列表中隐藏"
            } else {
                "先读取官方版本"
            },
            onRefreshOfficialVersions = onRefreshOfficialVersions,
            onSelectVersion = onSelectVersion,
        )
        Text(
            text = sourceText,
            color = sourceColor,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VersionExecutionSection(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    actionsLocked: Boolean,
    onUpdate: () -> Unit,
    onRollback: () -> Unit,
) {
    val primaryAction = preferredVersionPrimaryAction(actionState)
    val actionEnabled = !actionsLocked && when (primaryAction) {
        VersionPrimaryAction.Update -> actionState.updateAvailable
        VersionPrimaryAction.Rollback -> actionState.rollbackAvailable
    }
    val status = executionStatus(
        currentVersionInfo = currentVersionInfo,
        selectedVersion = selectedVersion,
        actionState = actionState,
        actionsLocked = actionsLocked,
    )
    val disabledReason = when {
        actionsLocked -> "当前有其他任务正在处理，请等待任务完成。"
        primaryAction == VersionPrimaryAction.Update -> actionState.updateDisabledReason
        else -> actionState.rollbackDisabledReason
    }

    SectionPanel(
        title = "准备执行",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = status.text,
                    active = true,
                    toneColor = status.tone,
                    activeBackground = status.background,
                )
                InfoPopoverButton(
                    contentDescription = "查看版本切换说明",
                    title = "更新与回退",
                    body = "这里会根据当前版本和目标版本，自动显示“执行更新”或“执行回退”。\n按钮变灰时，请先按页面提示处理，例如停止酒馆、读取版本或处理本地修改。\n真正执行前还会再次显示当前版本、目标版本和下载源，并自动创建安全备份。",
                )
            }
        },
    ) {
        VersionTransitionCard(
            currentVersion = currentVersionInfo.displayVersion,
            targetVersion = selectedVersion?.label ?: "尚未选择",
            targetSelected = selectedVersion != null,
        )

        Text(
            text = executionSummary(
                currentVersionInfo = currentVersionInfo,
                selectedVersion = selectedVersion,
                actionState = actionState,
                primaryAction = primaryAction,
                actionEnabled = actionEnabled,
                disabledReason = disabledReason,
            ),
            color = if (actionEnabled) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        SecondaryActionButton(
            text = executionButtonLabel(
                currentVersionInfo = currentVersionInfo,
                selectedVersion = selectedVersion,
                actionState = actionState,
                actionsLocked = actionsLocked,
                primaryAction = primaryAction,
            ),
            enabled = actionEnabled,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = when (primaryAction) {
                VersionPrimaryAction.Update -> onUpdate
                VersionPrimaryAction.Rollback -> onRollback
            },
        )
    }
}

@Composable
private fun VersionTransitionCard(
    currentVersion: String,
    targetVersion: String,
    targetSelected: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VersionEndpoint(
                label = "当前",
                value = currentVersion,
                color = LukoaColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "→",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            VersionEndpoint(
                label = "目标",
                value = targetVersion,
                color = if (targetSelected) LukoaColors.Primary else LukoaColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VersionEndpoint(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun currentVersionStatus(info: TavernVersionInfo): VersionStatusStyle = when {
    info.hasLocalChanges -> VersionStatusStyle("本地已修改", LukoaColors.Accent, LukoaColors.AccentSoft)
    info.hasData -> VersionStatusStyle("已读取", LukoaColors.Primary, LukoaColors.PrimarySoft)
    info.notInstalled -> VersionStatusStyle("未安装", LukoaColors.TextSecondary, LukoaColors.Elevated)
    else -> VersionStatusStyle("等待读取", LukoaColors.TextSecondary, LukoaColors.Elevated)
}

private fun executionStatus(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    actionsLocked: Boolean,
): VersionStatusStyle = when {
    actionsLocked -> VersionStatusStyle("任务处理中", LukoaColors.Primary, LukoaColors.PrimarySoft)
    actionState.instanceActive -> VersionStatusStyle("请先停止酒馆", LukoaColors.Accent, LukoaColors.AccentSoft)
    currentVersionInfo.hasLocalChanges -> VersionStatusStyle("需要处理修改", LukoaColors.Accent, LukoaColors.AccentSoft)
    currentVersionInfo.notInstalled -> VersionStatusStyle("等待首次安装", LukoaColors.TextSecondary, LukoaColors.Elevated)
    !currentVersionInfo.hasData -> VersionStatusStyle("等待读取", LukoaColors.TextSecondary, LukoaColors.Elevated)
    selectedVersion == null -> VersionStatusStyle("等待选择目标", LukoaColors.TextSecondary, LukoaColors.Elevated)
    actionState.relation == TavernTargetRelation.Same -> VersionStatusStyle("已经是此版本", LukoaColors.TextSecondary, LukoaColors.Elevated)
    actionState.updateAvailable -> VersionStatusStyle("可以更新", LukoaColors.Primary, LukoaColors.PrimarySoft)
    actionState.rollbackAvailable -> VersionStatusStyle("可以回退", LukoaColors.Primary, LukoaColors.PrimarySoft)
    else -> VersionStatusStyle("暂不可用", LukoaColors.TextSecondary, LukoaColors.Elevated)
}

private fun executionSummary(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    primaryAction: VersionPrimaryAction,
    actionEnabled: Boolean,
    disabledReason: String?,
): String = when {
    currentVersionInfo.notInstalled -> "目标版本会用于首次安装，请回到启动页继续安装酒馆。"
    actionEnabled && primaryAction == VersionPrimaryAction.Update ->
        "目标比当前版本新。执行前会再次确认，并先创建安全备份。"
    actionEnabled -> "目标比当前版本旧。执行前会再次确认，并先创建安全备份。"
    actionState.relation == TavernTargetRelation.Same -> "当前酒馆已经是你选择的目标版本。"
    selectedVersion == null -> "先在上方选择目标版本，启动器会自动判断应该更新还是回退。"
    !disabledReason.isNullOrBlank() -> disabledReason
    else -> "当前条件还不满足，请先完成上方提示的步骤。"
}

private fun executionButtonLabel(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    actionsLocked: Boolean,
    primaryAction: VersionPrimaryAction,
): String = when {
    actionsLocked -> "当前任务处理中"
    actionState.instanceActive -> "请先停止酒馆"
    currentVersionInfo.notInstalled -> "请先安装酒馆"
    !currentVersionInfo.hasData -> "请先读取当前版本"
    currentVersionInfo.hasLocalChanges -> "处理本地修改后再继续"
    selectedVersion == null -> "请先选择目标版本"
    actionState.relation == TavernTargetRelation.Same -> "已经是当前版本"
    primaryAction == VersionPrimaryAction.Rollback -> "执行回退"
    else -> "执行更新"
}

@Composable
private fun OfficialVersionChooser(
    officialVersions: TavernOfficialVersions,
    officialListLoaded: Boolean,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    refreshEnabled: Boolean = !actionsLocked,
    emptyStateText: String = "先读取官方版本",
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    var customInput by rememberSaveable { mutableStateOf("") }
    val allChoices = officialVersions.all

    if (showCustomDialog) {
        CustomVersionDialog(
            value = customInput,
            onValueChange = { customInput = it },
            onConfirm = {
                val normalized = customInput.trim()
                if (LauncherInputGuards.validateVersionTarget(normalized) == null) {
                    onSelectVersion(
                        TavernVersionChoice(
                            kind = TavernVersionKind.Custom,
                            name = normalized,
                            target = normalized,
                        ),
                    )
                    showCustomDialog = false
                }
            },
            onDismiss = { showCustomDialog = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = !actionsLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.46f)),
                shape = LukoaCapsuleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LukoaColors.Primary.copy(alpha = 0.04f),
                    contentColor = LukoaColors.Primary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = LukoaColors.Dim,
                ),
            ) {
                Text(
                    text = selectedVersion?.label ?: "选择目标版本",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = LukoaColors.Elevated,
            ) {
                if (allChoices.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(emptyStateText) },
                        enabled = false,
                        onClick = {},
                    )
                }
                if (officialVersions.stable.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("稳定版") },
                        enabled = false,
                        onClick = {},
                    )
                    officialVersions.stable.forEach { choice ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = choice.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelectVersion(choice)
                            },
                        )
                    }
                }
                if (officialVersions.test.isNotEmpty()) {
                    HorizontalDivider(color = LukoaColors.Border)
                    DropdownMenuItem(
                        text = { Text("测试版") },
                        enabled = false,
                        onClick = {},
                    )
                    officialVersions.test.forEach { choice ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = choice.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                expanded = false
                                onSelectVersion(choice)
                            },
                        )
                    }
                }
                HorizontalDivider(color = LukoaColors.Border)
                DropdownMenuItem(
                    text = { Text("自定义版本 / 分支 / commit") },
                    onClick = {
                        expanded = false
                        customInput = selectedVersion
                            ?.takeIf { it.kind == TavernVersionKind.Custom }
                            ?.target
                            .orEmpty()
                        showCustomDialog = true
                    },
                )
            }
        }

        SecondaryActionButton(
            text = if (officialListLoaded) "刷新官方版本" else "读取官方版本",
            enabled = refreshEnabled,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefreshOfficialVersions,
        )
    }
}

@Composable
private fun CustomVersionDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val normalized = value.trim()
    val validationMessage = LauncherInputGuards.validateVersionTarget(normalized)
    val valid = validationMessage == null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text("自定义酒馆版本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写版本标签、分支名或 commit。只有明确知道目标时才使用自定义输入。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text("版本 / 分支 / commit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = LukoaCapsuleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.TextPrimary,
                        unfocusedTextColor = LukoaColors.TextPrimary,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.Elevated,
                        unfocusedContainerColor = LukoaColors.Elevated,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Primary,
                        unfocusedBorderColor = LukoaColors.Border,
                        disabledBorderColor = LukoaColors.Border,
                        focusedLabelColor = LukoaColors.Primary,
                        unfocusedLabelColor = LukoaColors.TextSecondary,
                        cursorColor = LukoaColors.Primary,
                    ),
                )
                if (!valid && value.isNotBlank()) {
                    Text(
                        text = validationMessage ?: "版本格式无效。",
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "使用这个版本",
                enabled = valid,
                accentColor = LukoaColors.Primary,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton(
                text = "取消",
                enabled = true,
                accentColor = LukoaColors.Primary,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
internal fun VersionStatusValueCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                color = accentColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun VersionInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
