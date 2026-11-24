package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SpotlightOnboardingTourTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalog_coversAllTabsAndAdaptsSetupStep() {
        val freshSteps = SpotlightTourCatalog.buildSteps(needsInitialSetup = true)
        val readySteps = SpotlightTourCatalog.buildSteps(needsInitialSetup = false)

        assertEquals(7, freshSteps.size)
        assertEquals(7, readySteps.size)
        assertEquals(
            setOf(
                LauncherTab.Launch,
                LauncherTab.Tavern,
                LauncherTab.Toolbox,
                LauncherTab.Settings,
                LauncherTab.Docs,
            ),
            freshSteps.map { it.requiredTab }.toSet(),
        )

        val freshPrimary = freshSteps.first { it.id == SpotlightTourCatalog.STEP_LAUNCH_PRIMARY }
        val readyPrimary = readySteps.first { it.id == SpotlightTourCatalog.STEP_LAUNCH_PRIMARY }
        assertEquals("立即进入首次配置", freshPrimary.actionLabel)
        assertNull(readyPrimary.actionLabel)
        assertEquals("一键启动或停止酒馆", readyPrimary.title)
    }

    @Test
    fun layoutMath_placesTooltipBelowTopTargetAndAboveBottomTarget() {
        val belowPlacement = SpotlightOverlayLayoutMath.computeCardPlacement(
            containerWidthPx = 1080f,
            containerHeightPx = 2400f,
            safeTopPx = 100f,
            safeBottomPx = 100f,
            horizontalMarginPx = 48f,
            targetRect = Rect(left = 80f, top = 220f, right = 1000f, bottom = 520f),
            cardHeightPx = 420f,
            gapPx = 36f,
            arrowEdgeInsetPx = 72f,
        )
        assertTrue(belowPlacement.placeBelow)
        assertEquals(556f, belowPlacement.topOffsetPx, 0.5f)
        assertEquals(540f, requireNotNull(belowPlacement.arrowCenterXPx), 0.5f)

        val abovePlacement = SpotlightOverlayLayoutMath.computeCardPlacement(
            containerWidthPx = 1080f,
            containerHeightPx = 2400f,
            safeTopPx = 100f,
            safeBottomPx = 100f,
            horizontalMarginPx = 48f,
            targetRect = Rect(left = 860f, top = 2120f, right = 1060f, bottom = 2280f),
            cardHeightPx = 420f,
            gapPx = 36f,
            arrowEdgeInsetPx = 72f,
        )
        assertFalse(abovePlacement.placeBelow)
        assertEquals(1664f, abovePlacement.topOffsetPx, 0.5f)
        // Clamped within right card edge inset: 1080 - 48 - 72 = 960
        assertEquals(960f, requireNotNull(abovePlacement.arrowCenterXPx), 0.5f)

        val fallbackPlacement = SpotlightOverlayLayoutMath.computeCardPlacement(
            containerWidthPx = 1080f,
            containerHeightPx = 2400f,
            safeTopPx = 100f,
            safeBottomPx = 100f,
            horizontalMarginPx = 48f,
            targetRect = null,
            cardHeightPx = 400f,
            gapPx = 36f,
            arrowEdgeInsetPx = 72f,
        )
        assertNull(fallbackPlacement.arrowCenterXPx)
        assertEquals(1000f, fallbackPlacement.topOffsetPx, 0.5f)
    }

    @Test
    fun spotlightOverlay_navigatesThroughStepsActionsAndFinish() {
        val steps = SpotlightTourCatalog.buildSteps(needsInitialSetup = true)
        val registry = SpotlightTargetRegistry().apply {
            setFixedRectForTest(SpotlightTargetId.LaunchStatusCard, Rect(40f, 120f, 360f, 260f))
            setFixedRectForTest(SpotlightTargetId.LaunchPrimaryAction, Rect(40f, 280f, 360f, 340f))
            setFixedRectForTest(SpotlightTargetId.DocsSpotlightReplay, Rect(240f, 90f, 350f, 140f))
        }
        var skippedCount = 0
        var finishedCount = 0
        var triggeredActionStepId: String? = null

        composeRule.setContent {
            LukoaTheme {
                var stepIndex by remember { mutableIntStateOf(0) }
                SpotlightTourOverlay(
                    active = true,
                    steps = steps,
                    currentStepIndex = stepIndex,
                    registry = registry,
                    rootCoords = null,
                    onPrevious = { if (stepIndex > 0) stepIndex -= 1 },
                    onNext = { if (stepIndex < steps.lastIndex) stepIndex += 1 },
                    onSkip = { skippedCount += 1 },
                    onFinish = { finishedCount += 1 },
                    onStepAction = { triggeredActionStepId = it.id },
                )
            }
        }

        composeRule.onNodeWithTag("spotlight-tour-overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("1 / 7")
        composeRule.onNodeWithText("一眼看清酒馆状态").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-prev").assertDoesNotExist()

        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("2 / 7")
        composeRule.onNodeWithText("从这里完成首次配置与启动").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-prev").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-step-action").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(SpotlightTourCatalog.STEP_LAUNCH_PRIMARY, triggeredActionStepId)
        }

        composeRule.onNodeWithTag("spotlight-tour-prev").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("1 / 7")

        repeat(steps.lastIndex) {
            composeRule.onNodeWithTag("spotlight-tour-next").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("7 / 7")
        composeRule.onNodeWithText("开始使用").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.runOnIdle {
            assertEquals(1, finishedCount)
        }

        composeRule.onNodeWithTag("spotlight-tour-skip").performClick()
        composeRule.runOnIdle {
            assertEquals(1, skippedCount)
        }
    }

    @Test
    fun launchControlSection_incompleteSetupPrimaryButtonOpensSetupWizardWithoutExtraPanel() {
        var setupWizardClicks = 0
        composeRule.setContent {
            LukoaTheme {
                TavernControlSection(
                    tavernRunning = false,
                    tavernStarting = false,
                    tavernVersion = "",
                    actionInProgress = false,
                    busyLabel = null,
                    wakeEnabled = false,
                    primaryEnabled = false,
                    primaryDisabledReason = "请先安装并打开 Termux。",
                    onWakeTermux = {},
                    onPrimaryAction = {},
                    onOpenTavern = {},
                    onExportLog = {},
                    onOpenSetupGuide = { setupWizardClicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText("第一次使用").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithText("先安装 Termux").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(1, setupWizardClicks)
        }
    }

    @Test
    fun documentationHeader_exposesSpotlightTourReplayAction() {
        var replayClicks = 0
        composeRule.setContent {
            LukoaTheme {
                DocumentationSection(
                    onStartSpotlightTour = { replayClicks += 1 },
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithTag("documentation-spotlight-tour-button")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, replayClicks)
        }
    }

    @Test
    fun launcherStateStore_persistsSpotlightTourSeenFlag() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = LauncherStateStore(context)
        assertFalse(store.hasSeenSpotlightTour())
        store.markSpotlightTourSeen()
        assertTrue(store.hasSeenSpotlightTour())
        assertNotNull(store)
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
    }
}
