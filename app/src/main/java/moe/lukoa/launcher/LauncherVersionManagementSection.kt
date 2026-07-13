package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


private enum class VersionPageView {
    Current,
    Target,
}


@Composable
fun VersionManagementSection(
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    tavernVersionInfo: TavernVersionInfo,
    officialVersions: TavernOfficialVersions,
    currentRepoUrl: String,
    selectedVersion: TavernVersionChoice?,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onTavernVersion: () -> Unit,
    onTavernUpdate: () -> Unit,
    onTavernRollback: () -> Unit,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    val actionState = TavernVersionActionGuards.evaluate(
        current = tavernVersionInfo,
        target = selectedVersion,
        officialVersions = officialVersions,
        currentRepoUrl = currentRepoUrl,
        tavernRunning = tavernRunning,
        tavernStarting = tavernStarting,
    )
    val versionManagementChoices = TavernVersionSelection.versionManagementChoices(
        officialVersions = officialVersions,
        current = tavernVersionInfo,
    )
    val updateEnabled = !actionsLocked && actionState.updateAvailable
    val rollbackEnabled = !actionsLocked && actionState.rollbackAvailable
    val disabledReasons = listOfNotNull(
        actionState.updateDisabledReason?.let { "更新：$it" },
        actionState.rollbackDisabledReason?.let { "回退：$it" },
    ).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        VersionOverviewCard(
            tavernVersionInfo = tavernVersionInfo,
            selectedVersion = selectedVersion,
            officialVersions = officialVersions,
            actionsLocked = actionsLocked,
            onRefreshCurrentVersion = onTavernVersion,
        )
        listOf(VersionPageView.Current, VersionPageView.Target).forEach { view ->
            when (view) {
            VersionPageView.Current -> SectionPanel(title = "安装详情", accentColor = LukoaColors.Accent) {
                Text(
                    text = tavernVersionInfo.displayVersion,
                    color = when {
                        tavernVersionInfo.hasData -> LukoaColors.Text
                        tavernVersionInfo.notInstalled -> LukoaColors.Amber
                        else -> LukoaColors.Muted
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (tavernVersionInfo.hasData) {
                    VersionInfoLine("分支", tavernVersionInfo.branch.ifBlank { "未读取" })
                    VersionInfoLine("提交", tavernVersionInfo.commit.ifBlank { "未读取" })
                    VersionInfoLine("Git 描述", tavernVersionInfo.describe.ifBlank { "未读取" })
                    VersionInfoLine("回退点", tavernVersionInfo.rollbackDisplay)
                    VersionInfoLine("目录", tavernVersionInfo.directory.ifBlank { "未读取" })
                    if (tavernVersionInfo.hasLocalChanges) {
                        Text(
                            text = "酒馆源码有改动，先处理后再更新或回退。",
                            color = LukoaColors.Danger,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = tavernVersionInfo.changedFilesPreview,
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (tavernVersionInfo.notInstalled) {
                    Text(
                        text = "没有找到酒馆目录。先读取目标版本，再回启动页安装酒馆。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (tavernVersionInfo.directory.isNotBlank()) {
                        VersionInfoLine("检测目录", tavernVersionInfo.directory)
                    }
                } else {
                    Text(
                        text = "点上面的重新检测酒馆版本，就能读取当前安装信息。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            VersionPageView.Target -> SectionPanel(title = "目标版本与切换", accentColor = LukoaColors.Accent) {
                Text(
                    text = when {
                        tavernVersionInfo.hasData -> "先读取列表，再选目标版本。"
                        tavernVersionInfo.notInstalled -> "酒馆未安装，也可以先读取官方版本给安装使用。"
                        else -> "可以先读取官方版本；更新或回退前再检测当前版本。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )

                OfficialVersionChooser(
                    title = "官方版本下拉框",
                    officialVersions = versionManagementChoices,
                    selectedVersion = selectedVersion,
                    actionsLocked = actionsLocked,
                    refreshEnabled = !actionsLocked,
                    emptyStateText = if (officialVersions.hasData) {
                        "当前版本已经隐藏，暂时没有别的官方版本。"
                    } else {
                        "先读取官方版本"
                    },
                    onRefreshOfficialVersions = onRefreshOfficialVersions,
                    onSelectVersion = onSelectVersion,
                )
                Text(
                    text = when {
                        !officialVersions.hasData -> "当前还没读到这个 Git 源的官方版本列表。"
                        !TavernVersionCatalog.listMatchesCurrentMirror(officialVersions, currentRepoUrl) ->
                            "当前显示的是旧 Git 源的版本列表，请先刷新。"
                        else -> "当前列表来源：${repoLabelFor(officialVersions.repoUrl.ifBlank { currentRepoUrl })}"
                    },
                    color = when {
                        !officialVersions.hasData -> LukoaColors.Muted
                        !TavernVersionCatalog.listMatchesCurrentMirror(officialVersions, currentRepoUrl) ->
                            LukoaColors.Amber
                        else -> LukoaColors.Muted
                    },
                    style = MaterialTheme.typography.bodySmall,
                )

                VersionOperationStatusCard(
                    currentVersionInfo = tavernVersionInfo,
                    selectedVersion = selectedVersion,
                    actionState = actionState,
                    disabledReasons = disabledReasons,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SecondaryActionButton(
                        text = "更新到 ${selectedVersion?.name ?: "目标版本"}",
                        enabled = updateEnabled,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier
                            .weight(1f),
                        onClick = onTavernUpdate,
                    )
                    SecondaryActionButton(
                        text = "回退到 ${selectedVersion?.name ?: "目标版本"}",
                        enabled = rollbackEnabled,
                        accentColor = LukoaColors.Accent,
                        modifier = Modifier
                            .weight(1f),
                        onClick = onTavernRollback,
                    )
                }

                Text(
                    text = if (tavernVersionInfo.notInstalled) {
                        "未安装时不能更新或回退，先安装酒馆。"
                    } else {
                        "执行前会先自动创建一份安全备份，只切换程序版本，不删聊天、角色、世界书和插件。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        }
    }
}

@Composable
private fun VersionOverviewCard(
    tavernVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    officialVersions: TavernOfficialVersions,
    actionsLocked: Boolean,
    onRefreshCurrentVersion: () -> Unit,
) {
    val statusText: String
    val statusColor: Color
    when {
        tavernVersionInfo.hasLocalChanges -> {
            statusText = "本地已修改"
            statusColor = LukoaColors.Danger
        }
        tavernVersionInfo.hasData -> {
            statusText = "就绪"
            statusColor = LukoaColors.Accent
        }
        tavernVersionInfo.notInstalled -> {
            statusText = "未安装"
            statusColor = LukoaColors.Amber
        }
        else -> {
            statusText = "检测中"
            statusColor = LukoaColors.Muted
        }
    }

    DashedSection(
        label = "当前安装",
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (tavernVersionInfo.hasData) tavernVersionInfo.displayVersion else "尚未读取版本",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(
                    text = statusText,
                    active = statusColor != LukoaColors.Muted,
                    toneColor = statusColor,
                    activeBackground = statusColor.copy(alpha = 0.15f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                    VersionStatBlock(
                        label = "当前版本",
                        value = if (tavernVersionInfo.hasData) tavernVersionInfo.displayVersion else "未知",
                        accentColor = if (tavernVersionInfo.hasData) LukoaColors.Text else LukoaColors.Dim,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = LukoaColors.Line,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    VersionStatBlock(
                        label = "目标版本",
                        value = selectedVersion?.label ?: "未选择",
                        accentColor = if (selectedVersion != null) LukoaColors.Accent else LukoaColors.Dim,
                        modifier = Modifier.weight(1f)
                    )
            }
            SecondaryActionButton(
                text = "重新检测酒馆版本",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefreshCurrentVersion,
            )
        }
    }
}

@Composable
private fun VersionOperationStatusCard(
    currentVersionInfo: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionState: TavernVersionActionState,
    disabledReasons: List<String>,
) {
    val statusText: String
    val statusColor: Color
    when {
        actionState.instanceActive -> {
            statusText = "先停止当前实例"
            statusColor = LukoaColors.Danger
        }
        currentVersionInfo.notInstalled -> {
            statusText = "先安装酒馆"
            statusColor = LukoaColors.Amber
        }
        !currentVersionInfo.hasData -> {
            statusText = "先读取当前版本"
            statusColor = LukoaColors.Amber
        }
        currentVersionInfo.hasLocalChanges -> {
            statusText = "源码有本地改动"
            statusColor = LukoaColors.Danger
        }
        selectedVersion == null -> {
            statusText = "先选目标版本"
            statusColor = LukoaColors.Amber
        }
        actionState.updateAvailable -> {
            statusText = "可以更新"
            statusColor = LukoaColors.Accent
        }
        actionState.rollbackAvailable -> {
            statusText = "可以回退"
            statusColor = LukoaColors.Accent
        }
        actionState.relation == TavernTargetRelation.Same -> {
            statusText = "已经是这个版本"
            statusColor = LukoaColors.Muted
        }
        else -> {
            statusText = "暂时不能执行"
            statusColor = LukoaColors.Muted
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "操作状态",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionStatusValueCard(
                    label = "当前版本",
                    value = currentVersionInfo.displayVersion,
                    accentColor = when {
                        currentVersionInfo.hasData -> LukoaColors.Text
                        currentVersionInfo.notInstalled -> LukoaColors.Amber
                        else -> LukoaColors.Muted
                    },
                    modifier = Modifier.weight(1f),
                )
                VersionStatusValueCard(
                    label = "目标版本",
                    value = selectedVersion?.label ?: "未选择",
                    accentColor = if (selectedVersion == null) LukoaColors.Muted else LukoaColors.Accent,
                    modifier = Modifier.weight(1f),
                )
            }

            selectedVersion?.let { choice ->
                Text(
                    text = when (choice.kind) {
                        TavernVersionKind.Stable -> "稳定版，适合大多数人。"
                        TavernVersionKind.Test -> "测试版，可能有新功能，也可能不稳定。"
                        TavernVersionKind.Custom -> "自定义目标，请确认版本名、分支名或 commit 没填错。"
                    },
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            TavernVersionActionGuards.relationHint(actionState, selectedVersion)
                ?.takeIf { it.isNotBlank() }
                ?.let { hint ->
                    Text(
                        text = hint,
                        color = if (statusColor == LukoaColors.Accent) LukoaColors.Accent else LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

            if (disabledReasons.isNotEmpty()) {
                HorizontalDivider(color = LukoaColors.Line)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前限制",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    disabledReasons.forEach { reason ->
                        Text(
                            text = reason,
                            color = LukoaColors.Text,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionStatBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
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
        border = BorderStroke(1.dp, LukoaColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = LukoaColors.Muted,
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
private fun OfficialVersionChooser(
    title: String,
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    refreshEnabled: Boolean = !actionsLocked,
    refreshDisabledMessage: String? = null,
    emptyStateText: String = "先读取官方版本",
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = selectedVersion?.label ?: "未选择",
                color = if (selectedVersion == null) LukoaColors.Muted else LukoaColors.Amber,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !actionsLocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.46f)),
                    shape = LukoaCapsuleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = LukoaColors.Accent,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = LukoaColors.Dim,
                    ),
                ) {
                    Text(
                        text = selectedVersion?.label ?: "选择版本",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = LukoaColors.Surface,
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
                        HorizontalDivider(color = LukoaColors.Line)
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
                    HorizontalDivider(color = LukoaColors.Line)
                    DropdownMenuItem(
                        text = { Text("自定义版本 / 分支 / commit") },
                        onClick = {
                            expanded = false
                            customInput = selectedVersion?.takeIf { it.kind == TavernVersionKind.Custom }?.target.orEmpty()
                            showCustomDialog = true
                        },
                    )
                }
            }

            SecondaryActionButton(
                text = if (!refreshEnabled && refreshDisabledMessage != null) refreshDisabledMessage else if (officialVersions.hasData) "刷新" else "读取",
                enabled = refreshEnabled,
                accentColor = LukoaColors.Accent,
                modifier = Modifier.weight(0.55f),
                onClick = onRefreshOfficialVersions,
            )
        }

        if (!officialVersions.hasData) {
            Text(
                text = "读取官方版本，也可以自定义输入。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (allChoices.isEmpty()) {
            Text(
                text = "当前同版本已经从列表里隐藏。想切换别的目标，也可以手动输入。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("自定义酒馆版本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写 tag、分支名或 commit。",
                    color = LukoaColors.Muted,
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
                accentColor = LukoaColors.Amber,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Amber, onClick = onDismiss)
        },
    )
}

@Composable
internal fun VersionInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
