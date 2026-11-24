package moe.lukoa.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
    SetupStepProgress,
    SetupPrimaryAction,
    SetupSecondaryActions,
    TavernHubGrid,
    ToolboxHealthCard,
    SettingsQuickToggles,
    BottomTabSettings,
    DocsSpotlightReplay,
}

enum class SpotlightSetupStage {
    InstallTermux,
    RequestPermission,
    EnableExternalApps,
    PrepareEnvironment,
    DetectTavern,
    InstallTavern,
    Ready,
}

data class SpotlightTourStep(
    val id: String,
    val targetId: SpotlightTargetId,
    val fallbackTargetId: SpotlightTargetId? = null,
    val requiredTab: LauncherTab,
    val requiredSecondaryPage: LauncherSecondaryPage? = null,
    val badge: String = "新手引导",
    val title: String,
    val description: String,
    val cutoutPaddingDp: Float = 6f,
    val cutoutCornerRadiusDp: Float = 14f,
    val nextLabel: String? = null,
    val actionLabel: String? = null,
    val allowTargetTap: Boolean = false,
    val targetTapHint: String? = null,
)

object SpotlightTourCatalog {
    const val STEP_LAUNCH_STATUS = "launch_status"
    const val STEP_LAUNCH_PRIMARY = "launch_primary"
    const val STEP_SETUP_PROGRESS = "setup_progress"
    const val STEP_SETUP_PRIMARY = "setup_primary"
    const val STEP_SETUP_SECONDARY = "setup_secondary"
    const val STEP_LAUNCH_TOOLS = "launch_tools"
    const val STEP_TAVERN_HUB = "tavern_hub"
    const val STEP_TOOLBOX_HEALTH = "toolbox_health"
    const val STEP_SETTINGS_OVERVIEW = "settings_overview"
    const val STEP_DOCS_REPLAY = "docs_replay"

    fun resolveSetupStage(
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        externalAppsBlocked: Boolean,
        termuxSetupRecommended: Boolean,
        tavernInstallDetected: Boolean?,
    ): SpotlightSetupStage = when {
        !termuxInstalled -> SpotlightSetupStage.InstallTermux
        !runCommandPermissionGranted -> SpotlightSetupStage.RequestPermission
        externalAppsBlocked -> SpotlightSetupStage.EnableExternalApps
        termuxSetupRecommended -> SpotlightSetupStage.PrepareEnvironment
        tavernInstallDetected == null -> SpotlightSetupStage.DetectTavern
        tavernInstallDetected == false -> SpotlightSetupStage.InstallTavern
        else -> SpotlightSetupStage.Ready
    }

