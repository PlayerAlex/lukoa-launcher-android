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
fun TermuxStoragePermissionDialog(
    archivePath: String,
    actionsLocked: Boolean,
    onGrantPermission: () -> Unit,
    onRetryApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text("需要 Termux 存储权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Termux 现在读不到 Downloads 里的备份。请先授权，否则不能应用备份。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开 Termux。看到权限弹窗时点允许，再回启动器继续。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (archivePath.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LukoaColors.SurfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = archivePath,
                            modifier = Modifier.padding(10.dp),
                            color = LukoaColors.Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton(
                    text = "去授权",
                    enabled = !actionsLocked,
                    tone = ActionTone.Warning,
                    onClick = onGrantPermission,
                )
                DialogActionButton(
                    text = "我已授权，继续应用",
                    enabled = !actionsLocked && archivePath.isNotBlank(),
                    tone = ActionTone.Safe,
                    onClick = onRetryApply,
                )
            }
        },
        dismissButton = {
            DialogActionButton("取消", tone = ActionTone.Warning, onClick = onDismiss)
        },
    )
}
@Composable
fun BackgroundRunPermissionDialog(
    granted: Boolean,
    onOpenPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text(if (granted) "后台运行已允许" else "需要后台运行权限") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (granted) {
                        "系统已经放行后台运行。若自动备份还是卡住，可以再进一次权限页检查省电策略。"
                    } else {
                        "自动备份想在你离开软件后也准时运行，需要把露科亚启动器加入后台运行白名单。"
                    },
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "点“去授权”后会打开系统页面。部分手机还要额外允许后台运行、自启动或取消省电限制。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = if (granted) "重新打开权限页" else "去授权",
                tone = ActionTone.Warning,
                onClick = onOpenPermission,
            )
        },
        dismissButton = {
            DialogActionButton("稍后", tone = ActionTone.Safe, onClick = onDismiss)
        },
    )
}

@Composable
fun FirstTavernStartGuideDialog(
    guide: FirstTavernStartGuide,
    onPrimaryAction: () -> Unit,
    onContinueStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission -> "第一次启动前先开 Termux 后台"
        FirstTavernStartGuideKind.KeepTermuxInSmallWindow -> "第一次启动前先把 Termux 挂小窗"
    }
    val summary = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission ->
            "这台手机看起来是 iQOO。第一次启动酒馆前，建议先给 Termux 打开后台运行或省电白名单。"

        FirstTavernStartGuideKind.KeepTermuxInSmallWindow ->
            "这台手机不是 iQOO。第一次启动酒馆时，更稳的做法是先把 Termux 挂到小窗或分屏，不要直接完全退到后台。"
    }
    val detail = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission ->
            "不然系统更容易在后台把 Termux 停掉，安装、启动和日志同步都可能中断。点下面按钮可直接去给 Termux 授权。"

        FirstTavernStartGuideKind.KeepTermuxInSmallWindow ->
            "启动器会先打开 Termux，再自动回到启动器。如果系统没有保住 Termux，请手动把 Termux 挂到小窗或分屏后再继续启动。"
    }
    val primaryLabel = when (guide.kind) {
        FirstTavernStartGuideKind.IQooBackgroundPermission -> "给 Termux 开后台"
        FirstTavernStartGuideKind.KeepTermuxInSmallWindow -> "唤醒 Termux 并返回"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = summary,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LukoaColors.Amber.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
                ) {
                    Text(
                        text = detail,
                        modifier = Modifier.padding(12.dp),
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                DialogActionButton(
                    text = primaryLabel,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPrimaryAction,
                )
                DialogActionButton(
                    text = "我已了解，继续启动",
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinueStart,
                )
                DialogActionButton(
                    text = "稍后再看",
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}
