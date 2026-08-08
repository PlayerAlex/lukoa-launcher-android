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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
    ActionTone.Safe -> LukoaColors.Primary
    ActionTone.Warning -> LukoaColors.Accent
    ActionTone.Danger -> LukoaColors.Danger
    ActionTone.Neutral -> LukoaColors.TextSecondary
}

internal data class SecondaryActionStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
)

internal fun resolveSecondaryActionStyle(
    enabled: Boolean,
    accentColor: Color,
): SecondaryActionStyle {
    if (!enabled) {
        return SecondaryActionStyle(
            containerColor = Color.Transparent,
            contentColor = LukoaColors.Dim,
            borderColor = LukoaColors.Border,
        )
    }

    return when (accentColor) {
        LukoaColors.Primary -> SecondaryActionStyle(
            containerColor = Color.Transparent,
            contentColor = LukoaColors.TextPrimary,
            borderColor = LukoaColors.Border,
        )
        LukoaColors.TextSecondary,
        LukoaColors.Dim,
        -> SecondaryActionStyle(
            containerColor = Color.Transparent,
            contentColor = accentColor,
            borderColor = LukoaColors.Border,
        )
        LukoaColors.Accent -> SecondaryActionStyle(
            containerColor = LukoaColors.AccentSoft,
            contentColor = LukoaColors.Accent,
            borderColor = LukoaColors.Accent,
        )
        LukoaColors.Danger -> SecondaryActionStyle(
            containerColor = LukoaColors.DangerSoft,
            contentColor = LukoaColors.Danger,
            borderColor = LukoaColors.Danger,
        )
        LukoaColors.Stop -> SecondaryActionStyle(
            containerColor = LukoaColors.DangerSoft,
            contentColor = LukoaColors.Stop,
            borderColor = LukoaColors.Stop,
        )
        else -> SecondaryActionStyle(
            containerColor = Color.Transparent,
            contentColor = accentColor,
            borderColor = LukoaColors.Border,
        )
    }
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
    accentColor: Color = LukoaColors.TextSecondary,
    onClick: () -> Unit,
) {
    val feedbackClick = rememberFeedbackClick(onClick)
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = feedbackClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = LukoaColors.Elevated,
            shape = LukoaCapsuleShape,
            border = BorderStroke(1.dp, LukoaColors.Border),
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
}

@Composable
fun InfoPopoverButton(
    contentDescription: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(title, body) { mutableStateOf(false) }

    Box(modifier = modifier) {
        InfoIconButton(
            contentDescription = contentDescription,
            accentColor = LukoaColors.TextSecondary,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
            containerColor = LukoaColors.Elevated,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                body.lineSequence()
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .forEach { paragraph ->
                        Text(
                            text = paragraph,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
            }
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
    val style = resolveSecondaryActionStyle(
        enabled = enabled,
        accentColor = accentColor,
    )
    OutlinedButton(
        onClick = feedbackClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        border = BorderStroke(1.dp, style.borderColor),
        shape = LukoaCapsuleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = style.containerColor,
            contentColor = style.contentColor,
            disabledContainerColor = style.containerColor,
            disabledContentColor = style.contentColor,
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
    accentColor: Color = LukoaColors.Primary,
    onDecreaseLarge: (() -> Unit)? = null,
    onIncreaseLarge: (() -> Unit)? = null,
) {
    val hasLargeStep = onDecreaseLarge != null || onIncreaseLarge != null
    val buttonWidth = if (hasLargeStep) 44.dp else 52.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
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
                    color = LukoaColors.TextSecondary,
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
                        color = if (enabled) LukoaColors.TextPrimary else LukoaColors.Dim,
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
        border = BorderStroke(1.dp, if (enabled) styleColor.copy(alpha = 0.3f) else LukoaColors.Border),
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
    toneColor: Color = if (active) LukoaColors.Primary else LukoaColors.TextSecondary,
    activeBackground: Color = LukoaColors.PrimarySoft,
) {
    val shape = RoundedCornerShape(999.dp)
    val background = resolvedStatusPillBackground(
        active = active,
        requestedBackground = if (active) activeBackground else LukoaColors.Elevated,
    )
    val contentColor = if (active) toneColor else LukoaColors.TextSecondary
    val borderColor = if (active) toneColor.copy(alpha = 0.3f) else Color.Transparent
    Surface(
        modifier = modifier
            .heightIn(min = 32.dp)
            .border(1.dp, borderColor, shape),
        color = background,
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

internal fun resolvedStatusPillBackground(
    active: Boolean,
    requestedBackground: Color,
): Color {
    val alpha = if (active) {
        (requestedBackground.alpha * 0.82f).coerceIn(0.12f, 0.82f)
    } else {
        requestedBackground.alpha * 0.5f
    }
    return requestedBackground.copy(alpha = alpha)
}

@Composable
fun NoticeCard(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LukoaColors.Accent,
    actionLabel: String? = null,
    actionTone: ActionTone = ActionTone.Warning,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = detail,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                DialogActionButton(
                    text = actionLabel,
                    tone = actionTone,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAction,
                )
            }
        }
    }
}
