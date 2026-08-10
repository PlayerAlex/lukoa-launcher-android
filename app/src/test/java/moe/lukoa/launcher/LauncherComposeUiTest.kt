package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
class LauncherComposeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentationNavigation_scrollsLongPageWithoutHidingOtherSections() {
        composeRule.setContent {
            LukoaTheme {
                val pageScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState),
                ) {
                    DocumentationSection(pageScrollState = pageScrollState)
                }
            }
        }

        composeRule.onNodeWithText("新手上手").assertExists()
        composeRule.onNodeWithText("多实例与设置").assertExists()
        composeRule.onNodeWithText("备份与恢复").assertExists()
        composeRule.onNodeWithText("左右滑动 · 8 章").assertExists()
        advancePastClickDebounce()
        composeRule.onNode(hasText("更新") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("安装、更新与回退").assertIsDisplayed()
        composeRule.onNodeWithText("新手上手").assertExists()
        composeRule.onNodeWithText("备份与恢复").assertExists()
    }

    @Test
    fun bottomNavigation_selectingBackupUpdatesSelectedTab() {
        var selectedByCallback: LauncherTab? = null

        composeRule.setContent {
            LukoaTheme {
                var selectedTab by remember { mutableStateOf(LauncherTab.Launch) }
                LauncherBottomBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        selectedTab = tab
                        selectedByCallback = tab
                    },
                )
            }
        }

        composeRule.onNode(hasText("启动") and hasClickAction()).assertIsSelected()
        composeRule.onNode(hasText("备份") and hasClickAction()).performClick()

        composeRule.onNode(hasText("备份") and hasClickAction()).assertIsSelected()
        composeRule.onNode(hasText("启动") and hasClickAction()).assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(LauncherTab.Backup, selectedByCallback)
        }
    }

    @Test
    fun quickStartGuide_focusesOnOneNextStepAndShowsVerticalProgress() {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    QuickStartGuideSection(
                        termuxInstalled = false,
                        runCommandPermissionGranted = false,
                        externalAppsBlocked = false,
                        tavernInstallDetected = null,
                        tavernVersionChecking = false,
                        termuxSetupRecommended = false,
                        officialVersions = TavernOfficialVersions(),
                        selectedVersion = null,
                        mirrorRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
                        commandText = "allow-external-apps=true",
                        actionsLocked = false,
                        onOpenTermuxDownload = {},
                        onOpenTermuxGithub = {},
                        onRecheckTermux = {},
                        onRequestPermission = {},
                        onOpenPermissionSettings = {},
                        onCopyPermissionCommand = {},
                        onOpenTermux = {},
                        onRecheckPermission = {},
                        onPrepareTermux = {},
                        onCheckTavern = {},
                        onShowInstall = {},
                        onRefreshOfficialVersions = {},
                        onSelectVersion = {},
                        onUseRecommendedVersion = {},
                        onInstallTavern = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("第一次使用").assertExists()
        composeRule.onNodeWithText("露科亚安装向导").assertDoesNotExist()
        composeRule.onNodeWithText("已完成 0/4").assertExists()
        composeRule.onNodeWithText("现在只做这一项").assertExists()
        composeRule.onNodeWithText("安装并打开一次 Termux").assertIsDisplayed()
        composeRule.onNodeWithText("安装 Termux").assertExists()
        composeRule.onNodeWithText("连接 Termux").assertExists()
        composeRule.onNodeWithText("准备运行环境").assertExists()
        composeRule.onNodeWithText("确认并安装酒馆").assertExists()
    }

    @Test
    fun backupLibrary_riskyRecordActionsPassExactArchivePath() {
        val archivePath =
            "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-ui-test.tar.gz"
        var appliedPath: String? = null
        var renamedPath: String? = null
        var deletedPath: String? = null

        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BackupSection(
                        activeInstanceLabel = "主实例",
                        actionsLocked = false,
                        backupListRefreshing = false,
                        autoBackupEnabled = false,
                        autoBackupIntervalMinutes = 60,
                        autoBackupKeepCount = 5,
                        backupHistory = listOf(archivePath),
                        backupArchiveDetails = mapOf(
                            archivePath to BackupLibraryArchiveDetails(
                                fileName = "sd-ui-test.tar.gz",
                                termuxReadablePath = archivePath,
                                size = 2_048L,
                                modifiedAtMillis = 1_700_000_000_000L,
                            ),
                        ),
                        onCreateManualBackup = {},
                        onToggleAutoBackup = {},
                        onRefreshBackups = {},
                        onOpenAutoBackupSettings = {},
                        onApplyBackup = { appliedPath = it },
                        onCopyBackup = {},
                        onRenameBackup = { renamedPath = it },
                        onDeleteBackup = { deletedPath = it },
                        onExportBackup = {},
                        onImportBackup = {},
                        onCopyBackupLibraryPath = {},
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("数据安全").assertDoesNotExist()
        composeRule.onNodeWithText("备份分区").assertDoesNotExist()
        composeRule.onNodeWithText("备份概览").assertDoesNotExist()
        composeRule.onNodeWithText("快速操作").assertDoesNotExist()
        composeRule.onNodeWithText("手动保护").assertDoesNotExist()
        composeRule.onNodeWithText("备份操作").assertExists()
        composeRule.onNodeWithText("主实例").assertExists()
        composeRule.onNodeWithText("修改自动规则").assertExists()
        composeRule.onNodeWithText("备份库").assertExists()
        composeRule.onNodeWithText("sd-ui-test.tar.gz")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("2.0 KB").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(archivePath).performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("重命名") and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNode(hasText("应用并覆盖") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("删除") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(archivePath, appliedPath)
            assertEquals(archivePath, renamedPath)
            assertEquals(archivePath, deletedPath)
        }
    }

    @Test
    fun restorePreview_confirmInvokesOnlyDangerousAction() {
        var confirmCount = 0
        var dismissCount = 0

        composeRule.setContent {
            LukoaTheme {
                ApplyBackupPreviewDialog(
                    preview = restorePreview(),
                    onConfirm = { confirmCount += 1 },
                    onDismiss = { dismissCount += 1 },
                )
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("确认应用备份").assertIsDisplayed()
        composeRule.onNodeWithText("sd-ui-test.tar.gz").assertExists()
        composeRule.onNodeWithText("~/SillyTavern").assertExists()
        composeRule.onNodeWithText(
            "完整恢复会替换当前酒馆程序和全部数据。",
        ).assertExists()
        composeRule.onNode(hasText("确认应用") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(1, confirmCount)
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun restorePreview_cancelDoesNotInvokeDangerousAction() {
        var confirmCount = 0
        var dismissCount = 0

        composeRule.setContent {
            LukoaTheme {
                ApplyBackupPreviewDialog(
                    preview = restorePreview(),
                    onConfirm = { confirmCount += 1 },
                    onDismiss = { dismissCount += 1 },
                )
            }
        }
        advancePastClickDebounce()

        composeRule.onNode(hasText("取消") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(0, confirmCount)
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun restorePreview_showsContentAndAllowsUserDataOnlySelection() {
        composeRule.setContent {
            LukoaTheme {
                var mode by remember { mutableStateOf(BackupRestoreMode.Full) }
                ApplyBackupPreviewDialog(
                    preview = restorePreview().copy(
                        contentSummary = BackupArchiveContentSummary(
                            entryCount = 42,
                            hasUserData = true,
                            hasExtensions = true,
                            hasConfiguration = true,
                            hasLukoaManifest = true,
                            truncated = false,
                            groups = listOf(
                                BackupArchiveContentGroup(
                                    kind = BackupArchiveContentKind.CharacterCards,
                                    entryCount = 2,
                                    names = listOf("Mint", "Aqua"),
                                    namesTruncated = false,
                                ),
                                BackupArchiveContentGroup(
                                    kind = BackupArchiveContentKind.Presets,
                                    entryCount = 1,
                                    names = listOf("CoolPreset"),
                                    namesTruncated = false,
                                ),
                                BackupArchiveContentGroup(
                                    kind = BackupArchiveContentKind.RegexScripts,
                                    entryCount = 1,
                                    names = listOf("CleanRegex"),
                                    namesTruncated = false,
                                ),
                            ),
                        ),
                    ),
                    restoreMode = mode,
                    onRestoreModeChange = { mode = it },
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("备份内容预览").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("角色卡").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("预设").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("正则").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mint").assertDoesNotExist()
        composeRule.onNodeWithText("角色卡").performScrollTo().performClick()
        composeRule.onNodeWithText("Mint").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Aqua").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("只恢复用户数据").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "只恢复用户数据会保留当前酒馆程序，只替换聊天、角色、世界书和用户设置。",
        ).performScrollTo().assertIsDisplayed()
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(300L))
        composeRule.waitForIdle()
    }

    private fun restorePreview(): BackupRestorePreview {
        return BackupRestorePreview(
            archivePath =
                "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-ui-test.tar.gz",
            backupName = "sd-ui-test.tar.gz",
            modifiedAtMillis = null,
            sizeBytes = 2_048L,
            restoreTargetDir = "~/SillyTavern",
        )
    }
}
