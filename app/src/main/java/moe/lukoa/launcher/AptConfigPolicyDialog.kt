package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AptConfigPolicyDialog(
    pendingTask: PendingAptConfigTask,
    actionsLocked: Boolean,
    onChoose: (AptConfigPolicy) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = { Text("遇到配置文件冲突时怎么办") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${pendingTask.task.actionLabel} 可能会升级 Termux 包。若遇到 openssl.cnf 这类配置文件冲突，需要提前选择处理方式。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "推荐保留当前配置。这样最不容易把 Termux 里已有设置覆盖掉。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "只有你明确想恢复软件包默认配置时，才选择使用新版配置。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogActionButton(
                    text = AptConfigPolicy.KeepCurrent.label,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionsLocked,
                    tone = ActionTone.Safe,
                    onClick = { onChoose(AptConfigPolicy.KeepCurrent) },
                )
                DialogActionButton(
                    text = AptConfigPolicy.UsePackageVersion.label,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !actionsLocked,
                    tone = ActionTone.Warning,
                    onClick = { onChoose(AptConfigPolicy.UsePackageVersion) },
                )
                DialogActionButton(
                    text = "取消",
                    modifier = Modifier.fillMaxWidth(),
                    tone = ActionTone.Neutral,
                    onClick = onDismiss,
                )
            }
        },
        dismissButton = null,
    )
}
