package moe.lukoa.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SpotlightTargetId {
    LaunchStatusCard,
    LaunchPrimaryAction,
    LaunchQuickTools,
    TavernHubGrid,
    ToolboxHealthCard,
    BottomTabSettings,
    DocsSpotlightReplay,
}

data class SpotlightTourStep(
    val id: String,
    val targetId: SpotlightTargetId,
    val requiredTab: LauncherTab,
    val title: String,
    val description: String,
    val cutoutPaddingDp: Float = 6f,
    val cutoutCornerRadiusDp: Float = 14f,
    val actionLabel: String? = null,
)

object SpotlightTourCatalog {
    const val STEP_LAUNCH_STATUS = "launch_status"
    const val STEP_LAUNCH_PRIMARY = "launch_primary"
    const val STEP_LAUNCH_TOOLS = "launch_tools"
    const val STEP_TAVERN_HUB = "tavern_hub"
    const val STEP_TOOLBOX_HEALTH = "toolbox_health"
    const val STEP_SETTINGS_TAB = "settings_tab"
    const val STEP_DOCS_REPLAY = "docs_replay"

    fun buildSteps(needsInitialSetup: Boolean): List<SpotlightTourStep> = listOf(
        SpotlightTourStep(
            id = STEP_LAUNCH_STATUS,
            targetId = SpotlightTargetId.LaunchStatusCard,
            requiredTab = LauncherTab.Launch,
            title = "一眼看清酒馆状态",
            description = "这里实时显示酒馆运行状态、当前版本与实用提示；开启多实例时，点这里还能快速切换不同酒馆分身。",
            cutoutPaddingDp = 6f,
            cutoutCornerRadiusDp = 18f,
        ),
        SpotlightTourStep(
            id = STEP_LAUNCH_PRIMARY,
            targetId = SpotlightTargetId.LaunchPrimaryAction,
            requiredTab = LauncherTab.Launch,
            title = if (needsInitialSetup) "从这里完成首次配置与启动" else "一键启动或停止酒馆",
            description = if (needsInitialSetup) {
                "首次使用点这个主按钮，即可按 4 步向导安装并授权 Termux、一键装好酒馆；配好后，日常点一下就能直接启停。"
            } else {
                "日常点这里即可启动或停止 SillyTavern；如果 Termux 在后台休眠了，可先点上方「唤醒 Termux 并返回」。"
            },
            cutoutPaddingDp = 6f,
            cutoutCornerRadiusDp = 15f,
            actionLabel = if (needsInitialSetup) "立即进入首次配置" else null,
        ),
        SpotlightTourStep(
            id = STEP_LAUNCH_TOOLS,
            targetId = SpotlightTargetId.LaunchQuickTools,
            requiredTab = LauncherTab.Launch,
            title = "返回网页与导出日志",
            description = "酒馆跑起来后点「返回酒馆」直达浏览器，默认切回已有窗口避免堆叠十几个标签（长按可强制重开网址）；排查问题点「导出日志」。",
            cutoutPaddingDp = 6f,
            cutoutCornerRadiusDp = 16f,
        ),
        SpotlightTourStep(
            id = STEP_TAVERN_HUB,
            targetId = SpotlightTargetId.TavernHubGrid,
            requiredTab = LauncherTab.Tavern,
            title = "酒馆管理中心",
            description = "切到底部「酒馆」页，可以备份与恢复聊天数据、更新或回退版本、批量快捷导入角色卡和世界书，以及管理扩展与局域网访问。",
            cutoutPaddingDp = 6f,
            cutoutCornerRadiusDp = 16f,
        ),
        SpotlightTourStep(
            id = STEP_TOOLBOX_HEALTH,
            targetId = SpotlightTargetId.ToolboxHealthCard,
            requiredTab = LauncherTab.Toolbox,
            title = "一键体检与守护工具箱",
            description = "遇到打不开或报错先点「一键体检」自动排查；下方还内置聊天防丢档守护、空间清理、修复依赖与 Debug 诊断区。",
            cutoutPaddingDp = 6f,
            cutoutCornerRadiusDp = 14f,
        ),
        SpotlightTourStep(
            id = STEP_SETTINGS_TAB,
            targetId = SpotlightTargetId.BottomTabSettings,
            requiredTab = LauncherTab.Settings,
            title = "设置、外观与网络加速",
            description = "「设置」页可切换日/夜间外观、指定默认浏览器、开启自动启动；国内网络下载慢时，可在「网络与镜像源」一键测速优选。",
            cutoutPaddingDp = 4f,
            cutoutCornerRadiusDp = 16f,
        ),
        SpotlightTourStep(
            id = STEP_DOCS_REPLAY,
            targetId = SpotlightTargetId.DocsSpotlightReplay,
            requiredTab = LauncherTab.Docs,
            title = "内置文档与随时重看指引",
            description = "「文档」页整理了酒馆黑话、常见报错答疑与进阶玩法，支持关键词搜索；以后想再看一遍新手指引，随时点这里或去「设置」重新打开。",
            cutoutPaddingDp = 5f,
            cutoutCornerRadiusDp = 12f,
        ),
    )
}

