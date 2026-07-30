package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val TERMUX_TUNA_REPO = "https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main"
private const val TERMUX_OFFICIAL_REPO = "https://packages.termux.dev/apt/termux-main"

@Composable
fun MirrorSettingsSection(
    tavernMirrorConfig: TavernMirrorConfig,
    tavernRepoInput: String,
    npmRegistryInput: String,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    customTermuxRepoInput: String,
    actionsLocked: Boolean,
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
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
            onTavernRepoInputChange = onTavernRepoInputChange,
            onNpmRegistryInputChange = onNpmRegistryInputChange,
            onCustomTermuxRepoInputChange = onCustomTermuxRepoInputChange,
            onSaveTavernMirror = onSaveTavernMirror,
            onUseOfficialMirror = onUseOfficialMirror,
            onUseGithubProxyMirror = onUseGithubProxyMirror,
            onUseNpmMirror = onUseNpmMirror,
            onCheckTavernMirror = onCheckTavernMirror,
            onReadTermuxRepoStatus = onReadTermuxRepoStatus,
            onApplyCustomTermuxMirror = onApplyCustomTermuxMirror,
            onDismiss = { showMirrorDialog = false },
        )
    }

    SectionPanel(title = "网络与镜像源", accentColor = LukoaColors.Primary) {
        MirrorSummaryCard(
            tavernMirrorConfig = tavernMirrorConfig,
            mirrorProbeStatus = mirrorProbeStatus,
            termuxRepoStatus = termuxRepoStatus,
            actionsLocked = actionsLocked,
            onCheckTavernMirror = onCheckTavernMirror,
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
    onTavernRepoInputChange: (String) -> Unit,
    onNpmRegistryInputChange: (String) -> Unit,
    onCustomTermuxRepoInputChange: (String) -> Unit,
    onSaveTavernMirror: () -> Unit,
    onUseOfficialMirror: () -> Unit,
    onUseGithubProxyMirror: () -> Unit,
    onUseNpmMirror: () -> Unit,
    onCheckTavernMirror: () -> Unit,
    onReadTermuxRepoStatus: () -> Unit,
    onApplyCustomTermuxMirror: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "切换下载源",
                infoText = "下载源决定安装或更新时从哪里下载文件。能正常下载时不需要修改。\n酒馆程序、程序依赖和 Termux 工具使用不同地址，可以分别设置。下载很慢或反复失败时，再尝试“国内推荐”或其他可用地址。\n切换下载源不会删除已经安装的酒馆和聊天数据。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MirrorSummaryCard(
                    tavernMirrorConfig = tavernMirrorConfig,
                    mirrorProbeStatus = mirrorProbeStatus,
                    termuxRepoStatus = termuxRepoStatus,
                    actionsLocked = actionsLocked,
                    onCheckTavernMirror = onCheckTavernMirror,
                )
                MirrorSubsection(
                    title = "酒馆下载源",
                    description = "这里设置酒馆程序和运行所需文件的下载地址。\n如果现在能正常安装和更新，请保持不变。下载慢或失败时，国内网络可以先试“国内推荐”；需要恢复时选“官方源”。",
                ) {
                    MirrorTextField(
                        value = tavernRepoInput,
                        onValueChange = onTavernRepoInputChange,
                        enabled = !actionsLocked,
                        label = "酒馆 Git 源",
                        placeholder = TavernMirrorDefaults.OFFICIAL_REPO,
                    )
                    MirrorTextField(
                        value = npmRegistryInput,
                        onValueChange = onNpmRegistryInputChange,
                        enabled = !actionsLocked,
                        label = "npm 源",
                        placeholder = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
                    )

                    MirrorButtonRow {
                        SecondaryActionButton(
                            text = "国内推荐",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onUseGithubProxyMirror,
                        )
                        SecondaryActionButton(
                            text = "官方源",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onUseOfficialMirror,
                        )
                    }
                    MirrorButtonRow {
                        SecondaryActionButton(
                            text = "只换 npm",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onUseNpmMirror,
                        )
                        SecondaryActionButton(
                            text = "保存自定义",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onSaveTavernMirror,
                        )
                    }
                }

                MirrorSubsection(
                    title = "Termux 包源",
                    description = "这里设置 Termux 下载 Node.js、Git 等工具时使用的地址。酒馆能正常安装时不需要修改。\n按钮“填入清华源”或“填入官方源”只会先填写地址，最后还要点“应用到 Termux”才会真正切换。",
                ) {
                    MirrorInfoLine(
                        label = "当前",
                        value = if (termuxRepoStatus.hasData) {
                            "${termuxRepoStatus.label} · ${termuxRepoStatus.uri}"
                        } else {
                            "未读取，点下面按钮读取"
                        },
                    )
                    MirrorButtonRow {
                        SecondaryActionButton(
                            text = "读取当前源",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onReadTermuxRepoStatus,
                        )
                        SecondaryActionButton(
                            text = "填入清华源",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onCustomTermuxRepoInputChange(TERMUX_TUNA_REPO) },
                        )
                    }
                    MirrorButtonRow {
                        SecondaryActionButton(
                            text = "填入官方源",
                            enabled = !actionsLocked,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = { onCustomTermuxRepoInputChange(TERMUX_OFFICIAL_REPO) },
                        )
                        val customError = TavernMirrorValidator.validateTermuxAptUrl(customTermuxRepoInput.trim())
                        SecondaryActionButton(
                            text = "应用到 Termux",
                            enabled = !actionsLocked && customTermuxRepoInput.isNotBlank() && customError == null,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.weight(1f),
                            onClick = onApplyCustomTermuxMirror,
                        )
                    }
                    MirrorTextField(
                        value = customTermuxRepoInput,
                        onValueChange = onCustomTermuxRepoInputChange,
                        enabled = !actionsLocked,
                        label = "自定义 Termux 包源",
                        placeholder = TERMUX_TUNA_REPO,
                    )
                    val validationMessage = TavernMirrorValidator.validateTermuxAptUrl(customTermuxRepoInput.trim())
                    if (customTermuxRepoInput.isNotBlank() && validationMessage != null) {
                        Text(
                            text = validationMessage,
                            color = LukoaColors.Danger,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "点“应用到 Termux”后才会真正切换下载地址并更新软件列表。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
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
private fun MirrorSummaryCard(
    tavernMirrorConfig: TavernMirrorConfig,
    mirrorProbeStatus: TavernMirrorProbeStatus,
    termuxRepoStatus: TermuxRepoStatus,
    actionsLocked: Boolean,
    onCheckTavernMirror: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "当前方案",
                color = LukoaColors.Primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            MirrorInfoLine("酒馆源", tavernMirrorConfig.repoLabel)
            MirrorInfoLine("npm 源", tavernMirrorConfig.npmLabel)
            MirrorInfoLine("Termux 源", if (termuxRepoStatus.hasData) termuxRepoStatus.label else "未读取")
            Text(
                text = "App 检测",
                color = LukoaColors.Primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            MirrorProbeLine("Git 源", mirrorProbeStatus.repoStatus)
            MirrorProbeLine("版本列表", mirrorProbeStatus.versionStatus)
            MirrorProbeLine("npm 源", mirrorProbeStatus.npmStatus)
            Text(
                text = mirrorProbeStatus.lastCheckedText,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            SecondaryActionButton(
                text = if (mirrorProbeStatus.checking) "检测中" else "立即检测",
                enabled = !actionsLocked && !mirrorProbeStatus.checking,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCheckTavernMirror,
            )
        }
    }
}

@Composable
private fun MirrorProbeLine(
    label: String,
    status: MirrorProbeItemStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.26f),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        StatusPill(
            text = status.level.label(),
            active = status.level != MirrorProbeLevel.Unknown,
            modifier = Modifier.weight(0.22f),
            toneColor = status.level.toneColor(),
            activeBackground = status.level.backgroundColor(),
        )
        Text(
            text = status.message,
            modifier = Modifier.weight(0.52f),
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MirrorSubsection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                InfoPopoverButton(
                    contentDescription = "查看$title 说明",
                    title = title,
                    body = description,
                )
            }
            content()
        }
    }
}

@Composable
private fun MirrorInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.34f),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.66f),
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MirrorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
}

@Composable
private fun MirrorButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
