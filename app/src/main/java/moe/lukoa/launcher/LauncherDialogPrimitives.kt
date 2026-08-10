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
import androidx.compose.material3.HorizontalDivider
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
internal fun RiskyActionDialogScaffold(
    title: String,
    titleTone: ActionTone,
    confirmText: String,
    confirmTone: ActionTone,
    confirmEnabled: Boolean = true,
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    LauncherActionDialogScaffold(
        title = title,
        titleTone = titleTone,
        confirmText = confirmText,
        confirmTone = confirmTone,
        confirmEnabled = confirmEnabled,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        content = content,
    )
}

@Composable
internal fun LauncherActionDialogScaffold(
    title: String,
    titleTone: ActionTone = ActionTone.Safe,
    confirmText: String? = null,
    confirmTone: ActionTone = ActionTone.Safe,
    confirmEnabled: Boolean = true,
    dismissText: String = "取消",
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        titleContentColor = titleTone.color(),
        textContentColor = LukoaColors.TextPrimary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                HorizontalDivider(color = LukoaColors.Border)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
            }
        },
        confirmButton = {
            if (!confirmText.isNullOrBlank() && onConfirm != null) {
                DialogActionButton(
                    text = confirmText,
                    enabled = confirmEnabled,
                    tone = confirmTone,
                    onClick = onConfirm,
                )
            }
        },
        dismissButton = {
            DialogActionButton(dismissText, tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}

@Composable
internal fun LauncherDialogSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
internal fun LauncherDialogBulletList(items: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            Text(
                text = "• $item",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
