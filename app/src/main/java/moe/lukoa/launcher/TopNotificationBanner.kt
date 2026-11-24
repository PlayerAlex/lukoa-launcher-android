package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class LukoaTopBannerTone {
    Neutral,
    Success,
    Warning,
    Danger,
}

data class LukoaTopBannerMessage(
    val id: Long,
    val title: String,
    val detail: String = "",
    val tone: LukoaTopBannerTone = LukoaTopBannerTone.Neutral,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val autoDismissMs: Long = 3_800L,
    val dedupeKey: String? = null,
)

class LukoaTopBannerController {
    private var nextId: Long = 1L

    var messages by mutableStateOf<List<LukoaTopBannerMessage>>(emptyList())
        private set

    val currentMessage: LukoaTopBannerMessage?
        get() = messages.lastOrNull()

    fun show(
        title: String,
        detail: String = "",
        tone: LukoaTopBannerTone = inferTopBannerTone(title, detail),
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        autoDismissMs: Long = if (actionLabel != null) 7_500L else 3_600L,
        dedupeKey: String? = null,
    ): Long {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return currentMessage?.id ?: 0L
        val cleanDetail = detail.trim()

        if (dedupeKey != null) {
            val existingSameKey = messages.firstOrNull { it.dedupeKey == dedupeKey }
            if (
                existingSameKey != null &&
                existingSameKey.title == cleanTitle &&
                existingSameKey.detail == cleanDetail
            ) {
                return existingSameKey.id
            }
        }

        val assignedId = nextId++
        val nextMessage = LukoaTopBannerMessage(
            id = assignedId,
            title = cleanTitle,
            detail = cleanDetail,
            tone = tone,
            actionLabel = actionLabel?.trim()?.takeIf { it.isNotEmpty() },
            onAction = onAction,
            autoDismissMs = autoDismissMs.coerceAtLeast(0L),
            dedupeKey = dedupeKey,
        )
        val filtered = if (dedupeKey != null) {
            messages.filterNot { it.dedupeKey == dedupeKey }
        } else {
            messages
        }
        messages = (filtered + nextMessage).takeLast(MAX_VISIBLE_MESSAGES)
        return assignedId
    }

    fun dismiss(id: Long? = null) {
        if (messages.isEmpty()) return
        messages = if (id == null) {
            emptyList()
        } else {
            messages.filterNot { it.id == id }
        }
    }

    companion object {
        const val MAX_VISIBLE_MESSAGES = 4
    }
}

val LocalLukoaTopBanner = staticCompositionLocalOf<LukoaTopBannerController?> { null }

internal fun inferTopBannerTone(title: String, detail: String = ""): LukoaTopBannerTone {
    val text = "$title $detail"
    return when {
        listOf("失败", "错误", "报错", "异常", "不合法", "找不到", "没找到", "无法", "已被占用", "冲突").any { text.contains(it) } ->
            LukoaTopBannerTone.Danger
        listOf("待处理", "提醒", "尚未", "还没", "未授予", "先停止", "先安装", "请先", "暂不支持", "再按一次").any { text.contains(it) } ->
            LukoaTopBannerTone.Warning
        listOf("成功", "已保存", "已复制", "已开启", "已切换", "已救回", "已完成", "已清理", "已刷新", "已添加", "已请求").any { text.contains(it) } ->
            LukoaTopBannerTone.Success
        else -> LukoaTopBannerTone.Neutral
    }
}

