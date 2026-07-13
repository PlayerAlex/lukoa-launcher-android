package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

@Composable
fun Header(
    instanceLabel: String,
    instancePort: Int,
    showVersionUpdateBadge: Boolean,
    onVersionClick: () -> Unit,
) {
    val context = LocalContext.current
    val feedbackVersionClick = rememberFeedbackClick(onVersionClick)
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "..."
    } catch (_: Exception) {
        "..."
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lukoa_launcher),
                    contentDescription = "露科亚启动器",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "露科亚启动器",
                        style = MaterialTheme.typography.titleMedium,
                        color = LukoaColors.Text,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "$instanceLabel · 端口 $instancePort",
                        style = MaterialTheme.typography.bodySmall,
                        color = LukoaColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = feedbackVersionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "v$versionName",
                    color = if (showVersionUpdateBadge) LukoaColors.Text else LukoaColors.Muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (showVersionUpdateBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 7.dp)
                            .size(8.dp)
                            .background(LukoaColors.Danger, LukoaCapsuleShape)
                            .border(2.dp, LukoaColors.Background, LukoaCapsuleShape),
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewPanel(
    summary: String,
    status: String,
    verified: Boolean,
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    syncActive: Boolean,
) {
    val stateLabel = when {
        tavernRunning -> "运行中"
        tavernStarting -> "启动中"
        verified -> "未运行"
        else -> "未就绪"
    }
    val stateColor = when {
        tavernRunning || tavernStarting -> LukoaColors.Accent
        verified -> LukoaColors.Text
        else -> LukoaColors.Amber
    }
    val statusLine = status
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .ifBlank { "等待操作" }
    DashedSection(
        label = "酒馆状态",
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = stateLabel,
                color = stateColor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = summary.ifBlank { statusLine },
                color = LukoaColors.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                DotStatusPill(
                    text = if (tavernRunning || tavernStarting) "酒馆运行中" else "酒馆未运行",
                    active = tavernRunning || tavernStarting,
                )
                DotStatusPill(
                    text = if (syncActive) "Termux 同步中" else "Termux 未同步",
                    active = syncActive,
                )
            }
        }
    }
}

@Composable
fun SectionPanel(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    DashedSection(
        label = title,
        headerAction = headerAction,
    ) {
        content()
    }
}


@Composable
fun LogPanel(
    title: String,
    content: String,
    accentColor: androidx.compose.ui.graphics.Color,
    subtitle: String? = null,
    followLatestByDefault: Boolean = true,
    showFollowControls: Boolean = true,
    maxVisibleLines: Int? = 900,
) {
    val displayContent = remember(content, maxVisibleLines) {
        maxVisibleLines?.let(content::keepLatestLines) ?: content
    }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var followLatest by remember(followLatestByDefault) { mutableStateOf(followLatestByDefault) }
    var autoScrollInProgress by remember { mutableStateOf(false) }
    var suppressUserPause by remember { mutableStateOf(false) }
    val isNearBottom by remember {
        derivedStateOf { scrollState.maxValue - scrollState.value <= 24 }
    }

    LaunchedEffect(displayContent, scrollState.maxValue, followLatest) {
        if (followLatest) {
            autoScrollInProgress = true
            suppressUserPause = true
            try {
                withFrameNanos { }
                scrollState.scrollTo(scrollState.maxValue)
                withFrameNanos { }
                if (followLatest) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            } finally {
                autoScrollInProgress = false
                withFrameNanos { }
                suppressUserPause = false
            }
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress, isNearBottom, autoScrollInProgress, suppressUserPause) {
        if (
            showFollowControls &&
            scrollState.isScrollInProgress &&
            !autoScrollInProgress &&
            !suppressUserPause &&
            !isNearBottom
        ) {
            followLatest = false
        }
    }

    LaunchedEffect(isNearBottom, followLatest) {
        if (showFollowControls && isNearBottom && !followLatest) {
            followLatest = true
        }
    }

    DashedSection(
        label = title,
        headerAction = if (showFollowControls) {
            {
                Text(
                    text = if (followLatest) "追踪中" else "已暂停",
                    color = if (followLatest) LukoaColors.Accent else LukoaColors.Amber,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            null
        },
    ) {
        subtitle?.let {
            Text(
                text = it,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 430.dp)
                .background(LukoaColors.Terminal, RoundedCornerShape(14.dp)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(12.dp)
                    .verticalScroll(scrollState),
            ) {
                TerminalText(text = displayContent)
            }
            if (showFollowControls && !followLatest && !isNearBottom) {
                ReturnToLatestChip(
                    accentColor = accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    onClick = {
                        followLatest = true
                        scope.launch {
                            autoScrollInProgress = true
                            suppressUserPause = true
                            try {
                                withFrameNanos { }
                                scrollState.scrollTo(scrollState.maxValue)
                                withFrameNanos { }
                                scrollState.animateScrollTo(scrollState.maxValue)
                            } finally {
                                autoScrollInProgress = false
                                withFrameNanos { }
                                suppressUserPause = false
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReturnToLatestChip(
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    Surface(
        modifier = modifier
            .clickable(onClick = feedbackClick),
        color = LukoaColors.Background.copy(alpha = 0.94f),
        shape = LukoaCapsuleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "↓",
                color = accentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "回到底部",
                color = accentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun String.keepLatestLines(maxLines: Int): String {
    val lines = lineSequence().toList()
    if (lines.size <= maxLines) return this
    val omitted = lines.size - maxLines
    return buildString {
        appendLine("... 已隐藏前面 $omitted 行，只显示最新 $maxLines 行 ...")
        append(lines.takeLast(maxLines).joinToString("\n"))
    }
}
