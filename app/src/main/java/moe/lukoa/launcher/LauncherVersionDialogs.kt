package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun TavernVersionActionConfirmDialog(
    confirmation: TavernVersionActionConfirmation,
    actionsLocked: Boolean,
    onConfirm: (discardLocalChanges: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var discardConsent by remember(confirmation) { mutableStateOf(false) }
    val consentSatisfied = !confirmation.requiresDiscardConsent || discardConsent
    RiskyActionDialogScaffold(
        title = confirmation.kind.dialogTitle,
        titleTone = ActionTone.Safe,
        confirmText = confirmation.kind.confirmLabel,
        confirmTone = ActionTone.Safe,
        confirmEnabled = !actionsLocked && consentSatisfied,
        onConfirm = { onConfirm(confirmation.requiresDiscardConsent && discardConsent) },
        onDismiss = onDismiss,
    ) {
        Text(
            text = confirmation.summary,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
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
                VersionInfoLine("当前版本", confirmation.currentVersion)
                VersionInfoLine("目标版本", confirmation.targetVersion)
                VersionInfoLine("当前源", confirmation.sourceLabel)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Accent.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.28f)),
        ) {
            Text(
                text = confirmation.detail,
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (confirmation.requiresDiscardConsent) {
            LocalChangesConsentCard(
                confirmation = confirmation,
                checked = discardConsent,
                onCheckedChange = { discardConsent = it },
            )
        }
        Text(
            text = confirmation.riskTip,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LocalChangesConsentCard(
    confirmation: TavernVersionActionConfirmation,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val actionText = when (confirmation.kind) {
        TavernVersionActionKind.Update -> "更新"
        TavernVersionActionKind.Rollback -> "回退"
    }
    val shown = confirmation.userOwnedChanges.take(MAX_LISTED_LOCAL_CHANGES)
    val hiddenCount = confirmation.userOwnedChanges.size - shown.size
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("version-local-changes-consent"),
        color = LukoaColors.AccentSoft,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "酒馆程序文件有你自己改过的地方",
                color = LukoaColors.Accent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (shown.isEmpty()) {
                Text(
                    text = "这次没读到具体是哪些文件。",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                shown.forEach { file ->
                    Text(
                        text = file,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hiddenCount > 0) {
                    Text(
                        text = "还有 $hiddenCount 个文件",
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = "${actionText}前会把这些改动存进 Git 暂存区（stash），酒馆会以干净的官方文件继续。" +
                    "改动不会自动恢复，需要时可以在 Termux 里用 git stash 找回。安全备份里也有一份。",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag("version-local-changes-consent-checkbox"),
                    colors = CheckboxDefaults.colors(
                        checkedColor = LukoaColors.Accent,
                        uncheckedColor = LukoaColors.TextSecondary,
                    ),
                )
                Text(
                    text = "我知道了，先存起来再${actionText}",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private const val MAX_LISTED_LOCAL_CHANGES = 6
