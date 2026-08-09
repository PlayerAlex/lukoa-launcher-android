package moe.lukoa.launcher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LauncherStateRestorationUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `user creation dialog and typed values survive state restoration`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            LukoaTheme {
                TavernUserManagementSettingsPanel(
                    state = TavernUserManagementState(),
                    instanceLabel = "主实例",
                    actionsLocked = false,
                    tavernRunning = false,
                    onRefresh = {},
                    onCreate = { _, _ -> },
                    onDelete = {},
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("管理酒馆用户").performClick()
        advancePastClickDebounce()
        composeRule.onNodeWithText("新增用户").performClick()
        composeRule.onNodeWithText("登录标识").performTextInput("alice")
        composeRule.onNodeWithText("显示名称").performTextInput("清凉用户")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("新增酒馆用户").assertExists()
        composeRule.onNodeWithText("alice").assertExists()
        composeRule.onNodeWithText("清凉用户").assertExists()
    }

    @Test
    fun `extension dialog and search query survive state restoration`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            LukoaTheme {
                TavernExtensionManagementSettingsPanel(
                    state = TavernExtensionManagementState(
                        rootDirectory = "~/SillyTavern/public/scripts/extensions/third-party",
                        extensions = listOf(
                            TavernExtensionRecord("mint", "Mint", "1.0", true, "Lukoa", 10),
                            TavernExtensionRecord("warm", "Warm", "1.0", true, "Lukoa", 10),
                        ),
                    ),
                    instanceLabel = "主实例",
                    actionsLocked = false,
                    tavernRunning = false,
                    onRefresh = {},
                    onDelete = {},
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("管理已安装扩展").performClick()
        composeRule.onNodeWithText("搜索扩展").performTextInput("Mint")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("显示 1 / 2 个扩展").assertExists()
    }

    @Test
    fun `extension install dialog and repository survive state restoration`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            LukoaTheme {
                TavernExtensionManagementSettingsPanel(
                    state = TavernExtensionManagementState(
                        rootDirectory = "~/SillyTavern/public/scripts/extensions/third-party",
                    ),
                    instanceLabel = "主实例",
                    actionsLocked = false,
                    tavernRunning = false,
                    onRefresh = {},
                    onDelete = {},
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("管理已安装扩展").performClick()
        advancePastClickDebounce()
        composeRule.onNodeWithText("安装扩展").performClick()
        composeRule.onNodeWithText("GitHub 扩展地址")
            .performTextInput("https://github.com/owner/mint")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("安装酒馆扩展").assertExists()
        composeRule.onNodeWithText("https://github.com/owner/mint").assertExists()
    }

    @Test
    fun `repair tools dialog survives state restoration`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            LukoaTheme {
                RepairToolsSettingsPanel(
                    instanceLabel = "主实例",
                    summaryText = "未体检",
                    summaryColor = LukoaColors.TextSecondary,
                    actionsLocked = false,
                    tavernRunning = false,
                    uploadLimitStatus = TavernUploadLimitStatus(),
                    onRepairDependencies = {},
                    onResetTheme = {},
                    onSetNodeMemory = {},
                    onCheckUploadLimit = {},
                    onSetUploadLimit = {},
                    onResetUploadLimit = {},
                    onShowHint = {},
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("检查与修复").performClick()
        composeRule.onNodeWithText("修复 npm 依赖").assertExists()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("修复 npm 依赖").assertExists()
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(300L))
        composeRule.waitForIdle()
    }
}
