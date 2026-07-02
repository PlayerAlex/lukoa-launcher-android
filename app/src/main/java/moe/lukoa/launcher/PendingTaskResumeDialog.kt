package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("检测到上次任务没收尾")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "上次正在${task.title}，这次可以直接继续检查结果，或者放弃这次任务记录。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                        PendingTaskInfoLine("任务", task.title)
                        task.detail.takeIf { it.isNotBlank() }?.let { PendingTaskInfoLine("阶段", it) }
                        task.targetLabel.takeIf { it.isNotBlank() }?.let { PendingTaskInfoLine("目标", it) }
                        PendingTaskInfoLine("开始时间", formatPendingTaskTime(task.startedAtMillis))
                        task.safetyBackupPath.takeIf { it.isNotBlank() }?.let { PendingTaskInfoLine("安全备份", it) }
                        task.archivePath.takeIf { it.isNotBlank() }?.let { PendingTaskInfoLine("备份包", it) }
                    }
                }
                activeLockLabel?.takeIf { it.isNotBlank() }?.let {
                    Surface(
                        color = LukoaColors.AmberSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = "系统里还记着一个进行中的步骤：$it。Termux 可能还在继续跑，先点“继续检查”最稳。",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = LukoaColors.Amber,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "继续检查",
                tone = ActionTone.Safe,
                onClick = onContinueCheck,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "放弃本次任务",
                tone = ActionTone.Warning,
                onClick = onAbandon,
            )
        },
    )
}

@Composable
private fun PendingTaskInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 12.dp),
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatPendingTaskTime(timeMillis: Long): String {
    if (timeMillis <= 0L) return "刚刚"
    return PENDING_TASK_TIME_FORMATTER.format(
        Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()),
    )
}

private val PENDING_TASK_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
