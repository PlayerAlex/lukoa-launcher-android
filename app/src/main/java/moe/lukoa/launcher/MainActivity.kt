package moe.lukoa.launcher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var runner: TermuxCommandRunner
    private lateinit var controller: TavernController
    private lateinit var stateStore: LauncherStateStore
    private lateinit var githubUpdateStore: GithubUpdateStore
    private lateinit var githubUpdateManager: GithubUpdateManager
    private lateinit var tavernOfficialVersionFetcher: TavernOfficialVersionFetcher
    private lateinit var tavernMirrorProbeManager: TavernMirrorProbeManager
    private var termuxWakeInProgress = false
    private var termuxWakeScheduled = false
    private var lastTermuxWakeAt = 0L
    private var pendingBackupImportCallback: ((ExternalBackupImportResult) -> Unit)? = null
    private var pendingBackupExportRequest: PendingBackupExportRequest? = null
    private val backupFilePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = pendingBackupImportCallback ?: return@registerForActivityResult
        pendingBackupImportCallback = null
        if (uri == null) {
            callback(ExternalBackupImportResult(ok = false, message = "已取消选择外部备份。"))
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                ExternalBackupImporter.copyToBackupLibrary(applicationContext, uri)
            }
            callback(result)
        }
    }
    private val backupExportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val request = pendingBackupExportRequest ?: return@registerForActivityResult
        pendingBackupExportRequest = null
        if (uri == null) {
            request.callback(
                BackupExportDestinationResult(
                    ok = false,
                    message = "已取消导出备份。",
                ),
            )
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupExportDestinationResolver.prepareDestination(
                    context = applicationContext,
                    uri = uri,
                    sourcePath = request.sourcePath,
                )
            }
            request.callback(result)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()

        runner = TermuxCommandRunner(applicationContext)
        controller = TavernController(applicationContext, runner)
        stateStore = LauncherStateStore(applicationContext)
        githubUpdateStore = GithubUpdateStore(applicationContext)
        githubUpdateManager = GithubUpdateManager(applicationContext)
        tavernOfficialVersionFetcher = TavernOfficialVersionFetcher(applicationContext)
        tavernMirrorProbeManager = TavernMirrorProbeManager(applicationContext)
        startTaskRemovedWatcher()

        val isTermuxInstalled = runner.isTermuxInstalled()
        val hasRunCommandPermission = isTermuxInstalled && runner.hasRunCommandPermission()
        val backgroundRunPermissionGranted = BackgroundRunAccess.isGranted(applicationContext)
        if (isTermuxInstalled) {
            requestRunCommandPermissionIfNeeded()
        }
        val isProcessColdStart = !LauncherProcessState.started
        val loadResult = stateStore.load(
            isTermuxInstalled = isTermuxInstalled,
            allowColdStartFallback = isProcessColdStart,
        )
        val initialState = loadResult.state
        LauncherProcessState.started = true
        stateStore.armColdStartClearFallback()
        val versionInfo = VersionBackupManager.versionInfo(applicationContext)
        val githubRepository = githubUpdateStore.loadRepository()
        val tavernMirrorStore = TavernMirrorStore(applicationContext)
        val tavernMirrorConfig = tavernMirrorStore.load()
        val tavernPathStore = TavernPathStore(applicationContext)
        val tavernPathConfig = tavernPathStore.load()
        val ignoredUpdateTag = githubUpdateStore.loadIgnoredUpdateTag()
        val shouldRunStartupRefresh = isTermuxInstalled && hasRunCommandPermission
        val startupRefreshSignal = if (shouldRunStartupRefresh) 1 else 0
        val allFilesAccessGranted = hasAllFilesAccessPermission()
        val installUnknownAppsGranted = canInstallUnknownApps()
        AutoBackupScheduler.syncFromState(
            context = applicationContext,
            enabled = initialState.autoBackupEnabled,
            intervalMinutes = initialState.autoBackupIntervalMinutes,
            resetCountdown = false,
        )

        setContent {
            LukoaTheme {
                LukoaLauncherScreen(
                    initialState = initialState,
                    versionInfo = versionInfo,
                    initialGithubRepository = githubRepository,
                    initialTavernMirrorConfig = tavernMirrorConfig,
                    initialTavernPathConfig = tavernPathConfig,
                    initialIgnoredUpdateTag = ignoredUpdateTag,
                    initialTermuxInstalled = isTermuxInstalled,
                    initialRunCommandPermissionGranted = hasRunCommandPermission,
                    initialBackgroundRunPermissionGranted = backgroundRunPermissionGranted,
                    initialAllFilesAccessGranted = allFilesAccessGranted,
                    initialInstallUnknownAppsGranted = installUnknownAppsGranted,
                    startupRefreshSignal = startupRefreshSignal,
                    onPersistState = stateStore::save,
                    onCommand = { command, update ->
                        controller.handleCommand(lifecycleScope, command, update)
                    },
                    onLatestTermuxResult = {
                        controller.latestTermuxResultDisplay()
                    },
                    onRefreshLogs = { updateTermuxLog ->
                        controller.refreshLogSnapshot(lifecycleScope, updateTermuxLog)
                    },
                    onForegroundStart = { update ->
                        controller.handleForegroundStart(lifecycleScope, update)
                    },
                    onOpenTavern = { update ->
                        controller.openTavern(update)
                    },
                    onWakeTermux = { returnDelayMs ->
                        wakeTermuxWithReturn(auto = false, returnDelayMs = returnDelayMs)
                    },
                    onOpenTermuxOnly = {
                        runner.wakeTermux()
                    },
                    onCheckTermuxInstalled = {
                        runner.isTermuxInstalled()
                    },
                    onCheckRunCommandPermission = {
                        runner.hasRunCommandPermission()
                    },
                    onRequestRunCommandPermission = ::requestRunCommandPermissionIfNeeded,
                    onCheckBackgroundRunPermission = {
                        BackgroundRunAccess.isGranted(applicationContext)
                    },
                    onRequestBackgroundRunPermission = {
                        BackgroundRunAccess.request(applicationContext)
                    },
                    onCheckAllFilesAccessPermission = ::hasAllFilesAccessPermission,
                    onCheckInstallUnknownAppsPermission = ::canInstallUnknownApps,
                    onConfigureAutoBackupSchedule = { enabled, intervalMinutes, resetCountdown ->
                        AutoBackupScheduler.syncFromState(
                            context = applicationContext,
                            enabled = enabled,
                            intervalMinutes = intervalMinutes,
                            resetCountdown = resetCountdown,
                        )
                    },
                    onPersistAutoBackupConfig = stateStore::saveAutoBackupConfig,
                    onOpenLauncherPermissionSettings = ::openLauncherPermissionSettings,
                    onOpenAllFilesAccessSettings = ::openAllFilesAccessSettings,
                    onOpenUnknownAppSourcesSettings = ::openUnknownAppSourcesSettings,
                    onCopyText = ::copyTextToClipboard,
                    onOpenExternalUrl = ::openExternalUrl,
                    onExportLog = { summary, status, termuxLog, appLog, mode, update ->
                        controller.exportLog(summary, status, termuxLog, appLog, mode, update)
                    },
                    onExportDiagnostic = { snapshot, update ->
                        controller.exportDiagnostic(snapshot, update)
                    },
                    onExportBackup = { state, update ->
                        controller.exportBackup(state, update)
                    },
                    onExportVersionReport = { update ->
                        controller.exportVersionReport(update)
                    },
                    onPickExternalBackup = ::pickExternalBackup,
                    onPickBackupExportDestination = ::pickBackupExportDestination,
                    onOpenBackupExportLocation = ::openBackupExportLocation,
                    onSaveTavernMirrorConfig = tavernMirrorStore::save,
                    onSaveTavernPathConfig = tavernPathStore::save,
                    onRestoreDefaultTavernPath = tavernPathStore::restoreDefault,
                    onSaveGithubRepository = githubUpdateStore::saveRepository,
                    onIgnoreGithubUpdate = githubUpdateStore::ignoreUpdateTag,
                    onCheckGithubUpdate = { repository, callback ->
                        githubUpdateManager.checkLatest(
                            scope = lifecycleScope,
                            repository = repository,
                            currentVersionName = versionInfo.versionName,
                            callback = callback,
                        )
                    },
                    onFetchOfficialTavernVersions = { mirrorConfig, callback ->
                        tavernOfficialVersionFetcher.fetchOfficialVersions(
                            scope = lifecycleScope,
                            mirrorConfig = mirrorConfig,
                            callback = callback,
                        )
                    },
                    onCheckTavernMirror = { mirrorConfig, callback ->
                        tavernMirrorProbeManager.check(
                            scope = lifecycleScope,
                            config = mirrorConfig,
                            callback = callback,
                        )
                    },
                    onInstallGithubUpdate = { updateInfo, callback ->
                        githubUpdateManager.downloadAndInstall(
                            scope = lifecycleScope,
                            updateInfo = updateInfo,
                            callback = callback,
                        )
                    },
                    onOpenGithubRelease = { updateInfo ->
                        githubUpdateManager.openReleasePage(updateInfo)
                    },
                )
            }
        }

        if (shouldRunStartupRefresh && isTermuxInstalled) {
            scheduleAutoWakeTermux(initialState.termuxReturnDelayMs)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hideStatusBar()
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun onDestroy() {
        if (isFinishing) {
            stateStore.markClearOnNextLaunch()
        }
        super.onDestroy()
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun requestRunCommandPermissionIfNeeded() {
        if (checkSelfPermission(TermuxCommandRunner.PERMISSION_RUN_COMMAND) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(TermuxCommandRunner.PERMISSION_RUN_COMMAND), REQUEST_RUN_COMMAND_PERMISSION)
    }

    private fun startTaskRemovedWatcher() {
        try {
            startService(Intent(this, TaskRemovedWatcherService::class.java))
        } catch (_: Exception) {
            // The app can still work; the next launch just may not know the task was swiped away.
        }
    }

    private fun scheduleAutoWakeTermux(returnDelayMs: Long) {
        if (!::controller.isInitialized || termuxWakeInProgress || termuxWakeScheduled) return
        val now = System.currentTimeMillis()
        if (now - lastTermuxWakeAt < TERMUX_WAKE_COOLDOWN_MS) return

        termuxWakeScheduled = true
        lifecycleScope.launch {
            delay(350)
            termuxWakeScheduled = false
            wakeTermuxWithReturn(auto = true, returnDelayMs = returnDelayMs)
        }
    }

    private fun wakeTermuxWithReturn(auto: Boolean, returnDelayMs: Long): Boolean {
        if (!::controller.isInitialized) return false
        if (!runner.isTermuxInstalled()) return false
        val now = System.currentTimeMillis()
        if (termuxWakeInProgress || termuxWakeScheduled || now - lastTermuxWakeAt < TERMUX_WAKE_COOLDOWN_MS) {
            return true
        }
        if (!stateStore.claimTermuxWakeSlot(now)) {
            lastTermuxWakeAt = now
            return true
        }

        termuxWakeInProgress = true
        lastTermuxWakeAt = now
        val woke = controller.wakeTermuxThenReturn(lifecycleScope, returnDelayMs)
        lifecycleScope.launch {
            delay(TERMUX_WAKE_GUARD_MS)
            termuxWakeInProgress = false
        }
        if (!woke) {
            termuxWakeInProgress = false
        }
        return woke
    }

    private fun pickExternalBackup(callback: (ExternalBackupImportResult) -> Unit) {
        if (pendingBackupImportCallback != null) {
            callback(ExternalBackupImportResult(ok = false, message = "已经打开了一个备份选择器，请先完成当前选择。"))
            return
        }
        pendingBackupImportCallback = callback
        try {
            backupFilePicker.launch(arrayOf("application/gzip", "application/x-gzip", "application/octet-stream", "*/*"))
        } catch (error: Exception) {
            pendingBackupImportCallback = null
            callback(ExternalBackupImportResult(ok = false, message = "打开文件选择器失败：${error.message ?: error.javaClass.simpleName}"))
        }
    }

    private fun pickBackupExportDestination(
        sourcePath: String,
        defaultFileName: String,
        callback: (BackupExportDestinationResult) -> Unit,
    ) {
        if (!ensureBackupSourceReadableForExport(sourcePath, callback)) {
            return
        }
        if (pendingBackupExportRequest != null) {
            callback(
                BackupExportDestinationResult(
                    ok = false,
                    message = "已经打开了导出位置选择器，请先完成当前选择。",
                ),
            )
            return
        }
        val safeFileName = BackupExportDestinationResolver.defaultFileName(defaultFileName)
        pendingBackupExportRequest = PendingBackupExportRequest(sourcePath.trim(), callback)
        try {
            backupExportPicker.launch(safeFileName)
        } catch (error: Exception) {
            pendingBackupExportRequest = null
            callback(
                BackupExportDestinationResult(
                    ok = false,
                    message = "打开文件管理器失败：${error.message ?: error.javaClass.simpleName}",
                ),
            )
        }
    }

    private fun ensureBackupSourceReadableForExport(
        sourcePath: String,
        callback: (BackupExportDestinationResult) -> Unit,
    ): Boolean {
        if (BackupLibraryFiles.canReadLibrarySource(applicationContext, sourcePath)) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val opened = openAllFilesAccessSettings()
            callback(
                BackupExportDestinationResult(
                    ok = false,
                    message = if (opened) {
                        "需要先允许“管理所有文件”。授权后回来再点导出。"
                    } else {
                        "导出需要文件管理权限。请在系统设置里允许本应用管理所有文件。"
                    },
                ),
            )
            return false
        }
        callback(
            BackupExportDestinationResult(
                ok = false,
                message = "读不到备份源文件。请确认备份还在 Download/lukoa/backups/sd 或 zd。",
            ),
        )
        return false
    }

    private fun openAllFilesAccessSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val intents = listOf(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
        return intents.any { intent ->
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun openUnknownAppSourcesSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hasAllFilesAccessPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    private fun canInstallUnknownApps(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
    }

    private fun openExternalUrl(url: String): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun openBackupExportLocation(exportedPath: String): Boolean {
        val relativeFolder = backupExportRelativeFolder(exportedPath)
        return openDownloadRelativeFolder(relativeFolder)
    }

    private fun openDownloadRelativeFolder(
        relativeFolder: String,
        allowLooseFallback: Boolean = true,
    ): Boolean {
        val safeRelativeFolder = relativeFolder.trim('/').ifBlank { BackupLibraryFiles.RELATIVE_DIR }
        val documentId = "primary:Download/$safeRelativeFolder"
        val downloadsDocumentId = "primary:Download"
        val treeUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            documentId,
        )
        val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val documentUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            documentId,
        )
        val downloadsTreeUri = DocumentsContract.buildTreeDocumentUri(
            "com.android.externalstorage.documents",
            downloadsDocumentId,
        )
        val downloadsDocumentUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            downloadsDocumentId,
        )
        val externalStorageRootUri = DocumentsContract.buildRootUri(
            "com.android.externalstorage.documents",
            "primary",
        )
        val encodedDocumentId = Uri.encode(documentId)
        val exactFolderIntents = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(treeDocumentUri, "vnd.android.document/directory")
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(treeUri, "vnd.android.document/directory")
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, "vnd.android.document/directory")
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = documentUri
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.externalstorage.documents/document/$encodedDocumentId")
                addFolderIntentFlags()
            },
        )
        val targetPickerFallbackIntents = listOf(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeUri)
                addFolderIntentFlags()
            },
        )
        val looseFallbackIntents = targetPickerFallbackIntents + listOf(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, downloadsTreeUri)
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(downloadsDocumentUri, "vnd.android.document/directory")
                addFolderIntentFlags()
            },
            Intent(Intent.ACTION_VIEW).apply {
                data = externalStorageRootUri
                addFolderIntentFlags()
            },
        )
        val intents = if (allowLooseFallback) exactFolderIntents + looseFallbackIntents else exactFolderIntents
        return intents.any { intent ->
            try {
                startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun Intent.addFolderIntentFlags() {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
    }

    private fun backupExportRelativeFolder(exportedPath: String): String {
        val normalized = exportedPath.trim().replace('\\', '/')
        val externalDownloadPrefix = "${Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')}/Download/"
        val relative = when {
            normalized.contains("/storage/downloads/") -> normalized.substringAfter("/storage/downloads/")
            normalized.contains("/storage/emulated/0/Download/") -> normalized.substringAfter("/storage/emulated/0/Download/")
            normalized.contains(externalDownloadPrefix) -> normalized.substringAfter(externalDownloadPrefix)
            else -> "lukoa/exports/${normalized.substringAfterLast('/')}"
        }
        return relative.substringBeforeLast('/', missingDelimiterValue = "lukoa/exports")
            .trim('/')
            .ifBlank { "lukoa/exports" }
    }

    private fun openLauncherPermissionSettings(): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun copyTextToClipboard(label: String, text: String): Boolean {
        return try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val REQUEST_RUN_COMMAND_PERMISSION = 4001
        const val TERMUX_WAKE_COOLDOWN_MS = 12_000L
        const val TERMUX_WAKE_GUARD_MS = 4_000L
    }
}

private object LauncherProcessState {
    var started: Boolean = false
}

private data class PendingBackupExportRequest(
    val sourcePath: String,
    val callback: (BackupExportDestinationResult) -> Unit,
)
