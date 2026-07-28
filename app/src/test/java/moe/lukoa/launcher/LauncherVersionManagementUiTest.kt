package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LauncherVersionManagementUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun versionPage_usesContinuousFlowAndOnlyShowsRelevantAction() {
        var updateCount = 0
        var rollbackCount = 0
        setVersionPageContent(
            target = versionChoice("1.14.0"),
            onUpdate = { updateCount += 1 },
            onRollback = { rollbackCount += 1 },
        )

        composeRule.onNodeWithText("当前安装").assertIsDisplayed()
        composeRule.onNodeWithText("版本分区").assertDoesNotExist()
        composeRule.onNodeWithText("提交").assertDoesNotExist()

        advancePastClickDebounce()
        composeRule.onNodeWithText("查看技术信息").performScrollTo().performClick()
        composeRule.onNodeWithText("提交").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("执行回退").assertDoesNotExist()
        composeRule.onNodeWithText("执行更新")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, updateCount)
            assertEquals(0, rollbackCount)
        }
    }

    @Test
    fun versionPage_olderTargetOnlyShowsRollbackAction() {
        var rollbackCount = 0
        setVersionPageContent(
            target = versionChoice("1.12.0"),
            onRollback = { rollbackCount += 1 },
        )

        composeRule.onNodeWithText("执行更新").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithText("执行回退")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, rollbackCount) }
    }

    private fun setVersionPageContent(
        target: TavernVersionChoice,
        onUpdate: () -> Unit = {},
        onRollback: () -> Unit = {},
    ) {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    VersionManagementSection(
                        actionsLocked = false,
                        tavernRunning = false,
                        tavernStarting = false,
                        tavernVersionInfo = TavernVersionInfo(
                            hasData = true,
                            directory = "~/SillyTavern",
                            packageVersion = "1.13.0",
                            branch = "release",
                            commit = "abcdef123456",
                            describe = "1.13.0",
                        ),
                        officialVersions = TavernOfficialVersions(
                            stable = listOf(target),
                        ),
                        currentRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
                        selectedVersion = target,
                        onRefreshOfficialVersions = {},
                        onSelectVersion = {},
                        onTavernVersion = {},
                        onTavernUpdate = onUpdate,
                        onTavernRollback = onRollback,
                    )
                }
            }
        }
    }

    private fun versionChoice(version: String): TavernVersionChoice {
        return TavernVersionChoice(
            kind = TavernVersionKind.Stable,
            name = version,
            target = version,
            repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
        )
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(300L))
        composeRule.waitForIdle()
    }
}
