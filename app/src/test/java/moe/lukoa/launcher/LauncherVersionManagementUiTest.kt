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
import androidx.compose.ui.test.onNodeWithTag
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

        composeRule.onNodeWithText("程序文件有你改过的地方").assertIsDisplayed()
        composeRule.onNodeWithText(
            "改过的文件：public/index.html。更新或回退时会先把它们存进 Git 暂存区再继续，执行前会再让你确认。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("当前酒馆位置").assertIsDisplayed()
        composeRule.onNodeWithText("~/SillyTavern").assertIsDisplayed()
        composeRule.onNodeWithText("当前酒馆来源").assertIsDisplayed()
        composeRule.onNodeWithText("https://mirror.example.com/SillyTavern.git").assertIsDisplayed()
        composeRule.onNodeWithText("版本来源").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("恢复聊天文件大小默认值").assertDoesNotExist()
        // Repair tools cannot do anything about a user's own edits, so no detour button here.
        composeRule.onNodeWithText("去修复工具查看").assertDoesNotExist()
        // The update button stays usable; consent is collected in the confirmation dialog instead.
        composeRule.onNodeWithText("处理本地修改后再继续").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, openRepairToolsCount) }
    }

    @Test
    fun versionPage_managedOnlyLocalChangesStayInformational() {
        var openRepairToolsCount = 0
        setVersionPageContent(
            target = versionChoice("1.14.0"),
            currentInfo = TavernVersionInfo(
                hasData = true,
                directory = "~/SillyTavern",
                packageVersion = "1.13.0",
                branch = "release",
                commit = "abcdef123456",
                describe = "1.13.0",
                remote = "https://github.com/SillyTavern/SillyTavern.git",
                localChanges = "1",
                changedFilesPreview = " M src/server-main.js\n M package-lock.json",
                changedFiles = listOf("src/server-main.js", "package-lock.json"),
            ),
            onOpenRepairTools = { openRepairToolsCount += 1 },
        )

        composeRule.onNodeWithText("含启动器自己的修改").assertIsDisplayed()
        composeRule.onNodeWithText("程序文件有你改过的地方").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithText("去修复工具查看").performScrollTo().performClick()
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
        composeRule.onNodeWithText("本地改动已存入").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithText("到备份页查看安全备份")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, openBackupCount) }
    }

    @Test
    fun versionPage_tellsWhereStashedLocalChangesWent() {
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
                localChangesStash = "lukoa-before-update-20260904-013000",
            ),
        )

        composeRule.onNodeWithText("本地改动已存入").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("git stash「lukoa-before-update-20260904-013000」")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "你之前改过的程序文件没有丢，在 Termux 的酒馆目录里执行 git stash list 可以看到，需要时用 git stash pop 取回。",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun versionActionDialog_userEditsMustBeAcknowledgedBeforeConfirm() {
        val confirmations = mutableListOf<Boolean>()
        setConfirmDialogContent(
            userOwnedChanges = listOf("public/index.html", "start.sh"),
            onConfirm = { confirmations += it },
        )

        composeRule.onNodeWithText("酒馆程序文件有你自己改过的地方").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("public/index.html").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("start.sh").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("我知道了，先存起来再更新").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("确认更新").assertIsNotEnabled()

        advancePastClickDebounce()
        composeRule.onNodeWithText("确认更新").performClick()
        composeRule.runOnIdle { assertEquals(emptyList<Boolean>(), confirmations) }

        composeRule.onNodeWithTag("version-local-changes-consent-checkbox").performScrollTo().performClick()
        composeRule.onNodeWithText("确认更新").assertIsEnabled()
        advancePastClickDebounce()
        composeRule.onNodeWithText("确认更新").performClick()
        composeRule.runOnIdle { assertEquals(listOf(true), confirmations) }
    }

    @Test
    fun versionActionDialog_cleanCheckoutConfirmsWithoutStashConsent() {
        val confirmations = mutableListOf<Boolean>()
        setConfirmDialogContent(
            userOwnedChanges = emptyList(),
            onConfirm = { confirmations += it },
        )

        composeRule.onNodeWithText("酒馆程序文件有你自己改过的地方").assertDoesNotExist()
        composeRule.onNodeWithTag("version-local-changes-consent").assertDoesNotExist()
        composeRule.onNodeWithText("确认更新").assertIsEnabled()

        advancePastClickDebounce()
        composeRule.onNodeWithText("确认更新").performClick()
        composeRule.runOnIdle { assertEquals(listOf(false), confirmations) }
    }

    private fun setConfirmDialogContent(
        userOwnedChanges: List<String>,
        onConfirm: (Boolean) -> Unit,
    ) {
        val current = TavernVersionInfo(
            hasData = true,
            directory = "~/SillyTavern",
            packageVersion = "1.13.0",
            branch = "release",
            commit = "abcdef123456",
            describe = "1.13.0",
            localChanges = if (userOwnedChanges.isEmpty()) "" else "1",
            changedFiles = userOwnedChanges,
        )
        val confirmation = TavernVersionActionConfirmationBuilder.build(
            kind = TavernVersionActionKind.Update,
            current = current,
            target = versionChoice("1.14.0"),
            fallbackRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
        )
        composeRule.setContent {
            LukoaTheme {
                TavernVersionActionConfirmDialog(
                    confirmation = confirmation,
                    actionsLocked = false,
                    onConfirm = onConfirm,
                    onDismiss = {},
                )
            }
        }
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
