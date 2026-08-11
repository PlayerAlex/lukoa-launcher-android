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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    tavernRunning: Boolean,
    tavernStarting: Boolean,
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
            modifier = Modifier.fillMaxWidth(),
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
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "露科亚启动器",
                        style = MaterialTheme.typography.titleLarge,
                        color = LukoaColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Termux 控制台",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LukoaColors.TextSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    color = when {
                                        tavernRunning -> LukoaColors.Primary
                                        tavernStarting -> LukoaColors.Primary
                                        else -> LukoaColors.Dim
                                    },
                                    shape = RoundedCornerShape(5.dp),
                                ),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 112.dp)
                    .clickable(onClick = feedbackVersionClick)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "v$versionName",
                    modifier = Modifier
                        .padding(end = if (showVersionUpdateBadge) 8.dp else 0.dp)
                        .testTag("launcher-version"),
                    color = if (showVersionUpdateBadge) LukoaColors.Primary else LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showVersionUpdateBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .testTag("launcher-update-dot")
                            .background(
                                color = LukoaColors.Danger,
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionPanel(
    title: String,
    accentColor: Color,
    containerColor: Color = LukoaColors.Surface,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, LukoaColors.Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(accentColor, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                headerAction?.invoke()
            }
            content()
        }
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, LukoaColors.Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showFollowControls) {
                    StatusPill(
                        text = if (followLatest) "追踪最新" else "已暂停",
                        active = followLatest,
                        toneColor = if (followLatest) LukoaColors.Primary else LukoaColors.TextSecondary,
                        activeBackground = if (followLatest) LukoaColors.PrimarySoft else LukoaColors.Elevated,
                    )
                }
            }
            subtitle?.let {
                Text(
                    text = it,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 430.dp)
                    .background(LukoaColors.Terminal, RoundedCornerShape(12.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(10.dp)
                        .verticalScroll(scrollState),
                ) {
                    TerminalText(
                        text = displayContent,
                    )
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
        color = LukoaColors.Background,
        shape = LukoaCapsuleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, LukoaColors.Border),
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
