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
fun ExportLogDialog(
    onExportTermux: () -> Unit,
    onExportApp: () -> Unit,
    onExportBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("导出运行日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "导出包含清除后累计内容。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只导出酒馆运行日志",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTermux,
                )
                SecondaryActionButton(
                    text = "只导出 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportApp,
                )
                SecondaryActionButton(
                    text = "全部都导出",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}
@Composable
fun ClearLogScopeDialog(
    onClearTermux: () -> Unit,
    onClearApp: () -> Unit,
    onClearBoth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("选择清除范围") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "只清空这里的显示，不删酒馆文件。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                SecondaryActionButton(
                    text = "只清除 Termux 侧",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearTermux,
                )
                SecondaryActionButton(
                    text = "只清除 App 操作反馈",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearApp,
                )
                SecondaryActionButton(
                    text = "全部都清除",
                    enabled = true,
                    accentColor = LukoaColors.Accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearBoth,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            SecondaryActionButton("取消", true, LukoaColors.Accent, onClick = onDismiss)
        },
    )
}

@Composable
fun ClearLogDangerDialog(
    mode: ExportLogMode,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val target = when (mode) {
        ExportLogMode.TermuxOnly -> "Termux 前台回传和酒馆运行日志"
        ExportLogMode.AppOnly -> "App 操作反馈"
        ExportLogMode.Both -> "Termux 前台回传、酒馆运行日志和 App 操作反馈"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Danger,
        textContentColor = LukoaColors.Text,
        title = { Text("确认清除日志") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "将清空页面显示：$target。",
                    color = LukoaColors.Text,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "只清空页面显示；后台诊断归档会继续记录，导出日志和诊断仍保留完整内容。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = onConfirmTextChange,
                    singleLine = true,
                    label = { Text("输入“清除”继续") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        focusedBorderColor = LukoaColors.Danger,
                        unfocusedBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Danger,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Danger,
                    ),
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认清除",
                enabled = confirmText.trim() == "清除",
                tone = ActionTone.Danger,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionButton("返回", tone = ActionTone.Danger, onClick = onBack)
                DialogActionButton("取消", tone = ActionTone.Danger, onClick = onDismiss)
            }
        },
    )
}
