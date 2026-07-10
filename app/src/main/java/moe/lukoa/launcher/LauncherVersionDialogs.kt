package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = confirmation.kind.dialogTitle,
        titleTone = ActionTone.Warning,
        confirmText = confirmation.kind.confirmLabel,
        confirmTone = ActionTone.Warning,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = confirmation.summary,
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
                VersionInfoLine("当前版本", confirmation.currentVersion)
                VersionInfoLine("目标版本", confirmation.targetVersion)
                VersionInfoLine("当前源", confirmation.sourceLabel)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Amber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
        ) {
            Text(
                text = confirmation.detail,
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = confirmation.riskTip,
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
