package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun resolveSetupStage_mapsEveryFirstTimeConditionAccurately() {
        assertEquals(
            SpotlightSetupStage.InstallTermux,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = false,
                runCommandPermissionGranted = false,
                externalAppsBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = null,
            ),
        )
        assertEquals(
            SpotlightSetupStage.RequestPermission,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = false,
                externalAppsBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = null,
            ),
        )
        assertEquals(
            SpotlightSetupStage.EnableExternalApps,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                externalAppsBlocked = true,
                termuxSetupRecommended = false,
                tavernInstallDetected = null,
            ),
        )
        assertEquals(
            SpotlightSetupStage.PrepareEnvironment,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                externalAppsBlocked = false,
                termuxSetupRecommended = true,
                tavernInstallDetected = null,
            ),
        )
        assertEquals(
            SpotlightSetupStage.DetectTavern,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                externalAppsBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = null,
            ),
        )
        assertEquals(
            SpotlightSetupStage.InstallTavern,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                externalAppsBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = false,
            ),
        )
        assertEquals(
            SpotlightSetupStage.Ready,
            SpotlightTourCatalog.resolveSetupStage(
                termuxInstalled = true,
                runCommandPermissionGranted = true,
                externalAppsBlocked = false,
                termuxSetupRecommended = false,
                tavernInstallDetected = true,
            ),
        )
    }

    @Test
    fun catalog_unifiesFirstTimeSetupStepsAndStreamlinesReadyTour() {
        val freshSteps = SpotlightTourCatalog.buildSteps(
            needsInitialSetup = true,
            setupStage = SpotlightSetupStage.InstallTermux,
        )
        val readySteps = SpotlightTourCatalog.buildSteps(
            needsInitialSetup = false,
            setupStage = SpotlightSetupStage.Ready,
        )

        assertEquals(10, freshSteps.size)
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
        assertEquals(
            3,
            freshSteps.count { it.requiredSecondaryPage == LauncherSecondaryPage.SetupWizard },
        )
        assertEquals(
            0,
            readySteps.count { it.requiredSecondaryPage == LauncherSecondaryPage.SetupWizard },
        )

        val freshLaunchPrimary = freshSteps.first { it.id == SpotlightTourCatalog.STEP_LAUNCH_PRIMARY }
        assertTrue(freshLaunchPrimary.allowTargetTap)
        assertEquals("打开安装向导", freshLaunchPrimary.nextLabel)

        val setupPrimary = freshSteps.first { it.id == SpotlightTourCatalog.STEP_SETUP_PRIMARY }
        assertTrue(setupPrimary.allowTargetTap)
        assertEquals("立即下载 Termux", setupPrimary.actionLabel)
        assertTrue(setupPrimary.description.contains("开启代理"))

        val readyPrimary = readySteps.first { it.id == SpotlightTourCatalog.STEP_LAUNCH_PRIMARY }
        assertFalse(readyPrimary.allowTargetTap)
        assertEquals("一键启动或停止酒馆", readyPrimary.title)

        val setupOnly = SpotlightTourCatalog.buildSetupOnlySteps(
            setupStage = SpotlightSetupStage.EnableExternalApps,
            followedByFeatureTour = false,
        )
        assertEquals(3, setupOnly.size)
        assertEquals("复制权限命令", setupOnly[1].actionLabel)
        assertNull(setupOnly[2].actionLabel)

        val installTavernGuide = SpotlightTourCatalog.buildSetupOnlySteps(
            setupStage = SpotlightSetupStage.InstallTavern,
            followedByFeatureTour = false,
        )
        assertTrue(installTavernGuide[1].description.contains("开启代理"))
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
    fun spotlightOverlay_navigatesUnifiedFirstTimeFlowActionsAndFinish() {
        val steps = SpotlightTourCatalog.buildSteps(
            needsInitialSetup = true,
            setupStage = SpotlightSetupStage.InstallTermux,
        )
        val registry = SpotlightTargetRegistry().apply {
            setFixedRectForTest(SpotlightTargetId.LaunchStatusCard, Rect(40f, 120f, 360f, 260f))
            setFixedRectForTest(SpotlightTargetId.LaunchPrimaryAction, Rect(40f, 280f, 360f, 340f))
            setFixedRectForTest(SpotlightTargetId.SetupStepProgress, Rect(40f, 120f, 360f, 210f))
            setFixedRectForTest(SpotlightTargetId.SetupPrimaryAction, Rect(40f, 300f, 360f, 360f))
            setFixedRectForTest(SpotlightTargetId.SetupSecondaryActions, Rect(40f, 380f, 360f, 460f))
            setFixedRectForTest(SpotlightTargetId.BottomTabSettings, Rect(260f, 680f, 360f, 740f))
            setFixedRectForTest(SpotlightTargetId.DocsSpotlightReplay, Rect(240f, 90f, 350f, 140f))
        }
        var skippedCount = 0
        var finishedCount = 0
        val triggeredActionStepIds = mutableListOf<String>()

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
                    onStepAction = { triggeredActionStepIds += it.id },
                )
            }
        }

        composeRule.onNodeWithTag("spotlight-tour-overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("1 / 10")
        composeRule.onNodeWithText("欢迎使用露科亚启动器").assertIsDisplayed()
        composeRule.onNodeWithText("开始第 1 步").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-prev").assertDoesNotExist()

        // Step 2: Launch primary button -> opens setup wizard
        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("2 / 10")
        composeRule.onNodeWithText("从这里进入首次安装向导").assertIsDisplayed()
        composeRule.onNodeWithText("打开安装向导").assertIsDisplayed()

        // Step 3: SetupWizard 4-step progress bar
        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("3 / 10")
        composeRule.onNodeWithText("一共只需完成这 4 步").assertIsDisplayed()

        // Step 4: SetupWizard current stage primary action
        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("4 / 10")
        composeRule.onNodeWithText("当前步骤：安装并打开一次 Termux").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight-tour-step-action").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(SpotlightTourCatalog.STEP_SETUP_PRIMARY), triggeredActionStepIds)
        }

        // Step 5: SetupWizard secondary / recheck actions
        composeRule.onNodeWithTag("spotlight-tour-next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("5 / 10")
        composeRule.onNodeWithText("留在这里开始配置").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    SpotlightTourCatalog.STEP_SETUP_PRIMARY,
                    SpotlightTourCatalog.STEP_SETUP_SECONDARY,
                ),
                triggeredActionStepIds,
            )
        }

        // Go back one step cleanly
        composeRule.onNodeWithTag("spotlight-tour-prev").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("4 / 10")

        // Walk to final step (10 / 10)
        repeat(steps.lastIndex - 3) {
            composeRule.onNodeWithTag("spotlight-tour-next").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("spotlight-tour-step-indicator").assertTextEquals("10 / 10")
        composeRule.onNodeWithText("完成引导").assertIsDisplayed()
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
    fun spotlightOverlay_locksNextButtonDuringCrossPageTransition() {
        val steps = SpotlightTourCatalog.buildSteps(needsInitialSetup = false)
        val registry = SpotlightTargetRegistry()

        composeRule.setContent {
            LukoaTheme {
                SpotlightTourOverlay(
                    active = true,
                    steps = steps,
                    currentStepIndex = 0,
                    registry = registry,
                    rootCoords = null,
                    isTransitioning = true,
                    onPrevious = {},
                    onNext = {},
                    onSkip = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithTag("spotlight-tour-next").assertIsNotEnabled()
    }

    @Test
    fun setupWizardPage_rendersModernHorizontalStepperAndSpotlightTrigger() {
        var spotlightClicks = 0
        var primaryClicks = 0

        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    QuickStartGuideSection(
                        termuxInstalled = false,
                        runCommandPermissionGranted = false,
                        externalAppsBlocked = false,
                        tavernInstallDetected = null,
                        tavernVersionChecking = false,
                        termuxSetupRecommended = false,
                        officialVersions = TavernOfficialVersions(),
                        selectedVersion = null,
                        mirrorRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
                        commandText = "allow-external-apps=true",
                        actionsLocked = false,
                        onOpenTermuxDownload = { primaryClicks += 1 },
                        onOpenTermuxGithub = {},
                        onRecheckTermux = {},
                        onRequestPermission = {},
                        onOpenPermissionSettings = {},
                        onCopyPermissionCommand = {},
                        onOpenTermux = {},
                        onRecheckPermission = {},
                        onPrepareTermux = {},
                        onCheckTavern = {},
                        onShowInstall = {},
                        onRefreshOfficialVersions = {},
                        onSelectVersion = {},
                        onUseRecommendedVersion = {},
                        onInstallTavern = {},
                        showSectionContainer = false,
                        onStartSetupSpotlight = { spotlightClicks += 1 },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("setup-wizard-stepper").assertIsDisplayed()
        composeRule.onNodeWithTag("setup-wizard-primary-action").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("setup-wizard-secondary-actions").performScrollTo().assertIsDisplayed()

        advancePastClickDebounce()
        composeRule.onNodeWithTag("setup-wizard-spotlight-button").performScrollTo().assertIsDisplayed().performClick()
        advancePastClickDebounce()
        composeRule.onNodeWithTag("setup-wizard-primary-action").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, spotlightClicks)
            assertEquals(1, primaryClicks)
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
