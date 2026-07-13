package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
) {
    val permissionReady = termuxInstalled && runCommandPermissionGranted && !externalAppsBlocked
    val selectedInstallVersion = selectedVersion ?: TavernInstallDefaults.releaseChoice(mirrorRepoUrl)
    val currentStep = when {
        !termuxInstalled -> 1
        !permissionReady -> 2
        termuxSetupRecommended -> 3
        tavernInstallDetected != true -> 4
        else -> 5
    }
    val action = when {
        !termuxInstalled -> GuideAction(
            primaryText = "下载 Termux",
            primary = onOpenTermuxDownload,
            secondary = listOf(
                GuideSecondary("备用下载", onOpenTermuxGithub),
                GuideSecondary("重新检测", onRecheckTermux),
            ),
        )
        !runCommandPermissionGranted -> GuideAction(
            primaryText = "请求权限",
            primary = onRequestPermission,
            secondary = listOf(
                GuideSecondary("权限设置", onOpenPermissionSettings),
                GuideSecondary("重新检测", onRecheckPermission),
            ),
        )
        externalAppsBlocked -> GuideAction(
            primaryText = "复制权限命令",
            primary = onCopyPermissionCommand,
            secondary = listOf(
                GuideSecondary("打开 Termux", onOpenTermux),
                GuideSecondary("重新检测", onRecheckPermission),
            ),
            note = commandText,
        )
        termuxSetupRecommended -> GuideAction(
            primaryText = "准备 Termux 环境",
            primary = onPrepareTermux,
            secondary = listOf(
                GuideSecondary("重新检测酒馆", onCheckTavern, enabled = !tavernVersionChecking),
                GuideSecondary("直接安装酒馆", onShowInstall),
            ),
        )
        tavernInstallDetected == null -> GuideAction(
            primaryText = if (tavernVersionChecking) "检测中…" else "重新检测酒馆",
            primary = onCheckTavern,
            primaryEnabled = !tavernVersionChecking,
            secondary = listOf(
                GuideSecondary("直接安装酒馆", onShowInstall),
                GuideSecondary("准备环境", onPrepareTermux),
            ),
        )
        tavernInstallDetected == false -> GuideAction(
            primaryText = "安装 ${selectedInstallVersion.label}",
            primary = onInstallTavern,
            secondary = listOf(
                GuideSecondary(
                    text = if (officialVersions.hasData) "使用默认稳定版" else "读取版本列表",
                    onClick = if (officialVersions.hasData) onUseRecommendedVersion else onRefreshOfficialVersions,
                ),
                GuideSecondary("重新检测", onCheckTavern, enabled = !tavernVersionChecking),
            ),
            note = "第一次安装通常需要 5–10 分钟。Termux 仍在输出内容时请继续等待，不要重复点击。",
        )
        else -> GuideAction(
            primaryText = "启动酒馆",
            primary = {},
            primaryEnabled = false,
            secondary = emptyList(),
        )
    }

    DashedSection(
        label = "新手引导 · $currentStep / 5",
        headerAction = {
            HelpHint("按顺序完成五步即可。绿色圆点表示已完成，当前步骤会显示可执行按钮。")
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GuideStep(
                number = 1,
                title = "安装并打开 Termux",
                detail = if (termuxInstalled) "已检测到" else "先安装并打开一次",
                state = guideStepState(1, currentStep),
            )
            GuideStep(
                number = 2,
                title = "允许启动器调用 Termux",
                detail = when {
                    permissionReady -> "RUN_COMMAND 已授权"
                    externalAppsBlocked -> "还需要打开 Termux 外部调用"
                    else -> "等待系统授权"
                },
                state = guideStepState(2, currentStep),
            )
            GuideStep(
                number = 3,
                title = "准备 Termux 环境",
                detail = "安装 git / node / npm",
                state = guideStepState(3, currentStep),
            )
            GuideStep(
                number = 4,
                title = "检测 / 安装酒馆",
                detail = when (tavernInstallDetected) {
                    true -> "已检测到酒馆"
                    false -> "当前实例尚未安装"
                    null -> "等待检测"
                },
                state = guideStepState(4, currentStep),
            )
            GuideStep(
                number = 5,
                title = "启动酒馆",
                detail = if (currentStep >= 5) "基础准备已完成" else "完成前四步后可用",
                state = guideStepState(5, currentStep),
            )
        }

        PrimaryActionButton(
            text = action.primaryText,
            enabled = !actionsLocked && action.primaryEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            onClick = action.primary,
        )

        if (action.secondary.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                action.secondary.take(2).forEach { secondary ->
                    SecondaryActionButton(
                        text = secondary.text,
                        enabled = !actionsLocked && secondary.enabled,
                        accentColor = LukoaColors.Text,
                        modifier = Modifier.weight(1f),
                        onClick = secondary.onClick,
                    )
                }
            }
        }

        if (action.note.isNotBlank()) {
            StateNote(action.note)
        }
    }

}

private enum class GuideStepState {
    Done,
    Current,
    Todo,
}

private fun guideStepState(number: Int, current: Int): GuideStepState = when {
    number < current -> GuideStepState.Done
    number == current -> GuideStepState.Current
    else -> GuideStepState.Todo
}

@Composable
private fun GuideStep(
    number: Int,
    title: String,
    detail: String,
    state: GuideStepState,
) {
    val circleColor = when (state) {
        GuideStepState.Done -> LukoaColors.Accent
        GuideStepState.Current -> LukoaColors.AccentSoft
        GuideStepState.Todo -> Color.White.copy(alpha = 0.04f)
    }
    val numberColor = when (state) {
        GuideStepState.Done -> LukoaColors.AccentDark
        GuideStepState.Current -> LukoaColors.Accent
        GuideStepState.Todo -> LukoaColors.Dim
    }
    val textColor = if (state == GuideStepState.Todo) LukoaColors.Dim else LukoaColors.Text
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = circleColor,
            shape = RoundedCornerShape(12.dp),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (state == GuideStepState.Done) "✓" else number.toString(),
                    color = numberColor,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = if (state == GuideStepState.Todo) LukoaColors.Dim else LukoaColors.Dim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

private data class GuideAction(
    val primaryText: String,
    val primary: () -> Unit,
    val primaryEnabled: Boolean = true,
    val secondary: List<GuideSecondary>,
    val note: String = "",
)

private data class GuideSecondary(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)
