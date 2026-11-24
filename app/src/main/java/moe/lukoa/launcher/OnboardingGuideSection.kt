package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    mirrorRepoUrl: String,
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
    showSectionContainer: Boolean = true,
    onStartSetupSpotlight: (() -> Unit)? = null,
    onReturnToLaunch: (() -> Unit)? = null,
) {
    val permissionReady = termuxInstalled && runCommandPermissionGranted && !externalAppsBlocked
    val effectiveSelectedVersion = selectedVersion ?: TavernInstallDefaults.releaseChoice(mirrorRepoUrl)
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
            title = "安装并打开一次 Termux",
            detail = "先安装 Termux。安装完成后必须打开一次，让它做好初始准备，再回来重新检测。",
            primaryText = "下载 Termux",
            primaryEnabled = !actionsLocked,
            primary = onOpenTermuxDownload,
            secondary = listOf(
                WizardSecondaryAction("备用下载", !actionsLocked, onOpenTermuxGithub),
                WizardSecondaryAction("我装好了，重新检测", !actionsLocked, onRecheckTermux),
            ),
            tone = LukoaColors.Primary,
        )
        termuxInstalled && !runCommandPermissionGranted -> WizardAction(
            step = "第 2 步",
            title = "允许启动器发送任务",
            detail = "先请求系统权限；如果手机没有弹出授权窗口，再打开权限设置手动允许。",
            primaryText = "请求系统权限",
            primaryEnabled = !actionsLocked,
            primary = onRequestPermission,
            secondary = listOf(
                WizardSecondaryAction("权限设置", !actionsLocked, onOpenPermissionSettings),
                WizardSecondaryAction("重新检测", !actionsLocked, onRecheckPermission),
            ),
            tone = LukoaColors.Primary,
        )
        externalAppsBlocked -> WizardAction(
            step = "第 2 步",
            title = "打开 Termux 外部调用",
            detail = "复制下面的命令，打开 Termux 粘贴并回车。完成后回到这里重新检测。",
            primaryText = "复制权限命令",
            primaryEnabled = !actionsLocked,
            primary = onCopyPermissionCommand,
            secondary = listOf(
                WizardSecondaryAction("打开 Termux", !actionsLocked, onOpenTermux),
                WizardSecondaryAction("重新检测", !actionsLocked, onRecheckPermission),
            ),
            tone = LukoaColors.Primary,
            commandText = commandText,
        )
        termuxSetupRecommended -> WizardAction(
            step = "第 3 步",
            title = "准备 Termux 环境",
            detail = "启动器会自动安装酒馆需要的基础工具。第一次准备可能需要几分钟。",
            primaryText = "准备 Termux 环境",
            primaryEnabled = !actionsLocked,
            primary = onPrepareTermux,
            secondary = listOf(
                WizardSecondaryAction("重新检测酒馆", !actionsLocked && !tavernVersionChecking, onCheckTavern),
                WizardSecondaryAction("直接安装酒馆", !actionsLocked, onShowInstall),
            ),
            tone = LukoaColors.Primary,
        )
        tavernInstallDetected == null -> WizardAction(
            step = "第 4 步",
            title = "确认手机里有没有酒馆",
            detail = "以前安装过酒馆就先检测；第一次使用可以直接进入安装。",
            primaryText = if (tavernVersionChecking) "检测中..." else "检测本机酒馆",
            primaryEnabled = !actionsLocked && !tavernVersionChecking,
            primary = onCheckTavern,
            secondary = listOf(
                WizardSecondaryAction("第一次用，安装酒馆", !actionsLocked, onShowInstall),
                WizardSecondaryAction("准备环境", !actionsLocked, onPrepareTermux),
            ),
            tone = LukoaColors.Primary,
        )
        tavernInstallDetected == false -> WizardAction(
            step = "第 4 步",
            title = "安装酒馆",
            detail = "默认稳定版适合大多数人。安装通常需要 5–10 分钟，完成后就能启动酒馆。",
            primaryText = "安装 ${effectiveSelectedVersion.label}",
            primaryEnabled = !actionsLocked,
            primary = onInstallTavern,
            secondary = listOf(
                WizardSecondaryAction(
                    if (officialVersions.hasData) "恢复默认稳定版" else "读取可选版本",
                    !actionsLocked,
                    if (officialVersions.hasData) onUseRecommendedVersion else onRefreshOfficialVersions,
                ),
                WizardSecondaryAction("重新检测", !actionsLocked && !tavernVersionChecking, onCheckTavern),
            ),
            tone = LukoaColors.Primary,
        )
        else -> WizardAction(
            step = "已完成",
            title = "可以启动酒馆了",
            detail = "Termux、权限和酒馆环境都已就绪，现在可以回到启动页一键启动 SillyTavern。",
            primaryText = if (onReturnToLaunch != null) "回到启动页" else "完成",
            primaryEnabled = onReturnToLaunch != null,
            primary = onReturnToLaunch ?: {},
            secondary = emptyList(),
            tone = LukoaColors.Primary,
        )
    }

    val completedSteps = (stepIndex - 1).coerceIn(0, 4)

    val currentStepCard: @Composable () -> Unit = {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup-wizard-active-card"),
            color = if (showSectionContainer) LukoaColors.Elevated else LukoaColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = LukoaColors.PrimarySoft,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.36f)),
                    ) {
                        Text(
                            text = current.step,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = LukoaColors.Primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "现在只做这一项",
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = current.title,
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = current.detail,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp,
                )

                if (current.commandText.isNotBlank()) {
                    CommandSnippet(text = current.commandText)
                }

                if (tavernInstallDetected == false && !termuxSetupRecommended && officialVersions.hasData) {
                    WizardVersionPicker(
                        officialVersions = officialVersions,
                        selectedVersion = selectedVersion,
                        mirrorRepoUrl = mirrorRepoUrl,
                        actionsLocked = actionsLocked,
                        onSelectVersion = onSelectVersion,
                    )
                }

                val primaryClick = rememberFeedbackClick(current.primary)
                Button(
                    onClick = primaryClick,
                    enabled = current.primaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .spotlightTarget(SpotlightTargetId.SetupPrimaryAction)
                        .testTag("setup-wizard-primary-action"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = current.tone,
                        contentColor = LukoaColors.OnPrimary,
                        disabledContainerColor = LukoaColors.Elevated,
                        disabledContentColor = LukoaColors.Dim,
                    ),
                ) {
                    Text(
                        text = current.primaryText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (current.secondary.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .spotlightTarget(SpotlightTargetId.SetupSecondaryActions)
                            .testTag("setup-wizard-secondary-actions"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        current.secondary.take(2).forEach { action ->
                            SecondaryActionButton(
                                text = action.text,
                                enabled = action.enabled,
                                accentColor = LukoaColors.TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = action.onClick,
                            )
                        }
                    }
                }

                if (actionsLocked) {
                    Text(
                        text = "这一步正在执行，完成后按钮会自动恢复，不需要重复点击。",
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    if (showSectionContainer) {
        SectionPanel(
            title = "第一次使用",
            accentColor = LukoaColors.Primary,
            headerAction = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = "已完成 $completedSteps/4",
                        active = completedSteps > 0,
                        toneColor = LukoaColors.Primary,
                        activeBackground = LukoaColors.PrimarySoft,
                    )
                    InfoPopoverButton(
                        contentDescription = "查看第一次使用说明",
                        title = "第一次使用",
                        body = "这里每次只推荐下一步，按主按钮继续即可；下方按钮用于重新检测或打开备用入口。\n酒馆会安装到设置里的当前实例路径，默认是 ~/SillyTavern。安装通常需要 5–10 分钟，期间保持 Termux 可以在后台运行。\n不知道版本怎么选时使用默认稳定版。完成安装后，回到启动页点击“启动酒馆”。",
                    )
                }
            },
        ) {
            currentStepCard()
            Text(
                text = "准备进度",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            OnboardingProgressList(currentStep = stepIndex)
            Text(
                text = "完成这 4 步后，回到启动页点「启动酒馆」即可开始使用。",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsGroupLabel(
                    title = "首次配置进度 · 已完成 $completedSteps/4",
                    modifier = Modifier.weight(1f),
                )
                if (onStartSetupSpotlight != null) {
                    Text(
                        text = "聚光灯指引",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                role = Role.Button,
                                onClick = rememberFeedbackClick(onStartSetupSpotlight),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("setup-wizard-spotlight-button"),
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            SetupHorizontalStepper(
                currentStep = stepIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .spotlightTarget(SpotlightTargetId.SetupStepProgress)
                    .testTag("setup-wizard-stepper"),
            )

            currentStepCard()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsGroupLabel("全部 4 步明细")
                OnboardingProgressList(currentStep = stepIndex)
            }
        }
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
private fun SetupHorizontalStepper(
    currentStep: Int,
    modifier: Modifier = Modifier,
) {
    val shortLabels = listOf("Termux", "权限", "环境", "酒馆")
    Surface(
        modifier = modifier,
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shortLabels.forEachIndexed { index, label ->
                val number = index + 1
                val done = number < currentStep
                val active = number == currentStep
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> LukoaColors.Primary
                                    active -> LukoaColors.PrimarySoft
                                    else -> LukoaColors.Elevated
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (done) "✓" else number.toString(),
                            color = when {
                                done -> LukoaColors.OnPrimary
                                active -> LukoaColors.Primary
                                else -> LukoaColors.TextSecondary
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = label,
                        color = if (done || active) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index != shortLabels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .padding(bottom = 0.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (number < currentStep) LukoaColors.Primary else LukoaColors.Border,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressList(currentStep: Int) {
    val steps = listOf(
        "安装 Termux",
        "连接 Termux",
        "准备运行环境",
        "确认并安装酒馆",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, label ->
                val number = index + 1
                val done = number < currentStep
                val current = number == currentStep
                OnboardingProgressRow(
                    number = number,
                    label = label,
                    done = done,
                    current = current,
                )
                if (index != steps.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = LukoaColors.Border,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgressRow(
    number: Int,
    label: String,
    done: Boolean,
    current: Boolean,
) {
    val tone = if (done || current) LukoaColors.Primary else LukoaColors.TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> LukoaColors.Primary
                        current -> LukoaColors.PrimarySoft
                        else -> LukoaColors.Elevated
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (done) "✓" else number.toString(),
                color = if (done) LukoaColors.OnPrimary else tone,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (done || current) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
        )
        Text(
            text = when {
                done -> "已完成"
                current -> "当前"
                else -> "稍后"
            },
            color = tone,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (done || current) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CommandSnippet(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Terminal,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(14.dp)
                        .background(LukoaColors.Primary, RoundedCornerShape(2.dp)),
                )
                Text(
                    text = "复制后粘贴到 Termux，回车执行",
                    modifier = Modifier.padding(start = 8.dp),
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = text,
                color = LukoaColors.TextPrimary,
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
    mirrorRepoUrl: String,
    actionsLocked: Boolean,
    onSelectVersion: (TavernVersionChoice) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val choices = officialVersions.all
    val effectiveSelectedVersion = selectedVersion ?: TavernInstallDefaults.releaseChoice(mirrorRepoUrl)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = effectiveSelectedVersion.label,
                        color = LukoaColors.TextPrimary,
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
                        modifier = Modifier.height(38.dp),
                        border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.46f)),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = LukoaColors.Primary,
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
                        containerColor = LukoaColors.Elevated,
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
                text = "不懂就用默认稳定版；当前源：${repoLabelFor(effectiveSelectedVersion.repoUrl)}",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
