package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


enum class LauncherTab(
    val label: String,
    val shortLabel: String,
) {
    Docs("文档", "文"),
    Version("版本", "版"),
    Launch("启动", "启"),
    Backup("备份", "备"),
    Settings("设置", "设"),
}


@Composable
fun LauncherBottomBar(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Background.copy(alpha = 0.98f),
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = LukoaColors.Line.copy(alpha = 0.18f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LauncherTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val isLaunch = tab == LauncherTab.Launch
                    val color = if (selected) LukoaColors.Accent else LukoaColors.Dim
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                role = Role.Tab,
                            )
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (isLaunch) {
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .background(
                                        color = if (selected) LukoaColors.Accent else LukoaColors.AccentSoft.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 15.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LauncherNavIcon(
                                    tab = tab,
                                    color = if (selected) LukoaColors.AccentDark else LukoaColors.Accent,
                                )
                            }
                        } else {
                            LauncherNavIcon(tab = tab, color = color)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherNavIcon(tab: LauncherTab, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            LauncherTab.Docs -> {
                drawLine(color, start = androidx.compose.ui.geometry.Offset(4f, size.height * .28f), end = androidx.compose.ui.geometry.Offset(size.width - 4f, size.height * .28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(4f, size.height * .5f), end = androidx.compose.ui.geometry.Offset(size.width - 4f, size.height * .5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, start = androidx.compose.ui.geometry.Offset(4f, size.height * .72f), end = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .72f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            LauncherTab.Version -> {
                val top = Path().apply {
                    moveTo(size.width / 2f, 3f)
                    lineTo(size.width - 3f, size.height * .3f)
                    lineTo(size.width / 2f, size.height * .55f)
                    lineTo(3f, size.height * .3f)
                    close()
                }
                drawPath(top, color, style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(4f, size.height * .58f), androidx.compose.ui.geometry.Offset(size.width / 2f, size.height - 3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width - 4f, size.height * .58f), androidx.compose.ui.geometry.Offset(size.width / 2f, size.height - 3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            LauncherTab.Launch -> {
                val play = Path().apply {
                    moveTo(size.width * .34f, size.height * .2f)
                    lineTo(size.width * .78f, size.height * .5f)
                    lineTo(size.width * .34f, size.height * .8f)
                    close()
                }
                drawPath(play, color)
            }
            LauncherTab.Backup -> {
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(3f, 3f),
                    size = androidx.compose.ui.geometry.Size(size.width - 6f, size.height - 6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawLine(color, androidx.compose.ui.geometry.Offset(6f, size.height * .35f), androidx.compose.ui.geometry.Offset(size.width - 6f, size.height * .35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            LauncherTab.Settings -> {
                drawCircle(color = color, radius = size.minDimension * .38f, style = stroke)
                drawCircle(color = color, radius = size.minDimension * .12f, style = stroke)
            }
        }
    }
}


@Composable
fun BusyPanel(label: String, startedAtMillis: Long) {
    var nowMillis by remember(label, startedAtMillis) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(label, startedAtMillis) {
        while (true) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val elapsedSeconds = if (startedAtMillis > 0L) {
        ((nowMillis - startedAtMillis).coerceAtLeast(0L) / 1000L).toInt()
    } else {
        0
    }
    val elapsedText = formatBusyElapsed(elapsedSeconds)
    val detail = busyDetailFor(label, elapsedSeconds)
    SectionPanel(title = "当前任务", accentColor = LukoaColors.Accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = LukoaColors.Text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = elapsedText,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = detail,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "安装、更新、回退、备份、恢复和用户修改已暂时锁定，避免多个命令互相冲突。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun formatBusyElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return "%02d:%02d".format(minutes, rest)
}

private fun busyDetailFor(label: String, seconds: Int): String {
    if (!label.contains("准备 Termux 环境")) {
        return "Termux 正在处理这个操作。"
    }
    return when {
        seconds < 20 -> "已发送命令，正在连接 Termux 包源。"
        seconds < 90 -> "可能正在执行 apt update 或升级基础包。"
        seconds < 240 -> "可能正在安装 git、node、npm，首次安装会比较久。"
        else -> "仍在等待 Termux 回传。只要按钮还锁着，就说明启动器还在等结果。"
    }
}
