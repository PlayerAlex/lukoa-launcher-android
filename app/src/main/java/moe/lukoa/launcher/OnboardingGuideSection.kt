package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun QuickStartGuideSection(
    termuxInstalled: Boolean,
    runCommandPermissionGranted: Boolean,
    externalAppsBlocked: Boolean,
    tavernInstallDetected: Boolean?,
    tavernVersionChecking: Boolean,
    termuxSetupRecommended: Boolean,
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    commandText: String,
    actionsLocked: Boolean,
    onOpenTermuxDownload: () -> Unit,
    onOpenTermuxGithub: () -> Unit,
    onRecheckTermux: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onCopyPermissionCommand: () -> Unit,
    onOpenTermux: () -> Unit,
    onRecheckPermission: () -> Unit,
    onPrepareTermux: () -> Unit,
    onCheckTavern: () -> Unit,
    onShowInstall: () -> Unit,
    onRefreshOfficialVersions: () -> Unit,
    onSelectVersion: (TavernVersionChoice) -> Unit,
    onUseRecommendedVersion: () -> Unit,
    onInstallTavern: () -> Unit,
) {
    val permissionReady = termuxInstalled && runCommandPermissionGranted && !externalAppsBlocked
    val stepIndex = when {
        !termuxInstalled -> 1
        !permissionReady -> 2
        termuxSetupRecommended -> 3
        tavernInstallDetected != true -> 4
        else -> 5
    }
    val current = when {
        !termuxInstalled -> WizardAction(
            step = "第 1 步",
            title = "先把 Termux 装好",
            detail = "点下载，装好后打开 Termux 一次，再回来检测。",
            primaryText = "下载 Termux",
            primaryEnabled = !actionsLocked,
            primary = onOpenTermuxDownload,
            secondary = listOf(
                WizardSecondaryAction("备用下载", !actionsLocked, onOpenTermuxGithub),
                WizardSecondaryAction("我装好了，重新检测", !actionsLocked, onRecheckTermux),
            ),
            tone = LukoaColors.Accent,
        )
        termuxInstalled && !runCommandPermissionGranted -> WizardAction(
            step = "第 2 步",
            title = "允许启动器调用 Termux",
            detail = "先点请求权限。没弹窗就点权限设置。",
            primaryText = "请求系统权限",
            primaryEnabled = !actionsLocked,
            primary = onRequestPermission,
            secondary = listOf(
                WizardSecondaryAction("权限设置", !actionsLocked, onOpenPermissionSettings),
                WizardSecondaryAction("重新检测", !actionsLocked, onRecheckPermission),
            ),
            tone = LukoaColors.Accent,
        )
        externalAppsBlocked -> WizardAction(
            step = "第 2 步",
            title = "打开 Termux 外部调用",
            detail = "复制命令，打开 Termux 粘贴执行一次。",
            primaryText = "复制权限命令",
            primaryEnabled = !actionsLocked,
            primary = onCopyPermissionCommand,
            secondary = listOf(
                WizardSecondaryAction("打开 Termux", !actionsLocked, onOpenTermux),
                WizardSecondaryAction("重新检测", !actionsLocked, onRecheckPermission),
            ),
            tone = LukoaColors.Accent,
            commandText = commandText,
        )
        termuxSetupRecommended -> WizardAction(
            step = "第 3 步",
            title = "准备 Termux 环境",
            detail = "Termux 上次安装可能没收尾。点这里自动修一下。",
            primaryText = "准备 Termux 环境",
            primaryEnabled = !actionsLocked,
            primary = onPrepareTermux,
            secondary = listOf(
                WizardSecondaryAction("重新检测酒馆", !actionsLocked && !tavernVersionChecking, onCheckTavern),
                WizardSecondaryAction("直接安装酒馆", !actionsLocked, onShowInstall),
            ),
            tone = LukoaColors.Accent,
        )
        tavernInstallDetected == null -> WizardAction(
            step = "第 4 步",
            title = "看看手机里有没有酒馆",
            detail = "老用户点检测；第一次用就直接进入安装。",
            primaryText = if (tavernVersionChecking) "检测中..." else "检测本机酒馆",
            primaryEnabled = !actionsLocked && !tavernVersionChecking,
            primary = onCheckTavern,
            secondary = listOf(
                WizardSecondaryAction("第一次用，安装酒馆", !actionsLocked, onShowInstall),
                WizardSecondaryAction("准备环境", !actionsLocked, onPrepareTermux),
            ),
            tone = LukoaColors.Accent,
        )
        tavernInstallDetected == false -> WizardAction(
            step = "第 4 步",
            title = "安装酒馆",
            detail = "默认安装 release 分支。会自动补 git、nodejs，然后执行 npm install。",
            primaryText = "安装 ${selectedVersion?.label ?: TavernInstallDefaults.Release.label}",
            primaryEnabled = !actionsLocked,
            primary = onInstallTavern,
            secondary = listOf(
                WizardSecondaryAction("换个版本", !actionsLocked, onRefreshOfficialVersions),
                WizardSecondaryAction("重新检测", !actionsLocked && !tavernVersionChecking, onCheckTavern),
            ),
            tone = LukoaColors.Accent,
        )
        else -> WizardAction(
            step = "完成",
            title = "可以启动酒馆",
            detail = "基础准备已经完成。",
            primaryText = "完成",
            primaryEnabled = false,
            primary = {},
            secondary = emptyList(),
            tone = LukoaColors.Accent,
        )
    }

    SectionPanel(title = "露科亚安装向导", accentColor = LukoaColors.Accent) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.AccentSoft.copy(alpha = 0.72f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, current.tone.copy(alpha = 0.58f)),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepStamp(text = current.step, color = current.tone)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = current.title,
                            color = LukoaColors.Text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = current.detail,
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                SecondaryActionButton(
                    text = current.primaryText,
                    enabled = current.primaryEnabled,
                    accentColor = current.tone,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = current.primary,
                )

                if (tavernInstallDetected == false) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "release 是当前的最新稳定版。",
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "安装通常会持续 5-10 分钟，这是正常的，等待即可。",
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (current.secondary.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        current.secondary.take(2).forEach { action ->
                            SecondaryActionButton(
                                text = action.text,
                                enabled = action.enabled,
                                accentColor = LukoaColors.Accent,
                                modifier = Modifier.weight(1f),
                                onClick = action.onClick,
                            )
                        }
                    }
                }

                if (current.commandText.isNotBlank()) {
                    CommandSnippet(text = current.commandText)
                }

                if (tavernInstallDetected == false && !termuxSetupRecommended && officialVersions.hasData) {
                    WizardVersionPicker(
                        officialVersions = officialVersions,
                        selectedVersion = selectedVersion,
                        actionsLocked = actionsLocked,
                        onSelectVersion = onSelectVersion,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WizardStepChip("1", "Termux", stepIndex > 1, stepIndex == 1, Modifier.weight(1f))
            WizardStepChip("2", "权限", stepIndex > 2, stepIndex == 2, Modifier.weight(1f))
            WizardStepChip("3", "环境", stepIndex > 3, stepIndex == 3, Modifier.weight(1f))
            WizardStepChip("4", "酒馆", stepIndex > 4, stepIndex == 4, Modifier.weight(1f))
        }

        Text(
            text = if (actionsLocked) "正在执行上一步，等按钮恢复再继续。" else "按绿色按钮继续；不确定就不要点别的。",
            color = if (actionsLocked) LukoaColors.Amber else LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (actionsLocked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private data class WizardAction(
    val step: String,
    val title: String,
    val detail: String,
    val primaryText: String,
    val primaryEnabled: Boolean,
    val primary: () -> Unit,
    val secondary: List<WizardSecondaryAction>,
    val tone: Color,
    val commandText: String = "",
)

private data class WizardSecondaryAction(
    val text: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun StepStamp(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = LukoaCapsuleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun WizardStepChip(
    number: String,
    label: String,
    done: Boolean,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val tone = when {
        done || current -> LukoaColors.Accent
        else -> LukoaColors.Muted
    }
    val background = when {
        done -> LukoaColors.AccentSoft
        current -> LukoaColors.SurfaceAlt
        else -> LukoaColors.Surface
    }
    Surface(
        modifier = modifier.heightIn(min = 46.dp),
        color = background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (done || current) tone.copy(alpha = 0.5f) else LukoaColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = if (done) "✓" else number,
                color = tone,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = label,
                color = if (done || current) LukoaColors.Text else LukoaColors.Muted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommandSnippet(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Terminal,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .background(LukoaColors.Accent, RoundedCornerShape(2.dp)),
                )
                Text(
                    text = "复制后粘贴到 Termux，回车执行",
                    modifier = Modifier.padding(start = 8.dp),
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = text,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WizardVersionPicker(
    officialVersions: TavernOfficialVersions,
    selectedVersion: TavernVersionChoice?,
    actionsLocked: Boolean,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val choices = officialVersions.all

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "安装版本",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = selectedVersion?.label ?: TavernInstallDefaults.Release.label,
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        enabled = !actionsLocked && choices.isNotEmpty(),
                        modifier = Modifier.height(40.dp),
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
                            text = "更换",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = LukoaColors.Surface,
                    ) {
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
                    }
                }
            }
            Text(
                text = "不懂就用默认稳定版；测试版可能不稳定。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "当前源：${repoLabelFor(selectedVersion?.repoUrl.orEmpty().ifBlank { TavernMirrorDefaults.OFFICIAL_REPO })}",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