class SpotlightTargetRegistry {
    private val coordinates = mutableStateMapOf<SpotlightTargetId, LayoutCoordinates>()
    private val lastBounds = HashMap<SpotlightTargetId, Rect>()
    private val fixedRects = mutableStateMapOf<SpotlightTargetId, Rect>()
    private var layoutVersion by mutableIntStateOf(0)

    fun update(id: SpotlightTargetId, coords: LayoutCoordinates) {
        if (!coords.isAttached) return
        val bounds = coords.boundsInRoot()
        if (!isFiniteValidRect(bounds)) return
        coordinates[id] = coords
        val previous = lastBounds[id]
        if (previous == null || !approxEqualRect(previous, bounds)) {
            lastBounds[id] = bounds
            layoutVersion += 1
        }
    }

    fun remove(id: SpotlightTargetId) {
        coordinates.remove(id)
        lastBounds.remove(id)
    }

    internal fun setFixedRectForTest(id: SpotlightTargetId, rect: Rect) {
        fixedRects[id] = rect
        layoutVersion += 1
    }

    fun resolveRectInRoot(
        id: SpotlightTargetId,
        rootCoords: LayoutCoordinates?,
        recheckKey: Any? = null,
    ): Rect? {
        @Suppress("UNUSED_EXPRESSION")
        layoutVersion
        @Suppress("UNUSED_EXPRESSION")
        recheckKey
        fixedRects[id]?.let { fixed ->
            if (isFiniteValidRect(fixed)) return fixed
        }
        val targetCoords = coordinates[id]?.takeIf { it.isAttached } ?: return null
        val rect = if (rootCoords != null && rootCoords.isAttached) {
            runCatching { rootCoords.localBoundingBoxOf(targetCoords, clipBounds = false) }.getOrNull()
        } else {
            runCatching { targetCoords.boundsInRoot() }.getOrNull()
        } ?: return null
        if (!isFiniteValidRect(rect)) return null
        if (rootCoords != null && rootCoords.isAttached) {
            val rootWidth = rootCoords.size.width.toFloat()
            val rootHeight = rootCoords.size.height.toFloat()
            val visibleHorizontal = rect.right > 0f && rect.left < rootWidth
            val visibleVertical = rect.bottom > 0f && rect.top < rootHeight
            if (!visibleHorizontal || !visibleVertical) return null
        }
        return rect
    }

    private fun isFiniteValidRect(rect: Rect): Boolean =
        rect.left.isFinite() &&
            rect.top.isFinite() &&
            rect.right.isFinite() &&
            rect.bottom.isFinite() &&
            rect.width > 1f &&
            rect.height > 1f

    private fun approxEqualRect(a: Rect, b: Rect): Boolean =
        abs(a.left - b.left) < 1f &&
            abs(a.top - b.top) < 1f &&
            abs(a.right - b.right) < 1f &&
            abs(a.bottom - b.bottom) < 1f
}

val LocalSpotlightRegistry = staticCompositionLocalOf<SpotlightTargetRegistry?> { null }

fun Modifier.spotlightTarget(id: SpotlightTargetId): Modifier = composed {
    val registry = LocalSpotlightRegistry.current ?: return@composed this
    DisposableEffect(registry, id) {
        onDispose { registry.remove(id) }
    }
    this.onGloballyPositioned { coords ->
        if (coords.isAttached) {
            registry.update(id, coords)
        }
    }
}

internal data class SpotlightCardPlacement(
    val placeBelow: Boolean,
    val topOffsetPx: Float,
    val arrowCenterXPx: Float?,
)

