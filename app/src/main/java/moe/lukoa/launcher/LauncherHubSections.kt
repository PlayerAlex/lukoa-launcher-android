package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TavernHubSection(
    tavernRunning: Boolean,
    tavernStarting: Boolean,
    actionInProgress: Boolean,
    onOpenVersionManagement: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenExtensionManagement: () -> Unit,
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
                        text = "管理当前实例的版本、备份与扩展。",
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
        )
    }
}

@Composable
private fun TavernManagementGrid(
    onOpenVersionManagement: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenExtensionManagement: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column {
            HubGridRow(
                left = HubGridEntry("版本管理", "查看与切换版本", onOpenVersionManagement),
                right = HubGridEntry("备份", "保护与恢复数据", onOpenBackup),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("扩展管理", "管理第三方扩展", onOpenExtensionManagement),
                right = HubGridEntry("敬请期待", "", null),
            )
            HorizontalDivider(color = LukoaColors.Border)
            HubGridRow(
                left = HubGridEntry("敬请期待", "", null),
                right = HubGridEntry("敬请期待", "", null),
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
                .heightIn(min = 120.dp),
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

    Box(
        modifier = modifier
            .heightIn(min = 120.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(min = 80.dp)
                    .clickable(role = Role.Button, onClick = feedbackClick),
                color = LukoaColors.Elevated,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (entry.emphasized) LukoaColors.Accent else LukoaColors.Border,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = entry.label,
                            color = if (entry.emphasized) LukoaColors.Accent else LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = entry.description,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = LukoaColors.Primary,
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
fun LauncherSecondaryPageHeader(
    title: String,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 32.dp, end = 4.dp, bottom = 8.dp),
        ) {
            val feedbackBack = rememberFeedbackClick(onBack)
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(88.dp)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = feedbackBack),
                color = LukoaColors.Surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LukoaColors.Border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
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
                    .padding(horizontal = 92.dp)
                    .semantics { heading() },
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
