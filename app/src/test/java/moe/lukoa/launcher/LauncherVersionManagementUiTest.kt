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

    @Test
    fun versionPage_localChangesShowsWhereAndHowToRestore() {
        val target = versionChoice("1.14.0")
        setVersionPageContent(
            target = target,
            currentInfo = TavernVersionInfo(
                hasData = true,
                directory = "~/SillyTavern",
                packageVersion = "1.13.0",
                branch = "release",
                commit = "abcdef123456",
                describe = "1.13.0",
                localChanges = "1",
                changedFilesPreview = " M public/index.html",
            ),
        )

        composeRule.onNodeWithText("检测到本地修改").assertIsDisplayed()
        composeRule.onNodeWithText("修改位置：~/SillyTavern").assertIsDisplayed()
        composeRule.onNodeWithText(
            "要改回原文件：先到备份页生成手动备份，再打开 Termux 进入这个目录，用 Git 恢复改动；完成后回到这里重新检测。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("检测到的文件").assertIsDisplayed()
    }

    @Test
    fun versionPage_uploadLimitChangeOffersSafeDefaultRestore() {
        var resetCount = 0
        val target = versionChoice("1.14.0")
        setVersionPageContent(
            target = target,
            currentInfo = TavernVersionInfo(
                hasData = true,
                directory = "~/SillyTavern",
                packageVersion = "1.13.0",
                branch = "release",
                localChanges = "1",
                changedFilesPreview = " M src/server-main.js",
            ),
            uploadLimitStatus = TavernUploadLimitStatus(
                currentMegabytes = 1024,
                patchState = TavernUploadLimitPatchState.Active,
            ),
            onResetUploadLimit = { resetCount += 1 },
        )

        composeRule.onNodeWithText(
            "这很可能是你在“设置 → 修复工具 → 聊天文件大小”中修改过数值。要更新或回退，请先恢复当前酒馆版本的默认值。",
        ).assertIsDisplayed()
        advancePastClickDebounce()
        composeRule.onNodeWithText("恢复聊天文件大小默认值")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(0, resetCount) }
        composeRule.onNodeWithText("确认恢复默认值").performClick()
        composeRule.runOnIdle { assertEquals(1, resetCount) }
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
        uploadLimitStatus: TavernUploadLimitStatus = TavernUploadLimitStatus(),
        lastOperationSummary: TavernVersionOperationSummary? = null,
        onResetUploadLimit: () -> Unit = {},
        onUpdate: () -> Unit = {},
        onRollback: () -> Unit = {},
        onOpenSafetyBackup: () -> Unit = {},
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
                        onRefreshOfficialVersions = {},
                        onSelectVersion = {},
                        onTavernVersion = {},
                        onTavernUpdate = onUpdate,
                        onTavernRollback = onRollback,
                        onOpenSafetyBackup = onOpenSafetyBackup,
                        uploadLimitStatus = uploadLimitStatus,
                        onResetUploadLimit = onResetUploadLimit,
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
