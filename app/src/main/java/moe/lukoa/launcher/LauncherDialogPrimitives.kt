package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
    AlertDialog(
        modifier = Modifier.widthIn(max = 330.dp),
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.DialogSurface,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = titleTone.color(),
        textContentColor = LukoaColors.Text,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = titleTone.color().copy(alpha = 0.12f),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (titleTone) {
                                ActionTone.Danger -> "!"
                                ActionTone.Warning -> "!"
                                ActionTone.Safe -> "↑"
                                ActionTone.Neutral -> "·"
                            },
                            color = titleTone.color(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
                Text(
                    text = title,
                    color = LukoaColors.Text,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } },
        confirmButton = {
            DialogActionButton(
                text = confirmText,
                enabled = confirmEnabled,
                tone = confirmTone,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(dismissText, tone = ActionTone.Neutral, onClick = onDismiss)
        },
    )
}

@Composable
internal fun LauncherDetailDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.widthIn(max = 330.dp),
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.DialogSurface,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = LukoaColors.Text,
        textContentColor = LukoaColors.Text,
        title = {
            Text(
                text = title,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                content()
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "关闭",
                enabled = true,
                accentColor = LukoaColors.Text,
                onClick = onDismiss,
            )
        },
    )
}
