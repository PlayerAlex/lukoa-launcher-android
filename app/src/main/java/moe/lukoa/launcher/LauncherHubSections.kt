package moe.lukoa.launcher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TavernHubSection(
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    actionInProgress: Boolean,
    onOpenVersionManagement: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenExtensionManagement: () -> Unit,
    onOpenTavernConfig: () -> Unit = {},
    onOpenServerPlugins: () -> Unit = {},
    onOpenQuickImport: () -> Unit = {},
) {
    val statusText = when {
        actionInProgress -> "正在处理任务"
        tavernStarting -> "酒馆正在启动"
        tavernRunning -> "酒馆当前运行中"
        else -> "酒馆当前未启动"
    }
    val statusTone = if (tavernRunning || tavernStarting || actionInProgress) {
        LukoaColors.Primary
    } else {
        LukoaColors.TextSecondary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "酒馆管理中心",
                        modifier = Modifier.semantics { heading() },
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "在这里管理酒馆版本、数据备份与常用设置。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                StatusPill(
                    text = statusText,
                    active = tavernRunning || tavernStarting || actionInProgress,
                    toneColor = statusTone,
                    activeBackground = LukoaColors.PrimarySoft,
                )
            }
        }

        TavernManagementGrid(
            onOpenVersionManagement = onOpenVersionManagement,
            onOpenBackup = onOpenBackup,
            onOpenExtensionManagement = onOpenExtensionManagement,
            onOpenTavernConfig = onOpenTavernConfig,
            onOpenServerPlugins = onOpenServerPlugins,
            onOpenQuickImport = onOpenQuickImport,
        )
    }
}

@Composable
private fun TavernManagementGrid(
    onOpenVersionManagement: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenExtensionManagement: () -> Unit,
    onOpenTavernConfig: () -> Unit,
    onOpenServerPlugins: () -> Unit,
    onOpenQuickImport: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .spotlightTarget(SpotlightTargetId.TavernHubGrid),
        color = LukoaColors.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column {
            HubGridRow(
                left = HubGridEntry("备份", "保存与恢复数据", onOpenBackup),
                right = HubGridEntry("版本管理", "更新与回退酒馆", onOpenVersionManagement),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("快捷导入", "导入角色与世界书", onOpenQuickImport),
                right = HubGridEntry("扩展管理", "管理与更新扩展", onOpenExtensionManagement),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("酒馆设置", "电脑访问与流畅度", onOpenTavernConfig),
                right = HubGridEntry("服务器插件", "安装后端插件", onOpenServerPlugins),
            )
        }
    }
}

internal data class HubGridEntry(
    val label: String,
    val description: String,
    val onClick: (() -> Unit)?,
    val emphasized: Boolean = false,
)

@Composable
internal fun HubGridRow(
    left: HubGridEntry,
    right: HubGridEntry,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        HubGridTile(entry = left, modifier = Modifier.weight(1f))
        Surface(
            modifier = Modifier
                .width(1.dp)
                .heightIn(min = 104.dp),
            color = LukoaColors.Border,
            content = {},
        )
        HubGridTile(entry = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HubGridTile(
    entry: HubGridEntry,
    modifier: Modifier = Modifier,
) {
    val enabled = entry.onClick != null
    val feedbackClick = rememberFeedbackClick(onClick = { entry.onClick?.invoke() })
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "hub-tile-scale",
    )

    Box(
        modifier = modifier
            .heightIn(min = 104.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 82.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = feedbackClick,
                    ),
                color = LukoaColors.Elevated,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (entry.emphasized) LukoaColors.Accent else LukoaColors.Border,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        HubGridTitle(entry)
                        Text(
                            text = entry.description,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = LukoaColors.TextSecondary,
                    )
                }
            }
        } else {
            Text(
                text = entry.label,
                modifier = Modifier
                    .semantics { disabled() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HubGridTitle(entry: HubGridEntry) {
    val baseStyle = MaterialTheme.typography.titleMedium
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.roundToPx() }
        val fittedFontSize = remember(entry.label, availableWidthPx, density.fontScale) {
            var candidate = baseStyle.fontSize.value
            while (candidate > 8f) {
                val measuredWidth = textMeasurer.measure(
                    text = entry.label,
                    style = baseStyle.copy(
                        fontSize = candidate.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    softWrap = false,
                ).size.width
                if (measuredWidth <= availableWidthPx) break
                candidate -= 0.5f
            }
            candidate.sp
        }

        Text(
            text = entry.label,
            color = if (entry.emphasized) LukoaColors.Accent else LukoaColors.TextPrimary,
            style = baseStyle.copy(fontSize = fittedFontSize),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

@Composable
fun LauncherSecondaryPageHeader(
    title: String,
    infoText: String? = null,
    onBack: () -> Unit,
) {
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerTopPadding = maxOf(cutoutTop, statusTop, 36.dp) + 4.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Background,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = headerTopPadding, end = 8.dp, bottom = 10.dp),
            ) {
                val feedbackBack = rememberFeedbackClick(onBack)
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .heightIn(min = 42.dp)
                        .clickable(role = Role.Button, onClick = feedbackBack),
                    color = LukoaColors.Surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = LukoaColors.Primary,
                        )
                        Text(
                            text = "返回",
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 88.dp)
                        .semantics { heading() },
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!infoText.isNullOrBlank()) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        InfoPopoverButton(
                            contentDescription = "查看${title}说明",
                            title = title,
                            body = infoText,
                        )
                    }
                }
            }
            HorizontalDivider(color = LukoaColors.Border)
        }
    }
}
