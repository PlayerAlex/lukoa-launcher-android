package moe.lukoa.launcher

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class LauncherToolboxUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toolboxSection_matchesSketchStructureAndEmptyHealthState() {
        setToolboxContent()

        composeRule.onNodeWithText("工具箱").assertIsDisplayed()
        composeRule.onNodeWithText("一键体检工具").assertIsDisplayed()
        composeRule.onNodeWithText("当前状态：").assertIsDisplayed()
        composeRule.onNodeWithText("未体检").assertIsDisplayed()
        composeRule.onNodeWithTag("toolbox-health-status-plain").assertExists()
        composeRule.onNodeWithText("上次体检时间：暂无").assertIsDisplayed()
        composeRule.onNodeWithText("修复工具").assertIsDisplayed()
        composeRule.onNodeWithText("Debug 区").assertIsDisplayed()
        composeRule.onNodeWithText("任务中心").assertIsDisplayed()
        composeRule.onAllNodesWithText("敬请期待").assertCountEquals(3)
    }

    @Test
    fun toolboxSection_primaryEntriesOpenTheirExistingFunctions() {
        var healthCheckCount = 0
        var taskCenterCount = 0
        setToolboxContent(
            onRunHealthCheck = { healthCheckCount += 1 },
            onOpenBackgroundTaskCenter = { taskCenterCount += 1 },
        )

        advancePastClickDebounce()
        composeRule.onNodeWithText("一键体检").performClick()
        composeRule.runOnIdle { assertEquals(1, healthCheckCount) }

        advancePastClickDebounce()
        composeRule.onNodeWithText("查看详情").performClick()
        composeRule.onNodeWithText("还没体检").assertIsDisplayed()
        composeRule.onNodeWithTag("health-check-run-action").assertDoesNotExist()
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("修复工具").performClick()
        composeRule.onNodeWithText("常用修复").assertIsDisplayed()
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("Debug 区").performClick()
        composeRule.onNodeWithText("导出诊断日志").assertIsDisplayed()
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("任务中心").performClick()
        composeRule.runOnIdle { assertEquals(1, taskCenterCount) }
    }

    @Test
    fun toolboxSection_completedHealthStatusRemainsPlainText() {
        setToolboxContent(
            healthCheckReport = LauncherHealthReport(
                checkedAtMillis = 1_786_428_240_000L,
                summaryTitle = "基本正常",
                summaryDetail = "当前环境可以使用",
                items = listOf(LauncherHealthItem("Termux", "已就绪", LauncherHealthLevel.Good)),
            ),
        )

        composeRule.onNodeWithText("基本正常").assertIsDisplayed()
        composeRule.onNodeWithTag("toolbox-health-status-plain").assertIsDisplayed()
    }

    @Test
    fun toolboxSection_externalRepairSignalIsConsumedOnce() {
        var consumedCount = 0
        setToolboxContent(
            repairToolsOpenSignal = 1,
            onRepairToolsOpenSignalConsumed = { consumedCount += 1 },
        )

        composeRule.onNodeWithText("常用修复").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, consumedCount) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(1, consumedCount) }
    }

    private fun setToolboxContent(
        healthCheckReport: LauncherHealthReport? = null,
        repairToolsOpenSignal: Int = 0,
        onRepairToolsOpenSignalConsumed: () -> Unit = {},
        onRunHealthCheck: () -> Unit = {},
        onOpenBackgroundTaskCenter: () -> Unit = {},
    ) {
        composeRule.setContent {
            LukoaTheme {
                ToolboxSection(
                    healthCheckReport = healthCheckReport,
                    repairToolsOpenSignal = repairToolsOpenSignal,
                    onRepairToolsOpenSignalConsumed = onRepairToolsOpenSignalConsumed,
                    onRunHealthCheck = onRunHealthCheck,
                    onOpenBackgroundTaskCenter = onOpenBackgroundTaskCenter,
                )
            }
        }
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
    }
}
