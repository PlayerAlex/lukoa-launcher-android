package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TopNotificationBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun controller_appendsDownwardUpToMaxVisibleAndDismissesIndependently() {
        val controller = LukoaTopBannerController()

        val id1 = controller.show(title = "第一条提示", autoDismissMs = 0L)
        val id2 = controller.show(title = "第二条提示", autoDismissMs = 0L)
        assertEquals(listOf("第一条提示", "第二条提示"), controller.messages.map { it.title })
        assertEquals("第二条提示", controller.currentMessage?.title)

        // Same dedupeKey with identical content does not duplicate.
        val dupA = controller.show(title = "后台权限提醒", dedupeKey = "perm-bg", autoDismissMs = 0L)
        val dupB = controller.show(title = "后台权限提醒", dedupeKey = "perm-bg", autoDismissMs = 0L)
        assertEquals(dupA, dupB)
        assertEquals(3, controller.messages.size)

        // Dismissing only the first message leaves the rest intact in order.
        controller.dismiss(id1)
        assertEquals(listOf("第二条提示", "后台权限提醒"), controller.messages.map { it.title })

        // Pushing beyond MAX_VISIBLE_MESSAGES trims oldest while keeping newest downward.
        controller.show(title = "第三条提示", autoDismissMs = 0L)
        controller.show(title = "第四条提示", autoDismissMs = 0L)
        controller.show(title = "第五条提示", autoDismissMs = 0L)
        assertEquals(LukoaTopBannerController.MAX_VISIBLE_MESSAGES, controller.messages.size)
        assertEquals(
            listOf("后台权限提醒", "第三条提示", "第四条提示", "第五条提示"),
            controller.messages.map { it.title },
        )

        // Dismissing an unrelated id has no unintended effect; dismiss(null) clears all.
        controller.dismiss(id2)
        assertEquals(4, controller.messages.size)
        controller.dismiss()
        assertTrue(controller.messages.isEmpty())
    }

    @Test
    fun topBannerHost_stacksNewBannersDownwardAndRemovesEachIndependently() {
        val controller = LukoaTopBannerController()
        var busyLabel by mutableStateOf<String?>(null)

        composeRule.setContent {
            LukoaTheme {
                LukoaTopBannerHost(
                    controller = controller,
                    busyLabel = busyLabel,
                    busyStartedAtMillis = 1_000L,
                )
            }
        }

        var firstId = 0L
        composeRule.runOnIdle {
            firstId = controller.show(title = "已切换外观配色：夜间", autoDismissMs = 0L)
            controller.show(title = "已开启自动启动酒馆", autoDismissMs = 0L)
        }
        composeRule.waitForIdle()

        val firstNode = composeRule.onNodeWithText("已切换外观配色：夜间").assertIsDisplayed()
        val secondNode = composeRule.onNodeWithText("已开启自动启动酒馆").assertIsDisplayed()

        val firstBounds = firstNode.getBoundsInRoot()
        val secondBounds = secondNode.getBoundsInRoot()
        assertTrue(
            "新提示应向下追加到旧提示下方：firstBottom=${firstBounds.bottom}, secondTop=${secondBounds.top}",
            secondBounds.top >= firstBounds.bottom,
        )

        // Busy status bar and stacked notifications coexist cleanly without overwriting.
        composeRule.runOnIdle {
            busyLabel = "启动酒馆"
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("floating-busy-banner").assertIsDisplayed()
        composeRule.onNodeWithText("已开启自动启动酒馆").assertIsDisplayed()

        // Removing the first banner keeps the second banner visible.
        composeRule.runOnIdle {
            controller.dismiss(firstId)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("已切换外观配色：夜间").assertDoesNotExist()
        composeRule.onNodeWithText("已开启自动启动酒馆").assertIsDisplayed()
    }
}