internal object SpotlightOverlayLayoutMath {
    fun computeCardPlacement(
        containerWidthPx: Float,
        containerHeightPx: Float,
        safeTopPx: Float,
        safeBottomPx: Float,
        horizontalMarginPx: Float,
        targetRect: Rect?,
        cardHeightPx: Float,
        gapPx: Float,
        arrowEdgeInsetPx: Float,
    ): SpotlightCardPlacement {
        val usableTop = safeTopPx.coerceAtLeast(0f)
        val usableBottom = (containerHeightPx - safeBottomPx).coerceAtLeast(usableTop + 1f)
        val maxTop = (usableBottom - cardHeightPx).coerceAtLeast(usableTop)

        if (targetRect == null) {
            val centered = (usableTop + ((usableBottom - usableTop - cardHeightPx) / 2f))
                .coerceIn(usableTop, maxTop)
            return SpotlightCardPlacement(
                placeBelow = true,
                topOffsetPx = centered,
                arrowCenterXPx = null,
            )
        }

        val spaceBelow = usableBottom - (targetRect.bottom + gapPx)
        val spaceAbove = (targetRect.top - gapPx) - usableTop
        val placeBelow = spaceBelow >= cardHeightPx || spaceBelow >= spaceAbove

        val desiredTop = if (placeBelow) {
            targetRect.bottom + gapPx
        } else {
            targetRect.top - gapPx - cardHeightPx
        }
        val clampedTop = desiredTop.coerceIn(usableTop, maxTop)

        val minArrowX = horizontalMarginPx + arrowEdgeInsetPx
        val maxArrowX = (containerWidthPx - horizontalMarginPx - arrowEdgeInsetPx)
            .coerceAtLeast(minArrowX)
        val arrowCenterX = targetRect.center.x.coerceIn(minArrowX, maxArrowX)

        return SpotlightCardPlacement(
            placeBelow = placeBelow,
            topOffsetPx = clampedTop,
            arrowCenterXPx = arrowCenterX,
        )
    }
}