    fun buildSteps(
        needsInitialSetup: Boolean,
        setupStage: SpotlightSetupStage = if (needsInitialSetup) {
            SpotlightSetupStage.InstallTermux
        } else {
            SpotlightSetupStage.Ready
        },
        includeFeatureTour: Boolean = true,
    ): List<SpotlightTourStep> = buildList {
        if (includeFeatureTour) {
            add(
                SpotlightTourStep(
                    id = STEP_LAUNCH_STATUS,
                    targetId = SpotlightTargetId.LaunchStatusCard,
                    requiredTab = LauncherTab.Launch,
                    badge = if (needsInitialSetup) "欢迎使用" else "新手引导",
                    title = if (needsInitialSetup) "欢迎使用露科亚启动器" else "一眼看清酒馆状态",
                    description = if (needsInitialSetup) {
                        "顶部状态卡实时显示酒馆运行状态和当前版本。首次使用按步骤完成环境准备后，再顺带看一遍常用功能。"
                    } else {
                        "这里实时显示酒馆运行状态、当前版本与实用提示；配置多实例分身时，点这里还能快速切换不同酒馆。"
                    },
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 18f,
                    nextLabel = if (needsInitialSetup) "开始第 1 步" else "下一步",
                ),
            )

            add(
                SpotlightTourStep(
                    id = STEP_LAUNCH_PRIMARY,
                    targetId = SpotlightTargetId.LaunchPrimaryAction,
                    requiredTab = LauncherTab.Launch,
                    badge = if (needsInitialSetup) "首次配置" else "启动控制",
                    title = if (needsInitialSetup) "从这里进入首次安装向导" else "一键启动或停止酒馆",
                    description = if (needsInitialSetup) {
                        "当前还没检测到完整的 Termux 或酒馆环境。点击高亮的主按钮（或下方按钮）进入向导，按提示把 Termux 和酒馆装好。"
                    } else {
                        "日常点这里即可启动或停止 SillyTavern；如果 Termux 在后台休眠了，可以先点上方「唤醒 Termux 并返回」。"
                    },
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 15f,
                    nextLabel = if (needsInitialSetup) "打开安装向导" else "下一步",
                    allowTargetTap = needsInitialSetup,
                    targetTapHint = if (needsInitialSetup) "直接点亮处按钮，或点「打开安装向导」继续" else null,
                ),
            )
        }

        if (needsInitialSetup) {
            addAll(buildSetupOnlySteps(setupStage = setupStage, followedByFeatureTour = includeFeatureTour))
        }

        if (includeFeatureTour) {
            add(
                SpotlightTourStep(
                    id = STEP_LAUNCH_TOOLS,
                    targetId = SpotlightTargetId.LaunchQuickTools,
                    requiredTab = LauncherTab.Launch,
                    badge = "日常操作",
                    title = "返回网页与导出日志",
                    description = "酒馆跑起来后点「返回酒馆」直达浏览器，默认切回已有窗口避免连开十几个标签（长按可强制重开网址）；排查问题点「导出日志」。",
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 16f,
                ),
            )
            add(
                SpotlightTourStep(
                    id = STEP_TAVERN_HUB,
                    targetId = SpotlightTargetId.TavernHubGrid,
                    requiredTab = LauncherTab.Tavern,
                    badge = "酒馆中心",
                    title = "备份、版本与快捷导入",
                    description = "切到底部「酒馆」页，可随时备份与恢复聊天数据、安全更新或回退酒馆版本、批量导入角色卡与世界书，以及管理扩展和局域网访问。",
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 16f,
                ),
            )
            add(
                SpotlightTourStep(
                    id = STEP_TOOLBOX_HEALTH,
                    targetId = SpotlightTargetId.ToolboxHealthCard,
                    requiredTab = LauncherTab.Toolbox,
                    badge = "守护排错",
                    title = "一键体检与防丢档工具箱",
                    description = "遇到打不开或报错先点「一键体检」自动排查；下方还内置防丢档的聊天守护、空间清理、修复依赖与 Debug 诊断区。",
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 14f,
                ),
            )
            add(
                SpotlightTourStep(
                    id = STEP_SETTINGS_OVERVIEW,
                    targetId = SpotlightTargetId.SettingsQuickToggles,
                    fallbackTargetId = SpotlightTargetId.BottomTabSettings,
                    requiredTab = LauncherTab.Settings,
                    badge = "个性设置",
                    title = "外观、浏览器与镜像加速",
                    description = "「设置」页可切换日/夜间外观、指定默认浏览器、开启自动启动；国内网络安装慢时，往下进「网络与镜像源」一键测速优选即可。",
                    cutoutPaddingDp = 6f,
                    cutoutCornerRadiusDp = 14f,
                ),
            )
            add(
                SpotlightTourStep(
                    id = STEP_DOCS_REPLAY,
                    targetId = SpotlightTargetId.DocsSpotlightReplay,
                    requiredTab = LauncherTab.Docs,
                    badge = "随时重看",
                    title = "内置文档与随时重看指引",
                    description = "「文档」页整理了酒馆黑话、报错答疑与进阶玩法，支持关键词搜索；以后随时点亮处的「新手引导」就能再走一遍本指引。",
                    cutoutPaddingDp = 5f,
                    cutoutCornerRadiusDp = 12f,
                    actionLabel = if (needsInitialSetup) "去完成首次安装" else null,
                    nextLabel = "完成引导",
                ),
            )
        }
    }

