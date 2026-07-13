package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
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
class LauncherSettingsUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launcherVersionSummary_withoutCheckResult_keepsCurrentVersion() {
        assertEquals(
            "0.9.3-beta3",
            launcherVersionSummary(
                currentVersion = "0.9.3-beta3",
                latest = null,
            ),
        )
    }

    @Test
    fun launcherVersionSummary_withNewerRelease_showsCurrentAndTargetVersions() {
        assertEquals(
            "0.9.3-beta3 → 0.9.3-beta4",
            launcherVersionSummary(
                currentVersion = "0.9.3-beta3",
                latest = updateInfo(isNewer = true),
            ),
        )
    }

    @Test
    fun launcherVersionSummary_withNonNewerRelease_keepsCurrentVersion() {
        assertEquals(
            "0.9.3-beta3",
            launcherVersionSummary(
                currentVersion = "0.9.3-beta3",
                latest = updateInfo(isNewer = false),
            ),
        )
    }

    @Test
    fun launcherUpdateSettingsPanel_showsUpgradeAndTwoCapsuleActions() {
        setUpdatePanelContent()

        composeRule.onNodeWithText("启动器更新").assertIsDisplayed()
        composeRule.onNodeWithText("0.9.3-beta3 → 0.9.3-beta4")
            .performScrollTo()
            .assertIsDisplayed()

        val actionMatcher = hasClickAction() and (
            hasText("检查更新") or hasText("打开发布页")
        )
        composeRule.onAllNodes(actionMatcher).assertCountEquals(2)
        composeRule.onNode(hasText("检查更新") and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(hasText("打开发布页") and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun launcherUpdateSettingsPanel_dispatchesUpdateActions() {
        var openSettingsCount = 0
        var installUpdateCount = 0
        var checkUpdateCount = 0
        var openReleaseCount = 0
        setUpdatePanelContent(
            onOpenSettings = { openSettingsCount += 1 },
            onInstallUpdate = { installUpdateCount += 1 },
            onCheckUpdate = { checkUpdateCount += 1 },
            onOpenRelease = { openReleaseCount += 1 },
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("修改仓库地址") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(
            hasText("0.9.3-beta3 → 0.9.3-beta4") and hasClickAction(),
        )
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("检查更新") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("打开发布页") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, openSettingsCount)
            assertEquals(1, installUpdateCount)
            assertEquals(1, checkUpdateCount)
            assertEquals(1, openReleaseCount)
        }
    }

    @Test
    fun launcherUpdateSettingsPanel_withoutCheckResult_keepsReleasePageReachable() {
        var openReleaseCount = 0
        setUpdatePanelContent(
            latest = null,
            onOpenRelease = { openReleaseCount += 1 },
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("打开发布页") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, openReleaseCount)
        }
    }

    @Test
    fun instanceManagementPanel_dispatchesEachSettingsEntry() {
        var pathCount = 0
        var mirrorCount = 0
        var wakeCount = 0
        var permissionCount = 0
        val pathConfig = TavernPathConfig()

        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    InstanceManagementPanel(
                        termuxReturnDelayMs = 600L,
                        tavernMirrorConfig = TavernMirrorConfig(),
                        tavernPathConfig = pathConfig,
                        activePathInfo = TavernProfilePathPolicy.describe(pathConfig.activeProfile),
                        mirrorProbeStatus = TavernMirrorProbeStatus(),
                        permissionNotice = PermissionStatusNotice(
                            title = "权限基本就绪",
                            detail = "当前权限基本就绪。",
                        ),
                        onOpenPathSettings = { pathCount += 1 },
                        onOpenMirrorSettings = { mirrorCount += 1 },
                        onOpenWakeDelaySettings = { wakeCount += 1 },
                        onOpenPermissionCenter = { permissionCount += 1 },
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNode(hasText("酒馆路径") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("网络与镜像源") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("唤醒延迟") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("权限中心") and hasClickAction()).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, pathCount)
            assertEquals(1, mirrorCount)
            assertEquals(1, wakeCount)
            assertEquals(1, permissionCount)
        }
    }

    @Test
    fun userAndRepairSections_runningDisablesMutationsButKeepsInspection() {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TavernUserManagementSection(
                        state = TavernUserManagementState(),
                        instanceLabel = "主实例",
                        actionsLocked = false,
                        tavernRunning = true,
                        onRefresh = {},
                        onCreate = { _, _ -> },
                        onDelete = {},
                    )
                    RepairToolsSection(
                        actionsLocked = false,
                        tavernRunning = true,
                        uploadLimitStatus = TavernUploadLimitStatus(),
                        onRepairDependencies = {},
                        onResetTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("读取用户").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("新增用户").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("修复 npm 依赖").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("500MB").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("重新检查当前限制").performScrollTo().assertIsEnabled()
    }

    @Test
    fun repairSection_requiresConfirmationBeforeMutation() {
        var repairCount = 0
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    RepairToolsSection(
                        actionsLocked = false,
                        tavernRunning = false,
                        uploadLimitStatus = TavernUploadLimitStatus(),
                        onRepairDependencies = { repairCount += 1 },
                        onResetTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("修复 npm 依赖").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(0, repairCount) }
        composeRule.onNodeWithText("确认执行").performClick()
        composeRule.runOnIdle { assertEquals(1, repairCount) }
    }

    @Test
    fun userSection_defaultUserDeleteRemainsDisabled() {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TavernUserManagementSection(
                        state = TavernUserManagementState(
                            users = listOf(
                                TavernUserRecord(
                                    handle = "default-user",
                                    name = "默认用户",
                                    admin = true,
                                    enabled = true,
                                    directoryExists = true,
                                    directoryKilobytes = 1024L,
                                ),
                            ),
                            message = "已读取 1 位用户。",
                        ),
                        instanceLabel = "主实例",
                        actionsLocked = false,
                        tavernRunning = false,
                        onRefresh = {},
                        onCreate = { _, _ -> },
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("删除").performScrollTo().assertIsNotEnabled()
    }

    private fun setUpdatePanelContent(
        latest: GithubUpdateInfo? = updateInfo(isNewer = true),
        onOpenSettings: () -> Unit = {},
        onInstallUpdate: () -> Unit = {},
        onCheckUpdate: () -> Unit = {},
        onOpenRelease: () -> Unit = {},
    ) {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    LauncherUpdateSettingsPanel(
                        currentLauncherVersion = "0.9.3-beta3",
                        repositoryInput = "PlayerAlex/lukoa-launcher-android",
                        githubUpdateState = GithubUpdateUiState(
                            repository = "PlayerAlex/lukoa-launcher-android",
                            channel = GithubReleaseChannel.Test,
                            latest = latest,
                            message = "发现新版本。",
                        ),
                        onOpenSettings = onOpenSettings,
                        onCheckUpdate = onCheckUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenRelease = onOpenRelease,
                    )
                }
            }
        }
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(300L))
        composeRule.waitForIdle()
    }

    private companion object {
        fun updateInfo(isNewer: Boolean): GithubUpdateInfo {
            return GithubUpdateInfo(
                repository = "PlayerAlex/lukoa-launcher-android",
                tagName = "v0.9.3-beta4",
                versionName = "0.9.3-beta4",
                releaseName = "0.9.3-beta4",
                releaseUrl = "https://github.com/PlayerAlex/lukoa-launcher-android/releases/tag/v0.9.3-beta4",
                apkName = "lukoa-launcher-0.9.3-beta4.apk",
                apkDownloadUrl = "https://example.com/lukoa-launcher-0.9.3-beta4.apk",
                publishedAt = "2026-07-14T00:00:00Z",
                body = "",
                prerelease = true,
                isNewer = isNewer,
            )
        }
    }
}
