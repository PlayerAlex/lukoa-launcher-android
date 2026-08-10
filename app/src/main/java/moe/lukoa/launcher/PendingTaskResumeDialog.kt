package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PendingTaskResumeDialog(
    task: PendingLauncherTask,
    activeLockLabel: String?,
    onContinueCheck: () -> Unit,
    onAbandon: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmAbandon by remember(task.kind, task.startedAtMillis) { mutableStateOf(false) }
    val taskMayStillBeRunning = !activeLockLabel.isNullOrBlank()

    if (confirmAbandon) {
        PendingTaskAbandonDialog(
            task = task,
            onConfirm = {
                confirmAbandon = false
                onAbandon()
            },
            onDismiss = { confirmAbandon = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            Text(if (taskMayStillBeRunning) "任务跟踪已中断" else "上次操作需要确认")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PendingTaskStateHeader(activeLockLabel = activeLockLabel)
                Text(
                    text = if (taskMayStillBeRunning) {
                        "启动器重新打开后，无法继续实时跟踪这次操作。Termux 里的任务可能仍在执行，这不代表任务失败，也不会自动重做。\n推荐先检查结果：只会查找已有返回并刷新状态。"
                    } else {
                        "启动器没有收到上次操作的最终结果。这不代表操作失败，也不会自动再执行一次。\n推荐先检查结果：只会查找已有返回并刷新状态。"
                    },
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                PendingTaskSummaryCard(task = task)
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToneActionButton(
                    text = "检查上次操作的结果",
                    enabled = true,
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinueCheck,
                )
                ToneActionButton(
                    text = "不再跟踪这次操作",
                    enabled = true,
                    tone = ActionTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmAbandon = true },
                )
            }
        },
        dismissButton = {},
    )
}

@Composable
fun PendingTaskNoticePanel(
    task: PendingLauncherTask,
    activeLockLabel: String?,
    onContinueCheck: () -> Unit,
    onAbandon: () -> Unit,
) {
    var confirmAbandon by remember(task.kind, task.startedAtMillis) { mutableStateOf(false) }
    val taskMayStillBeRunning = !activeLockLabel.isNullOrBlank()

    if (confirmAbandon) {
        PendingTaskAbandonDialog(
            task = task,
            onConfirm = {
                confirmAbandon = false
                onAbandon()
            },
            onDismiss = { confirmAbandon = false },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.AccentSoft,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(LukoaColors.Accent, RoundedCornerShape(4.dp)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "上次操作等待确认",
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${task.title} · ${formatPendingTaskTime(task.startedAtMillis)}",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                InfoPopoverButton(
                    contentDescription = "查看未完成操作详情",
                    title = task.title,
                    body = pendingTaskDetails(task),
                )
            }

            Text(
                text = if (taskMayStillBeRunning) {
                    "启动器已重新打开，Termux 里的任务可能仍在执行。先检查已有结果；不会重新执行${task.title}。"
                } else {
                    "等待时间已经结束，但启动器还没收到最终结果。先检查已有结果；不会重新执行${task.title}。"
                },
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
            ToneActionButton(
                text = "检查上次操作的结果",
                enabled = true,
                tone = ActionTone.Safe,
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueCheck,
            )
            ToneActionButton(
                text = "不再跟踪这次操作",
                enabled = true,
                tone = ActionTone.Warning,
                modifier = Modifier.fillMaxWidth(),
                onClick = { confirmAbandon = true },
            )
        }
    }
}

@Composable
private fun PendingTaskStateHeader(activeLockLabel: String?) {
    val taskMayBeRunning = !activeLockLabel.isNullOrBlank()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (taskMayBeRunning) LukoaColors.AccentSoft else LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (taskMayBeRunning) LukoaColors.Accent.copy(alpha = 0.35f) else LukoaColors.Border,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (taskMayBeRunning) "任务可能仍在执行" else "任务结果尚未确认",
                modifier = Modifier.weight(1f),
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            StatusPill(
                text = if (taskMayBeRunning) "先检查" else "待确认",
                active = taskMayBeRunning,
                toneColor = if (taskMayBeRunning) LukoaColors.Accent else LukoaColors.TextSecondary,
                activeBackground = if (taskMayBeRunning) LukoaColors.Elevated else LukoaColors.Surface,
            )
        }
    }
}

@Composable
private fun PendingTaskSummaryCard(
    task: PendingLauncherTask,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
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
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (task.safetyBackupPath.isNotBlank() || task.archivePath.isNotBlank()) {
                    InfoPopoverButton(
                        contentDescription = "查看任务相关文件",
                        title = "任务相关文件",
                        body = buildString {
                            task.safetyBackupPath.takeIf(String::isNotBlank)?.let { append("安全备份：$it") }
                            task.archivePath.takeIf(String::isNotBlank)?.let {
                                if (isNotEmpty()) append('\n')
                                append("备份包：$it")
                            }
                        },
                    )
                }
            }
            task.detail.takeIf(String::isNotBlank)?.let { detail ->
                Text(
                    text = detail,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            task.targetLabel.takeIf(String::isNotBlank)?.let { PendingTaskInfoLine("目标", it) }
            PendingTaskInfoLine("开始时间", formatPendingTaskTime(task.startedAtMillis))
        }
    }
}

