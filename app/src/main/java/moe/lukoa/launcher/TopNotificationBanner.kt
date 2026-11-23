package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var currentMessage by mutableStateOf<LukoaTopBannerMessage?>(null)
        private set

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
        val existing = currentMessage
        if (
            existing != null &&
            dedupeKey != null &&
            existing.dedupeKey == dedupeKey &&
            existing.title == cleanTitle &&
            existing.detail == cleanDetail
        ) {
            return existing.id
        }
        val assignedId = nextId++
        currentMessage = LukoaTopBannerMessage(
            id = assignedId,
            title = cleanTitle,
            detail = cleanDetail,
            tone = tone,
            actionLabel = actionLabel?.trim()?.takeIf { it.isNotEmpty() },
            onAction = onAction,
            autoDismissMs = autoDismissMs.coerceAtLeast(0L),
            dedupeKey = dedupeKey,
        )
        return assignedId
    }

    fun dismiss(id: Long? = null) {
        val existing = currentMessage ?: return
        if (id == null || existing.id == id) {
            currentMessage = null
        }
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
    val message = controller.currentMessage

    LaunchedEffect(message?.id) {
        val active = message ?: return@LaunchedEffect
        if (active.autoDismissMs > 0L) {
            delay(active.autoDismissMs)
            controller.dismiss(active.id)
        }
    }

    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val safeTopPadding = maxOf(cutoutTop, statusTop, 36.dp) + if (inSecondaryPage) 62.dp else 8.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = safeTopPadding, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(animationSpec = tween(210)) { -it } + fadeIn(tween(180)),
            exit = slideOutVertically(animationSpec = tween(180)) { -it } + fadeOut(tween(150)),
        ) {
            message?.let { activeMessage ->
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
            visible = busyLabel.isNullOrBlank() && pendingTask != null && message == null,
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
    }
}

@Composable
private fun TopBannerNotificationCard(
    message: LukoaTopBannerMessage,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    var cumulativeDragY by remember(message.id) { mutableFloatStateOf(0f) }
    val barColor: Color
    val borderColor: Color
    val containerColor: Color
    val titleColor: Color
    when (message.tone) {
        LukoaTopBannerTone.Danger -> {
            barColor = LukoaColors.Danger
            borderColor = LukoaColors.Danger.copy(alpha = 0.55f)
            containerColor = LukoaColors.DangerSoft
            titleColor = LukoaColors.TextPrimary
        }
        LukoaTopBannerTone.Warning -> {
            barColor = LukoaColors.Accent
            borderColor = LukoaColors.Border
            containerColor = LukoaColors.Elevated
            titleColor = LukoaColors.TextPrimary
        }
        LukoaTopBannerTone.Success,
        LukoaTopBannerTone.Neutral,
        -> {
            barColor = LukoaColors.Primary
            borderColor = LukoaColors.Border
            containerColor = LukoaColors.Elevated
            titleColor = LukoaColors.TextPrimary
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .heightIn(min = if (message.detail.isNotBlank()) 32.dp else 18.dp)
                    .background(barColor, RoundedCornerShape(2.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = message.title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (message.detail.isNotBlank()) {
                    Text(
                        text = message.detail,
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!message.actionLabel.isNullOrBlank() && message.onAction != null) {
                Surface(
                    modifier = Modifier
                        .testTag("top-notification-banner-action")
                        .clickable(role = Role.Button, onClick = onAction),
                    color = if (message.tone == LukoaTopBannerTone.Danger) {
                        LukoaColors.Danger.copy(alpha = 0.22f)
                    } else {
                        LukoaColors.PrimarySoft
                    },
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(
                        1.dp,
                        if (message.tone == LukoaTopBannerTone.Danger) {
                            LukoaColors.Danger.copy(alpha = 0.6f)
                        } else {
                            LukoaColors.Border
                        },
                    ),
                ) {
                    Text(
                        text = message.actionLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (message.tone == LukoaTopBannerTone.Danger) {
                            LukoaColors.Danger
                        } else {
                            LukoaColors.Primary
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
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
        }
    }
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("floating-busy-banner"),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.35f)),
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(LukoaColors.Primary, RoundedCornerShape(4.dp)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = label,
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = presentation.activityText,
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    text = elapsedText,
                    active = true,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
            }
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

@Composable
private fun FloatingPendingTaskBar(
    task: PendingLauncherTask,
    onContinue: (() -> Unit)?,
    onOpenDetails: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("floating-pending-task-banner"),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .background(LukoaColors.Accent, RoundedCornerShape(2.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "有未确认任务：${task.title}",
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = task.detail.ifBlank { "可继续检查最新结果或进入任务中心处理。" },
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onContinue != null) {
                Surface(
                    modifier = Modifier.clickable(role = Role.Button, onClick = onContinue),
                    color = LukoaColors.PrimarySoft,
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Text(
                        text = "继续",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (onOpenDetails != null) {
                Surface(
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpenDetails),
                    color = LukoaColors.Surface,
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Text(
                        text = "详情",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
