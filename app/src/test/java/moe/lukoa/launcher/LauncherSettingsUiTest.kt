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
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun infoPopover_initiallyHiddenAndOpensLightweightExplanation() {
        composeRule.setContent {
            LukoaTheme {
                InfoPopoverButton(
                    contentDescription = "查看测试说明",
                    title = "轻量说明",
                    body = "这段说明只在点击感叹号后出现。",
                )
            }
        }

        composeRule.onNodeWithText("这段说明只在点击感叹号后出现。")
            .assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNodeWithContentDescription("查看测试说明")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("轻量说明").assertExists()
        composeRule.onNodeWithText("这段说明只在点击感叹号后出现。")
            .assertExists()
    }

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
        var openRepositorySettingsCount = 0
        var openUpdateChannelSettingsCount = 0
        var installUpdateCount = 0
        var checkUpdateCount = 0
        var openReleaseCount = 0
        setUpdatePanelContent(
            onOpenRepositorySettings = { openRepositorySettingsCount += 1 },
            onOpenUpdateChannelSettings = { openUpdateChannelSettingsCount += 1 },
            onInstallUpdate = { installUpdateCount += 1 },
            onCheckUpdate = { checkUpdateCount += 1 },
            onOpenRelease = { openReleaseCount += 1 },
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("修改仓库地址") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNode(hasText("更新通道") and hasClickAction())
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
            assertEquals(1, openRepositorySettingsCount)
            assertEquals(1, openUpdateChannelSettingsCount)
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
    fun launcherUpdateSettingsPanel_opensCurrentInstalledVersionReleaseNotes() {
        setUpdatePanelContent(
            currentRelease = updateInfo(isNewer = false).copy(
                versionName = "0.9.3-beta3",
                tagName = "v0.9.3-beta3",
                body = "## 新增\n- 新增自动备份保护\n## 修复\n- 修复后台划掉后设置重置",
            ),
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("当前版本更新内容") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("v0.9.3-beta3 更新内容").assertIsDisplayed()
        composeRule.onNodeWithText("0.9.3-beta3 版本更新日志：\n1. 新增自动备份保护\n2. 修复后台划掉后设置重置")
            .assertIsDisplayed()
    }

    @Test
    fun launcherUpdateSettingsPanel_withoutCurrentRelease_explainsHowToLoadIt() {
        val hints = mutableListOf<String>()
        setUpdatePanelContent(currentRelease = null, onShowHint = hints::add)
        advancePastClickDebounce()

        composeRule.onNode(hasText("当前版本更新内容") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("点“检查更新”后，启动器会同时读取当前已安装版本的更新内容。", hints.last())
        }
    }

    @Test
    fun launcherUpdateSettingsPanel_withoutNewVersion_explainsVersionRowTap() {
        val hints = mutableListOf<String>()
        setUpdatePanelContent(
            latest = updateInfo(isNewer = false),
            onShowHint = hints::add,
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("0.9.3-beta3") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                "当前没有待安装的新版本，可以点下方“检查更新”重新检查。",
                hints.last(),
            )
        }
    }

    @Test
    fun settingsSection_opensRepairToolsFromCompactEntry() {
        setSettingsSectionContent(onSaveTavernDirectory = { true })

        composeRule.onNodeWithText("修复 npm 依赖").assertDoesNotExist()
        advancePastClickDebounce()
        composeRule.onNode(hasText("检查、修复与诊断") and hasClickAction())
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("修复 npm 依赖").assertIsDisplayed()
        composeRule.onNodeWithText("网页打不开时重置主题").assertIsDisplayed()
    }

    @Test
    fun instanceManagementPanel_dispatchesEachSettingsEntry() {
        var profileCount = 0
        var directoryCount = 0
        var portCount = 0
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
                        tavernPathConfig = pathConfig,
                        mirrorProbeStatus = TavernMirrorProbeStatus(),
                        permissionNotice = PermissionStatusNotice(
                            title = "权限基本就绪",
                            detail = "当前权限基本就绪。",
                        ),
                        onOpenProfileManagement = { profileCount += 1 },
                        onOpenDirectorySettings = { directoryCount += 1 },
                        onOpenPortSettings = { portCount += 1 },
                        onOpenMirrorSettings = { mirrorCount += 1 },
                        onOpenWakeDelaySettings = { wakeCount += 1 },
                        onOpenPermissionCenter = { permissionCount += 1 },
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("选择、新增或删除酒馆实例；删除托管分身前会再次确认。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("每个实例使用不同端口，避免启动冲突。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("唤醒 Termux 后，自动返回启动器前等待多久。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("实例与运行环境").assertExists()
        composeRule.onNodeWithText("当前实例").assertExists()
        composeRule.onNodeWithText("运行环境").assertExists()

        composeRule.onNode(hasText("实例名称") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("酒馆路径") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("访问端口") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("网络与镜像源") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("唤醒延迟") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNode(hasText("权限中心") and hasClickAction()).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, profileCount)
            assertEquals(1, directoryCount)
            assertEquals(1, portCount)
            assertEquals(1, mirrorCount)
            assertEquals(1, wakeCount)
            assertEquals(1, permissionCount)
        }
    }

    @Test
    fun userAndRepairSections_runningDisablesMutationsButKeepsInspection() {
        val hints = mutableListOf<String>()
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
                        onShowHint = hints::add,
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
                        onShowHint = hints::add,
                    )
                }
            }
        }

        composeRule.onNodeWithText("读取用户").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("新增用户").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("修复 npm 依赖").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("500MB").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("当前上传限制").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("酒馆正在运行；修改类操作需要先停止酒馆。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("低内存设备不建议选择过高，设置过高可能导致系统结束 Termux。")
            .assertDoesNotExist()
        composeRule.onNodeWithText("1GB 以上会明显增加内存压力，更容易被系统结束后台。")
            .assertDoesNotExist()

        advancePastClickDebounce()
        composeRule.onNodeWithText("读取用户").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals("酒馆正在运行，请先停止酒馆再管理用户。", hints.last())
        }
    }

    @Test
    fun extensionManagementSection_confirmsExactTargetBeforeDelete() {
        var deletedDirectory = ""
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TavernExtensionManagementSection(
                        state = TavernExtensionManagementState(
                            rootDirectory = "/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/third-party",
                            extensions = listOf(
                                TavernExtensionRecord(
                                    directoryName = "Extension-A",
                                    displayName = "清凉扩展",
                                    version = "1.2.3",
                                    hasManifest = true,
                                ),
                            ),
                            message = "已读取 1 个扩展。",
                        ),
                        instanceLabel = "主实例",
                        actionsLocked = false,
                        tavernRunning = false,
                        onRefresh = {},
                        onDelete = { deletedDirectory = it },
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("清凉扩展").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("版本：1.2.3 · 目录：Extension-A").assertIsDisplayed()
        composeRule.onNode(hasText("删除") and hasClickAction()).performClick()
        composeRule.onNodeWithText("当前实例：主实例").assertIsDisplayed()
        composeRule.onNodeWithText("目标目录：/data/data/com.termux/files/home/SillyTavern/public/scripts/extensions/third-party/Extension-A")
            .assertIsDisplayed()
        composeRule.onNode(hasText("确认删除") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals("Extension-A", deletedDirectory) }
    }

    @Test
    fun extensionManagementSection_runningKeepsReadEnabledAndLocksDelete() {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    TavernExtensionManagementSection(
                        state = TavernExtensionManagementState(
                            extensions = listOf(
                                TavernExtensionRecord("Extension-A", "清凉扩展", "", false),
                            ),
                        ),
                        instanceLabel = "主实例",
                        actionsLocked = false,
                        tavernRunning = true,
                        onRefresh = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNode(hasText("读取扩展") and hasClickAction()).assertIsEnabled()
        composeRule.onNode(hasText("删除") and hasClickAction()).assertIsNotEnabled()
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
    fun repairSection_restoresUploadLimitDefaultOnlyAfterConfirmation() {
        var resetCount = 0
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
                        uploadLimitStatus = TavernUploadLimitStatus(
                            currentMegabytes = 1024,
                            patchState = TavernUploadLimitPatchState.Active,
                        ),
                        onRepairDependencies = {},
                        onResetTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                        onResetUploadLimit = { resetCount += 1 },
                    )
                }
            }
        }
        advancePastClickDebounce()

        composeRule.onNodeWithText("恢复酒馆默认值")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle { assertEquals(0, resetCount) }
        composeRule.onNodeWithText("确认执行").performClick()
        composeRule.runOnIdle { assertEquals(1, resetCount) }
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

    @Test
    fun directoryDialog_rejectedSaveKeepsDialogOpen() {
        var saveCount = 0
        setSettingsSectionContent(
            onSaveTavernDirectory = {
                saveCount += 1
                false
            },
        )
        advancePastClickDebounce()

        composeRule.onNode(hasText("酒馆路径") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("保存路径").assertIsDisplayed()
        advancePastClickDebounce()
        composeRule.onNodeWithText("保存路径").performClick()

        composeRule.runOnIdle { assertEquals(1, saveCount) }
        composeRule.onNodeWithText("保存路径").assertIsDisplayed()
    }

    private fun setSettingsSectionContent(
        onSaveTavernDirectory: () -> Boolean,
    ) {
        composeRule.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsSection(
                        termuxReturnDelayMs = 600L,
                        termuxInstalled = true,
                        runCommandPermissionGranted = true,
                        backgroundRunPermissionGranted = true,
                        termuxBackgroundRunPermissionGranted = true,
                        termuxExternalAppsBlocked = false,
                        termuxStoragePermissionBlocked = false,
                        allFilesAccessGranted = true,
                        installUnknownAppsGranted = true,
                        tavernMirrorConfig = TavernMirrorConfig(),
                        tavernPathConfig = TavernPathConfig(),
                        tavernRepoInput = TavernMirrorDefaults.OFFICIAL_REPO,
                        npmRegistryInput = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
                        tavernPathInput = TavernPathDefaults.DEFAULT_TAVERN_DIR,
                        tavernPortInput = TavernPortDefaults.DEFAULT_TAVERN_PORT.toString(),
                        mirrorProbeStatus = TavernMirrorProbeStatus(),
                        termuxRepoStatus = TermuxRepoStatus(),
                        customTermuxRepoInput = "",
                        repositoryInput = "PlayerAlex/lukoa-launcher-android",
                        githubUpdateState = GithubUpdateUiState(),
                        currentLauncherVersion = "0.9.3-beta20",
                        healthCheckReport = null,
                        healthCheckInFlight = false,
                        actionsLocked = false,
                        tavernRunning = false,
                        uploadLimitStatus = TavernUploadLimitStatus(),
                        tavernUserState = TavernUserManagementState(),
                        tavernExtensionState = TavernExtensionManagementState(),
                        forceCleanupSuggestion = null,
                        onTavernRepoInputChange = {},
                        onNpmRegistryInputChange = {},
                        onTavernPathInputChange = {},
                        onTavernPortInputChange = {},
                        onSelectTavernProfile = {},
                        onAddTavernProfile = {},
                        onRemoveCurrentTavernProfile = {},
                        onMigrateToManagedTavernPath = {},
                        onMigrateToTraditionalTavernPath = {},
                        onMigrateToCustomTavernPath = {},
                        onCustomTermuxRepoInputChange = {},
                        onSaveTavernDirectory = onSaveTavernDirectory,
                        onRestoreDefaultTavernDirectory = {},
                        onSaveTavernPort = { true },
                        onRestoreDefaultTavernPort = {},
                        onSaveTavernMirror = {},
                        onUseOfficialMirror = {},
                        onUseGithubProxyMirror = {},
                        onUseNpmMirror = {},
                        onCheckTavernMirror = {},
                        onReadTermuxRepoStatus = {},
                        onApplyCustomTermuxMirror = {},
                        onRequestBackgroundRunPermission = {},
                        onRequestTermuxBackgroundRunPermission = {},
                        onRequestRunCommandPermission = {},
                        onOpenPermissionSettings = {},
                        onCopyExternalAppsCommand = {},
                        onOpenTermuxOnly = {},
                        onOpenAllFilesAccessSettings = {},
                        onOpenUnknownAppSourcesSettings = {},
                        onShowTermuxStoragePermissionGuide = {},
                        onRepositoryInputChange = {},
                        onSaveRepository = {},
                        onRestoreDefaultRepository = {},
                        onSaveUpdateChannel = {},
                        onCheckUpdate = {},
                        onInstallUpdate = {},
                        onOpenRelease = {},
                        onRunHealthCheck = {},
                        onRunHealthCheckPrimaryAction = {},
                        onForceCleanup = {},
                        onRepairDependencies = {},
                        onResetTavernTheme = {},
                        onSetNodeMemory = {},
                        onCheckUploadLimit = {},
                        onSetUploadLimit = {},
                        onResetUploadLimit = {},
                        onRefreshTavernUsers = {},
                        onCreateTavernUser = { _, _ -> },
                        onDeleteTavernUser = {},
                        onRefreshTavernExtensions = {},
                        onDeleteTavernExtension = {},
                        onClearLogs = {},
                        onExportDiagnostic = {},
                        onDecreaseTermuxReturnDelay = {},
                        onIncreaseTermuxReturnDelay = {},
                    )
                }
            }
        }
    }

    private fun setUpdatePanelContent(
        latest: GithubUpdateInfo? = updateInfo(isNewer = true),
        currentRelease: GithubUpdateInfo? = null,
        onOpenRepositorySettings: () -> Unit = {},
        onOpenUpdateChannelSettings: () -> Unit = {},
        onInstallUpdate: () -> Unit = {},
        onCheckUpdate: () -> Unit = {},
        onOpenRelease: () -> Unit = {},
        onShowHint: (String) -> Unit = {},
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
                            currentRelease = currentRelease,
                            message = "发现新版本。",
                        ),
                        onOpenRepositorySettings = onOpenRepositorySettings,
                        onOpenUpdateChannelSettings = onOpenUpdateChannelSettings,
                        onCheckUpdate = onCheckUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onOpenRelease = onOpenRelease,
                        onShowHint = onShowHint,
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
