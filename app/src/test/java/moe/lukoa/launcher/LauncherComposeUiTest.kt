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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
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
    fun bottomNavigation_exposesAllFiveDestinations() {
        composeRule.setContent {
            LukoaTheme {
                LauncherBottomBar(
                    selectedTab = LauncherTab.Launch,
                    onSelectTab = {},
                )
            }
        }

        LauncherTab.entries.forEach { tab ->
            composeRule.onNodeWithText(tab.label).assertExists()
        }
    }

    @Test
    fun overviewPanel_runningAndIdleStatesUseNewLabels() {
        composeRule.setContent {
            LukoaTheme {
                Column {
                    OverviewPanel(
                        summary = "酒馆已运行",
                        status = "运行正常",
                        verified = true,
                        tavernRunning = true,
                        tavernStarting = false,
                        syncActive = true,
                    )
                    OverviewPanel(
                        summary = "等待启动",
                        status = "未运行",
                        verified = true,
                        tavernRunning = false,
                        tavernStarting = false,
                        syncActive = false,
                    )
                }
            }
        }

        composeRule.onNodeWithText("运行中").assertIsDisplayed()
        composeRule.onNodeWithText("酒馆运行中").assertIsDisplayed()
        composeRule.onNodeWithText("Termux 同步中").assertIsDisplayed()
        composeRule.onNodeWithText("未运行").assertIsDisplayed()
        composeRule.onNodeWithText("酒馆未运行").assertIsDisplayed()
        composeRule.onNodeWithText("Termux 未同步").assertIsDisplayed()
    }

    @Test
    fun tavernControl_startingKeepsStopActionEnabled() {
        var stopCount = 0
        composeRule.setContent {
            LukoaTheme {
                TavernControlSection(
                    tavernRunning = false,
                    tavernStarting = true,
                    actionInProgress = true,
                    busyLabel = "启动酒馆",
                    wakeEnabled = true,
                    primaryEnabled = true,
                    primaryDisabledReason = null,
                    onWakeTermux = {},
                    onPrimaryAction = { stopCount += 1 },
                    onOpenTavern = {},
                    onExportLog = {},
                )
            }
        }

        composeRule.onNode(hasText("停止酒馆") and hasClickAction())
            .assertIsEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, stopCount) }
    }

    @Test
    fun repairTools_exposesUploadLimitStatusAndChoices() {
        composeRule.setContent {
            LukoaTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RepairToolsSection(
                        actionsLocked = false,
                        tavernRunning = false,
                        uploadLimitStatus = TavernUploadLimitStatus(
                            currentMegabytes = 500,
                            targetCompatible = true,
                            patchState = TavernUploadLimitPatchState.NotManaged,
                            message = "已识别当前限制，但还没有由启动器管理。",
                        ),
                        onRepairDependencies = {},
                        onResetTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("当前检查结果：500MB").assertIsDisplayed()
        composeRule.onNodeWithText("补丁未应用").assertIsDisplayed()
        composeRule.onNodeWithText("1GB").assertExists()
        composeRule.onAllNodesWithText("2GB").assertCountEquals(2)
        composeRule.onNodeWithText("重新检查当前限制").assertExists()
    }

    @Test
    fun repairTools_keepsUploadLimitChangesLockedUntilTargetIsCompatible() {
        composeRule.setContent {
            LukoaTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RepairToolsSection(
                        actionsLocked = false,
                        tavernRunning = false,
                        uploadLimitStatus = TavernUploadLimitStatus(
                            currentMegabytes = 500,
                            targetCompatible = false,
                            patchState = TavernUploadLimitPatchState.Unknown,
                            message = "当前版本没有识别到兼容目标代码。",
                        ),
                        onRepairDependencies = {},
                        onResetTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("目标代码不兼容").assertIsDisplayed()
        composeRule.onNode(hasText("1GB") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText(
            "请先重新检查当前限制。只有唯一识别到兼容目标代码后，修改按钮才会启用。",
        ).assertExists()
    }

    @Test
    fun backupLibrary_applyingRecordPassesExactArchivePath() {
        val archivePath =
            "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-ui-test.tar.gz"
        var appliedPath: String? = null

        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    BackupSection(
                        instanceLabel = "主实例",
                        instanceDirectory = "~/LukoaLauncher/SillyTavern",
                        instancePort = 8000,
                        actionsLocked = false,
                        backupListRefreshing = false,
                        autoBackupEnabled = false,
                        autoBackupIntervalMinutes = 60,
                        autoBackupKeepCount = 5,
                        backupHistory = listOf(archivePath),
                        onCreateManualBackup = {},
                        onToggleAutoBackup = {},
                        onRefreshBackups = {},
                        onOpenAutoBackupSettings = {},
                        onApplyBackup = { appliedPath = it },
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
        advancePastClickDebounce()

        composeRule.onNodeWithText("sd-ui-test.tar.gz")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(hasText("应用") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(archivePath, appliedPath)
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
            "会把选中的备份直接恢复到酒馆目录，并覆盖当前酒馆数据。",
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
