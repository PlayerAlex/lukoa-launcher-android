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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
internal fun ManagementDialogSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
        color = LukoaColors.TextPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun ManagementDialogIntroCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = LukoaColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun ManagementDialogActionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ManagementDialogSectionTitle(title)
        SettingsEntryGroup {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
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
internal fun TimedDangerFeedbackActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    unavailableHint: String? = null,
    onShowHint: (String) -> Unit = {},
    onConfirmed: () -> Unit,
) {
    var armed by rememberSaveable(text) { mutableStateOf(false) }
    LaunchedEffect(armed, enabled) {
        when {
            !enabled -> armed = false
            armed -> {
                delay(4_000L)
                armed = false
            }
        }
    }
    SettingsFeedbackActionButton(
        text = if (armed) "真的吗？" else text,
        enabled = enabled,
        accentColor = if (armed) LukoaColors.Danger else LukoaColors.Primary,
        unavailableHint = unavailableHint,
        onShowHint = onShowHint,
        modifier = modifier,
        onClick = {
            if (armed) {
                armed = false
                onConfirmed()
            } else {
                armed = true
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
    selected: Boolean = false,
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
    val containerColor = if (selected && enabled) LukoaColors.Primary else style.containerColor
    val borderColor = if (selected && enabled) LukoaColors.Primary else style.borderColor
    val contentColor = if (selected && enabled) LukoaColors.OnPrimary else style.contentColor
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(LukoaCapsuleShape)
            .background(containerColor)
            .border(1.dp, borderColor, LukoaCapsuleShape)
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
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected && enabled) FontWeight.Bold else FontWeight.SemiBold,
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

@Composable
internal fun LukoaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (focused) LukoaColors.Primary else LukoaColors.Border,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = LukoaColors.Dim,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled) LukoaColors.TextPrimary else LukoaColors.Dim,
                    ),
                    cursorBrush = SolidColor(LukoaColors.Primary),
                )
            }
            if (value.isNotEmpty() && enabled) {
                Text(
                    text = "×",
                    modifier = Modifier
                        .clickable(role = Role.Button) { onValueChange("") }
                        .padding(horizontal = 4.dp),
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun LukoaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(10.dp),
        label = label?.let { text -> { Text(text) } },
        placeholder = placeholder?.let { text ->
            {
                Text(
                    text = text,
                    color = LukoaColors.Dim,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = LukoaColors.TextPrimary,
            unfocusedTextColor = LukoaColors.TextPrimary,
            disabledTextColor = LukoaColors.Dim,
            focusedContainerColor = LukoaColors.Elevated,
            unfocusedContainerColor = LukoaColors.Elevated,
            disabledContainerColor = LukoaColors.Surface,
            focusedBorderColor = LukoaColors.Primary,
            unfocusedBorderColor = LukoaColors.Border,
            disabledBorderColor = LukoaColors.Border,
            focusedLabelColor = LukoaColors.Primary,
            unfocusedLabelColor = LukoaColors.TextSecondary,
            disabledLabelColor = LukoaColors.Dim,
            cursorColor = LukoaColors.Primary,
        ),
    )
}

@Composable
internal fun LukoaSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) {
                onCheckedChange(!checked)
            }
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
            Text(
                text = detail,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LukoaColors.Background,
                checkedTrackColor = LukoaColors.Primary,
                uncheckedThumbColor = LukoaColors.TextSecondary,
                uncheckedTrackColor = LukoaColors.Elevated,
                uncheckedBorderColor = LukoaColors.Border,
            ),
        )
    }
}

@Composable
internal fun LukoaCollapsibleCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsEntryGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = if (expanded) "收起 ▴" else "展开 ▾",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (expanded) {
            SettingsEntryDivider()
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}
