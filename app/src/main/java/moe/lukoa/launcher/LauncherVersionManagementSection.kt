package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun VersionManagementSection(
    modifier: Modifier = Modifier,
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
    onTavernInstall: () -> Unit = {},
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
    val selectableVersions = TavernVersionSelection.versionManagementChoices(
        officialVersions = officialVersions,
        current = tavernVersionInfo,
    )
    val pageAction = TavernVersionPageActionResolver.resolve(
        actionsLocked = actionsLocked,
        current = tavernVersionInfo,
        selectedVersion = selectedVersion,
        actionState = actionState,
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CurrentVersionBlock(
                tavernVersionInfo = tavernVersionInfo,
                actionsLocked = actionsLocked,
                onRefreshCurrentVersion = onTavernVersion,
            )
            OfficialVersionList(
                officialVersions = officialVersions,
                selectableVersions = selectableVersions,
                currentVersion = tavernVersionInfo,
                currentRepoUrl = currentRepoUrl,
                selectedVersion = selectedVersion,
                actionsLocked = actionsLocked,
                onRefreshOfficialVersions = onRefreshOfficialVersions,
                onSelectVersion = onSelectVersion,
                onPagerLockChange = onPagerLockChange,
            )
        }

        VersionStickyActionBar(
            action = pageAction,
            onInstall = onTavernInstall,
            onUpdate = onTavernUpdate,
            onRollback = onTavernRollback,
        )
    }
}

@Composable
private fun CurrentVersionBlock(
    tavernVersionInfo: TavernVersionInfo,
    actionsLocked: Boolean,
    onRefreshCurrentVersion: () -> Unit,
) {
    val stateColor = when {
        tavernVersionInfo.hasLocalChanges -> LukoaColors.Danger
        tavernVersionInfo.hasData -> LukoaColors.Accent
        tavernVersionInfo.notInstalled -> LukoaColors.Danger
        else -> LukoaColors.Dim
    }
    val versionText = when {
        tavernVersionInfo.notInstalled -> "未安装"
        tavernVersionInfo.hasData -> tavernVersionInfo.displayVersion
        else -> "尚未读取"
    }
    val detailText = when {
        tavernVersionInfo.hasData -> buildString {
            append(tavernVersionInfo.branch.ifBlank { "未知分支" })
            tavernVersionInfo.commit.takeIf { it.isNotBlank() }?.let { commit ->
                append(" · ")
                append(commit.take(8))
            }
            append(if (tavernVersionInfo.hasLocalChanges) " · 工作区有改动" else " · 工作区干净")
        }
        tavernVersionInfo.notInstalled -> tavernVersionInfo.directory.ifBlank { "没有找到酒馆目录" }
        else -> "重新检测后会显示当前版本、分支和工作区状态"
    }

    DashedSection(
        label = "当前安装",
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(stateColor, LukoaCapsuleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = versionText,
                    color = if (tavernVersionInfo.notInstalled) LukoaColors.Dim else LukoaColors.Text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detailText,
                    color = if (tavernVersionInfo.hasLocalChanges) LukoaColors.Danger else LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SecondaryActionButton(
                text = if (tavernVersionInfo.notInstalled) "检测酒馆" else "重新检测",
                enabled = !actionsLocked,
                accentColor = LukoaColors.Text,
                modifier = Modifier.width(96.dp),
                onClick = onRefreshCurrentVersion,
            )
        }
    }
}