@Composable
fun LukoaTopBannerHost(
    controller: LukoaTopBannerController,
    busyLabel: String? = null,
    busyStartedAtMillis: Long = 0L,
    pendingTask: PendingLauncherTask? = null,
    onContinuePendingTask: (() -> Unit)? = null,
    onOpenPendingTaskDialog: (() -> Unit)? = null,
    inSecondaryPage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val activeMessages = controller.messages

    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTopPadding = maxOf(cutoutTop, statusTop, 36.dp) + if (inSecondaryPage) 62.dp else 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = safeTopPadding, end = 12.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = !busyLabel.isNullOrBlank(),
            enter = slideInVertically(animationSpec = tween(200)) { -it } + fadeIn(tween(160)),
            exit = slideOutVertically(animationSpec = tween(160)) { -it } + fadeOut(tween(140)),
        ) {
            if (!busyLabel.isNullOrBlank()) {
                FloatingBusyStatusBar(
                    label = busyLabel,
                    startedAtMillis = busyStartedAtMillis,
                )
            }
        }

        AnimatedVisibility(
            visible = busyLabel.isNullOrBlank() && pendingTask != null && activeMessages.isEmpty(),
            enter = slideInVertically(animationSpec = tween(200)) { -it } + fadeIn(tween(160)),
            exit = slideOutVertically(animationSpec = tween(160)) { -it } + fadeOut(tween(140)),
        ) {
            pendingTask?.let { task ->
                FloatingPendingTaskBar(
                    task = task,
                    onContinue = onContinuePendingTask,
                    onOpenDetails = onOpenPendingTaskDialog,
                )
            }
        }

        activeMessages.forEach { activeMessage ->
            key(activeMessage.id) {
                LaunchedEffect(activeMessage.id) {
                    if (activeMessage.autoDismissMs > 0L) {
                        delay(activeMessage.autoDismissMs)
                        controller.dismiss(activeMessage.id)
                    }
                }
                val enterProgress = remember(activeMessage.id) { Animatable(0f) }
                LaunchedEffect(activeMessage.id) {
                    enterProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = enterProgress.value
                            alpha = p.coerceIn(0f, 1f)
                            translationY = (1f - p) * -18f
                        },
                ) {
                    TopBannerNotificationCard(
                        message = activeMessage,
                        onAction = {
                            val callback = activeMessage.onAction
                            controller.dismiss(activeMessage.id)
                            callback?.invoke()
                        },
                        onDismiss = { controller.dismiss(activeMessage.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedTopBannerShell(
    title: String,
    detail: String = "",
    tone: LukoaTopBannerTone = LukoaTopBannerTone.Neutral,
    titleMaxLines: Int = 2,
    modifier: Modifier = Modifier,
    bottomProgress: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val containerColor = if (tone == LukoaTopBannerTone.Danger) {
        LukoaColors.DangerSoft
    } else {
        LukoaColors.Elevated
    }
    val borderColor = if (tone == LukoaTopBannerTone.Danger) {
        LukoaColors.Danger.copy(alpha = 0.50f)
    } else {
        LukoaColors.Border
    }
    val titleColor = if (tone == LukoaTopBannerTone.Warning) {
        LukoaColors.Accent
    } else {
        LukoaColors.TextPrimary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = TOP_BANNER_SHAPE,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        color = titleColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                trailing()
            }
            if (bottomProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = LukoaColors.Primary,
                    trackColor = LukoaColors.Surface,
                )
            }
        }
    }
}

@Composable
private fun TopBannerActionChip(
    text: String,
    emphasized: Boolean = true,
    danger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor = when {
        danger -> LukoaColors.Danger.copy(alpha = 0.22f)
        emphasized -> LukoaColors.PrimarySoft
        else -> LukoaColors.Surface
    }
    val borderColor = if (danger) LukoaColors.Danger.copy(alpha = 0.55f) else LukoaColors.Border
    val contentColor = when {
        danger -> LukoaColors.Danger
        emphasized -> LukoaColors.Primary
        else -> LukoaColors.TextSecondary
    }
    val clickableModifier = if (onClick != null) {
        modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier,
        color = backgroundColor,
        shape = TOP_BANNER_ACTION_SHAPE,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TopBannerNotificationCard(
    message: LukoaTopBannerMessage,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    var cumulativeDragY by remember(message.id) { mutableFloatStateOf(0f) }

    UnifiedTopBannerShell(
        title = message.title,
        detail = message.detail,
        tone = message.tone,
        modifier = Modifier
            .testTag("top-notification-banner")
            .pointerInput(message.id) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (cumulativeDragY < -28f) {
                            onDismiss()
                        }
                        cumulativeDragY = 0f
                    },
                    onDragCancel = { cumulativeDragY = 0f },
                ) { _, dragAmount ->
                    cumulativeDragY += dragAmount
                    if (cumulativeDragY < -44f) {
                        onDismiss()
                    }
                }
            },
        trailing = {
            if (!message.actionLabel.isNullOrBlank() && message.onAction != null) {
                TopBannerActionChip(
                    text = message.actionLabel,
                    emphasized = true,
                    danger = message.tone == LukoaTopBannerTone.Danger,
                    modifier = Modifier.testTag("top-notification-banner-action"),
                    onClick = onAction,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .testTag("top-notification-banner-dismiss")
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭提示",
                    tint = LukoaColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

@Composable
private fun FloatingBusyStatusBar(
    label: String,
    startedAtMillis: Long,
) {
    var nowMillis by remember(label, startedAtMillis) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(label, startedAtMillis) {
        while (true) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    val elapsedSeconds = if (startedAtMillis > 0L) {
        ((nowMillis - startedAtMillis).coerceAtLeast(0L) / 1000L).toInt()
    } else {
        0
    }
    val elapsedText = BusyPanelPresentationResolver.formatElapsed(elapsedSeconds)
    val presentation = BusyPanelPresentationResolver.resolve(label, elapsedSeconds)

    UnifiedTopBannerShell(
        title = label,
        detail = presentation.activityText,
        tone = LukoaTopBannerTone.Neutral,
        titleMaxLines = 1,
        modifier = Modifier.testTag("floating-busy-banner"),
        bottomProgress = true,
        trailing = {
            TopBannerActionChip(
                text = elapsedText,
                emphasized = true,
            )
        },
    )
}

@Composable
private fun FloatingPendingTaskBar(
    task: PendingLauncherTask,
    onContinue: (() -> Unit)?,
    onOpenDetails: (() -> Unit)?,
) {
    UnifiedTopBannerShell(
        title = "有未确认任务：${task.title}",
        detail = task.detail.ifBlank { "可继续检查最新结果或进入任务中心处理。" },
        tone = LukoaTopBannerTone.Neutral,
        titleMaxLines = 1,
        modifier = Modifier.testTag("floating-pending-task-banner"),
        trailing = {
            if (onContinue != null) {
                TopBannerActionChip(
                    text = "继续",
                    emphasized = true,
                    onClick = onContinue,
                )
            }
            if (onOpenDetails != null) {
                TopBannerActionChip(
                    text = "详情",
                    emphasized = false,
                    onClick = onOpenDetails,
                )
            }
        },
    )
}

private val TOP_BANNER_SHAPE = RoundedCornerShape(14.dp)
private val TOP_BANNER_ACTION_SHAPE = RoundedCornerShape(8.dp)
