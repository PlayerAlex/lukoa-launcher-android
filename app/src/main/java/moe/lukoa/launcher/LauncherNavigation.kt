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
        color = LukoaColors.Surface,
    ) {
        Column {
            HorizontalDivider(color = LukoaColors.Line.copy(alpha = 0.3f))
            NavigationBar(
                containerColor = LukoaColors.Surface,
                contentColor = LukoaColors.Text,
                tonalElevation = 0.dp,
            ) {
                LauncherTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Text(
                                text = tab.shortLabel,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) LukoaColors.Text else LukoaColors.Muted,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LukoaColors.Accent,
                            selectedTextColor = LukoaColors.Text,
                            indicatorColor = LukoaColors.AccentSoft.copy(alpha = 0.6f),
                            unselectedIconColor = LukoaColors.Muted,
                            unselectedTextColor = LukoaColors.Muted,
                        ),
                    )
                }
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
    SectionPanel(title = "正在处理", accentColor = LukoaColors.Amber) {
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
            StatusPill(
                text = elapsedText,
                active = true,
                toneColor = LukoaColors.Amber,
                activeBackground = LukoaColors.AmberSoft,
            )
        }
        Text(
            text = detail,
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "按钮已锁定，别重复点。完成后会显示 Termux 完整返回。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatBusyElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return if (minutes > 0) {
        "%d:%02d".format(minutes, rest)
    } else {
        "${rest}s"
    }
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

