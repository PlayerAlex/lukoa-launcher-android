package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
        composeRule.onNodeWithText("聊天守护").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("空间清理").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("修复工具").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("任务中心").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Debug 区").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("暂存管理").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("敬请期待").assertCountEquals(0)
    }

    @Test
    fun toolboxSection_primaryEntriesOpenTheirExistingFunctions() {
        var healthCheckCount = 0
        var taskCenterCount = 0
        var chatGuardRefreshCount = 0
        var storageScanCount = 0
        var gitStashRefreshCount = 0
        setToolboxContent(
            onRunHealthCheck = { healthCheckCount += 1 },
            onOpenBackgroundTaskCenter = { taskCenterCount += 1 },
            onRefreshChatGuard = { chatGuardRefreshCount += 1 },
            onScanStorage = { storageScanCount += 1 },
            onRefreshGitStash = { gitStashRefreshCount += 1 },
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
        composeRule.onNodeWithText("修复工具").performScrollTo().performClick()
        composeRule.onNodeWithText("常用修复").assertIsDisplayed()
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("Debug 区").performScrollTo().performClick()
        composeRule.onNodeWithText("导出诊断日志").assertIsDisplayed()
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("任务中心").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, taskCenterCount) }

        advancePastClickDebounce()
        composeRule.onNodeWithText("聊天守护").performScrollTo().performClick()
        composeRule.onNodeWithText("刷新守护状态").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, chatGuardRefreshCount) }
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("空间清理").performScrollTo().performClick()
        composeRule.onNodeWithText("扫描当前空间占用").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, storageScanCount) }
        composeRule.onNodeWithText("关闭").performClick()

        advancePastClickDebounce()
        composeRule.onNodeWithText("暂存管理").performScrollTo().performClick()
        composeRule.onNodeWithText("读取暂存记录").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, gitStashRefreshCount) }
        composeRule.onNodeWithText("关闭").performClick()
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

    @Test
    fun toolboxDebug_usesGroupedCardsAndTimedDangerConfirmation() {
        var forceCleanupCount = 0
        setToolboxContent(onForceCleanup = { forceCleanupCount += 1 })

        advancePastClickDebounce()
        composeRule.onNodeWithText("Debug 区").performScrollTo().performClick()
        composeRule.onNodeWithText("诊断与日志").assertIsDisplayed()
        composeRule.onNodeWithText("强制处理").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "导出内容适合用于排查问题；清除只会整理启动器页面里的日志，不会删除酒馆聊天或备份。",
        ).performScrollTo().assertIsDisplayed()

        advancePastClickDebounce()
        composeRule.onNodeWithText("强制释放端口 / 清理残留进程")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("真的吗？").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, forceCleanupCount) }

        advancePastClickDebounce()
        composeRule.onNodeWithText("真的吗？").performClick()
        composeRule.runOnIdle { assertEquals(1, forceCleanupCount) }
    }

    private fun setToolboxContent(
        healthCheckReport: LauncherHealthReport? = null,
        repairToolsOpenSignal: Int = 0,
        onRepairToolsOpenSignalConsumed: () -> Unit = {},
        onRunHealthCheck: () -> Unit = {},
        onOpenBackgroundTaskCenter: () -> Unit = {},
        onRefreshChatGuard: () -> Unit = {},
        onScanStorage: () -> Unit = {},
        onRefreshGitStash: () -> Unit = {},
        onForceCleanup: () -> Unit = {},
    ) {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ToolboxSection(
                        healthCheckReport = healthCheckReport,
                        repairToolsOpenSignal = repairToolsOpenSignal,
                        onRepairToolsOpenSignalConsumed = onRepairToolsOpenSignalConsumed,
                        onRunHealthCheck = onRunHealthCheck,
                        onOpenBackgroundTaskCenter = onOpenBackgroundTaskCenter,
                        onRefreshChatGuard = onRefreshChatGuard,
                        onScanStorage = onScanStorage,
                        onRefreshGitStash = onRefreshGitStash,
                        onForceCleanup = onForceCleanup,
                    )
                }
            }
        }
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
    }
}
