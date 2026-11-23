package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MirrorSettingsSection(
    tavernMirrorConfig: TavernMirrorConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    actionsLocked: Boolean,
    autoSelecting: Boolean = false,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseGithubProxyAltMirror: () -> Unit = {},
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onAutoSelectBestMirror: () -> Unit = onCheckTavernMirror,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onApplyTermuxMirrorUrl: (String) -> Unit = {
        onCustomTermuxRepoInputChange(it)
        onApplyCustomTermuxMirror()
    },
    onShowHint: (String) -> Unit = {},
) {
    var showMirrorDialog by remember { mutableStateOf(false) }

    if (showMirrorDialog) {
        MirrorSettingsDialog(
            tavernMirrorConfig = tavernMirrorConfig,
            tavernRepoInput = tavernRepoInput,
            npmRegistryInput = npmRegistryInput,
            mirrorProbeStatus = mirrorProbeStatus,
            termuxRepoStatus = termuxRepoStatus,
            customTermuxRepoInput = customTermuxRepoInput,
            actionsLocked = actionsLocked,
            autoSelecting = autoSelecting,
            onTavernRepoInputChange = onTavernRepoInputChange,
            onNpmRegistryInputChange = onNpmRegistryInputChange,
            onCustomTermuxRepoInputChange = onCustomTermuxRepoInputChange,
            onSaveTavernMirror = onSaveTavernMirror,
            onUseOfficialMirror = onUseOfficialMirror,
            onUseGithubProxyMirror = onUseGithubProxyMirror,
            onUseGithubProxyAltMirror = onUseGithubProxyAltMirror,
            onUseNpmMirror = onUseNpmMirror,
            onCheckTavernMirror = onCheckTavernMirror,
            onAutoSelectBestMirror = onAutoSelectBestMirror,
            onReadTermuxRepoStatus = onReadTermuxRepoStatus,
            onApplyTermuxMirrorUrl = onApplyTermuxMirrorUrl,
            onApplyCustomTermuxMirror = onApplyCustomTermuxMirror,
            onShowHint = onShowHint,
            onDismiss = { showMirrorDialog = false },
        )
    }

    SectionPanel(title = "网络与镜像源", accentColor = LukoaColors.Primary) {
        MirrorStatusCard(
            tavernMirrorConfig = tavernMirrorConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            termuxRepoStatus = termuxRepoStatus,
            autoSelecting = autoSelecting,
            onCheckTavernMirror = onCheckTavernMirror,
            onAutoSelectBestMirror = onAutoSelectBestMirror,
            onShowHint = onShowHint,
        )
        SecondaryActionButton(
            text = "切换设置",
            enabled = !actionsLocked,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            onClick = { showMirrorDialog = true },
        )
    }
}

@Composable
internal fun MirrorSettingsDialog(
    tavernMirrorConfig: TavernMirrorConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    actionsLocked: Boolean,
    autoSelecting: Boolean = false,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseGithubProxyAltMirror: () -> Unit = {},
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onAutoSelectBestMirror: () -> Unit = onCheckTavernMirror,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onApplyTermuxMirrorUrl: (String) -> Unit = {
        onCustomTermuxRepoInputChange(it)
        onApplyCustomTermuxMirror()
    },
    onShowHint: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "网络与镜像源",
                infoText = "下载源决定安装或更新时从哪里下载文件，不会影响聊天数据。\n不确定选哪个时，直接点「一键测速优选」即可自动挑选最快线路。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                MirrorSettingsContent(
                    tavernMirrorConfig = tavernMirrorConfig,
                    tavernRepoInput = tavernRepoInput,
                    npmRegistryInput = npmRegistryInput,
                    mirrorProbeStatus = mirrorProbeStatus,
                    termuxRepoStatus = termuxRepoStatus,
                    customTermuxRepoInput = customTermuxRepoInput,
                    actionsLocked = actionsLocked,
                    autoSelecting = autoSelecting,
                    onTavernRepoInputChange = onTavernRepoInputChange,
                    onNpmRegistryInputChange = onNpmRegistryInputChange,
                    onCustomTermuxRepoInputChange = onCustomTermuxRepoInputChange,
                    onSaveTavernMirror = onSaveTavernMirror,
                    onUseOfficialMirror = onUseOfficialMirror,
                    onUseGithubProxyMirror = onUseGithubProxyMirror,
                    onUseGithubProxyAltMirror = onUseGithubProxyAltMirror,
                    onUseNpmMirror = onUseNpmMirror,
                    onCheckTavernMirror = onCheckTavernMirror,
                    onAutoSelectBestMirror = onAutoSelectBestMirror,
                    onReadTermuxRepoStatus = onReadTermuxRepoStatus,
                    onApplyTermuxMirrorUrl = onApplyTermuxMirrorUrl,
                    onApplyCustomTermuxMirror = onApplyCustomTermuxMirror,
                    onShowHint = onShowHint,
                )
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "关闭",
                enabled = true,
                accentColor = LukoaColors.Primary,
                onClick = onDismiss,
            )
        },
        dismissButton = null,
    )
}

