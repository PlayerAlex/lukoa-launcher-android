package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun IssueAnalysisPanel(
    issues: List<TavernIssue>,
    actionsLocked: Boolean,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
) {
    SectionPanel(title = "失败后一键修复", accentColor = LukoaColors.Accent) {
        if (issues.isEmpty()) {
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
                    Text(
                        text = "最近一次返回未发现常见报错。",
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "如果你体感上还是不对，先点一键体检，再看下方 Termux 返回。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            return@SectionPanel
        }

        issues.forEach { issue ->
            IssueAnalysisItem(
                issue = issue,
                actionsLocked = actionsLocked,
                onQuickFixAction = onQuickFixAction,
            )
        }
    }
}

@Composable
private fun IssueAnalysisItem(
    issue: TavernIssue,
    actionsLocked: Boolean,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
) {
    val toneColor = issue.severity.toneColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = issue.title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = issue.severity.label(),
                    active = issue.severity != IssueSeverity.Info,
                    toneColor = toneColor,
                    activeBackground = toneColor.copy(alpha = 0.14f),
                )
            }
            Text(
                text = issue.detail,
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LukoaColors.SurfaceAlt.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = issue.action,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            issue.quickFixAction?.let { action ->
                SecondaryActionButton(
                    text = action.label,
                    enabled = !actionsLocked,
                    accentColor = toneColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onQuickFixAction(action) },
                )
            }
        }
    }
}

private fun IssueSeverity.label(): String {
    return when (this) {
        IssueSeverity.Info -> "提示"
        IssueSeverity.Warning -> "注意"
        IssueSeverity.Danger -> "危险"
    }
}

private fun IssueSeverity.toneColor(): Color {
    return when (this) {
        IssueSeverity.Info -> LukoaColors.Muted
        IssueSeverity.Warning -> LukoaColors.Amber
        IssueSeverity.Danger -> LukoaColors.Danger
    }
}