@Composable
fun SpotlightTourOverlay(
    active: Boolean,
    steps: List<SpotlightTourStep>,
    currentStepIndex: Int,
    registry: SpotlightTargetRegistry,
    rootCoords: LayoutCoordinates?,
    recheckKey: Any? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onStepAction: (SpotlightTourStep) -> Unit = {},
) {
    val safeIndex = currentStepIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val currentStep = steps.getOrNull(safeIndex)
    val isFirstStep = safeIndex == 0
    val isLastStep = safeIndex >= steps.lastIndex

    BackHandler(enabled = active) {
        if (isFirstStep) {
            onSkip()
        } else {
            onPrevious()
        }
    }

    AnimatedVisibility(
        visible = active && currentStep != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (currentStep == null) return@AnimatedVisibility
        val density = LocalDensity.current
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val safeTopPx = with(density) { (maxOf(statusBarTop, cutoutTop, 28.dp) + 12.dp).toPx() }
        val safeBottomPx = with(density) { (maxOf(navBarBottom, 16.dp) + 12.dp).toPx() }
        val horizontalMarginDp = 16.dp
        val horizontalMarginPx = with(density) { horizontalMarginDp.toPx() }
        val gapPx = with(density) { 14.dp.toPx() }
        val arrowWidthPx = with(density) { 16.dp.toPx() }
        val arrowHeightPx = with(density) { 8.dp.toPx() }
        val arrowEdgeInsetPx = with(density) { 26.dp.toPx() }
        val cutoutPaddingPx = with(density) { currentStep.cutoutPaddingDp.dp.toPx() }
        val cutoutCornerPx = with(density) { currentStep.cutoutCornerRadiusDp.dp.toPx() }

        val resolvedRawRect = registry.resolveRectInRoot(
            id = currentStep.targetId,
            rootCoords = rootCoords,
            recheckKey = recheckKey,
        )
        var lastValidPaddedRect by remember { mutableStateOf<Rect?>(null) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("spotlight-tour-overlay")
                .pointerInput(safeIndex, steps.size) {
                    detectTapGestures {
                        // Consume scrim taps so underlying buttons cannot fire accidentally.
                    }
                },
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            val paddedTargetRect = resolvedRawRect?.let { raw ->
                Rect(
                    left = (raw.left - cutoutPaddingPx).coerceIn(4f, containerWidthPx - 4f),
                    top = (raw.top - cutoutPaddingPx).coerceIn(4f, containerHeightPx - 4f),
                    right = (raw.right + cutoutPaddingPx).coerceIn(4f, containerWidthPx - 4f),
                    bottom = (raw.bottom + cutoutPaddingPx).coerceIn(4f, containerHeightPx - 4f),
                )
            }
            LaunchedEffect(paddedTargetRect) {
                if (paddedTargetRect != null) {
                    lastValidPaddedRect = paddedTargetRect
                }
            }
            val effectiveCutoutTarget = paddedTargetRect ?: lastValidPaddedRect
            val animatedCutoutRect by animateRectAsState(
                targetValue = effectiveCutoutTarget ?: Rect.Zero,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "spotlight-cutout-rect",
            )
            val showCutout = effectiveCutoutTarget != null &&
                animatedCutoutRect.width > 2f &&
                animatedCutoutRect.height > 2f

            var measuredCardHeightPx by remember { mutableIntStateOf(0) }
            val fallbackCardHeightPx = with(density) { 216.dp.toPx() }
            val effectiveCardHeightPx = if (measuredCardHeightPx > 0) {
                measuredCardHeightPx.toFloat()
            } else {
                fallbackCardHeightPx
            }
            val placement = SpotlightOverlayLayoutMath.computeCardPlacement(
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                safeTopPx = safeTopPx,
                safeBottomPx = safeBottomPx,
                horizontalMarginPx = horizontalMarginPx,
                targetRect = if (showCutout) animatedCutoutRect else null,
                cardHeightPx = effectiveCardHeightPx,
                gapPx = gapPx,
                arrowEdgeInsetPx = arrowEdgeInsetPx,
            )

            val ringPrimaryColor = LukoaColors.Primary
            val ringHaloColor = LukoaColors.Primary.copy(alpha = 0.24f)
            val cardFillColor = LukoaColors.Elevated
            val cardBorderColor = LukoaColors.Border

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.72f))
                if (showCutout) {
                    val topLeft = Offset(animatedCutoutRect.left, animatedCutoutRect.top)
                    val rectSize = Size(animatedCutoutRect.width, animatedCutoutRect.height)
                    val corner = CornerRadius(cutoutCornerPx, cutoutCornerPx)

                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner,
                        blendMode = BlendMode.Clear,
                    )
                    drawRoundRect(
                        color = ringHaloColor,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner,
                        style = Stroke(width = 5.dp.toPx()),
                    )
                    drawRoundRect(
                        color = ringPrimaryColor,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = corner,
                        style = Stroke(width = 1.75.dp.toPx()),
                    )
                }

                placement.arrowCenterXPx?.let { arrowX ->
                    val halfW = arrowWidthPx / 2f
                    val borderStrokePx = 1.dp.toPx()
                    val arrowPath = Path()
                    val outlinePath = Path()
                    if (placement.placeBelow) {
                        val baseY = placement.topOffsetPx + borderStrokePx
                        val tipY = (placement.topOffsetPx - arrowHeightPx).coerceAtLeast(0f)
                        arrowPath.moveTo(arrowX - halfW, baseY)
                        arrowPath.lineTo(arrowX, tipY)
                        arrowPath.lineTo(arrowX + halfW, baseY)
                        arrowPath.close()

                        outlinePath.moveTo(arrowX - halfW, baseY)
                        outlinePath.lineTo(arrowX, tipY)
                        outlinePath.lineTo(arrowX + halfW, baseY)
                    } else {
                        val baseY = placement.topOffsetPx + effectiveCardHeightPx - borderStrokePx
                        val tipY = (placement.topOffsetPx + effectiveCardHeightPx + arrowHeightPx)
                            .coerceAtMost(size.height)
                        arrowPath.moveTo(arrowX - halfW, baseY)
                        arrowPath.lineTo(arrowX, tipY)
                        arrowPath.lineTo(arrowX + halfW, baseY)
                        arrowPath.close()

                        outlinePath.moveTo(arrowX - halfW, baseY)
                        outlinePath.lineTo(arrowX, tipY)
                        outlinePath.lineTo(arrowX + halfW, baseY)
                    }
                    drawPath(path = arrowPath, color = cardFillColor)
                    drawPath(
                        path = outlinePath,
                        color = cardBorderColor,
                        style = Stroke(width = borderStrokePx),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalMarginDp)
                    .offset { IntOffset(x = 0, y = placement.topOffsetPx.roundToInt()) }
                    .onSizeChanged { size ->
                        if (size.height > 0 && size.height != measuredCardHeightPx) {
                            measuredCardHeightPx = size.height
                        }
                    }
                    .testTag("spotlight-tour-card"),
                color = LukoaColors.Elevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LukoaColors.Border),
                shadowElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "新手引导",
                                color = LukoaColors.Primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${safeIndex + 1} / ${steps.size}",
                                modifier = Modifier.testTag("spotlight-tour-step-indicator"),
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = "跳过指引",
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = onSkip)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("spotlight-tour-skip"),
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        steps.indices.forEach { idx ->
                            val activeSegment = idx <= safeIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (activeSegment) LukoaColors.Primary else LukoaColors.Border,
                                    ),
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = currentStep.title,
                            color = LukoaColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = currentStep.description,
                            color = LukoaColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 21.sp,
                        )
                    }

                    if (!currentStep.actionLabel.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onStepAction(currentStep) },
                                )
                                .testTag("spotlight-tour-step-action"),
                            color = LukoaColors.PrimarySoft,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.36f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = currentStep.actionLabel,
                                    color = LukoaColors.Primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "›",
                                    color = LukoaColors.Primary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!isFirstStep) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button, onClick = onPrevious)
                                    .testTag("spotlight-tour-prev"),
                                color = LukoaColors.Surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LukoaColors.Border),
                            ) {
                                Text(
                                    text = "上一步",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    color = LukoaColors.TextPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = if (isLastStep) onFinish else onNext,
                            modifier = Modifier.testTag("spotlight-tour-next"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LukoaColors.Primary,
                                contentColor = LukoaColors.OnPrimary,
                            ),
                        ) {
                            Text(
                                text = if (isLastStep) "开始使用" else "下一步",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