@Composable
private fun OfficialVersionList(
    officialVersions: TavernOfficialVersions,
    selectableVersions: TavernOfficialVersions,
    currentVersion: TavernVersionInfo,
    currentRepoUrl: String,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onPagerLockChange: (Boolean) -> Unit,
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    val selectableKeys = remember(selectableVersions) {
        selectableVersions.all.map(::versionChoiceKey).toSet()
    }

    DisposableEffect(Unit) {
        onDispose { onPagerLockChange(false) }
    }

    fun closeCustomDialog() {
        showCustomDialog = false
        onPagerLockChange(false)
    }

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
                    closeCustomDialog()
                }
            },
            onDismiss = ::closeCustomDialog,
        )
    }

    DashedSection(
        label = "选择目标版本",
        headerAction = {
            HelpHint(
                text = "官方版本会自动判断更新或回退。自定义分支或 commit 无法可靠比较时，底部会显示“切换到”，不会猜测成更新或回退。",
            )
        },
    ) {
        if (!officialVersions.hasData) {
            Text(
                text = "还没有读取当前 Git 源的官方版本列表，也可以直接填写自定义版本。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        VersionChoiceGroup(
            choices = officialVersions.stable,
            groupLabel = "稳定版",
            latestChoice = officialVersions.stable.firstOrNull(),
            selectableKeys = selectableKeys,
            currentVersion = currentVersion,
            selectedVersion = selectedVersion,
            actionsLocked = actionsLocked,
            onSelectVersion = onSelectVersion,
        )
        VersionChoiceGroup(
            choices = officialVersions.test,
            groupLabel = "测试版",
            latestChoice = null,
            selectableKeys = selectableKeys,
            currentVersion = currentVersion,
            selectedVersion = selectedVersion,
            actionsLocked = actionsLocked,
            onSelectVersion = onSelectVersion,
        )

        if (officialVersions.hasData) {
            HorizontalDivider(color = LukoaColors.Divider)
        }
        VersionChoiceRow(
            title = "自定义版本 / 分支 / commit",
            detail = selectedVersion
                ?.takeIf { it.kind == TavernVersionKind.Custom }
                ?.let { "${it.target} · 已选中" }
                ?: "手动指定 git ref",
            selected = selectedVersion?.kind == TavernVersionKind.Custom,
            current = false,
            latest = false,
            enabled = !actionsLocked,
            onClick = {
                customInput = selectedVersion
                    ?.takeIf { it.kind == TavernVersionKind.Custom }
                    ?.target
                    .orEmpty()
                showCustomDialog = true
                onPagerLockChange(true)
            },
        )

        SecondaryActionButton(
            text = if (officialVersions.hasData) "↻ 刷新版本列表" else "↻ 读取版本列表",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Text,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefreshOfficialVersions,
        )
        Text(
            text = when {
                !officialVersions.hasData -> "当前还没读到这个 Git 源的官方版本列表。"
                !TavernVersionCatalog.listMatchesCurrentMirror(officialVersions, currentRepoUrl) ->
                    "当前显示的是旧 Git 源的版本列表，请先刷新。"
                else -> "列表来源：${repoLabelFor(officialVersions.repoUrl.ifBlank { currentRepoUrl })}"
            },
            color = if (
                officialVersions.hasData &&
                !TavernVersionCatalog.listMatchesCurrentMirror(officialVersions, currentRepoUrl)
            ) {
                LukoaColors.Amber
            } else {
                LukoaColors.Dim
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun VersionChoiceGroup(
    choices: List<TavernVersionChoice>,
    groupLabel: String,
    latestChoice: TavernVersionChoice?,
    selectableKeys: Set<String>,
    currentVersion: TavernVersionInfo,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    choices.forEachIndexed { index, choice ->
        if (index > 0) {
            HorizontalDivider(color = LukoaColors.Divider)
        }
        val current = TavernVersionComparator.matchesCurrent(currentVersion, choice)
        val selected = sameVersionChoice(choice, selectedVersion)
        val selectable = versionChoiceKey(choice) in selectableKeys
        VersionChoiceRow(
            title = choice.name,
            detail = buildString {
                append(groupLabel)
                choice.commit.takeIf { it.isNotBlank() }?.let { commit ->
                    append(" · ")
                    append(commit.take(8))
                }
                when {
                    current -> append(" · 当前安装")
                    selected -> append(" · 已选中")
                }
            },
            selected = selected,
            current = current,
            latest = latestChoice?.let { sameVersionChoice(choice, it) } == true,
            enabled = !actionsLocked && selectable && !current,
            onClick = { onSelectVersion(choice) },
        )
    }
    if (choices.isNotEmpty()) {
        HorizontalDivider(color = LukoaColors.Divider)
    }
}

@Composable
private fun VersionChoiceRow(
    title: String,
    detail: String,
    selected: Boolean,
    current: Boolean,
    latest: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = feedbackClick,
            )
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = if (selected) LukoaColors.Accent else LukoaColors.Text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                color = if (selected) LukoaColors.Accent else LukoaColors.Dim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            selected -> SelectedVersionMark()
            current -> CompactVersionPill("当前", LukoaColors.Muted)
            latest -> CompactVersionPill("最新", LukoaColors.Accent)
            else -> Text(
                text = "›",
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun SelectedVersionMark() {
    Surface(
        modifier = Modifier.size(22.dp),
        color = LukoaColors.Accent,
        shape = LukoaCapsuleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "✓",
                color = LukoaColors.AccentDark,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CompactVersionPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = LukoaCapsuleShape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun VersionStickyActionBar(
    action: TavernVersionPageAction,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    onRollback: () -> Unit,
) {
    val onClick = when (action.kind) {
        TavernVersionPageActionKind.Install -> onInstall
        TavernVersionPageActionKind.Update,
        TavernVersionPageActionKind.Switch -> onUpdate
        TavernVersionPageActionKind.Rollback -> onRollback
        TavernVersionPageActionKind.None -> ({})
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Background,
    ) {
        Column {
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = action.currentLabel,
                            color = LukoaColors.Dim,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = " → ",
                            color = LukoaColors.Dim,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = action.targetLabel,
                            color = LukoaColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactVersionPill(
                        text = action.badgeLabel,
                        color = if (action.kind == TavernVersionPageActionKind.None) LukoaColors.Muted else LukoaColors.Accent,
                    )
                }
                action.disabledReason?.takeIf { !action.enabled }?.let { reason ->
                    Text(
                        text = reason,
                        color = LukoaColors.Dim,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PrimaryActionButton(
                    text = action.buttonLabel,
                    enabled = action.enabled,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClick,
                )
            }
        }
    }
}

private fun sameVersionChoice(
    first: TavernVersionChoice,
    second: TavernVersionChoice?,
): Boolean {
    if (second == null) return false
    return versionChoiceKey(first) == versionChoiceKey(second)
}

private fun versionChoiceKey(choice: TavernVersionChoice): String {
    return "${choice.kind}:${choice.name}:${choice.target}"
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
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = { Text("自定义酒馆版本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "填写 tag、分支名或 commit。自定义目标无法可靠比较新旧时，会按“切换到”处理。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    label = { Text("版本 / 分支 / commit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Accent,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Accent,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Accent,
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
                accentColor = LukoaColors.Accent,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Text, onClick = onDismiss)
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
            textAlign = TextAlign.End,
            softWrap = true,
        )
    }
}
