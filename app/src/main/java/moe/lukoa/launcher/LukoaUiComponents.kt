package moe.lukoa.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LukoaPillTone {
    Accent,
    Idle,
    Warning,
    Danger,
    Muted,
}

@Composable
fun HelpHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val feedbackClick = rememberFeedbackClick(onClick = { expanded = !expanded })
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = "查看这项功能的说明"
                    role = Role.Button
                }
                .clickable(onClick = feedbackClick),
            color = Color.Transparent,
            shape = LukoaCapsuleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(18.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    shape = LukoaCapsuleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "?",
                            color = LukoaColors.Dim,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 280.dp),
            containerColor = LukoaColors.DialogSurface,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun DashedSection(
    label: String,
    modifier: Modifier = Modifier,
    headerLeading: (@Composable () -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 14.dp),
    verticalSpacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedRoundBorder(
                    color = Color.White.copy(alpha = 0.10f),
                    width = 1.dp,
                    radius = 16.dp,
                )
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            content()
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp)
                .background(LukoaColors.Background)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            headerLeading?.invoke()
            Text(
                text = label,
                color = LukoaColors.Dim,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            headerAction?.invoke()
        }
    }
}

@Composable
fun LukoaRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleColor: Color = LukoaColors.Text,
    detailColor: Color = LukoaColors.Dim,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val feedbackClick = onClick?.let { click -> rememberFeedbackClick(onClick = click) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (feedbackClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = feedbackClick)
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.46f)
            .padding(horizontal = 2.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = detailColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun LukoaRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = LukoaColors.Divider,
    )
}

@Composable
fun RowIcon(
    symbol: String,
    modifier: Modifier = Modifier,
    color: Color = LukoaColors.Text,
) {
    Box(
        modifier = modifier.size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
            val w = size.width
            val h = size.height
            when (symbol) {
                "!" -> {
                    drawCircle(color = color, radius = w * 0.43f, style = stroke)
                    drawLine(color, Offset(w / 2f, h * 0.28f), Offset(w / 2f, h * 0.58f), stroke.width, StrokeCap.Round)
                    drawCircle(color = color, radius = stroke.width * 0.62f, center = Offset(w / 2f, h * 0.73f))
                }
                "✓" -> {
                    drawCircle(color = color, radius = w * 0.43f, style = stroke)
                    val check = Path().apply {
                        moveTo(w * 0.27f, h * 0.51f)
                        lineTo(w * 0.43f, h * 0.66f)
                        lineTo(w * 0.73f, h * 0.34f)
                    }
                    drawPath(check, color = color, style = stroke)
                }
                "↑" -> {
                    drawLine(color, Offset(w / 2f, h * 0.16f), Offset(w / 2f, h * 0.7f), stroke.width, StrokeCap.Round)
                    drawLine(color, Offset(w / 2f, h * 0.16f), Offset(w * 0.3f, h * 0.37f), stroke.width, StrokeCap.Round)
                    drawLine(color, Offset(w / 2f, h * 0.16f), Offset(w * 0.7f, h * 0.37f), stroke.width, StrokeCap.Round)
                    drawLine(color, Offset(w * 0.25f, h * 0.82f), Offset(w * 0.75f, h * 0.82f), stroke.width, StrokeCap.Round)
                }
                "◷" -> {
                    drawCircle(color = color, radius = w * 0.43f, style = stroke)
                    drawLine(color, Offset(w / 2f, h / 2f), Offset(w / 2f, h * 0.27f), stroke.width, StrokeCap.Round)
                    drawLine(color, Offset(w / 2f, h / 2f), Offset(w * 0.68f, h * 0.61f), stroke.width, StrokeCap.Round)
                }
                "♙" -> {
                    drawCircle(color = color, radius = w * 0.18f, center = Offset(w / 2f, h * 0.31f), style = stroke)
                    drawArc(color = color, startAngle = 205f, sweepAngle = 130f, useCenter = false, topLeft = Offset(w * 0.2f, h * 0.48f), size = Size(w * 0.6f, h * 0.45f), style = stroke)
                }
                "▦" -> {
                    drawRoundRect(color = color, cornerRadius = CornerRadius(3.dp.toPx()), style = stroke)
                    drawLine(color, Offset(w * 0.38f, 0f), Offset(w * 0.38f, h), stroke.width, StrokeCap.Round)
                    drawLine(color, Offset(0f, h * 0.5f), Offset(w * 0.38f, h * 0.5f), stroke.width, StrokeCap.Round)
                }
                "◇", "♢" -> {
                    val diamond = Path().apply {
                        moveTo(w / 2f, h * 0.08f)
                        lineTo(w * 0.88f, h / 2f)
                        lineTo(w / 2f, h * 0.92f)
                        lineTo(w * 0.12f, h / 2f)
                        close()
                    }
                    drawPath(diamond, color = color, style = stroke)
                    if (symbol == "♢") {
                        drawCircle(color = color, radius = w * 0.09f, style = stroke)
                    }
                }
                "▣" -> {
                    drawRoundRect(color = color, cornerRadius = CornerRadius(3.dp.toPx()), style = stroke)
                    listOf(0.3f, 0.5f, 0.7f).forEach { x ->
                        drawCircle(color = color, radius = stroke.width * 0.55f, center = Offset(w * x, h * 0.45f))
                    }
                }
                "⌁" -> {
                    drawArc(color = color, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.08f, h * 0.1f), size = Size(w * 0.84f, h * 0.65f), style = stroke)
                    drawLine(color, Offset(w * 0.18f, h * 0.78f), Offset(w * 0.82f, h * 0.78f), stroke.width, StrokeCap.Round)
                }
                else -> drawCircle(color = color, radius = w * 0.4f, style = stroke)
            }
        }
    }
}

