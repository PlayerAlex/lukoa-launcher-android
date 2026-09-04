package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun InstallRiskConfirmDialog(
    confirmation: TavernInstallConfirmation,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LauncherActionDialogScaffold(
        title = confirmation.title,
        titleTone = ActionTone.Safe,
        confirmText = "继续安装",
        confirmTone = ActionTone.Safe,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = confirmation.summary,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (confirmation.details.isNotEmpty()) {
            LauncherDialogSection(title = "安装前确认") {
                LauncherDialogBulletList(confirmation.details)
            }
        }
    }
}
@Composable
fun StartPreflightConfirmDialog(
    result: TavernStartPreflightResult,
    activeProfile: TavernProfile? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val actionTone = when (result.action?.type) {
        TavernStartPreflightActionType.ForceCleanupDetectedProcess -> ActionTone.Danger
        else -> ActionTone.Safe
    }
    LauncherActionDialogScaffold(
        title = result.title.ifBlank { "启动前发现问题" },
        titleTone = ActionTone.Warning,
        confirmText = result.action?.label,
        confirmTone = actionTone,
        dismissText = if (result.action == null) "知道了" else "稍后",
        onConfirm = if (result.action != null) onConfirm else null,
        onDismiss = onDismiss,
    ) {
        activeProfile?.let { profile ->
            StartPreflightProfileInfoCard(profile = profile)
        }
        Text(
            text = result.summary,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (result.details.isNotEmpty()) {
            LauncherDialogSection(title = "需要注意") {
                LauncherDialogBulletList(result.details)
            }
        }
    }
}

@Composable
private fun StartPreflightProfileInfoCard(profile: TavernProfile) {
    LauncherDialogSection(title = "当前实例") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VersionInfoLine("实例名称", profile.normalizedName)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("实例目录", color = LukoaColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = profile.displayTavernDir,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            VersionInfoLine("访问端口", profile.normalizedPort.toString())
        }
    }
}

@Composable
fun StopTavernConfirmDialog(
    profile: TavernProfile,
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = "停止酒馆",
        titleTone = ActionTone.Safe,
        confirmText = "停止",
        confirmTone = ActionTone.Safe,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "将停止「${profile.normalizedName}」（端口 ${profile.normalizedPort}），网页会立刻断开。",
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "聊天记录、角色和备份都不受影响，随时可以再启动。",
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun ForceCleanupTavernConfirmDialog(
    confirmation: TavernForceCleanupConfirmation,
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = confirmation.suggestion.kind.dialogTitle,
        titleTone = ActionTone.Danger,
        confirmText = "确认强制清理",
        confirmTone = ActionTone.Danger,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = confirmation.suggestion.summary,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        TavernActionProfileCard(
            profileName = confirmation.profileName,
            profilePath = confirmation.profilePath,
            profilePort = confirmation.profilePort,
        )
        LauncherDialogSection(title = "建议原因") {
            Text(
                text = confirmation.suggestion.reasonDetail,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LauncherDialogSection(title = "风险说明") {
            Text(
                text = confirmation.suggestion.riskTip,
                color = LukoaColors.Danger,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TavernActionProfileCard(
    profileName: String,
    profilePath: String,
    profilePort: Int,
) {
    LauncherDialogSection(title = "操作对象") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VersionInfoLine("实例名称", profileName)
            VersionInfoLine("实例目录", profilePath)
            VersionInfoLine("访问端口", profilePort.toString())
        }
    }
}
