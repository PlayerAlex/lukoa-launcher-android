package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun IssueAnalysisDialog(
    issues: List<TavernIssue>,
    actionsLocked: Boolean,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            SettingsDialogTitle(
                title = "失败后一键修复",
                infoText = "启动器会根据最近的报错自动匹配最常见的原因，默认只展示最关键的一条；如果有其他次要提醒，可按需展开查看。",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IssueAnalysisBody(
                    issues = issues,
                    actionsLocked = actionsLocked,
                    onQuickFixAction = { action ->
                        onDismiss()
                        onQuickFixAction(action)
                    },
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "关闭",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
        dismissButton = null,
    )
}

@Composable
fun IssueAnalysisPanel(
    issues: List<TavernIssue>,
    actionsLocked: Boolean,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
    showSectionContainer: Boolean = true,
) {
    if (!showSectionContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IssueAnalysisBody(
                issues = issues,
                actionsLocked = actionsLocked,
                onQuickFixAction = onQuickFixAction,
            )
        }
        return
    }
    val hasDanger = issues.any { it.severity == IssueSeverity.Danger }
    SectionPanel(
        title = "失败后一键修复",
        accentColor = if (hasDanger) LukoaColors.Danger else LukoaColors.Primary,
        headerAction = {
            InfoPopoverButton(
                contentDescription = "查看失败后一键修复说明",
                title = "失败后一键修复",
                body = "启动器会根据最近的报错自动匹配最常见的原因，默认只展示最关键的一条，避免满屏干扰；如果有其他次要提醒，可按需展开查看。",
            )
        },
    ) {
        IssueAnalysisBody(
            issues = issues,
            actionsLocked = actionsLocked,
            onQuickFixAction = onQuickFixAction,
        )
    }
}

@Composable
private fun IssueAnalysisBody(
    issues: List<TavernIssue>,
    actionsLocked: Boolean,
    onQuickFixAction: (LauncherQuickFixAction) -> Unit,
) {
    val sortedIssues = remember(issues) {
        issues.sortedWith(
            compareBy<TavernIssue>(
                {
                    when (it.severity) {
                        IssueSeverity.Danger -> 0
                        IssueSeverity.Warning -> 1
                        IssueSeverity.Info -> 2
                    }
                },
                { if (it.quickFixAction != null) 0 else 1 },
            ),
        )
    }
    var showSecondary by rememberSaveable(sortedIssues.size) { mutableStateOf(false) }

    if (sortedIssues.isEmpty()) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "最近未发现常见报错",
                detail = "如果使用仍异常，可去工具箱点「一键体检」。",
                detailMaxLines = 2,
            )
        }
        return
    }

    val primaryIssue = sortedIssues.first()
    val secondaryIssues = sortedIssues.drop(1)

    IssueAnalysisItem(
        issue = primaryIssue,
        actionsLocked = actionsLocked,
        onQuickFixAction = onQuickFixAction,
    )

    if (secondaryIssues.isNotEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { showSecondary = !showSecondary },
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (showSecondary) {
                        "收起其余 ${secondaryIssues.size} 条次要提醒"
                    } else {
                        "还有 ${secondaryIssues.size} 条次要提醒"
                    },
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (showSecondary) "收起 ▴" else "展开 ▾",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (showSecondary) {
            secondaryIssues.forEach { issue ->
                IssueAnalysisItem(
                    issue = issue,
                    actionsLocked = actionsLocked,
                    onQuickFixAction = onQuickFixAction,
                )
            }
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
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = issue.title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
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
                text = "${issue.detail} ${issue.action}",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            issue.quickFixAction?.let { action ->
                SecondaryActionButton(
                    text = action.label,
                    enabled = !actionsLocked,
                    accentColor = if (issue.severity == IssueSeverity.Danger) LukoaColors.Danger else LukoaColors.Primary,
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
        IssueSeverity.Danger -> "需处理"
    }
}

private fun IssueSeverity.toneColor(): Color {
    return when (this) {
        IssueSeverity.Info -> LukoaColors.TextSecondary
        IssueSeverity.Warning -> LukoaColors.Primary
        IssueSeverity.Danger -> LukoaColors.Danger
    }
}
