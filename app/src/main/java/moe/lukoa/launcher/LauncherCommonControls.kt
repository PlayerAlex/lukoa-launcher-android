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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val LukoaCapsuleShape = RoundedCornerShape(999.dp)

enum class ActionTone {
    Safe,
    Warning,
    Danger,
    Neutral,
}

fun ActionTone.color(): Color = when (this) {
    ActionTone.Safe -> LukoaColors.Accent
    ActionTone.Warning -> LukoaColors.Amber
    ActionTone.Danger -> LukoaColors.Danger
    ActionTone.Neutral -> LukoaColors.Info
}

@Composable
fun rememberFeedbackClick(
    onClick: () -> Unit,
    minIntervalMs: Long = 260L,
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    var lastClickAt by remember { mutableLongStateOf(0L) }
    return remember(onClick, haptic, minIntervalMs) {
        {
            val now = SystemClock.elapsedRealtime()
            if (minIntervalMs <= 0L || now - lastClickAt >= minIntervalMs) {
                lastClickAt = now
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        }
    }
}

@Composable
fun InfoIconButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LukoaColors.Muted,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    Surface(
        modifier = modifier
            .size(24.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = feedbackClick),
        color = LukoaColors.SurfaceAlt,
        shape = LukoaCapsuleShape,
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.5f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "!",
                color = accentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ToneActionButton(
    text: String,
    enabled: Boolean,
    tone: ActionTone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SecondaryActionButton(
        text = text,
        enabled = enabled,
        accentColor = tone.color(),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun DialogActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ActionTone = ActionTone.Safe,
    onClick: () -> Unit,
) {
    ToneActionButton(
        text = text,
        enabled = enabled,
        tone = tone,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun SecondaryActionButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    val styleColor = accentColor
    val toneColor = if (enabled) styleColor else LukoaColors.Dim
    val borderColor = if (enabled) styleColor.copy(alpha = 0.3f) else LukoaColors.Line.copy(alpha = 0.3f)
    OutlinedButton(
        onClick = feedbackClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        border = BorderStroke(1.dp, borderColor),
        shape = LukoaCapsuleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) styleColor.copy(alpha = 0.05f) else Color.Transparent,
            contentColor = toneColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = LukoaColors.Dim,
        ),
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun BackupStepper(
    label: String,
    value: String,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    accentColor: Color = LukoaColors.Info,
    onDecreaseLarge: (() -> Unit)? = null,
    onIncreaseLarge: (() -> Unit)? = null,
) {
    val hasLargeStep = onDecreaseLarge != null || onIncreaseLarge != null
    val buttonWidth = if (hasLargeStep) 44.dp else 52.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = value,
                    color = accentColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (hasLargeStep) 6.dp else 8.dp),
            ) {
                if (onDecreaseLarge != null) {
                    StepperButton(
                        text = "--",
                        enabled = enabled,
                        accentColor = accentColor,
                        modifier = Modifier.width(buttonWidth),
                        onClick = onDecreaseLarge,
                    )
                }
                StepperButton(
                    text = "-",
                    enabled = enabled,
                    accentColor = accentColor,
                    modifier = Modifier.width(buttonWidth),
                    onClick = onDecrease,
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    color = accentColor.copy(alpha = if (enabled) 0.08f else 0.04f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.2f else 0.1f)),
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        color = if (enabled) LukoaColors.Text else LukoaColors.Dim,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StepperButton(
                    text = "+",
                    enabled = enabled,
                    accentColor = accentColor,
                    modifier = Modifier.width(buttonWidth),
                    onClick = onIncrease,
                )
                if (onIncreaseLarge != null) {
                    StepperButton(
                        text = "++",
                        enabled = enabled,
                        accentColor = accentColor,
                        modifier = Modifier.width(buttonWidth),
                        onClick = onIncreaseLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    val styleColor = accentColor
    val toneColor = if (enabled) styleColor else LukoaColors.Dim
    OutlinedButton(
        onClick = feedbackClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        border = BorderStroke(1.dp, if (enabled) styleColor.copy(alpha = 0.3f) else LukoaColors.Line.copy(alpha = 0.4f)),
        shape = LukoaCapsuleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) styleColor.copy(alpha = 0.05f) else Color.Transparent,
            contentColor = toneColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = LukoaColors.Dim,
        ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    toneColor: Color = if (active) LukoaColors.Accent else LukoaColors.Muted,
    activeBackground: Color = LukoaColors.AccentSoft,
) {
    val shape = RoundedCornerShape(999.dp)
    val background = if (active) activeBackground else LukoaColors.SurfaceAlt
    val contentColor = if (active) toneColor else LukoaColors.Muted
    val borderColor = if (active) toneColor.copy(alpha = 0.3f) else Color.Transparent
    Surface(
        modifier = modifier
            .heightIn(min = 32.dp)
            .border(1.dp, borderColor, shape),
        color = if (active) background.copy(alpha = 0.8f) else background.copy(alpha = 0.5f),
        shape = shape,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
