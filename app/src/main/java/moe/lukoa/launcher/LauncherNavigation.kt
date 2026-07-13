package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


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
        color = Color(0xF20B0D11),
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = LukoaColors.Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(bottom = 4.dp),
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
                                    .size(width = 50.dp, height = 38.dp)
                                    .background(
                                        color = if (selected) LukoaColors.Accent else LukoaColors.AccentSoft,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
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
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
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
    Canvas(modifier = Modifier.size(21.dp)) {
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
fun BusyInlineBlock(label: String, startedAtMillis: Long) {
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
    val displayLabel = if (label.startsWith("正在")) label else "正在$label"
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp, bottom = 7.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.06f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayLabel,
                modifier = Modifier.weight(1f),
                color = LukoaColors.Text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = elapsedText,
                color = LukoaColors.Text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "安装、更新、回退、备份、恢复和用户修改已暂时锁定，避免多个命令互相冲突。",
            color = LukoaColors.Text,
            fontSize = 11.5.sp,
            lineHeight = 17.sp,
        )
    }
}

internal fun formatBusyElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return "%02d:%02d".format(minutes, rest)
}
