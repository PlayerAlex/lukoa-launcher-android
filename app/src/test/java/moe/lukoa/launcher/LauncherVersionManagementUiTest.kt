package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
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
    fun versionPage_newerTargetEnablesUpdateAndDisablesRollback() {
        var updateCount = 0
        var rollbackCount = 0
        setVersionPageContent(
            target = versionChoice("1.14.0"),
            onUpdate = { updateCount += 1 },
            onRollback = { rollbackCount += 1 },
        )

        composeRule.onNodeWithText("当前版本").assertIsDisplayed()
        composeRule.onAllNodesWithText("目标版本").assertCountEquals(2)
        composeRule.onNodeWithText("版本分区").assertDoesNotExist()
        composeRule.onNodeWithText("提交").assertDoesNotExist()
        composeRule.onNodeWithText("查看版本详情").assertDoesNotExist()
        composeRule.onNodeWithText("目标版本高于当前版本，可以更新；回退暂不可用。").assertDoesNotExist()

        advancePastClickDebounce()
        composeRule.onNodeWithText("回退版本")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("更新版本")
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
    fun versionPage_olderTargetDisablesUpdateAndEnablesRollback() {
        var rollbackCount = 0
        setVersionPageContent(
            target = versionChoice("1.12.0"),
            onRollback = { rollbackCount += 1 },
        )

        composeRule.onNodeWithText("更新版本")
            .performScrollTo()
            .assertIsNotEnabled()
        advancePastClickDebounce()
        composeRule.onNodeWithText("回退版本")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, rollbackCount) }
    }

    @Test
    fun versionPage_localChangesUsesCompactSketchWarning() {
        val target = versionChoice("1.14.0")
        var openRepairToolsCount = 0
        setVersionPageContent(
            target = target,
            currentInfo = TavernVersionInfo(
                hasData = true,
                directory = "~/SillyTavern",
                packageVersion = "1.13.0",
                branch = "release",
                commit = "abcdef123456",
                describe = "1.13.0",
                remote = "https://mirror.example.com/SillyTavern.git",
                localChanges = "1",
                changedFilesPreview = " M public/index.html",
            ),
            onOpenRepairTools = { openRepairToolsCount += 1 },
        )

        composeRule.onNodeWithText("警告").assertIsDisplayed()
        composeRule.onNodeWithText(
            "检测到本地文件被修改过，请前往修复工具检查并调整后，再继续更新或回退。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("当前酒馆位置").assertIsDisplayed()
        composeRule.onNodeWithText("~/SillyTavern").assertIsDisplayed()
        composeRule.onNodeWithText("当前酒馆来源").assertIsDisplayed()
        composeRule.onNodeWithText("https://mirror.example.com/SillyTavern.git").assertIsDisplayed()
        composeRule.onNodeWithText("版本来源").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("修改位置：~/SillyTavern").assertDoesNotExist()
        composeRule.onNodeWithText("检测到的文件").assertDoesNotExist()
        composeRule.onNodeWithText("恢复聊天文件大小默认值").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithText("前往修复工具").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1, openRepairToolsCount) }
    }

    @Test
    fun versionPage_showsLastOperationAndOpensSafetyBackupEntry() {
        var openBackupCount = 0
        setVersionPageContent(
            target = versionChoice("1.14.0"),
            lastOperationSummary = TavernVersionOperationSummary(
                kind = TavernVersionActionKind.Update,
                target = "1.14.0",
                beforeRevision = "abc1234",
                afterRevision = "def5678",
                exitCode = 0,
                npmExitCode = 0,
                safetyBackupPath = "/backups/update-safe.tar.gz",
            ),
            onOpenSafetyBackup = { openBackupCount += 1 },
        )

        composeRule.onNodeWithText("上次执行结果").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("更新已完成").assertIsDisplayed()
        advancePastClickDebounce()
        composeRule.onNodeWithText("到备份页查看安全备份")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, openBackupCount) }
    }

    private fun setVersionPageContent(
        target: TavernVersionChoice,
        currentInfo: TavernVersionInfo = TavernVersionInfo(
            hasData = true,
            directory = "~/SillyTavern",
            packageVersion = "1.13.0",
            branch = "release",
            commit = "abcdef123456",
            describe = "1.13.0",
        ),
        lastOperationSummary: TavernVersionOperationSummary? = null,
        onUpdate: () -> Unit = {},
        onRollback: () -> Unit = {},
        onOpenSafetyBackup: () -> Unit = {},
        onOpenRepairTools: () -> Unit = {},
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
                        tavernVersionInfo = currentInfo,
                        officialVersions = TavernOfficialVersions(
                            stable = listOf(target),
                        ),
                        currentRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
                        selectedVersion = target,
                        lastOperationSummary = lastOperationSummary,
                        onRefreshAllVersions = {},
                        onSelectVersion = {},
                        onTavernUpdate = onUpdate,
                        onTavernRollback = onRollback,
                        onOpenSafetyBackup = onOpenSafetyBackup,
                        onOpenRepairTools = onOpenRepairTools,
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