@Composable
private fun PendingTaskAbandonDialog(
    task: PendingLauncherTask,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        title = { Text("确认不再跟踪？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "这只会删除启动器保存的“${task.title}”等待记录，不会停止 Termux，也不会删除酒馆、备份或已经生成的文件。",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "如果 Termux 仍在执行，清除记录后启动器将无法继续阻止重复操作。只有确认任务已经结束时才使用。",
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "确认不再跟踪",
                tone = ActionTone.Warning,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "返回检查结果",
                tone = ActionTone.Safe,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun PendingTaskInfoLine(
    label: String,
    value: String,
    maxLines: Int = 1,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun pendingTaskDetails(task: PendingLauncherTask): String = buildString {
    append("开始时间：${formatPendingTaskTime(task.startedAtMillis)}")
    task.detail.takeIf(String::isNotBlank)?.let { append("\n当前记录：$it") }
    task.targetLabel.takeIf(String::isNotBlank)?.let { append("\n目标：$it") }
    task.safetyBackupPath.takeIf(String::isNotBlank)?.let { append("\n安全备份：$it") }
    task.archivePath.takeIf(String::isNotBlank)?.let { append("\n备份包：$it") }
}

private fun formatPendingTaskTime(timeMillis: Long): String {
    if (timeMillis <= 0L) return "刚刚"
    return PENDING_TASK_TIME_FORMATTER.format(
        Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()),
    )
}

private val PENDING_TASK_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

@Composable
fun BackgroundTaskSettingsPanel(
    status: String,
    needsAttention: Boolean,
    onOpen: () -> Unit,
) {
    SectionPanel(
        title = "后台任务",
        accentColor = if (needsAttention) LukoaColors.Accent else LukoaColors.Primary,
        containerColor = LukoaColors.Elevated,
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "任务中心",
                detail = "查看耗时操作、补查 Termux 返回结果，或前往任务对应页面。",
                value = status,
                valueColor = if (needsAttention) LukoaColors.Accent else LukoaColors.Primary,
                valueAsPill = true,
                highlightColor = if (needsAttention) LukoaColors.Accent else null,
                onClick = onOpen,
            )
        }
    }
}

@Composable
fun BackgroundTaskCenterDialog(
    pendingTask: PendingLauncherTask?,
    activeLockLabel: String?,
    recentResults: List<TermuxResultDisplay>,
    onCheckPendingTask: () -> Unit,
    onOpenPendingTaskPage: () -> Unit,
    onAbandonPendingTask: () -> Unit,
    onRefreshResults: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmAbandon by remember(pendingTask?.kind, pendingTask?.startedAtMillis) { mutableStateOf(false) }

    if (confirmAbandon && pendingTask != null) {
        PendingTaskAbandonDialog(
            task = pendingTask,
            onConfirm = {
                confirmAbandon = false
                onAbandonPendingTask()
            },
            onDismiss = { confirmAbandon = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = { Text("后台任务中心") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "这里读取启动器已经保存的任务记录和 Termux 返回，不会重新执行任务。切到后台期间产生的结果，回来后仍可在这里补查。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    pendingTask != null -> {
                        PendingTaskStateHeader(activeLockLabel = activeLockLabel)
                        PendingTaskSummaryCard(task = pendingTask)
                        ToneActionButton(
                            text = "检查最新结果",
                            enabled = true,
                            tone = ActionTone.Safe,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCheckPendingTask,
                        )
                        ToneActionButton(
                            text = "前往对应页面",
                            enabled = true,
                            tone = ActionTone.Neutral,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenPendingTaskPage,
                        )
                        ToneActionButton(
                            text = "不再跟踪这次操作",
                            enabled = true,
                            tone = ActionTone.Warning,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { confirmAbandon = true },
                        )
                    }

                    !activeLockLabel.isNullOrBlank() -> {
                        PendingTaskStateHeader(activeLockLabel = activeLockLabel)
                        Text(
                            text = "启动器当前仍在等待这个操作。离开页面不会取消 Termux 中的任务。",
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    else -> BackgroundTaskIdleCard()
                }

                Text(
                    text = "最近任务结果",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (recentResults.isEmpty()) {
                    Text(
                        text = "还没有可显示的耗时任务结果。",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    recentResults.forEach { result ->
                        BackgroundTaskResultCard(result = result)
                    }
                }
                ToneActionButton(
                    text = "刷新最近结果",
                    enabled = true,
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRefreshResults,
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
        dismissButton = {},
    )
}

@Composable
private fun BackgroundTaskIdleCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "当前没有待确认任务",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "新的备份、恢复、版本操作和实例目录任务会自动出现在这里。",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BackgroundTaskResultCard(result: TermuxResultDisplay) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = PendingLauncherTaskSupport.taskResultTitle(result),
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(
                    text = if (result.ok) "成功" else "失败",
                    active = result.ok,
                    toneColor = if (result.ok) LukoaColors.Primary else LukoaColors.Danger,
                    activeBackground = LukoaColors.Elevated,
                )
            }
            PendingTaskInfoLine("完成时间", formatPendingTaskTime(result.timeMillis))
            result.profileId.takeIf(String::isNotBlank)?.let {
                PendingTaskInfoLine("实例", it)
            }
        }
    }
}
