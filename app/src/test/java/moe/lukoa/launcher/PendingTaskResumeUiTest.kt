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

        composeRule.onNodeWithText("任务跟踪已中断").assertIsDisplayed()
        composeRule.onNodeWithText("检测到上次任务没收尾").assertDoesNotExist()
        composeRule.onNodeWithText(
            "启动器重新打开后，无法继续实时跟踪这次操作。Termux 里的任务可能仍在执行，这不代表任务失败，也不会自动重做。\n推荐先检查结果：只会查找已有返回并刷新状态。",
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
                        activeLockLabel = "正在应用酒馆备份",
                        onContinueCheck = {},
                        onAbandon = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("上次操作等待确认").assertIsDisplayed()
        composeRule.onNodeWithText("应用酒馆备份 ·", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("检查上次操作的结果").assertIsDisplayed()
        composeRule.onNodeWithText("不再跟踪这次操作").assertIsDisplayed()
        composeRule.onNodeWithText(
            "启动器已重新打开，Termux 里的任务可能仍在执行。先检查已有结果；不会重新执行应用酒馆备份。",
        ).assertIsDisplayed()
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
