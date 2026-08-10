package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal enum class SettingsValueLayout {
    Trailing,
    Supporting,
}

@Composable
internal fun SettingsDialogTitle(
    title: String,
    infoText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
        )
        InfoPopoverButton(
            contentDescription = "$title 说明",
            title = title,
            body = infoText,
        )
    }
}

@Composable
internal fun SettingsEntryGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroupLabel(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        color = LukoaColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun SettingsEntryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = LukoaColors.Border,
    )
}

@Composable
internal fun SettingsEntryRow(
    title: String,
    detail: String? = null,
    detailMaxLines: Int = 2,
    value: String? = null,
    valueColor: Color = LukoaColors.TextPrimary,
    valueLayout: SettingsValueLayout = SettingsValueLayout.Trailing,
    valueAsPill: Boolean = false,
    highlightColor: Color? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    unavailableHint: String? = null,
    onShowHint: ((String) -> Unit)? = null,
) {
    val canShowUnavailableHint = !unavailableHint.isNullOrBlank() && onShowHint != null
    val hasInteraction = (enabled && onClick != null) || canShowUnavailableHint
    val feedbackClick = rememberFeedbackClick(onClick = {
        if (enabled && onClick != null) {
            onClick()
        } else if (canShowUnavailableHint) {
            onShowHint(unavailableHint.orEmpty())
        }
    })
    val interactionModifier = if (hasInteraction) {
        Modifier.clickable(
            role = Role.Button,
            onClick = feedbackClick,
        )
    } else {
        Modifier
    }
    val backgroundModifier = highlightColor?.let { color ->
        Modifier.background(color.copy(alpha = 0.08f))
    } ?: Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(backgroundModifier)
            .semantics(mergeDescendants = true) {
                if (!enabled) disabled()
            }
            .then(interactionModifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = if (enabled) LukoaColors.TextPrimary else LukoaColors.Dim,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (valueLayout == SettingsValueLayout.Supporting && value != null) {
                Text(
                    text = value,
                    color = if (enabled) valueColor else LukoaColors.Dim,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = detailMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (valueLayout == SettingsValueLayout.Trailing && value != null) {
            SettingsTrailingValue(
                value = value,
                color = if (enabled || valueAsPill) valueColor else LukoaColors.Dim,
                asPill = valueAsPill,
            )
        }
        if (onClick != null) {
            Text(
                text = "›",
                modifier = Modifier.clearAndSetSemantics {},
                color = if (enabled) LukoaColors.TextSecondary else LukoaColors.Dim,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
internal fun SettingsFeedbackActionButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    unavailableHint: String?,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val canShowUnavailableHint = !enabled && !unavailableHint.isNullOrBlank()
    SecondaryActionButton(
        text = text,
        enabled = enabled || canShowUnavailableHint,
        accentColor = if (enabled) accentColor else LukoaColors.Dim,
        modifier = modifier.semantics {
            if (!enabled) disabled()
        },
        onClick = {
            if (enabled) {
                onClick()
            } else if (canShowUnavailableHint) {
                onShowHint(unavailableHint.orEmpty())
            }
        },
    )
}

@Composable
internal fun SettingsCompactChoiceButton(
    text: String,
    enabled: Boolean,
    accentColor: Color,
    unavailableHint: String?,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val canShowUnavailableHint = !enabled && !unavailableHint.isNullOrBlank()
    val acceptsClick = enabled || canShowUnavailableHint
    val feedbackClick = rememberFeedbackClick(onClick = {
        if (enabled) {
            onClick()
        } else if (canShowUnavailableHint) {
            onShowHint(unavailableHint.orEmpty())
        }
    })
    val style = resolveSecondaryActionStyle(
        enabled = acceptsClick,
        accentColor = if (enabled) accentColor else LukoaColors.Dim,
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(LukoaCapsuleShape)
            .background(style.containerColor)
            .border(1.dp, style.borderColor, LukoaCapsuleShape)
            .clickable(
                enabled = acceptsClick,
                role = Role.Button,
                onClick = feedbackClick,
            )
            .semantics {
                if (!enabled) disabled()
            }
            .padding(horizontal = 2.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = style.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsTrailingValue(
    value: String,
    color: Color,
    asPill: Boolean,
) {
    if (asPill) {
        Surface(
            modifier = Modifier.widthIn(max = 148.dp),
            color = color.copy(alpha = 0.10f),
            shape = LukoaCapsuleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Text(
            text = value,
            modifier = Modifier.widthIn(max = 148.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SettingsSectionDivider() {
    HorizontalDivider(color = LukoaColors.Border)
}

@Composable
internal fun SettingsSubsection(
    title: String,
    detail: String,
    statusText: String? = null,
    statusTone: Color = LukoaColors.Primary,
    statusActive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (!statusText.isNullOrBlank()) {
                StatusPill(
                    text = statusText,
                    active = statusActive,
                    toneColor = statusTone,
                    activeBackground = statusTone.copy(alpha = 0.16f),
                )
            }
            InfoPopoverButton(
                contentDescription = "查看$title 说明",
                title = title,
                body = detail,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