    fun buildSetupOnlySteps(
        setupStage: SpotlightSetupStage,
        followedByFeatureTour: Boolean = false,
    ): List<SpotlightTourStep> {
        val (primaryTitle, primaryDescription, primaryActionLabel) = when (setupStage) {
            SpotlightSetupStage.InstallTermux -> Triple(
                "当前步骤：安装并打开一次 Termux",
                "点亮处的「下载 Termux」获取安装包；若 F-Droid 或 GitHub 打不开、下不了，请先开启代理再试。装好后务必先打开一次 Termux 再切回来。",
                "立即下载 Termux",
            )
            SpotlightSetupStage.RequestPermission -> Triple(
                "当前步骤：允许启动器调用 Termux",
                "点亮处的「请求系统权限」授权；如果手机没有弹出授权窗，再点下方「权限设置」手动允许即可。",
                "立即请求权限",
            )
            SpotlightSetupStage.EnableExternalApps -> Triple(
                "当前步骤：打开 Termux 外部调用",
                "点亮处的「复制权限命令」，再打开 Termux 粘贴并回车执行一次，就能打通启动器与 Termux 的通信。",
                "复制权限命令",
            )
            SpotlightSetupStage.PrepareEnvironment -> Triple(
                "当前步骤：自动准备运行环境",
                "点亮处的「准备 Termux 环境」，启动器会自动安装 Node.js 等基础组件（首次需几分钟）；若下载失败或超时，请开启代理或去设置切国内包源。",
                "开始准备环境",
            )
            SpotlightSetupStage.DetectTavern -> Triple(
                "当前步骤：检测本机是否已有酒馆",
                "以前装过酒馆点亮处的「检测本机酒馆」即可直接接管；完全从零开始可点下方「第一次用，安装酒馆」。",
                "立即检测酒馆",
            )
            SpotlightSetupStage.InstallTavern -> Triple(
                "当前步骤：一键安装酒馆",
                "默认稳定版最省心。点亮处的安装按钮即可开始部署（约 5–10 分钟）；若一直下不动或报错，请开启代理，或先去「设置 → 网络与镜像源」一键测速优选。",
                "立即开始安装",
            )
            SpotlightSetupStage.Ready -> Triple(
                "当前步骤：基础准备已全部完成",
                "4 项准备都已经就绪！回到启动页点「启动酒馆」就能开始使用 SillyTavern。",
                null,
            )
        }

        return listOf(
            SpotlightTourStep(
                id = STEP_SETUP_PROGRESS,
                targetId = SpotlightTargetId.SetupStepProgress,
                requiredTab = LauncherTab.Launch,
                requiredSecondaryPage = LauncherSecondaryPage.SetupWizard,
                badge = "配置进度",
                title = "一共只需完成这 4 步",
                description = "顶部进度条对应 4 个阶段：装 Termux、连权限、配环境、装酒馆。每完成一项会自动推进到下一步。",
                cutoutPaddingDp = 6f,
                cutoutCornerRadiusDp = 16f,
                nextLabel = "看当前操作",
            ),
            SpotlightTourStep(
                id = STEP_SETUP_PRIMARY,
                targetId = SpotlightTargetId.SetupPrimaryAction,
                requiredTab = LauncherTab.Launch,
                requiredSecondaryPage = LauncherSecondaryPage.SetupWizard,
                badge = "当前操作",
                title = primaryTitle,
                description = primaryDescription,
                cutoutPaddingDp = 6f,
                cutoutCornerRadiusDp = 14f,
                actionLabel = primaryActionLabel,
                nextLabel = "下一步",
                allowTargetTap = primaryActionLabel != null,
                targetTapHint = if (primaryActionLabel != null) "可直接点亮处按钮执行，或先点「下一步」看完说明" else null,
            ),
            SpotlightTourStep(
                id = STEP_SETUP_SECONDARY,
                targetId = SpotlightTargetId.SetupSecondaryActions,
                requiredTab = LauncherTab.Launch,
                requiredSecondaryPage = LauncherSecondaryPage.SetupWizard,
                badge = "刷新与备用",
                title = "做完记得点这里重新检测",
                description = if (followedByFeatureTour) {
                    "装完软件或开完权限回到这里，点「重新检测」进度就会自动前进。可以直接留在这开始配置，或点「继续逛其他页面」。"
                } else {
                    "切到 Termux 或系统设置操作完成后，回来点这里的「重新检测」刷新状态，向导会自动进入下一步。"
                },
                cutoutPaddingDp = 6f,
                cutoutCornerRadiusDp = 14f,
                actionLabel = if (followedByFeatureTour) "留在这里开始配置" else null,
                nextLabel = if (followedByFeatureTour) "继续逛其他页面" else "知道了，开始操作",
            ),
        )
    }
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
    isTransitioning: Boolean = false,
    recheckKey: Any? = null,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onStepAction: (SpotlightTourStep) -> Unit = {},
    onTargetTap: (SpotlightTourStep) -> Unit = onStepAction,
) {
    val safeIndex = currentStepIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val currentStep = steps.getOrNull(safeIndex)
    val isFirstStep = safeIndex == 0
    val isLastStep = safeIndex >= steps.lastIndex

    BackHandler(enabled = active) {
        if (isTransitioning) return@BackHandler
        if (isFirstStep) {
            onSkip()
        } else {
            onPrevious()
        }
    }

    AnimatedVisibility(
        visible = active && currentStep != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (currentStep == null) return@AnimatedVisibility
        val smoothEasing = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
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

        val resolvedRawRect = if (isTransitioning) {
            null
        } else {
            registry.resolveRectInRoot(
                id = currentStep.targetId,
                rootCoords = rootCoords,
                recheckKey = recheckKey,
            ) ?: currentStep.fallbackTargetId?.let { fallbackId ->
                registry.resolveRectInRoot(
                    id = fallbackId,
                    rootCoords = rootCoords,
                    recheckKey = recheckKey,
                )
            }
        }
        var lastValidPaddedRect by remember { mutableStateOf<Rect?>(null) }

        val scrimAlpha by animateFloatAsState(
            targetValue = if (isTransitioning) 0.56f else 0.72f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "spotlight-scrim-alpha",
        )
        val focusAlpha by animateFloatAsState(
            targetValue = if (isTransitioning) 0f else 1f,
            animationSpec = tween(durationMillis = 260, easing = smoothEasing),
            label = "spotlight-focus-alpha",
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("spotlight-tour-overlay"),
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
            val animatedCutoutRect = if (effectiveCutoutTarget != null) {
                animateRectAsState(
                    targetValue = effectiveCutoutTarget,
                    animationSpec = tween(durationMillis = 420, easing = smoothEasing),
                    label = "spotlight-cutout-rect",
                ).value
            } else {
                Rect.Zero
            }
            val showCutout = !isTransitioning &&
                effectiveCutoutTarget != null &&
                animatedCutoutRect.width > 2f &&
                animatedCutoutRect.height > 2f

            var measuredCardHeightPx by remember { mutableIntStateOf(0) }
            val fallbackCardHeightPx = with(density) { 224.dp.toPx() }
            val effectiveCardHeightPx = if (measuredCardHeightPx > 0) {
                measuredCardHeightPx.toFloat()
            } else {
                fallbackCardHeightPx
            }
            val targetPlacement = SpotlightOverlayLayoutMath.computeCardPlacement(
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
            val animatedCardTopPx by animateFloatAsState(
                targetValue = targetPlacement.topOffsetPx,
                animationSpec = tween(durationMillis = 380, easing = smoothEasing),
                label = "spotlight-card-top",
            )

            val ringPrimaryColor = LukoaColors.Primary.copy(alpha = focusAlpha)
            val cardFillColor = LukoaColors.Elevated
            val cardBorderColor = LukoaColors.Border

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentStep.id, showCutout, isTransitioning, animatedCutoutRect) {
                        detectTapGestures { tapOffset ->
                            if (
                                !isTransitioning &&
                                showCutout &&
                                currentStep.allowTargetTap &&
                                animatedCutoutRect.contains(tapOffset)
                            ) {
                                onTargetTap(currentStep)
                            }
                        }
                    },
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    drawRect(color = Color.Black.copy(alpha = scrimAlpha))
                    if (showCutout && focusAlpha > 0.02f) {
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
                            color = ringPrimaryColor,
                            topLeft = topLeft,
                            size = rectSize,
                            cornerRadius = corner,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }

                if (focusAlpha > 0.05f && targetPlacement.arrowCenterXPx != null) {
                    val arrowX = targetPlacement.arrowCenterXPx
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = focusAlpha },
                    ) {
                        val halfW = arrowWidthPx / 2f
                        val borderStrokePx = 1.dp.toPx()
                        val arrowPath = Path()
                        val outlinePath = Path()
                        if (targetPlacement.placeBelow) {
                            val baseY = animatedCardTopPx + 2.dp.toPx()
                            val tipY = (animatedCardTopPx - arrowHeightPx).coerceAtLeast(0f)
                            arrowPath.moveTo(arrowX - halfW, baseY)
                            arrowPath.lineTo(arrowX, tipY)
                            arrowPath.lineTo(arrowX + halfW, baseY)
                            arrowPath.close()

                            outlinePath.moveTo(arrowX - halfW, baseY)
                            outlinePath.lineTo(arrowX, tipY)
                            outlinePath.lineTo(arrowX + halfW, baseY)
                        } else {
                            val baseY = animatedCardTopPx + effectiveCardHeightPx - 2.dp.toPx()
                            val tipY = (animatedCardTopPx + effectiveCardHeightPx + arrowHeightPx)
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
                        .offset { IntOffset(x = 0, y = animatedCardTopPx.roundToInt()) }
                        .graphicsLayer { alpha = focusAlpha }
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
                                    text = currentStep.badge,
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
                                    .clickable(
                                        enabled = !isTransitioning,
                                        role = Role.Button,
                                        onClick = onSkip,
                                    )
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

                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(durationMillis = 240)) +
                                    slideInVertically(animationSpec = tween(durationMillis = 240)) { it / 14 })
                                    .togetherWith(fadeOut(animationSpec = tween(durationMillis = 130)))
                            },
                            label = "spotlight-step-body",
                        ) { step ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = step.title,
                                    color = LukoaColors.TextPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = step.description,
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 21.sp,
                                )
                                if (!step.targetTapHint.isNullOrBlank()) {
                                    Text(
                                        text = step.targetTapHint,
                                        color = LukoaColors.Primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        if (!currentStep.actionLabel.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(
                                        enabled = !isTransitioning,
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
                                        .clickable(
                                            enabled = !isTransitioning,
                                            role = Role.Button,
                                            onClick = onPrevious,
                                        )
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

                            val nextButtonText = currentStep.nextLabel
                                ?: if (isLastStep) "开始使用" else "下一步"
                            Button(
                                onClick = if (isLastStep) onFinish else onNext,
                                enabled = !isTransitioning,
                                modifier = Modifier.testTag("spotlight-tour-next"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LukoaColors.Primary,
                                    contentColor = LukoaColors.OnPrimary,
                                ),
                            ) {
                                Text(
                                    text = nextButtonText,
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
}
