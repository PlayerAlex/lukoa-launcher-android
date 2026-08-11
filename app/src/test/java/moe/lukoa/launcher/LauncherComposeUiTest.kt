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
import androidx.compose.ui.test.onNodeWithContentDescription
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
class LauncherComposeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentationPage_matchesSketchAndNavigatesTwoChapters() {
        composeRule.setContent {
            LukoaTheme {
                DocumentationSection()
            }
        }

        composeRule.onNodeWithText("文档").assertIsDisplayed()
        composeRule.onNodeWithTag("documentation-content-frame").assertIsDisplayed()
        composeRule.onNodeWithText("2. 本软件当前仅适配原生酒馆，不支持云酒馆、电脑酒馆以及各类二创酒馆。").assertExists()
        composeRule.onNodeWithTag("documentation-menu-drawer").assertDoesNotExist()

        advancePastClickDebounce()
        composeRule.onNodeWithContentDescription("打开文档目录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("documentation-menu-drawer").assertIsDisplayed()
        composeRule.onNodeWithText("首页").assertIsDisplayed()
        composeRule.onNodeWithText("第一章：酒馆黑话篇").assertIsDisplayed()
        composeRule.onNodeWithText("第二章：启动器疑问篇").assertIsDisplayed()

        advancePastClickDebounce()
        composeRule.onNodeWithText("第一章：酒馆黑话篇").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("酒馆黑话篇").assertIsDisplayed()
        composeRule.onNodeWithText("API、API Key 与模型名").assertExists()

        advancePastClickDebounce()
        composeRule.onNodeWithContentDescription("打开文档目录").performClick()
        composeRule.waitForIdle()
        advancePastClickDebounce()
        composeRule.onNodeWithContentDescription("关闭文档目录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("documentation-menu-drawer").assertDoesNotExist()
    }

    @Test
    fun bottomNavigation_selectingToolboxUpdatesSelectedTab() {
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
        composeRule.onNode(hasText("工具箱") and hasClickAction()).performClick()

        composeRule.onNode(hasText("工具箱") and hasClickAction()).assertIsSelected()
        composeRule.onNode(hasText("启动") and hasClickAction()).assertIsNotSelected()
        composeRule.runOnIdle {
            assertEquals(LauncherTab.Toolbox, selectedByCallback)
        }
    }

    @Test
    fun tavernHub_opensEachAvailableSecondaryPage() {
        var openedPage: LauncherSecondaryPage? = null

        composeRule.setContent {
            LukoaTheme {
                TavernHubSection(
                    tavernRunning = false,
                    tavernStarting = false,
                    actionInProgress = false,
                    onOpenVersionManagement = {
                        openedPage = LauncherSecondaryPage.VersionManagement
                    },
                    onOpenBackup = {
                        openedPage = LauncherSecondaryPage.Backup
                    },
                    onOpenExtensionManagement = {
                        openedPage = LauncherSecondaryPage.ExtensionManagement
                    },
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNode(hasText("版本管理") and hasClickAction()).performClick()
        composeRule.runOnIdle {
            assertEquals(LauncherSecondaryPage.VersionManagement, openedPage)
        }

        advancePastClickDebounce()
        composeRule.onNode(hasText("备份") and hasClickAction()).performClick()
        composeRule.runOnIdle {
            assertEquals(LauncherSecondaryPage.Backup, openedPage)
        }

        advancePastClickDebounce()
        composeRule.onNode(hasText("扩展管理") and hasClickAction()).performClick()
        composeRule.runOnIdle {
            assertEquals(LauncherSecondaryPage.ExtensionManagement, openedPage)
        }
    }

    @Test
    fun secondaryPageHeader_backButtonReturnsToHub() {
        var backCount = 0

        composeRule.setContent {
            LukoaTheme {
                LauncherSecondaryPageHeader(
                    title = "版本管理",
                    onBack = { backCount += 1 },
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNode(hasText("返回") and hasClickAction()).performClick()
        composeRule.runOnIdle {
            assertEquals(1, backCount)
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
                        actionsLocked = false,
                        backupListRefreshing = false,
                        autoBackupEnabled = false,
                        backupHistory = listOf(archivePath),
                        backupArchiveDetails = mapOf(
                            archivePath to BackupLibraryArchiveDetails(
                                fileName = "sd-ui-test.tar.gz",
                                termuxReadablePath = archivePath,
                                size = 2_048L,
                                modifiedAtMillis = 1_700_000_000_000L,
                            ),
                        ),
                        backupContentStates = mapOf(
                            archivePath to BackupContentCatalogState(
                                summary = BackupArchiveContentSummary(
                                    entryCount = 1,
                                    hasUserData = true,
                                    hasExtensions = false,
                                    hasConfiguration = false,
                                    hasLukoaManifest = true,
                                    truncated = false,
                                    groups = listOf(
                                        BackupArchiveContentGroup(
                                            kind = BackupArchiveContentKind.CharacterCards,
                                            entryCount = 1,
                                            names = listOf("清凉角色"),
                                            namesTruncated = false,
                                        ),
                                        BackupArchiveContentGroup(
                                            kind = BackupArchiveContentKind.Beautification,
                                            entryCount = 1,
                                            names = listOf("清凉主题"),
                                            namesTruncated = false,
                                        ),
                                        BackupArchiveContentGroup(
                                            kind = BackupArchiveContentKind.RegexScripts,
                                            entryCount = 1,
                                            namesTruncated = false,
                                            children = listOf(
                                                BackupArchiveContentNode(
                                                    title = "全局正则",
                                                    entryCount = 1,
                                                    names = listOf("清理标记"),
                                                ),
                                            ),
                                        ),
                                        BackupArchiveContentGroup(
                                            kind = BackupArchiveContentKind.Extensions,
                                            entryCount = 1,
                                            names = listOf("酒馆助手"),
                                            namesTruncated = false,
                                        ),
                                    ),
                                ),
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
        composeRule.onNodeWithText("当前状态：").assertExists()
        composeRule.onNodeWithText("自动备份未开启").assertExists()
        composeRule.onNodeWithText("手动备份库：").assertExists()
        composeRule.onNodeWithText("自动备份库：").assertExists()
        composeRule.onNodeWithText("创建手动备份").assertExists()
        composeRule.onNodeWithText("自动备份规则").assertExists()
        composeRule.onNodeWithText("备份库").assertExists()
        composeRule.onNodeWithText("sd-ui-test.tar.gz")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("2.0 KB").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("文件地址").assertDoesNotExist()
        composeRule.onNodeWithText(archivePath).assertDoesNotExist()
        composeRule.onNodeWithText("备份内容").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("查看内容").assertDoesNotExist()
        composeRule.onNodeWithText("角色卡").assertDoesNotExist()
        composeRule.onNodeWithText("清凉角色").assertDoesNotExist()
        composeRule.onNodeWithText("备份内容").performClick()
        composeRule.onNodeWithText("正则").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("全局正则").assertDoesNotExist()
        composeRule.onNodeWithText("正则").performScrollTo().performClick()
        composeRule.onNodeWithText("全局正则").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("清理标记").assertDoesNotExist()
        composeRule.onNodeWithText("全局正则").performScrollTo().performClick()
        composeRule.onNodeWithText("清理标记").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("当前备份中的正则").assertDoesNotExist()
        composeRule.onNodeWithText("扩展").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("酒馆助手").assertDoesNotExist()
        composeRule.onNodeWithText("扩展").performScrollTo().performClick()
        composeRule.onNodeWithText("酒馆助手").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("当前备份中的扩展").assertDoesNotExist()
        composeRule.onNodeWithText("角色卡").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("美化").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("酒馆美化").assertDoesNotExist()
        composeRule.onNodeWithText("酒馆参数模板").assertDoesNotExist()
        composeRule.onNodeWithText("提示词模板").assertDoesNotExist()
        composeRule.onNodeWithText("清凉角色").assertDoesNotExist()
        composeRule.onNodeWithText("角色卡").performScrollTo().performClick()
        composeRule.onNodeWithText("清凉角色").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("重命名") and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNode(hasText("应用") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("真的吗？") and hasClickAction()).assertExists()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(4_100L)
        composeRule.waitForIdle()
        composeRule.onNode(hasText("应用") and hasClickAction()).assertExists()
        composeRule.mainClock.autoAdvance = true
        composeRule.onNode(hasText("应用") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("真的吗？") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("删除") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("真的吗？") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(archivePath, appliedPath)
            assertEquals(archivePath, renamedPath)
            assertEquals(archivePath, deletedPath)
        }
    }

    @Test
    fun backupContentRows_drillIntoScriptsAndChatsWithoutFlatTruncationMessage() {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BackupContentGroupRow(
                        BackupArchiveContentGroup(
                            kind = BackupArchiveContentKind.TavernHelperScripts,
                            entryCount = 3,
                            children = listOf(
                                BackupArchiveContentNode(
                                    title = "全局脚本",
                                    entryCount = 1,
                                    names = listOf("启动整理"),
                                ),
                                BackupArchiveContentNode(
                                    title = "预设脚本",
                                    entryCount = 1,
                                    children = listOf(
                                        BackupArchiveContentNode(
                                            title = "清凉预设",
                                            entryCount = 1,
                                            names = listOf("预设内脚本"),
                                        ),
                                    ),
                                ),
                                BackupArchiveContentNode(
                                    title = "局部脚本",
                                    entryCount = 1,
                                    children = listOf(
                                        BackupArchiveContentNode(
                                            title = "薄荷角色卡",
                                            entryCount = 1,
                                            names = listOf("角色局部脚本"),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
                    BackupContentGroupRow(
                        BackupArchiveContentGroup(
                            kind = BackupArchiveContentKind.Chats,
                            entryCount = 2,
                            namesTruncated = true,
                            children = listOf(
                                BackupArchiveContentNode(
                                    title = "薄荷聊天",
                                    entryCount = 2,
                                    names = listOf("2026-08-10", "2026-08-11"),
                                ),
                            ),
                        ),
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("全局脚本").assertDoesNotExist()
        composeRule.onNodeWithText("酒馆助手脚本").performClick()
        composeRule.onNodeWithText("全局脚本").assertIsDisplayed()
        composeRule.onNodeWithText("启动整理").assertDoesNotExist()
        composeRule.onNodeWithText("全局脚本").performClick()
        composeRule.onNodeWithText("启动整理").assertIsDisplayed()

        composeRule.onNodeWithText("预设脚本").performClick()
        composeRule.onNodeWithText("清凉预设").assertIsDisplayed()
        composeRule.onNodeWithText("预设内脚本").assertDoesNotExist()
        composeRule.onNodeWithText("清凉预设").performClick()
        composeRule.onNodeWithText("预设内脚本").assertIsDisplayed()

        composeRule.onNodeWithText("局部脚本").performClick()
        composeRule.onNodeWithText("薄荷角色卡").assertIsDisplayed()
        composeRule.onNodeWithText("角色局部脚本").assertDoesNotExist()
        composeRule.onNodeWithText("薄荷角色卡").performClick()
        composeRule.onNodeWithText("角色局部脚本").assertIsDisplayed()

        composeRule.onNodeWithText("聊天记录").performScrollTo().performClick()
        composeRule.onNodeWithText("薄荷聊天").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026-08-10").assertDoesNotExist()
        composeRule.onNodeWithText("薄荷聊天").performScrollTo().performClick()
        composeRule.onNodeWithText("2026-08-10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026-08-11").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("还有更多内容未展开列出。").assertDoesNotExist()
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
                                    namesTruncated = false,
                                    children = listOf(
                                        BackupArchiveContentNode(
                                            title = "全局正则",
                                            entryCount = 1,
                                            names = listOf("CleanRegex"),
                                        ),
                                    ),
                                ),
                                BackupArchiveContentGroup(
                                    kind = BackupArchiveContentKind.Extensions,
                                    entryCount = 1,
                                    names = listOf("酒馆助手"),
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
        composeRule.onNodeWithText("全局正则").assertDoesNotExist()
        composeRule.onNodeWithText("正则").performScrollTo().performClick()
        composeRule.onNodeWithText("全局正则").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("CleanRegex").assertDoesNotExist()
        composeRule.onNodeWithText("全局正则").performScrollTo().performClick()
        composeRule.onNodeWithText("CleanRegex").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("当前备份中的正则").assertDoesNotExist()
        composeRule.onNodeWithText("扩展").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("酒馆助手").assertDoesNotExist()
        composeRule.onNodeWithText("扩展").performScrollTo().performClick()
        composeRule.onNodeWithText("酒馆助手").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("当前备份中的扩展").assertDoesNotExist()
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
