package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PendingTaskResumeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resumeDialog_explainsCheckDoesNotRepeatTaskAndConfirmsAbandon() {
        var abandonCount = 0
        composeRule.setContent {
            LukoaTheme {
                PendingTaskResumeDialog(
                    task = pendingTask(),
                    activeLockLabel = "正在应用酒馆备份",
                    onContinueCheck = {},
                    onAbandon = { abandonCount += 1 },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("上次操作需要确认").assertIsDisplayed()
        composeRule.onNodeWithText("检测到上次任务没收尾").assertDoesNotExist()
        composeRule.onNodeWithText(
            "启动器没有收到上次操作的最终结果。这不代表操作失败，也不会自动再执行一次。\n推荐先检查结果：只会查找已有返回并刷新状态。",
        ).assertIsDisplayed()

        advancePastClickDebounce()
        composeRule.onNode(hasText("不再跟踪这次操作") and hasClickAction()).performClick()
        composeRule.runOnIdle { assertEquals(0, abandonCount) }
        composeRule.onNodeWithText("确认不再跟踪？").assertIsDisplayed()

        advancePastClickDebounce()
        composeRule.onNode(hasText("确认不再跟踪") and hasClickAction()).performClick()
        composeRule.runOnIdle { assertEquals(1, abandonCount) }
    }

    @Test
    fun noticePanel_usesFullClearActionsAndKeepsDetailsInInfoButton() {
        composeRule.setContent {
            LukoaTheme {
                Column {
                    PendingTaskNoticePanel(
                        task = pendingTask(),
                        activeLockLabel = null,
                        actionsLocked = false,
                        onContinueCheck = {},
                        onAbandon = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("有一项操作需要确认").assertIsDisplayed()
        composeRule.onNodeWithText("检查上次操作的结果").assertIsDisplayed()
        composeRule.onNodeWithText("不再跟踪这次操作").assertIsDisplayed()
        composeRule.onNodeWithText("检测到未完成任务").assertDoesNotExist()
    }

    private fun pendingTask() = PendingLauncherTask(
        kind = PendingLauncherTaskKind.RestoreBackup,
        commandName = "tavern-restore",
        detail = "正在应用酒馆备份",
        startedAtMillis = 1_700_000_000_000L,
        targetLabel = "主实例",
        archivePath = "/storage/emulated/0/Download/LukoaLauncher/backups/sd/test.tar.gz",
    )

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(300L))
        composeRule.waitForIdle()
    }
}
