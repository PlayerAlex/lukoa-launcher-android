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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text(confirmation.title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = confirmation.summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        confirmation.details.forEach { item ->
                            Text(
                                text = "• $item",
                                color = LukoaColors.Text,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "继续安装",
                tone = ActionTone.Safe,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}
@Composable
fun StartPreflightConfirmDialog(
    result: TavernStartPreflightResult,
    activeProfile: TavernProfile? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = {
            Text(result.title.ifBlank { "启动前发现问题" })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activeProfile?.let { profile ->
                    StartPreflightProfileInfoCard(profile = profile)
                }
                Text(
                    text = result.summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                result.details.takeIf { it.isNotEmpty() }?.let { details ->
                    Surface(
                        color = LukoaColors.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            details.forEach { item ->
                                Text(
                                    text = "• $item",
                                    color = LukoaColors.Text,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            result.action?.let { action ->
                DialogActionButton(
                    text = action.label,
                    tone = when (action.type) {
                        TavernStartPreflightActionType.PrepareTermuxEnvironment,
                        TavernStartPreflightActionType.ForceCleanupDetectedProcess -> ActionTone.Warning

                        TavernStartPreflightActionType.DownloadTermux,
                        TavernStartPreflightActionType.RequestRunPermission,
                        TavernStartPreflightActionType.CopyExternalAppsCommand,
                        TavernStartPreflightActionType.ChooseDetectedDirectory,
                        TavernStartPreflightActionType.OpenPathSettings,
                        TavernStartPreflightActionType.ReturnToTavern,
                        TavernStartPreflightActionType.Retry -> ActionTone.Safe
                    },
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            DialogActionButton(
                text = if (result.action == null) "知道了" else "稍后",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun StartPreflightProfileInfoCard(profile: TavernProfile) {
    Surface(
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "这次准备启动的是下面这个实例：",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            VersionInfoLine("当前实例", profile.normalizedName)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "当前目录",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = profile.displayTavernDir,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            VersionInfoLine("当前端口", profile.normalizedPort.toString())
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
        title = "确认停止酒馆",
        titleTone = ActionTone.Danger,
        confirmText = "确认停止",
        confirmTone = ActionTone.Danger,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "这一步只会尝试温和停止当前实例，不会顺手强制清理残留进程。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        TavernActionProfileCard(
            profileName = profile.normalizedName,
            profilePath = profile.displayTavernDir,
            profilePort = profile.normalizedPort,
        )
        Text(
            text = "如果普通停止后网页仍在响应，再改用“强制释放端口 / 强制清理残留进程”更稳。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "这一步不会删除聊天、角色、世界书或备份文件。",
            color = LukoaColors.Muted,
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
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        TavernActionProfileCard(
            profileName = confirmation.profileName,
            profilePath = confirmation.profilePath,
            profilePort = confirmation.profilePort,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "为什么现在会建议这样做",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = confirmation.suggestion.reasonDetail,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Danger.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Danger.copy(alpha = 0.24f)),
        ) {
            Text(
                text = confirmation.suggestion.riskTip,
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VersionInfoLine("当前实例", profileName)
            VersionInfoLine("当前目录", profilePath)
            VersionInfoLine("当前端口", profilePort.toString())
        }
    }
}
