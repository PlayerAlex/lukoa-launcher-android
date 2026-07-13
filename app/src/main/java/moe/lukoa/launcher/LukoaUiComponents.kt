package moe.lukoa.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
                    contentDescription = "查看说明"
                    role = Role.Button
                }
                .clickable(onClick = feedbackClick),
            color = Color.Transparent,
            shape = LukoaCapsuleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(18.dp),
                    color = LukoaColors.SurfaceAlt.copy(alpha = 0.75f),
                    shape = LukoaCapsuleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "?",
                            color = LukoaColors.Dim,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(top = 34.dp),
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clickable(onClick = { expanded = false }),
                color = LukoaColors.SurfaceAlt,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.28f)),
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun DashedSection(
    label: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedRoundBorder(
                    color = LukoaColors.Line.copy(alpha = 0.48f),
                    width = 1.dp,
                    radius = 16.dp,
                )
                .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
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
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val feedbackClick = onClick?.let { click -> rememberFeedbackClick(onClick = click) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (feedbackClick != null) Modifier.clickable(onClick = feedbackClick) else Modifier)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = LukoaColors.Dim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ChevronRight() {
    Text(
        text = "›",
        color = LukoaColors.Dim,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
fun DotStatusPill(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (active) LukoaColors.Accent else LukoaColors.Text
    val background = if (active) LukoaColors.AccentSoft else Color.White.copy(alpha = 0.06f)
    Surface(
        modifier = modifier.heightIn(min = 30.dp),
        color = background,
        shape = LukoaCapsuleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(color, LukoaCapsuleShape),
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
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
        topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = androidx.compose.ui.geometry.Size(
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
