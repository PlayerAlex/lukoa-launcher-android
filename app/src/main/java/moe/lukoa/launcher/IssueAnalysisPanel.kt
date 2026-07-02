package moe.lukoa.launcher

import androidx.compose.runtime.Composable

@Composable
fun IssueAnalysisPanel(issues: List<TavernIssue>) {
    LogPanel(
        title = "露科亚问题分析辅助",
        content = issues.toTerminalText(),
        accentColor = LukoaColors.Accent,
        followLatestByDefault = false,
        showFollowControls = false,
        subtitle = "该分析仅供参考，具体请看答疑人员的回复",
    )
}

private fun List<TavernIssue>.toTerminalText(): String {
    if (isEmpty()) {
        return "返回正常\n最近一次返回未发现常见报错。"
    }
    return buildString {
        appendLine("发现 ${this@toTerminalText.size} 个可能问题")
        this@toTerminalText.forEachIndexed { index, issue ->
            if (index > 0) appendLine()
            appendLine("${index + 1}. [${issue.severity.label()}] ${issue.title}")
            appendLine("原因：${issue.detail}")
            appendLine("建议：${issue.action}")
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