@Composable
fun ChevronRight(color: Color = LukoaColors.Dim) {
    Text(
        text = "›",
        color = color,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )
}

@Composable
fun LukoaPill(
    text: String,
    tone: LukoaPillTone,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    val contentColor = when (tone) {
        LukoaPillTone.Accent -> LukoaColors.Accent
        LukoaPillTone.Idle -> LukoaColors.Text
        LukoaPillTone.Warning -> LukoaColors.Amber
        LukoaPillTone.Danger -> LukoaColors.Danger
        LukoaPillTone.Muted -> LukoaColors.Muted
    }
    val background = when (tone) {
        LukoaPillTone.Accent -> LukoaColors.AccentSoft
        LukoaPillTone.Idle -> Color.White.copy(alpha = 0.06f)
        LukoaPillTone.Warning -> LukoaColors.AmberSoft
        LukoaPillTone.Danger -> LukoaColors.DangerSoft
        LukoaPillTone.Muted -> Color.White.copy(alpha = 0.04f)
    }
    Surface(
        modifier = modifier.heightIn(min = 28.dp),
        color = background,
        shape = LukoaCapsuleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(contentColor, LukoaCapsuleShape),
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DotStatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    LukoaPill(
        text = text,
        tone = if (active) LukoaPillTone.Accent else LukoaPillTone.Idle,
        modifier = modifier,
        showDot = true,
    )
}

@Composable
fun StateNote(
    text: String,
    modifier: Modifier = Modifier,
    tone: LukoaPillTone = LukoaPillTone.Muted,
    emphasizePrefix: String? = null,
) {
    val background = when (tone) {
        LukoaPillTone.Accent -> LukoaColors.AccentSoft
        LukoaPillTone.Warning -> LukoaColors.AmberSoft
        LukoaPillTone.Danger -> LukoaColors.DangerSoft
        else -> Color.White.copy(alpha = 0.035f)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append(text)
                emphasizePrefix
                    ?.takeIf { text.startsWith(it) }
                    ?.let { prefix ->
                        addStyle(
                            SpanStyle(
                                color = LukoaColors.Text,
                                fontWeight = FontWeight.Bold,
                            ),
                            start = 0,
                            end = prefix.length,
                        )
                    }
            },
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            color = LukoaColors.Muted,
            fontSize = 12.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
fun LukoaToggle(
    checked: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onCheckedChange: () -> Unit,
) {
    val click = rememberFeedbackClick(onClick = onCheckedChange)
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 25.dp)
            .alpha(if (enabled) 1f else 0.46f)
            .background(
                color = if (checked) LukoaColors.Accent else Color.White.copy(alpha = 0.10f),
                shape = LukoaCapsuleShape,
            )
            .clickable(enabled = enabled, role = Role.Switch, onClick = click)
            .padding(2.5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (checked) Color.White else LukoaColors.Dim,
                    shape = LukoaCapsuleShape,
                ),
        )
    }
}

@Composable
fun SelectedCheckMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(22.dp),
        color = LukoaColors.Accent,
        shape = LukoaCapsuleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "✓",
                color = LukoaColors.AccentDark,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun KeyValueLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LukoaColors.Text,
    monospace: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = LukoaColors.Dim,
            fontSize = 12.5.sp,
            lineHeight = 21.sp,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = valueColor,
            fontSize = 12.5.sp,
            lineHeight = 21.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End,
        )
    }
}

private fun Modifier.dashedRoundBorder(
    color: Color,
    width: Dp,
    radius: Dp,
): Modifier = drawBehind {
    val strokeWidth = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        ),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}
