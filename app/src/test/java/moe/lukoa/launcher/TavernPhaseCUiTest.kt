package moe.lukoa.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TavernPhaseCUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun documentation_supportsThirdChapterAndInlineSearch() {
        composeRule.setContent {
            LukoaTheme {
                DocumentationSection()
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithContentDescription("打开文档目录").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("第三章：进阶与维护篇").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("进阶与维护篇").assertIsDisplayed()
        composeRule.onNodeWithText("局域网访问、扫码打开与请求代理").assertExists()

        composeRule.onNodeWithTag("documentation-search-input").performTextInput("二维码")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("清空搜索").assertIsDisplayed()
        composeRule.onNodeWithText("局域网访问、扫码打开与请求代理").assertExists()
    }

    @Test
    fun backupSection_replacesPlaceholderWithLightweightBackupAndShowsAutoBackupStatus() {
        var lightweightClicked = false

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
                        autoBackupEnabled = true,
                        backupHistory = emptyList(),
                        autoBackupRuntimeStatus = AutoBackupRuntimeStatus(
                            nextTriggerAtMillis = 1_760_000_000_000L,
                            lastRunAtMillis = 1_759_990_000_000L,
                            lastRunOk = true,
                            lastRunMessage = "成功",
                        ),
                        onCreateManualBackup = {},
                        onCreateLightweightBackup = { lightweightClicked = true },
                        onToggleAutoBackup = {},
                        onRefreshBackups = {},
                        onOpenAutoBackupSettings = {},
                        onApplyBackup = {},
                        onCopyBackup = {},
                        onRenameBackup = {},
                        onDeleteBackup = {},
                        onExportBackup = {},
                        onImportBackup = {},
                        onCopyBackupLibraryPath = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("敬请期待").assertDoesNotExist()
        composeRule.onNodeWithText("下次自动备份：").assertExists()
        composeRule.onNodeWithText("最近自动备份：").assertExists()
        advancePastClickDebounce()
        composeRule.onNodeWithText("轻量备份数据").performClick()
        composeRule.runOnIdle { assertTrue(lightweightClicked) }
    }

    @Test
    fun logPanel_filtersErrorLinesAndSupportsSearch() {
        val sampleLog = "info line\nError: EADDRINUSE port 8000\nready line"
        composeRule.setContent {
            LukoaTheme {
                LogPanel(
                    title = "测试日志",
                    content = sampleLog,
                    accentColor = LukoaColors.Primary,
                )
            }
        }

        composeRule.onNodeWithText("只看报错 (1)").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Error: EADDRINUSE port 8000", substring = true).assertExists()
        composeRule.onNodeWithText("info line", substring = true).assertDoesNotExist()
    }

    @Test
    fun repairToolsSection_showsNodeMemoryStatusAndResetAction() {
        var checkCalled = false
        var resetCalled = false

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
                        nodeMemoryStatus = TavernNodeMemoryStatus(
                            configured = true,
                            megabytes = 4096,
                            message = "当前实例已设置上限 4GB。",
                        ),
                        onRepairDependencies = {},
                        onResetTheme = {},
                        onCheckNodeMemory = { checkCalled = true },
                        onSetNodeMemory = {},
                        onResetNodeMemory = { resetCalled = true },
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                    )
                }
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("当前内存上限").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(checkCalled) }

        advancePastClickDebounce()
        composeRule.onNodeWithText("恢复默认（不限制）").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("确认执行").performClick()
        composeRule.runOnIdle { assertTrue(resetCalled) }
    }

    @Test
    fun tavernControlSection_showsInstancePillAndTriggersQuickSwitch() {
        var switchOpened = false

        composeRule.setContent {
            LukoaTheme {
                TavernControlSection(
                    tavernRunning = false,
                    tavernStarting = false,
                    tavernVersion = "1.13.4",
                    actionInProgress = false,
                    busyLabel = null,
                    wakeEnabled = true,
                    primaryEnabled = true,
                    primaryDisabledReason = null,
                    activeProfileLabel = "分身 2",
                    activePort = 8001,
                    profileCount = 2,
                    onOpenQuickSwitchProfile = { switchOpened = true },
                    onWakeTermux = {},
                    onPrimaryAction = {},
                    onOpenTavern = {},
                    onExportLog = {},
                )
            }
        }

        composeRule.onNodeWithText("当前实例").assertIsDisplayed()
        composeRule.onNodeWithText("分身 2 · 8001 · 切换").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(switchOpened) }
    }

    @Test
    fun quickSwitchProfileDialog_switchesSelectedProfile() {
        var chosenId: String? = null
        val profiles = listOf(
            TavernProfile(id = "main", name = "主实例", tavernDir = "/home/SillyTavern", port = 8000),
            TavernProfile(id = "profile-2", name = "分身 2", tavernDir = "/home/LukoaLauncher/SillyTavern2", port = 8001),
        )
        val config = TavernPathConfig(activeProfileId = "main", profiles = profiles)

        composeRule.setContent {
            LukoaTheme {
                QuickSwitchProfileDialog(
                    tavernPathConfig = config,
                    actionsLocked = false,
                    onSelectProfile = { chosenId = it },
                    onOpenSettings = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("快速切换酒馆实例").assertIsDisplayed()
        composeRule.onNodeWithText("分身 2").performClick()
        composeRule.runOnIdle { assertEquals("profile-2", chosenId) }
    }

    @Test
    fun issueAnalysisPanel_showsPrimaryIssueFirstAndCollapsesSecondaryIssues() {
        val issues = listOf(
            TavernIssue(
                title = "次要警告一",
                detail = "网络抖动。",
                action = "稍后重试。",
                severity = IssueSeverity.Warning,
            ),
            TavernIssue(
                title = "酒馆端口被占用",
                detail = "8000 端口被占用。",
                action = "点下面强制释放。",
                severity = IssueSeverity.Danger,
                quickFixAction = LauncherQuickFixAction(
                    type = LauncherQuickFixActionType.ForceCleanupPort,
                    label = "强制释放端口",
                ),
            ),
            TavernIssue(
                title = "次要警告二",
                detail = "包源提示。",
                action = "可忽略。",
                severity = IssueSeverity.Info,
            ),
        )

        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    IssueAnalysisPanel(
                        issues = issues,
                        actionsLocked = false,
                        onQuickFixAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("酒馆端口被占用").assertIsDisplayed()
        composeRule.onNodeWithText("强制释放端口").assertIsDisplayed()
        composeRule.onNodeWithText("次要警告一").assertDoesNotExist()
        composeRule.onNodeWithText("还有 2 条次要提醒").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("次要警告一").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("次要警告二").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun topBannerHost_showsActionAndDismissesCleanly() {
        val controller = LukoaTopBannerController()
        var actionClicks = 0
        composeRule.setContent {
            LukoaTheme {
                LukoaTopBannerHost(controller = controller)
            }
        }

        composeRule.runOnIdle {
            controller.show(
                title = "聊天守护已为你救回 2 条聊天",
                detail = "原档已保存在同目录。",
                tone = LukoaTopBannerTone.Success,
                actionLabel = "查看",
                onAction = { actionClicks += 1 },
                autoDismissMs = 0L,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("聊天守护已为你救回 2 条聊天").assertIsDisplayed()
        composeRule.onNodeWithText("查看").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, actionClicks)
        composeRule.onNodeWithText("聊天守护已为你救回 2 条聊天").assertDoesNotExist()
    }

    @Test
    fun tavernControlSection_showsCopyableLocalUrlWhenRunning() {
        var copyClicks = 0
        composeRule.setContent {
            LukoaTheme {
                TavernControlSection(
                    tavernRunning = true,
                    tavernStarting = false,
                    tavernVersion = "1.13.4",
                    actionInProgress = false,
                    busyLabel = null,
                    wakeEnabled = true,
                    primaryEnabled = true,
                    primaryDisabledReason = null,
                    activePort = 8000,
                    onWakeTermux = {},
                    onPrimaryAction = {},
                    onOpenTavern = {},
                    onExportLog = {},
                    onCopyTavernUrl = { copyClicks += 1 },
                )
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("本机地址").assertIsDisplayed()
        composeRule.onNodeWithText("http://127.0.0.1:8000 · 复制").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, copyClicks) }
    }

    @Test
    fun logPanel_withCollapsedFilter_revealsFilterAndCopyInFooterWithoutCrowdingHeader() {
        composeRule.setContent {
            LukoaTheme {
                LogPanel(
                    title = "Termux 运行日志",
                    content = "info line\nError: EADDRINUSE",
                    accentColor = LukoaColors.Primary,
                    infoText = "测试说明",
                    filterExpandedByDefault = false,
                )
            }
        }

        composeRule.onNodeWithText("Termux 运行日志").assertIsDisplayed()
        composeRule.onNodeWithText("筛选 (1)").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("只看报错 (1)").assertIsDisplayed()
    }

    @Test
    fun toolboxSection_routesEntriesToFullPagesWhenCallbacksProvided() {
        var openedChatGuard = 0
        var openedRepair = 0
        var openedHealth = 0
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    ToolboxSection(
                        onOpenHealthCheckPage = { openedHealth += 1 },
                        onOpenChatGuardPage = { openedChatGuard += 1 },
                        onOpenRepairToolsPage = { openedRepair += 1 },
                    )
                }
            }
        }

        advancePastClickDebounce()
        composeRule.onNodeWithText("查看详情").performScrollTo().performClick()
        advancePastClickDebounce()
        composeRule.onNodeWithText("聊天守护").performScrollTo().performClick()
        advancePastClickDebounce()
        composeRule.onNodeWithText("修复工具").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(1, openedHealth)
            assertEquals(1, openedChatGuard)
            assertEquals(1, openedRepair)
        }
    }

    private fun advancePastClickDebounce() {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
    }
}