@Composable
internal fun MirrorSettingsContent(
    tavernMirrorConfig: TavernMirrorConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    actionsLocked: Boolean,
    autoSelecting: Boolean = false,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseGithubProxyAltMirror: () -> Unit = {},
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onAutoSelectBestMirror: () -> Unit = onCheckTavernMirror,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyTermuxMirrorUrl: (String) -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onShowHint: (String) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        if (!termuxRepoStatus.hasData && !actionsLocked) {
            onReadTermuxRepoStatus()
        }
        if (
            mirrorProbeStatus.overallLevel == MirrorProbeLevel.Unknown &&
            !mirrorProbeStatus.checking &&
            !autoSelecting
        ) {
            onCheckTavernMirror()
        }
    }

    val isDomesticPrimary = sameRepoUrl(tavernMirrorConfig.normalizedRepoUrl, TavernMirrorDefaults.GITHUB_PROXY_REPO) &&
        sameRepoUrl(tavernMirrorConfig.normalizedNpmRegistry, TavernMirrorDefaults.NPMMIRROR_REGISTRY)
    val isDomesticAlt = sameRepoUrl(tavernMirrorConfig.normalizedRepoUrl, TavernMirrorDefaults.GITHUB_PROXY_ALT_REPO) &&
        sameRepoUrl(tavernMirrorConfig.normalizedNpmRegistry, TavernMirrorDefaults.NPMMIRROR_REGISTRY)
    val isOfficialAll = sameRepoUrl(tavernMirrorConfig.normalizedRepoUrl, TavernMirrorDefaults.OFFICIAL_REPO) &&
        sameRepoUrl(tavernMirrorConfig.normalizedNpmRegistry, TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY)
    val isDirectGitMirrorNpm = sameRepoUrl(tavernMirrorConfig.normalizedRepoUrl, TavernMirrorDefaults.OFFICIAL_REPO) &&
        sameRepoUrl(tavernMirrorConfig.normalizedNpmRegistry, TavernMirrorDefaults.NPMMIRROR_REGISTRY)
    val isCustomTavernOrNpm = !isDomesticPrimary && !isDomesticAlt && !isOfficialAll && !isDirectGitMirrorNpm

    var showCustomTavern by rememberSaveable { mutableStateOf(isCustomTavernOrNpm) }
    var showCustomTermux by rememberSaveable { mutableStateOf(false) }

    val repoInputError = TavernMirrorValidator.validateRepoUrl(tavernRepoInput)
    val npmInputError = TavernMirrorValidator.validateNpmRegistry(npmRegistryInput)
    val customTavernDirty = !sameRepoUrl(tavernRepoInput, tavernMirrorConfig.normalizedRepoUrl) ||
        !sameRepoUrl(npmRegistryInput, tavernMirrorConfig.normalizedNpmRegistry)
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null
    val probingHint = if (mirrorProbeStatus.checking || autoSelecting) "正在测速检测中，请稍等。" else null

    val termuxUri = termuxRepoStatus.uri.trim().trimEnd('/')
    val isTermuxTuna = termuxRepoStatus.hasData && sameRepoUrl(termuxUri, TavernMirrorDefaults.TERMUX_TUNA_REPO)
    val isTermuxUstc = termuxRepoStatus.hasData && sameRepoUrl(termuxUri, TavernMirrorDefaults.TERMUX_USTC_REPO)
    val isTermuxOfficial = termuxRepoStatus.hasData && sameRepoUrl(termuxUri, TavernMirrorDefaults.TERMUX_OFFICIAL_REPO)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsGroupLabel(
                    title = "连通性与方案",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when {
                        autoSelecting -> "优选测速中"
                        mirrorProbeStatus.checking -> "检测中"
                        else -> mirrorProbeStatus.overallLevel.label()
                    },
                    color = if (autoSelecting || mirrorProbeStatus.checking) {
                        LukoaColors.Primary
                    } else {
                        mirrorProbeStatus.overallLevel.toneColor()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            MirrorStatusCard(
                tavernMirrorConfig = tavernMirrorConfig,
                mirrorProbeStatus = mirrorProbeStatus,
                termuxRepoStatus = termuxRepoStatus,
                autoSelecting = autoSelecting,
                onCheckTavernMirror = onCheckTavernMirror,
                onAutoSelectBestMirror = onAutoSelectBestMirror,
                onShowHint = onShowHint,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("酒馆与 npm 源")
            SettingsEntryGroup {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "切换预设会立即保存并自动测速验证。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsCompactChoiceButton(
                            text = "国内推荐",
                            enabled = !autoSelecting,
                            selected = isDomesticPrimary,
                            accentColor = if (isDomesticPrimary) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = probingHint,
                            onShowHint = onShowHint,
                            onClick = onUseGithubProxyMirror,
                        )
                        SettingsCompactChoiceButton(
                            text = "官方直连",
                            enabled = !autoSelecting,
                            selected = isOfficialAll,
                            accentColor = if (isOfficialAll) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = probingHint,
                            onShowHint = onShowHint,
                            onClick = onUseOfficialMirror,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsCompactChoiceButton(
                            text = "国内备用",
                            enabled = !autoSelecting,
                            selected = isDomesticAlt,
                            accentColor = if (isDomesticAlt) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = probingHint,
                            onShowHint = onShowHint,
                            onClick = onUseGithubProxyAltMirror,
                        )
                        SettingsCompactChoiceButton(
                            text = "仅换 npm 镜像",
                            enabled = !autoSelecting,
                            selected = isDirectGitMirrorNpm,
                            accentColor = if (isDirectGitMirrorNpm) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = probingHint,
                            onShowHint = onShowHint,
                            onClick = onUseNpmMirror,
                        )
                    }
                }
            }
            LukoaCollapsibleCard(
                title = "自定义酒馆 / npm 地址",
                subtitle = "手动填写私有 Git 镜像或自定义 npm 源",
                expanded = showCustomTavern,
                onToggle = { showCustomTavern = !showCustomTavern },
            ) {
                LukoaTextField(
                    value = tavernRepoInput,
                    onValueChange = onTavernRepoInputChange,
                    enabled = !autoSelecting,
                    label = "酒馆 Git 源",
                    placeholder = TavernMirrorDefaults.OFFICIAL_REPO,
                )
                if (repoInputError != null && tavernRepoInput.isNotBlank()) {
                    Text(
                        text = repoInputError,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                LukoaTextField(
                    value = npmRegistryInput,
                    onValueChange = onNpmRegistryInputChange,
                    enabled = !autoSelecting,
                    label = "npm 源",
                    placeholder = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
                )
                if (npmInputError != null && npmRegistryInput.isNotBlank()) {
                    Text(
                        text = npmInputError,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SettingsFeedbackActionButton(
                    text = "保存自定义源",
                    enabled = !autoSelecting && repoInputError == null && npmInputError == null && customTavernDirty,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    unavailableHint = probingHint ?: repoInputError ?: npmInputError ?: "当前地址与已保存配置一致。",
                    onShowHint = onShowHint,
                    onClick = onSaveTavernMirror,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsGroupLabel(
                    title = "Termux 包源",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (termuxRepoStatus.hasData) termuxRepoStatus.label else "未读取",
                    color = if (termuxRepoStatus.hasData) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前包源地址",
                    value = if (termuxRepoStatus.hasData) {
                        termuxRepoStatus.uri.ifBlank { termuxRepoStatus.label }
                    } else {
                        "尚未读取，点击下方按钮读取"
                    },
                    valueLayout = SettingsValueLayout.Supporting,
                    valueColor = if (termuxRepoStatus.hasData) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                )
                SettingsEntryDivider()
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "点选下方常用源会直接应用到 Termux 并刷新软件列表。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SettingsCompactChoiceButton(
                            text = "清华源",
                            enabled = !actionsLocked,
                            selected = isTermuxTuna,
                            accentColor = if (isTermuxTuna) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onClick = { onApplyTermuxMirrorUrl(TavernMirrorDefaults.TERMUX_TUNA_REPO) },
                        )
                        SettingsCompactChoiceButton(
                            text = "中科大源",
                            enabled = !actionsLocked,
                            selected = isTermuxUstc,
                            accentColor = if (isTermuxUstc) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onClick = { onApplyTermuxMirrorUrl(TavernMirrorDefaults.TERMUX_USTC_REPO) },
                        )
                        SettingsCompactChoiceButton(
                            text = "官方源",
                            enabled = !actionsLocked,
                            selected = isTermuxOfficial,
                            accentColor = if (isTermuxOfficial) LukoaColors.Primary else LukoaColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            unavailableHint = lockedHint,
                            onShowHint = onShowHint,
                            onClick = { onApplyTermuxMirrorUrl(TavernMirrorDefaults.TERMUX_OFFICIAL_REPO) },
                        )
                    }
                    SettingsFeedbackActionButton(
                        text = "重新读取 Termux 当前源",
                        enabled = !actionsLocked,
                        accentColor = LukoaColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        unavailableHint = lockedHint,
                        onShowHint = onShowHint,
                        onClick = onReadTermuxRepoStatus,
                    )
                }
            }
            LukoaCollapsibleCard(
                title = "自定义 Termux 包源",
                subtitle = "手动填写其他 apt 镜像地址并应用",
                expanded = showCustomTermux,
                onToggle = { showCustomTermux = !showCustomTermux },
            ) {
                val customError = TavernMirrorValidator.validateTermuxAptUrl(customTermuxRepoInput.trim())
                LukoaTextField(
                    value = customTermuxRepoInput,
                    onValueChange = onCustomTermuxRepoInputChange,
                    enabled = !actionsLocked,
                    label = "Termux 包源地址",
                    placeholder = TavernMirrorDefaults.TERMUX_TUNA_REPO,
                )
                if (customTermuxRepoInput.isNotBlank() && customError != null) {
                    Text(
                        text = customError,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                SettingsFeedbackActionButton(
                    text = "应用到 Termux",
                    enabled = !actionsLocked && customTermuxRepoInput.isNotBlank() && customError == null,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    unavailableHint = lockedHint ?: customError ?: "请先填写 Termux 包源地址。",
                    onShowHint = onShowHint,
                    onClick = onApplyCustomTermuxMirror,
                )
            }
        }
    }
}

@Composable
private fun MirrorStatusCard(
    tavernMirrorConfig: TavernMirrorConfig,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    autoSelecting: Boolean,
    onCheckTavernMirror: () -> Unit,
    onAutoSelectBestMirror: () -> Unit,
    onShowHint: (String) -> Unit,
) {
    val busyHint = if (mirrorProbeStatus.checking || autoSelecting) "正在测速检测，请稍等。" else null
    SettingsEntryGroup {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MirrorProbeDetailRow(
                label = "Git 源（${tavernMirrorConfig.repoLabel}）",
                status = mirrorProbeStatus.repoStatus,
            )
            MirrorProbeDetailRow(
                label = "版本列表",
                status = mirrorProbeStatus.versionStatus,
            )
            MirrorProbeDetailRow(
                label = "npm 源（${tavernMirrorConfig.npmLabel}）",
                status = mirrorProbeStatus.npmStatus,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Termux 包源",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = if (termuxRepoStatus.hasData) termuxRepoStatus.label else "未读取",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = mirrorProbeStatus.lastCheckedText,
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.labelSmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsFeedbackActionButton(
                    text = if (mirrorProbeStatus.checking && !autoSelecting) "检测中..." else "检测当前源",
                    enabled = !mirrorProbeStatus.checking && !autoSelecting,
                    accentColor = LukoaColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = busyHint,
                    onShowHint = onShowHint,
                    onClick = onCheckTavernMirror,
                )
                SettingsFeedbackActionButton(
                    text = if (autoSelecting) "优选测速中..." else "一键测速优选",
                    enabled = !mirrorProbeStatus.checking && !autoSelecting,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.weight(1f),
                    unavailableHint = busyHint,
                    onShowHint = onShowHint,
                    onClick = onAutoSelectBestMirror,
                )
            }
        }
    }
}

@Composable
private fun MirrorProbeDetailRow(
    label: String,
    status: MirrorProbeItemStatus,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status.level.label(),
                color = status.level.toneColor(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (status.message.isNotBlank() && status.message != "还没检测" && status.message != "未检测") {
            Text(
                text = status.message,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
