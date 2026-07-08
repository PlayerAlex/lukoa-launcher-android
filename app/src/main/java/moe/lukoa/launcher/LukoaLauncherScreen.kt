package moe.lukoa.launcher

import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.abs
import kotlin.math.hypot

private const val START_FAST_PATH_WINDOW_MS = 60_000L
private const val START_PREFLIGHT_CACHE_WINDOW_MS = 120_000L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LukoaLauncherScreen(
    initialState: LauncherUiState,
    versionInfo: VersionInfo,
    initialGithubRepository: String,
    initialGithubUpdateChannel: GithubReleaseChannel,
    initialTavernMirrorConfig: TavernMirrorConfig,
    initialTavernPathConfig: TavernPathConfig,
    initialIgnoredUpdateTag: String,
    initialTermuxInstalled: Boolean,
    initialRunCommandPermissionGranted: Boolean,
    initialBackgroundRunPermissionGranted: Boolean,
    initialTermuxBackgroundRunPermissionGranted: Boolean,
    initialFirstTavernStartGuideSeen: Boolean,
    initialAllFilesAccessGranted: Boolean,
    initialInstallUnknownAppsGranted: Boolean,
    startupRefreshSignal: Int,
    onPersistState: (LauncherUiState) -> Unit,
    onCommand: (String, LauncherUpdate) -> Unit,
    onLatestTermuxResult: () -> TermuxResultDisplay?,
    onRefreshLogs: ((String, Boolean) -> Unit) -> Unit,
    onForegroundStart: (LauncherUpdate) -> Unit,
    onOpenTavern: (LauncherUpdate) -> Unit,
    onWakeTermux: (Long) -> TermuxWakeResult,
    onOpenTermuxOnly: () -> Boolean,
    onCheckTermuxInstalled: () -> Boolean,
    onCheckRunCommandPermission: () -> Boolean,
    onRequestRunCommandPermission: () -> Unit,
    onCheckBackgroundRunPermission: () -> Boolean,
    onRequestBackgroundRunPermission: () -> Boolean,
    onCheckTermuxBackgroundRunPermission: () -> Boolean,
    onRequestTermuxBackgroundRunPermission: () -> Boolean,
    onMarkFirstTavernStartGuideSeen: () -> Unit,
    onCheckAllFilesAccessPermission: () -> Boolean,
    onCheckInstallUnknownAppsPermission: () -> Boolean,
    onConfigureAutoBackupSchedule: (Boolean, Int, Boolean) -> Unit,
    onPersistAutoBackupConfig: (Boolean, Int, Int) -> Unit,
    onOpenLauncherPermissionSettings: () -> Boolean,
    onOpenAllFilesAccessSettings: () -> Boolean,
    onOpenUnknownAppSourcesSettings: () -> Boolean,
    onCopyText: (String, String) -> Boolean,
    onOpenExternalUrl: (String) -> Boolean,
    onExportLog: (String, String, String, String, ExportLogMode, LauncherUpdate) -> Unit,
    onExportDiagnostic: (DiagnosticSnapshot, LauncherUpdate) -> Unit,
    onExportBackup: (LauncherUiState, LauncherUpdate) -> Unit,
    onExportVersionReport: (LauncherUpdate) -> Unit,
    onPickExternalBackup: ((ExternalBackupImportResult) -> Unit) -> Unit,
    onPickBackupExportDestination: (String, String, (BackupExportDestinationResult) -> Unit) -> Unit,
    onOpenBackupExportLocation: (String) -> Boolean,
    onSaveTavernMirrorConfig: (TavernMirrorConfig) -> TavernMirrorSaveResult,
    onSaveTavernPathConfig: (TavernPathConfig) -> TavernPathSaveResult,
    onRestoreDefaultTavernPath: () -> TavernPathSaveResult,
    onSaveGithubRepository: (String) -> GithubRepositorySaveResult,
    onSaveGithubUpdateChannel: (GithubReleaseChannel) -> GithubUpdateChannelSaveResult,
    onIgnoreGithubUpdate: (String) -> Unit,
    onCheckGithubUpdate: (String, GithubReleaseChannel, (GithubUpdateCheckResult) -> Unit) -> Unit,
    onFetchOfficialTavernVersions: (TavernMirrorConfig, (TavernOfficialVersionFetchResult) -> Unit) -> Unit,
    onCheckTavernMirror: (TavernMirrorConfig, (TavernMirrorProbeStatus) -> Unit) -> Unit,
    onInstallGithubUpdate: (GithubUpdateInfo, (GithubUpdateInstallResult) -> Unit) -> Unit,
    onOpenGithubRelease: (GithubUpdateInfo) -> GithubUpdateInstallResult,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val initialRuntimeText =
        "${initialState.status}\n${initialState.summary}\n${initialState.termuxLog}\n${initialState.appLog}"
    val initialRunningSignal = inferTavernRunning(initialRuntimeText)
    var status by remember { mutableStateOf(initialState.status) }
    var summary by remember { mutableStateOf(initialState.summary) }
    var termuxLog by remember { mutableStateOf(initialState.termuxLog) }
    var appLog by remember { mutableStateOf(initialState.appLog) }
    var verified by remember { mutableStateOf(initialState.verified) }
    var tavernRunning by remember { mutableStateOf(initialRunningSignal == true) }
    var tavernStarting by remember {
        mutableStateOf(initialRunningSignal == null && inferTavernStarting(initialRuntimeText))
    }
    var termuxInstalled by remember { mutableStateOf(initialTermuxInstalled) }
    var runCommandPermissionGranted by remember { mutableStateOf(initialRunCommandPermissionGranted) }
    var backgroundRunPermissionGranted by remember { mutableStateOf(initialBackgroundRunPermissionGranted) }
    var termuxBackgroundRunPermissionGranted by remember {
        mutableStateOf(initialTermuxBackgroundRunPermissionGranted)
    }
    var firstTavernStartGuideSeen by remember { mutableStateOf(initialFirstTavernStartGuideSeen) }
    var allFilesAccessGranted by remember { mutableStateOf(initialAllFilesAccessGranted) }
    var installUnknownAppsGranted by remember { mutableStateOf(initialInstallUnknownAppsGranted) }
    var termuxExternalAppsBlocked by remember {
        mutableStateOf(
            TermuxPermissionSignals.externalAppsBlocked(
                initialRuntimeText,
            ),
        )
    }
    val cachedTavernVersionInfo = TavernVersionParser.parse(initialState.termuxLog)
    val cachedOfficialVersions = (initialState.officialVersionsCache
        .takeIf { it.isNotBlank() }
        ?.let(TavernOfficialVersionParser::parse)
        ?: TavernOfficialVersionParser.parse(initialState.termuxLog))
        .takeUnless { it.hasData && !sameRepoUrl(it.repoUrl, initialTavernMirrorConfig.normalizedRepoUrl) }
        ?: TavernOfficialVersions()
    val cachedSelectedTavernVersion = if (startupRefreshSignal > 0) {
        null
    } else if (cachedTavernVersionInfo.hasData && !cachedTavernVersionInfo.notInstalled) {
        TavernVersionSelection.normalizeForVersionManagement(
            officialVersions = cachedOfficialVersions,
            current = cachedTavernVersionInfo,
            currentSelection = cachedOfficialVersions.all.firstOrNull(),
        )
    } else {
        TavernVersionSelection.normalizeForInstall(
            officialVersions = cachedOfficialVersions,
            currentSelection = cachedOfficialVersions.all.firstOrNull(),
        )
    }
    var tavernVersionInfo by remember {
        mutableStateOf(if (startupRefreshSignal > 0) TavernVersionInfo() else cachedTavernVersionInfo)
    }
    var tavernInstallDetected by remember {
        mutableStateOf<Boolean?>(
            if (startupRefreshSignal > 0) {
                null
            } else if (cachedTavernVersionInfo.notInstalled) {
                false
            } else {
                cachedTavernVersionInfo.takeIf { it.hasData }?.let { true }
            },
        )
    }
    var tavernVersionCheckInFlight by remember { mutableStateOf(false) }
    var officialVersions by remember {
        mutableStateOf(cachedOfficialVersions)
    }
    var selectedTavernVersion by remember {
        mutableStateOf(cachedSelectedTavernVersion)
    }
    var autoBackupEnabled by remember { mutableStateOf(initialState.autoBackupEnabled) }
    var autoBackupIntervalMinutes by remember {
        mutableIntStateOf(initialState.autoBackupIntervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        ))
    }
    var autoBackupKeepCount by remember { mutableIntStateOf(initialState.autoBackupKeepCount.coerceIn(1, 50)) }
    var backupHistory by remember { mutableStateOf(initialState.backupHistory) }
    var backupListRefreshing by remember { mutableStateOf(false) }
    var termuxReturnDelayMs by remember { mutableLongStateOf(initialState.termuxReturnDelayMs.coerceIn(300L, 2_000L)) }
    var logRefreshInFlight by remember { mutableStateOf(false) }
    var termuxBootstrapCompleted by remember { mutableStateOf(false) }
    var pendingTavernVersionActionConfirmation by remember {
        mutableStateOf<TavernVersionActionConfirmation?>(null)
    }
    var pendingTavernProfileRemovalConfirmation by remember {
        mutableStateOf<TavernProfileRemovalConfirmation?>(null)
    }
    var pendingTavernProfileMigrationConfirmation by remember {
        mutableStateOf<TavernProfileMigrationConfirmation?>(null)
    }
    var pendingTavernForceCleanupConfirmation by remember {
        mutableStateOf<TavernForceCleanupConfirmation?>(null)
    }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearLogScopeDialog by remember { mutableStateOf(false) }
    var showClearLogDangerDialog by remember { mutableStateOf(false) }
    var showManualBackupDialog by remember { mutableStateOf(false) }
    var showAutoBackupSettingsDialog by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var showBackgroundRunPermissionDialog by remember { mutableStateOf(false) }
    var showFirstTavernStartGuideDialog by remember { mutableStateOf(false) }
    var backgroundPermissionPromptShown by remember { mutableStateOf(false) }
    var showApplyBackupPathDialog by remember { mutableStateOf(false) }
    var showApplyBackupPreviewDialog by remember { mutableStateOf(false) }
    var showTermuxStoragePermissionDialog by remember { mutableStateOf(false) }
    var showCopyBackupDialog by remember { mutableStateOf(false) }
    var showRenameBackupDialog by remember { mutableStateOf(false) }
    var showDeleteBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var manualBackupName by remember { mutableStateOf("") }
    var selectedClearLogMode by remember { mutableStateOf(ExportLogMode.Both) }
    var clearLogConfirmText by remember { mutableStateOf("") }
    var applyBackupPath by remember { mutableStateOf("") }
    var applyBackupPreview by remember { mutableStateOf<BackupRestorePreview?>(null) }
    var storagePermissionRetryArchivePath by remember { mutableStateOf("") }
    var termuxStoragePermissionBlocked by remember {
        mutableStateOf(hasTermuxStoragePermissionProblem(initialRuntimeText))
    }
    var selectedBackupPath by remember { mutableStateOf("") }
    var renameBackupName by remember { mutableStateOf("") }
    var importBackupPath by remember { mutableStateOf("") }
    var pendingAptConfigTask by remember { mutableStateOf<PendingAptConfigTask?>(null) }
    var pendingInstallRiskRequest by remember { mutableStateOf<PendingTavernInstallRequest?>(null) }
    var installRiskConfirmation by remember { mutableStateOf<TavernInstallConfirmation?>(null) }
    var pendingStartPreflight by remember { mutableStateOf<TavernStartPreflightResult?>(null) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var busyStartedAtMillis by remember { mutableLongStateOf(0L) }
    var busyToken by remember { mutableIntStateOf(0) }
    var launchAttemptToken by remember { mutableIntStateOf(0) }
    var startupRefreshInFlight by remember { mutableStateOf(false) }
    var startupRefreshToken by remember { mutableIntStateOf(0) }
    var startupGithubCheckPending by remember { mutableStateOf(false) }
    var healthCheckInFlight by remember { mutableStateOf(false) }
    var healthCheckToken by remember { mutableIntStateOf(0) }
    var healthCheckReport by remember { mutableStateOf<LauncherHealthReport?>(null) }
    var lastLaunchReadinessSnapshotAtMillis by remember { mutableLongStateOf(0L) }
    var selectedTab by remember { mutableStateOf(LauncherTab.Launch) }
    var pagerInteractionLocked by remember { mutableStateOf(false) }
    val viewConfiguration = LocalViewConfiguration.current
    val pagerAxisGuard = remember(viewConfiguration.touchSlop) {
        PagerAxisGuardConnection(viewConfiguration.touchSlop)
    }
    pagerAxisGuard.interactionLocked = pagerInteractionLocked
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { LauncherTab.entries.size },
    )
    var githubRepository by remember { mutableStateOf(initialGithubRepository) }
    var githubUpdateChannel by remember { mutableStateOf(initialGithubUpdateChannel) }
    var githubRepositoryInput by remember { mutableStateOf(initialGithubRepository) }
    var tavernMirrorConfig by remember { mutableStateOf(initialTavernMirrorConfig) }
    var tavernPathConfig by remember { mutableStateOf(initialTavernPathConfig) }
    var tavernRepoInput by remember { mutableStateOf(initialTavernMirrorConfig.normalizedRepoUrl) }
    var npmRegistryInput by remember { mutableStateOf(initialTavernMirrorConfig.normalizedNpmRegistry) }
    var tavernPathInput by remember { mutableStateOf(initialTavernPathConfig.displayTavernDir) }
    var tavernPortInput by remember { mutableStateOf(initialTavernPathConfig.normalizedPort.toString()) }
    var mirrorProbeStatus by remember { mutableStateOf(TavernMirrorProbeStatus.unknown(initialTavernMirrorConfig)) }
    var termuxRepoStatus by remember { mutableStateOf(TermuxRepoStatus()) }
    var customTermuxRepoInput by remember { mutableStateOf("") }
    var ignoredUpdateTag by remember { mutableStateOf(initialIgnoredUpdateTag) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showInstallRiskDialog by remember { mutableStateOf(false) }
    var showTavernDirectoryChoiceDialog by remember { mutableStateOf(false) }
    var showCustomTavernPathMigrationDialog by remember { mutableStateOf(false) }
    var tavernDirectoryCandidates by remember { mutableStateOf<List<TavernDirectoryCandidateOption>>(emptyList()) }
    var customMigrationPathInput by remember { mutableStateOf("") }
    var pendingFirstTavernStartGuide by remember { mutableStateOf<FirstTavernStartGuide?>(null) }
    var githubUpdateState by remember {
        mutableStateOf(
            GithubUpdateUiState(
                repository = initialGithubRepository,
                channel = initialGithubUpdateChannel,
                message = if (initialGithubRepository.isBlank()) {
                    "未配置 GitHub 仓库。"
                } else {
                    "尚未检查 GitHub 更新。"
                },
            ),
        )
    }
    val initialPendingLauncherTask = remember { PendingLauncherTaskStore.load(context) }
    var pendingLauncherTask by remember { mutableStateOf(initialPendingLauncherTask) }
    var showPendingTaskDialog by remember { mutableStateOf(initialPendingLauncherTask != null) }
    val actionInProgress = busyLabel != null
    val issueAnalysis = TavernIssueAnalyzer.analyze(termuxLog, status)
    val scope = rememberCoroutineScope()
    val firstTavernStartGuide = remember {
        FirstTavernStartGuideResolver.resolve(
            brand = Build.BRAND.orEmpty(),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
        )
    }
    val showGithubUpdateBadge = githubUpdateState.hasUpdate &&
        githubUpdateState.latest?.tagName != ignoredUpdateTag
    var lastSyncedTermuxResultKey by remember {
        mutableStateOf(onLatestTermuxResult()?.key.orEmpty())
    }

    LaunchedEffect(Unit) {
        RuntimeLogArchive.ensureSeeded(context, initialState)
    }

    LaunchedEffect(autoBackupEnabled, backgroundRunPermissionGranted) {
        if (autoBackupEnabled && !backgroundRunPermissionGranted && !backgroundPermissionPromptShown) {
            backgroundPermissionPromptShown = true
            delay(700)
            showBackgroundRunPermissionDialog = true
        }
    }

    fun currentState(): LauncherUiState {
        return LauncherUiState(
            status = status,
            summary = summary,
            termuxLog = termuxLog,
            appLog = appLog,
            verified = verified,
            officialVersionsCache = TavernOfficialVersionParser.encode(officialVersions),
            autoBackupEnabled = autoBackupEnabled,
            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
            autoBackupKeepCount = autoBackupKeepCount,
            backupHistory = backupHistory,
            termuxReturnDelayMs = termuxReturnDelayMs,
        )
    }

    fun applyTavernPathSaveResult(result: TavernPathSaveResult) {
        tavernPathConfig = result.config
        tavernPathInput = result.config.displayTavernDir
        tavernPortInput = result.config.normalizedPort.toString()
    }

    fun customMigrationPathInputError(): String? {
        val trimmed = customMigrationPathInput.trim()
        if (trimmed.isBlank()) {
            return "请先填写要迁移到的目录。"
        }
        return TavernPathValidator.validate(trimmed)
    }

    fun rememberPendingLauncherTask(
        task: PendingLauncherTask,
        showDialog: Boolean = false,
    ) {
        pendingLauncherTask = task
        showPendingTaskDialog = showDialog
        PendingLauncherTaskStore.save(context, task)
    }

    fun clearPendingLauncherTask() {
        pendingLauncherTask = null
        showPendingTaskDialog = false
        PendingLauncherTaskStore.clear(context)
    }

    suspend fun recoverPendingManualBackup(task: PendingLauncherTask): PendingManualBackupRecoveryResult? {
        if (task.kind != PendingLauncherTaskKind.ManualBackup) return null
        return withContext(Dispatchers.IO) {
            val archiveDetails = BackupLibraryFiles.listLibraryArchives(context)
                .mapNotNull { path -> BackupLibraryFiles.describeLibraryArchive(context, path) }
            PendingManualBackupRecovery.recover(
                startedAtMillis = task.startedAtMillis,
                expectedLabel = task.targetLabel,
                archives = archiveDetails,
            )
        }
    }

    fun hideFirstTavernStartGuideDialog() {
        showFirstTavernStartGuideDialog = false
        pendingFirstTavernStartGuide = null
    }

    fun markFirstTavernStartGuideSeen() {
        if (firstTavernStartGuideSeen) return
        firstTavernStartGuideSeen = true
        onMarkFirstTavernStartGuideSeen()
    }

    fun maybeShowFirstTavernStartGuideBeforeStart(): Boolean {
        if (FirstTavernStartGuideResolver.hasSuccessfulStartHistory(termuxLog, appLog)) {
            markFirstTavernStartGuideSeen()
            return false
        }
        if (!FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = firstTavernStartGuideSeen,
                tavernInstallDetected = tavernInstallDetected,
                tavernRunning = tavernRunning,
                termuxLog = termuxLog,
                appLog = appLog,
                guideKind = firstTavernStartGuide.kind,
                termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            )
        ) {
            return false
        }
        pendingFirstTavernStartGuide = firstTavernStartGuide
        showFirstTavernStartGuideDialog = true
        return true
    }

    fun hasTavernDirectoryMissingSignal(text: String): Boolean {
        return text.contains("SillyTavern directory not found", ignoreCase = true) ||
            text.contains("酒馆目录不存在") ||
            text.contains("SillyTavern 目录不存在")
    }

    fun openTavernDirectoryChoice(candidates: List<String>) {
        val resolved = TavernDirectoryCandidateGuard.resolve(tavernPathConfig, candidates)
        if (resolved.isEmpty()) return
        tavernDirectoryCandidates = resolved
        showTavernDirectoryChoiceDialog = true
    }

    fun clearTavernDirectoryChoiceState() {
        showTavernDirectoryChoiceDialog = false
        tavernDirectoryCandidates = emptyList()
    }

    fun clearTransientTavernPathUiState() {
        clearTavernDirectoryChoiceState()
        pendingTavernProfileMigrationConfirmation = null
        showCustomTavernPathMigrationDialog = false
        customMigrationPathInput = ""
    }

    fun dismissTavernDirectoryChoiceDialog() {
        clearTavernDirectoryChoiceState()
    }

    fun maybePromptTavernDirectoryChoice(text: String) {
        if (text.isBlank() || !hasTavernDirectoryMissingSignal(text)) return
        if (
            !TavernDirectoryChoicePromptPolicy.shouldPrompt(
                text = text,
                currentPath = tavernPathConfig.activeProfile.normalizedTavernDir,
            )
        ) {
            return
        }
        openTavernDirectoryChoice(TavernDirectoryCandidateParser.parse(text))
    }

    fun inferTavernInstalledFromOutput(newStatus: String, termuxOutput: String): Boolean? {
        val parsedVersion = TavernVersionParser.parse(termuxOutput)
        if (parsedVersion.hasData) return true
        if (parsedVersion.notInstalled) return false
        val combined = "$newStatus\n$termuxOutput"
        if (hasTavernDirectoryMissingSignal(combined)) {
            return false
        }
        if (
            combined.contains("\"status\": \"running\"") ||
            combined.contains("\"status\": \"running-unknown\"") ||
            combined.contains("\"status\": \"unreachable\"") ||
            combined.contains("\"status\": \"stopped\"") ||
            combined.contains("\"status\": \"log\"")
        ) {
            return true
        }
        return null
    }

    fun normalizeTavernVersionSelection(
        currentInfo: TavernVersionInfo = tavernVersionInfo,
        availableVersions: TavernOfficialVersions = officialVersions,
        currentSelection: TavernVersionChoice? = selectedTavernVersion,
    ): TavernVersionChoice? {
        if (!availableVersions.hasData) {
            return currentSelection?.takeIf { it.kind == TavernVersionKind.Custom }
        }
        return if (currentInfo.hasData && !currentInfo.notInstalled) {
            TavernVersionSelection.normalizeForVersionManagement(
                officialVersions = availableVersions,
                current = currentInfo,
                currentSelection = currentSelection,
            )
        } else {
            TavernVersionSelection.normalizeForInstall(
                officialVersions = availableVersions,
                currentSelection = currentSelection,
            )
        }
    }

    fun applyOfficialVersions(parsed: TavernOfficialVersions) {
        officialVersions = parsed
        selectedTavernVersion = normalizeTavernVersionSelection(
            currentInfo = tavernVersionInfo,
            availableVersions = parsed,
            currentSelection = selectedTavernVersion,
        )
    }

    fun defaultInstallChoice(): TavernVersionChoice {
        return TavernInstallDefaults.releaseChoice(tavernMirrorConfig.normalizedRepoUrl)
    }

    fun currentMirrorProbeStatus(): TavernMirrorProbeStatus {
        return mirrorProbeStatus.takeIf { it.matches(tavernMirrorConfig) }
            ?: TavernMirrorProbeStatus.unknown(tavernMirrorConfig)
    }

    fun buildHealthCheckReport(
        doctorReport: TavernDoctorReport?,
        mirrorStatus: TavernMirrorProbeStatus = currentMirrorProbeStatus(),
    ): LauncherHealthReport {
        return LauncherHealthCheck.build(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
            tavernRunning = tavernRunning,
            mirrorProbeStatus = mirrorStatus,
            doctorReport = doctorReport,
        )
    }

    fun rememberLaunchReadinessSnapshot(output: String) {
        if (
            output.contains("==== SillyTavern version ====") ||
            output.contains("==== Current SillyTavern version ====") ||
            output.contains("==== Lukoa doctor ====")
        ) {
            lastLaunchReadinessSnapshotAtMillis = System.currentTimeMillis()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _: LifecycleOwner, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val installed = onCheckTermuxInstalled()
                val launcherBackgroundGranted = onCheckBackgroundRunPermission()
                termuxInstalled = installed
                runCommandPermissionGranted = installed && onCheckRunCommandPermission()
                backgroundRunPermissionGranted = launcherBackgroundGranted
                termuxBackgroundRunPermissionGranted = installed && onCheckTermuxBackgroundRunPermission()
                allFilesAccessGranted = onCheckAllFilesAccessPermission()
                installUnknownAppsGranted = onCheckInstallUnknownAppsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun applyTavernVersionInfo(parsed: TavernVersionInfo) {
        tavernVersionInfo = parsed
        tavernInstallDetected = parsed.hasData && !parsed.notInstalled
        selectedTavernVersion = normalizeTavernVersionSelection(
            currentInfo = parsed,
            availableVersions = officialVersions,
            currentSelection = selectedTavernVersion,
        )
    }

    fun applyTavernVersionInfoFromOutput(output: String): Boolean {
        val parsed = TavernVersionParser.parse(output)
        if (!parsed.hasData && !parsed.notInstalled) return false
        applyTavernVersionInfo(parsed)
        return true
    }

    fun applyTavernRunningSignal(source: String, allowHeuristic: Boolean = true) {
        if (source.isBlank()) return
        val portConflictDetected = inferTavernPortConflict(source)
        val inferredRunning = if (allowHeuristic) {
            inferTavernRunning(source)
        } else {
            inferExplicitTavernRunning(source)
        }
        if (portConflictDetected && inferredRunning != true) {
            tavernRunning = false
            tavernStarting = false
            showStopConfirmDialog = false
        }
        if (inferTavernStarting(source) && inferredRunning == null && allowHeuristic) {
            tavernStarting = true
        }
        inferredRunning?.let { running ->
            tavernRunning = running
            tavernStarting = false
            if (!running) {
                showStopConfirmDialog = false
            }
            if (running) {
                tavernInstallDetected = true
            }
        }
    }

    fun resetVisibleStateForActiveProfile(statusText: String, summaryText: String) {
        val activeLabel = tavernPathConfig.activeProfileLabel
        val waitingLog = "正在读取${activeLabel}的运行状态和版本信息。"
        val clearedVersionInfo = TavernVersionInfo()
        val customSelection = selectedTavernVersion?.takeIf { it.kind == TavernVersionKind.Custom }

        status = statusText
        summary = summaryText
        verified = true
        termuxLog = waitingLog
        appLog = "App：$statusText"
        tavernRunning = false
        tavernStarting = false
        tavernInstallDetected = null
        tavernVersionInfo = clearedVersionInfo
        selectedTavernVersion = normalizeTavernVersionSelection(
            currentInfo = clearedVersionInfo,
            availableVersions = officialVersions,
            currentSelection = customSelection,
        )
        healthCheckReport = null
        showStopConfirmDialog = false
        pendingStartPreflight = null
        pendingTavernForceCleanupConfirmation = null
        pendingTavernVersionActionConfirmation = null
        pendingTavernProfileRemovalConfirmation = null
        clearTransientTavernPathUiState()
        launchAttemptToken += 1
        lastSyncedTermuxResultKey = onLatestTermuxResult()?.key.orEmpty()
        onPersistState(currentState().copy(
            status = statusText,
            summary = summaryText,
            termuxLog = waitingLog,
            appLog = "App：$statusText",
            verified = true,
        ))
    }

    fun normalizeTermuxOutputForDisplay(output: String): String {
        return output.trim()
    }

    fun update(newStatus: String, termuxOutput: String, ok: Boolean, allowRunningInference: Boolean = true) {
        val displayTermuxOutput = normalizeTermuxOutputForDisplay(termuxOutput)
        val newSummary = StatusSummarizer.summarize(newStatus, termuxOutput, ok)
        RuntimeLogArchive.appendApp(context, newStatus)
        if (displayTermuxOutput.isNotBlank()) {
            RuntimeLogArchive.appendTermux(context, displayTermuxOutput)
        }
        val newAppLog = appendLog(appLog, "App", newStatus)
        val newTermuxLog = if (displayTermuxOutput.isNotBlank()) {
            appendRawLog(termuxLog, displayTermuxOutput)
        } else {
            termuxLog
        }
        val nextBackupHistory = BackupHistoryReducer.reduce(backupHistory, termuxOutput, ok)

        status = newStatus
        summary = newSummary
        verified = ok
        appLog = newAppLog
        termuxLog = newTermuxLog
        backupHistory = nextBackupHistory
        rememberLaunchReadinessSnapshot(termuxOutput)
        applyTavernVersionInfoFromOutput(termuxOutput)
        inferTavernInstalledFromOutput(newStatus, termuxOutput)?.let {
            tavernInstallDetected = it
        }
        TavernOfficialVersionParser.parse(termuxOutput).takeIf { it.hasData }?.let(::applyOfficialVersions)
        TermuxRepoStatusParser.parse(termuxOutput)?.let { parsed ->
            termuxRepoStatus = parsed
            if (customTermuxRepoInput.isBlank() || parsed.label == "自定义") {
                customTermuxRepoInput = parsed.uri
            }
        }
        val permissionText = "$newStatus\n$termuxOutput"
        maybePromptTavernDirectoryChoice(permissionText)
        if (TermuxPermissionSignals.externalAppsBlocked(permissionText)) {
            termuxExternalAppsBlocked = true
        }
        if (hasTermuxStoragePermissionProblem(permissionText)) {
            termuxStoragePermissionBlocked = true
            storagePermissionRetryArchivePath = applyBackupPath.ifBlank { storagePermissionRetryArchivePath }
            showTermuxStoragePermissionDialog = true
        } else if (termuxOutput.contains("storage.permission.ok=true", ignoreCase = true)) {
            termuxStoragePermissionBlocked = false
        }
        applyTavernRunningSignal(permissionText, allowRunningInference)
        if (permissionText.contains("缺少 RUN_COMMAND 权限")) {
            runCommandPermissionGranted = false
        } else if (ok && termuxOutput.isNotBlank()) {
            runCommandPermissionGranted = true
        }
        onPersistState(currentState().copy(
            status = newStatus,
            summary = newSummary,
            termuxLog = newTermuxLog,
            appLog = newAppLog,
            verified = ok,
            backupHistory = nextBackupHistory,
            termuxReturnDelayMs = termuxReturnDelayMs,
        ))
    }

    fun blockIfPendingTaskExists(actionName: String): Boolean {
        val task = pendingLauncherTask ?: return false
        if (actionInProgress) return false
        showPendingTaskDialog = true
        update(
            PendingLauncherTaskSupport.conflictMessage(task, actionName),
            "",
            false,
            allowRunningInference = false,
        )
        return true
    }

    fun syncTermuxResult(display: TermuxResultDisplay) {
        if (display.output.isBlank()) return
        val displayTermuxOutput = normalizeTermuxOutputForDisplay(display.output)
        val newStatus = "已同步 Termux：${display.command}"
        val newSummary = StatusSummarizer.summarize(newStatus, display.output, display.ok)
        RuntimeLogArchive.appendApp(context, newStatus)
        if (displayTermuxOutput.isNotBlank()) {
            RuntimeLogArchive.appendTermux(context, displayTermuxOutput)
        }
        val newTermuxLog = if (displayTermuxOutput.isNotBlank()) {
            appendRawLog(termuxLog, displayTermuxOutput)
        } else {
            termuxLog
        }
        val newAppLog = appendLog(appLog, "App", newStatus)
        val nextBackupHistory = BackupHistoryReducer.reduce(backupHistory, display.output, display.ok)

        status = newStatus
        summary = newSummary
        verified = display.ok
        termuxLog = newTermuxLog
        appLog = newAppLog
        backupHistory = nextBackupHistory
        rememberLaunchReadinessSnapshot(display.output)
        applyTavernVersionInfoFromOutput(display.output)
        inferTavernInstalledFromOutput(newStatus, display.output)?.let {
            tavernInstallDetected = it
        }
        TavernOfficialVersionParser.parse(display.output).takeIf { it.hasData }?.let(::applyOfficialVersions)
        TermuxRepoStatusParser.parse(display.output)?.let { parsed ->
            termuxRepoStatus = parsed
            if (customTermuxRepoInput.isBlank() || parsed.label == "自定义") {
                customTermuxRepoInput = parsed.uri
            }
        }
        if (TermuxPermissionSignals.externalAppsBlocked(display.output)) {
            termuxExternalAppsBlocked = true
        }
        maybePromptTavernDirectoryChoice(display.output)
        applyTavernRunningSignal(display.output, allowHeuristic = true)
        if (display.output.contains("缺少 RUN_COMMAND 权限")) {
            runCommandPermissionGranted = false
        } else if (display.ok) {
            runCommandPermissionGranted = true
        }
        onPersistState(currentState().copy(
            status = newStatus,
            summary = newSummary,
            termuxLog = newTermuxLog,
            appLog = newAppLog,
            verified = display.ok,
            backupHistory = nextBackupHistory,
            termuxReturnDelayMs = termuxReturnDelayMs,
        ))
    }

    fun isTransientStatus(text: String): Boolean {
        return text.startsWith("正在") ||
            text.startsWith("命令已发送到 Termux") ||
            text.startsWith("已发送 selftest") ||
            text.contains("命令已发送到 Termux") ||
            text.contains("等待 Termux") ||
            text.contains("等待 selftest")
    }

    fun releaseBusy() {
        busyLabel = null
        busyStartedAtMillis = 0L
        OperationLockStore.release(context)
    }

    fun beginBusy(label: String, timeoutMs: Long = 18000L): Boolean {
        val current = busyLabel
        if (current != null) {
            update("正在处理：$current。请等一下。", "", false)
            return false
        }

        busyLabel = label
        busyStartedAtMillis = SystemClock.elapsedRealtime()
        OperationLockStore.acquire(context, label, timeoutMs)
        busyToken += 1
        val token = busyToken
        scope.launch {
            delay(timeoutMs)
            if (busyToken == token && busyLabel != null) {
                val timedOutTask = pendingLauncherTask
                val recovered = timedOutTask?.let { recoverPendingManualBackup(it) }
                if (busyToken != token || busyLabel == null) return@launch
                if (recovered != null) {
                    clearPendingLauncherTask()
                    releaseBusy()
                    val nextBackupHistory = BackupHistoryReducer.sanitize(
                        listOf(recovered.archivePath) + backupHistory,
                    )
                    backupHistory = nextBackupHistory
                    onPersistState(currentState().copy(backupHistory = nextBackupHistory))
                    update(
                        "已继续检查这次创建备份：备份已经生成，但 Termux 没把最终返回带回来。\n备份在：${recovered.archivePath}",
                        "",
                        true,
                        allowRunningInference = false,
                    )
                    return@launch
                }
                releaseBusy()
                update("没收到 Termux 返回，按钮已恢复。", "", false)
            }
        }
        return true
    }

    fun runGuarded(
        label: String,
        timeoutMs: Long = 18000L,
        allowRunningInference: Boolean = true,
        action: (LauncherUpdate) -> Unit,
    ) {
        if (!beginBusy(label, timeoutMs)) return
        action { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference)
            if (!isTransientStatus(newStatus)) {
                releaseBusy()
            }
        }
    }

    fun runPendingGuardedCommand(
        task: PendingLauncherTask,
        label: String,
        timeoutMs: Long,
        allowRunningInference: Boolean = false,
        action: (LauncherUpdate) -> Unit,
    ) {
        if (!beginBusy(label, timeoutMs)) return
        rememberPendingLauncherTask(task)
        action { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference)
            if (!isTransientStatus(newStatus)) {
                clearPendingLauncherTask()
                releaseBusy()
            }
        }
    }

    fun runStartupRefresh() {
        if (
            startupRefreshInFlight ||
            busyLabel != null ||
            !termuxInstalled ||
            !runCommandPermissionGranted ||
            termuxExternalAppsBlocked
        ) return
        startupRefreshInFlight = true
        tavernVersionCheckInFlight = true
        startupRefreshToken += 1
        val refreshToken = startupRefreshToken
        scope.launch {
            delay(20_000L)
            if (startupRefreshInFlight && startupRefreshToken == refreshToken) {
                startupRefreshToken += 1
                startupRefreshInFlight = false
                tavernVersionCheckInFlight = false
                startupGithubCheckPending = true
                update("自动检测没收到返回。可以手动检测。", "", false, allowRunningInference = false)
            }
        }

        fun finishStartupRefresh(token: Int) {
            if (startupRefreshToken != token) return
            startupRefreshInFlight = false
            tavernVersionCheckInFlight = false
            startupGithubCheckPending = true
        }

        fun runStep(command: String, token: Int) {
            onCommand(command) { newStatus, termuxOutput, ok ->
                if (startupRefreshToken != token) {
                    return@onCommand
                }
                if (isTransientStatus(newStatus) && termuxOutput.isBlank()) {
                    return@onCommand
                }
                update(newStatus, termuxOutput, ok, allowRunningInference = true)
                val nextCommand = if (command == "status") {
                    "tavern-version-startup"
                } else {
                    null
                }
                scope.launch {
                    delay(350)
                    if (startupRefreshToken != token) {
                        return@launch
                    }
                    if (nextCommand != null) {
                        runStep(nextCommand, token)
                    } else {
                        finishStartupRefresh(token)
                    }
                }
            }
        }

        update("正在检测酒馆是否运行。", "", false, allowRunningInference = false)
        runStep("status", refreshToken)
    }

    fun refreshActiveProfileState(statusText: String) {
        val canAutoRefresh =
            termuxInstalled &&
                runCommandPermissionGranted &&
                !termuxExternalAppsBlocked &&
                !actionInProgress
        resetVisibleStateForActiveProfile(
            statusText = statusText,
            summaryText = if (canAutoRefresh) {
                "正在刷新当前实例状态"
            } else {
                "当前实例已切换，状态会在下一次检测时更新"
            },
        )
        if (canAutoRefresh) {
            scope.launch {
                delay(250)
                runStartupRefresh()
            }
        }
    }

    fun updateTermuxLogOnly(termuxOutput: String, ok: Boolean) {
        logRefreshInFlight = false
        if (TermuxPermissionSignals.externalAppsBlocked(termuxOutput)) {
            termuxExternalAppsBlocked = true
            update("Termux 外部调用未开启。请复制权限命令到 Termux 执行。", termuxOutput, false, allowRunningInference = false)
            return
        }
        inferTavernInstalledFromOutput("日志同步", termuxOutput)?.let {
            tavernInstallDetected = it
        }

        fun applyDetectedTavernState(source: String, nextTermuxLog: String = termuxLog) {
            val inferredRunning = inferTavernRunning(source)
            val startingDetected = inferTavernStarting(source)
            val portConflictDetected = inferTavernPortConflict(source)
            val newStatus = when (inferredRunning) {
                true -> "检测到酒馆正在运行。"
                false -> "检测到酒馆已停止。"
                null -> when {
                    portConflictDetected -> "检测到酒馆端口被别的进程占用。"
                    startingDetected -> "检测到酒馆正在启动。"
                    else -> status
                }
            }
            val newSummary = when (inferredRunning) {
                true -> if (source.contains("HTTP endpoint is not responding")) {
                    "酒馆进程存在，但网页暂时打不开"
                } else {
                    "酒馆正在运行"
                }
                false -> if (portConflictDetected) {
                    "酒馆端口已被别的进程占用"
                } else {
                    "酒馆当前未运行"
                }
                null -> when {
                    portConflictDetected -> "酒馆端口已被别的进程占用"
                    startingDetected -> "正在启动酒馆"
                    else -> summary
                }
            }
            val newVerified = if (inferredRunning != null || portConflictDetected) ok else verified
            val newTavernRunning = if (portConflictDetected && inferredRunning != true) {
                false
            } else {
                inferredRunning ?: tavernRunning
            }
            val newTavernStarting = when {
                portConflictDetected && inferredRunning != true -> false
                inferredRunning != null -> false
                startingDetected -> true
                else -> tavernStarting
            }
            if (newTavernRunning) {
                tavernInstallDetected = true
            }
            if (
                newStatus == status &&
                newSummary == summary &&
                newVerified == verified &&
                nextTermuxLog == termuxLog &&
                newTavernRunning == tavernRunning &&
                newTavernStarting == tavernStarting
            ) {
                return
            }
            status = newStatus
            summary = newSummary
            verified = newVerified
            termuxLog = nextTermuxLog
            tavernRunning = newTavernRunning
            tavernStarting = newTavernStarting
            onPersistState(currentState().copy(
                status = newStatus,
                summary = newSummary,
                termuxLog = nextTermuxLog,
                verified = newVerified,
            ))
        }

        if (termuxOutput.contains("缺少 RUN_COMMAND 权限")) {
            runCommandPermissionGranted = false
            return
        }

        val displayTermuxOutput = normalizeTermuxOutputForDisplay(termuxOutput)
        if (displayTermuxOutput.isBlank()) {
            applyDetectedTavernState(termuxOutput)
            return
        }

        val newTermuxLog = if (termuxLog.contains(displayTermuxOutput)) {
            termuxLog
        } else {
            RuntimeLogArchive.appendTermux(context, displayTermuxOutput)
            appendRawLog(termuxLog, displayTermuxOutput)
        }
        applyDetectedTavernState(displayTermuxOutput, newTermuxLog)
    }

    fun requestClearLogs() {
        if (actionInProgress) {
            update("正在处理，完成后再清除日志。", "", false)
            return
        }
        clearLogConfirmText = ""
        showClearLogScopeDialog = true
    }

    fun prepareClearLogs(mode: ExportLogMode) {
        selectedClearLogMode = mode
        clearLogConfirmText = ""
        showClearLogScopeDialog = false
        showClearLogDangerDialog = true
    }

    fun clearLogs(mode: ExportLogMode) {
        showClearLogDangerDialog = false
        clearLogConfirmText = ""
        val targetText = when (mode) {
            ExportLogMode.TermuxOnly -> "Termux 调用返回"
            ExportLogMode.AppOnly -> "App 操作反馈"
            ExportLogMode.Both -> "Termux 调用返回和 App 操作反馈"
        }
        val newStatus = "已清除$targetText。"
        val newSummary = "日志显示已清理"
        RuntimeLogArchive.clear(context, mode)
        RuntimeLogArchive.appendApp(context, newStatus)
        val newTermuxLog = if (mode.includeTermux) "暂无 Termux 回传。" else termuxLog
        val newAppLog = if (mode.includeApp) {
            "暂无 App 操作反馈。"
        } else {
            appendLog(appLog, "App", "已清除$targetText，酒馆文件未删。")
        }
        status = newStatus
        summary = newSummary
        verified = true
        termuxLog = newTermuxLog
        appLog = newAppLog
        onPersistState(
            LauncherUiState(
                status = newStatus,
                summary = newSummary,
                termuxLog = newTermuxLog,
                appLog = newAppLog,
                verified = true,
                officialVersionsCache = TavernOfficialVersionParser.encode(officialVersions),
                autoBackupEnabled = autoBackupEnabled,
                autoBackupIntervalMinutes = autoBackupIntervalMinutes,
                autoBackupKeepCount = autoBackupKeepCount,
                backupHistory = backupHistory,
                termuxReturnDelayMs = termuxReturnDelayMs,
            ),
        )
    }

    fun selectedVersionCommand(baseCommand: String): String? {
        val selectedVersion = selectedTavernVersion ?: return null
        val target = selectedVersion.target
        LauncherInputGuards.validateVersionTarget(target)?.let { reason ->
            update("选择的酒馆版本无效：$reason", "", false, allowRunningInference = false)
            return null
        }
        val repoUrl = selectedVersion.repoUrl.ifBlank { tavernMirrorConfig.normalizedRepoUrl }
        TavernMirrorValidator.validateRepoUrl(repoUrl)?.let { reason ->
            update("酒馆 Git 源地址无效：$reason", "", false, allowRunningInference = false)
            return null
        }
        val encoded = TavernVersionCommandCodec.encode(
            target = target,
            repoUrl = repoUrl,
            commit = selectedVersion.commit,
        )
        return "$baseCommand::$encoded"
    }

    fun runSelectedVersionCommand(
        baseCommand: String,
        emptyMessage: String,
        busyText: String,
        timeoutMs: Long,
    ) {
        val command = selectedVersionCommand(baseCommand)
        if (command == null) {
            update(emptyMessage, "", false, allowRunningInference = false)
            return
        }
        runGuarded(busyText, timeoutMs, allowRunningInference = false) { guardedUpdate ->
            onCommand(command, guardedUpdate)
        }
    }

    fun runSelectedVersionCommandWithSafetyBackup(
        baseCommand: String,
        emptyMessage: String,
        busyText: String,
        taskKind: PendingLauncherTaskKind,
        safetyBackupPrefix: String,
    ) {
        val command = selectedVersionCommand(baseCommand)
        if (command == null) {
            if (selectedTavernVersion == null) {
                update(emptyMessage, "", false, allowRunningInference = false)
            }
            return
        }
        val startedAtMillis = System.currentTimeMillis()
        val targetLabel = PendingLauncherTaskSupport.selectedVersionTargetLabel(selectedTavernVersion)
        val taskTitle = taskKind.title
        val safetyBackupCommand = "tavern-backup-manual::${PendingLauncherTaskSupport.buildSafetyBackupLabel(safetyBackupPrefix)}"
        if (!beginBusy(busyText, 1_200_000L)) return
        rememberPendingLauncherTask(
            PendingLauncherTask(
                kind = taskKind,
                commandName = "tavern-backup",
                detail = "正在自动创建安全备份",
                startedAtMillis = startedAtMillis,
                targetLabel = targetLabel,
                profileId = tavernPathConfig.activeProfile.id,
            ),
        )
        update("正在自动创建安全备份，完成后才会继续${taskTitle}。", "", false, allowRunningInference = false)
        onCommand(safetyBackupCommand) { backupStatus, backupOutput, backupOk ->
            val backupFinished = !isTransientStatus(backupStatus)
            val backupStatusText = if (backupFinished && !backupOk) {
                "$backupStatus\n这次${taskTitle}没有开始。"
            } else {
                backupStatus
            }
            update(backupStatusText, backupOutput, backupOk, allowRunningInference = false)
            if (!backupFinished) return@onCommand
            if (!backupOk) {
                clearPendingLauncherTask()
                releaseBusy()
                return@onCommand
            }
            val safetyBackupPath = BackupHistoryReducer.extractCreatedBackupArchive(backupOutput, backupOk).orEmpty()
            if (safetyBackupPath.isBlank()) {
                clearPendingLauncherTask()
                releaseBusy()
                update(
                    "自动安全备份已完成，但没有读到备份路径。为稳妥起见，这次没有开始${taskTitle}。",
                    backupOutput,
                    false,
                    allowRunningInference = false,
                )
                return@onCommand
            }
            rememberPendingLauncherTask(
                PendingLauncherTask(
                    kind = taskKind,
                    commandName = baseCommand,
                    detail = "自动安全备份已完成，正在${taskTitle}",
                    startedAtMillis = startedAtMillis,
                    targetLabel = targetLabel,
                    safetyBackupPath = safetyBackupPath,
                    profileId = tavernPathConfig.activeProfile.id,
                ),
            )
            onCommand(command) { newStatus, termuxOutput, ok ->
                val finished = !isTransientStatus(newStatus)
                val statusWithBackupPath = if (finished) {
                    buildString {
                        append(newStatus)
                        append('\n')
                        append(
                            if (ok) {
                                "自动安全备份已保留：$safetyBackupPath"
                            } else {
                                "自动安全备份在：$safetyBackupPath"
                            },
                        )
                    }
                } else {
                    newStatus
                }
                update(statusWithBackupPath, termuxOutput, ok, allowRunningInference = false)
                if (finished) {
                    clearPendingLauncherTask()
                    releaseBusy()
                }
            }
        }
    }

    fun updateBackupSettings(
        enabled: Boolean = autoBackupEnabled,
        intervalMinutes: Int = autoBackupIntervalMinutes,
        keepCount: Int = autoBackupKeepCount,
        resetCountdown: Boolean = false,
        message: String? = null,
    ) {
        val safeIntervalMinutes = intervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        )
        autoBackupEnabled = enabled
        autoBackupIntervalMinutes = safeIntervalMinutes
        autoBackupKeepCount = keepCount.coerceIn(1, 50)
        onPersistAutoBackupConfig(
            enabled,
            safeIntervalMinutes,
            keepCount.coerceIn(1, 50),
        )
        onConfigureAutoBackupSchedule(enabled, safeIntervalMinutes, resetCountdown)
        if (message != null) {
            update(message, "", true, allowRunningInference = false)
        } else {
            onPersistState(currentState().copy(
                autoBackupEnabled = enabled,
                autoBackupIntervalMinutes = safeIntervalMinutes,
                autoBackupKeepCount = keepCount.coerceIn(1, 50),
            ))
        }
    }

    fun toggleAutoBackup() {
        val enabled = !autoBackupEnabled
        updateBackupSettings(
            enabled = enabled,
            resetCountdown = enabled,
            message = if (enabled) {
                "自动备份已开启：每 ${formatBackupInterval(autoBackupIntervalMinutes)} 一次，保留 ${autoBackupKeepCount} 个。"
            } else {
                "自动备份已关闭。已有备份不会被删除。"
            },
        )
        if (enabled && !backgroundRunPermissionGranted) {
            showBackgroundRunPermissionDialog = true
        }
    }

    fun updateTermuxReturnDelay(nextDelayMs: Long) {
        val coerced = nextDelayMs.coerceIn(300L, 2_000L)
        termuxReturnDelayMs = coerced
        update(
            "Termux 唤醒返回等待已设为 ${"%.1f".format(coerced / 1000f)} 秒。",
            "",
            true,
            allowRunningInference = false,
        )
    }

    fun exportWithMode(mode: ExportLogMode) {
        showExportDialog = false
        onExportLog(summary, status, termuxLog, appLog, mode, ::update)
    }

    fun exportDiagnosticLog() {
        if (actionInProgress) {
            update("正在处理，完成后再导出诊断日志。", "", false, allowRunningInference = false)
            return
        }
        val snapshot = DiagnosticSnapshot(
            state = currentState(),
            versionInfo = versionInfo,
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
            tavernRunning = tavernRunning,
            tavernStarting = tavernStarting,
            tavernInstallDetected = tavernInstallDetected,
            actionInProgress = actionInProgress,
            busyLabel = busyLabel,
            tavernVersionInfo = tavernVersionInfo,
            officialVersions = officialVersions,
            selectedVersion = selectedTavernVersion,
            tavernMirrorConfig = tavernMirrorConfig,
            tavernPathConfig = tavernPathConfig,
            githubRepository = githubRepository,
            githubUpdateState = githubUpdateState,
            healthCheckReport = healthCheckReport,
            issueAnalysis = issueAnalysis,
        )
        update("正在生成诊断日志。日志较大时会稍等一会儿，但不该再把界面卡死。", "", true, allowRunningInference = false)
        onExportDiagnostic(snapshot) { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
        }
    }

    fun replaceBackupHistory(paths: List<String>): List<String> {
        val nextBackupHistory = BackupHistoryReducer.sanitize(paths)
        backupHistory = nextBackupHistory
        onPersistState(currentState().copy(backupHistory = nextBackupHistory))
        return nextBackupHistory
    }

    fun localBackupListMessage(paths: List<String>): String {
        return if (paths.isEmpty()) {
            "没有读到备份。请先生成或导入。"
        } else {
            "备份库已刷新，共 ${paths.size} 个。"
        }
    }

    fun readLocalBackupLibrary(): List<String> {
        return BackupHistoryReducer.sanitize(
            AutoBackupRetentionManager.enforceConfiguredLimit(
                context = context,
                reason = "backup-library-refresh",
            ),
        )
    }

    fun runLocalBackupLibraryOperation(
        label: String,
        operation: () -> Pair<List<String>, String>,
    ) {
        if (busyLabel != null) {
            update("正在处理：${busyLabel.orEmpty()}。请稍等。", "", false, allowRunningInference = false)
            return
        }
        busyLabel = label
        busyStartedAtMillis = SystemClock.elapsedRealtime()
        busyToken += 1
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { operation() }
            }
            releaseBusy()
            result.onSuccess { (paths, message) ->
                replaceBackupHistory(paths)
                update(message, "", true, allowRunningInference = false)
            }.onFailure { error ->
                update("$label 失败：${error.message ?: error.javaClass.simpleName}", "", false, allowRunningInference = false)
            }
        }
    }

    fun refreshBackupList() {
        if (backupListRefreshing) return
        if (busyLabel != null) {
            update("正在处理：${busyLabel.orEmpty()}。请稍等。", "", false, allowRunningInference = false)
            return
        }
        backupListRefreshing = true
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val result = withContext(Dispatchers.IO) {
                runCatching { readLocalBackupLibrary() }
            }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            if (elapsed < 450L) {
                delay(450L - elapsed)
            }
            result.onSuccess { paths ->
                replaceBackupHistory(paths)
            }.onFailure { error ->
                update(
                    "刷新备份库失败：${error.message ?: error.javaClass.simpleName}",
                    "",
                    false,
                    allowRunningInference = false,
                )
            }
            backupListRefreshing = false
        }
    }

    fun runPendingTaskFollowUpRefresh(
        refreshTargets: PendingTaskRefreshTargets,
        startupDelayMs: Long = 0L,
    ) {
        if (refreshTargets.backupList) {
            refreshBackupList()
        }
        if (refreshTargets.startupState) {
            if (startupDelayMs <= 0L) {
                runStartupRefresh()
            } else {
                scope.launch {
                    delay(startupDelayMs)
                    runStartupRefresh()
                }
            }
        }
    }

    fun applyProfileMutationTaskSideEffect(
        task: PendingLauncherTask,
        ok: Boolean,
        output: String,
    ): PendingTaskResolveResult? {
        if (!ok) return null
        return when (task.kind) {
            PendingLauncherTaskKind.MigrateTavernDirectory -> {
                val profile = tavernPathConfig.availableProfiles.firstOrNull { it.id == task.profileId }
                    ?: return PendingTaskResolveResult(
                        ok = false,
                        message = "酒馆目录已经迁移，但启动器没找到要更新的实例配置。请手动检查当前实例路径。",
                    )
                val targetPath = TavernProfilePathMutationOutputParser.migratedTargetPath(output)
                    ?: task.targetPath.takeIf { it.isNotBlank() }
                    ?: return PendingTaskResolveResult(
                        ok = false,
                        message = "酒馆目录已经迁移，但启动器没读到新的目录路径。请手动检查当前实例路径。",
                    )
                val saveResult = onSaveTavernPathConfig(
                    tavernPathConfig.withUpdatedProfile(
                        profileId = task.profileId,
                        tavernDir = targetPath,
                    ),
                )
                applyTavernPathSaveResult(saveResult)
                if (!saveResult.saved) {
                    return PendingTaskResolveResult(
                        ok = false,
                        message = "酒馆目录已经迁移，但启动器保存新路径失败：${saveResult.message}",
                    )
                }
                clearTransientTavernPathUiState()
                val updatedProfile = saveResult.config.availableProfiles
                    .firstOrNull { it.id == task.profileId }
                PendingTaskResolveResult(
                    ok = true,
                    message = "${profile.normalizedName}的酒馆目录已迁移到${updatedProfile?.displayTavernDir ?: TavernPathNormalizer.toDisplayPath(TavernPathNormalizer.normalize(targetPath))}。",
                    refreshTargets = PendingTaskRefreshTargets(
                        startupState = saveResult.config.activeProfile.id == task.profileId,
                    ),
                )
            }

            PendingLauncherTaskKind.RemoveManagedProfileDirectory -> {
                if (task.profileId.isBlank()) {
                    return PendingTaskResolveResult(
                        ok = false,
                        message = "分身实例托管目录已经删除，但启动器没找到要移除的实例配置。请手动检查实例列表。",
                    )
                }
                val profileName = tavernPathConfig.availableProfiles
                    .firstOrNull { it.id == task.profileId }
                    ?.normalizedName
                    ?: "这个分身实例"
                val saveResult = onSaveTavernPathConfig(tavernPathConfig.removeProfile(task.profileId))
                applyTavernPathSaveResult(saveResult)
                if (!saveResult.saved) {
                    return PendingTaskResolveResult(
                        ok = false,
                        message = "分身实例托管目录已经删除，但启动器移除实例配置失败：${saveResult.message}",
                    )
                }
                clearTransientTavernPathUiState()
                PendingTaskResolveResult(
                    ok = true,
                    message = "已删除${profileName}的托管目录，并切换到${saveResult.config.activeProfileLabel}继续管理。",
                    refreshTargets = PendingTaskRefreshTargets(startupState = true),
                )
            }

            else -> null
        }
    }

    fun runProfileMutationPendingCommand(
        task: PendingLauncherTask,
        label: String,
        timeoutMs: Long,
        command: String,
    ) {
        if (!beginBusy(label, timeoutMs)) return
        rememberPendingLauncherTask(task)
        onCommand(command) { newStatus, termuxOutput, ok ->
            val finalResult = if (isTransientStatus(newStatus)) {
                null
            } else {
                applyProfileMutationTaskSideEffect(task, ok, termuxOutput)
            }
            update(
                finalResult?.message ?: newStatus,
                termuxOutput,
                finalResult?.ok ?: ok,
                allowRunningInference = false,
            )
            if (!isTransientStatus(newStatus)) {
                clearPendingLauncherTask()
                releaseBusy()
                finalResult?.let { resolved ->
                    runPendingTaskFollowUpRefresh(
                        refreshTargets = resolved.refreshTargets,
                        startupDelayMs = 250L,
                    )
                }
            }
        }
    }

    fun checkTavernInstall() {
        if (!termuxInstalled || !runCommandPermissionGranted) {
            update("请先准备好 Termux。", "", false, allowRunningInference = false)
            return
        }
        if (!beginBusy("检测酒馆安装", 18000L)) return
        tavernVersionCheckInFlight = true
        val token = busyToken
        scope.launch {
            delay(20_000L)
            if (busyToken == token && tavernVersionCheckInFlight) {
                tavernVersionCheckInFlight = false
            }
        }
        onCommand("tavern-version") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            if (!isTransientStatus(newStatus)) {
                tavernVersionCheckInFlight = false
                releaseBusy()
            }
        }
    }

    fun refreshOfficialVersions() {
        if (!beginBusy("读取官方版本", 30000L)) return
        val requestToken = busyToken
        update("正在读取官方版本列表。", "", false, allowRunningInference = false)
        onFetchOfficialTavernVersions(tavernMirrorConfig) { result ->
            if (busyToken != requestToken) {
                return@onFetchOfficialTavernVersions
            }
            if (result.ok && result.versions.hasData) {
                applyOfficialVersions(result.versions)
                update(result.message, "", true, allowRunningInference = false)
            } else {
                update(result.message, "", false, allowRunningInference = false)
            }
            releaseBusy()
        }
    }

    fun enterTavernInstallFlow() {
        tavernInstallDetected = false
        if (selectedTavernVersion == null) {
            selectedTavernVersion = TavernVersionSelection.recommendedInstallChoice(
                officialVersions = officialVersions,
                fallbackRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
            )
        }
        update("已进入酒馆安装流程。", "", true, allowRunningInference = false)
    }

    fun useRecommendedTavernVersion() {
        val recommended = TavernVersionSelection.recommendedInstallChoice(
            officialVersions = officialVersions,
            fallbackRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
        )
        selectedTavernVersion = recommended
        update("已选择 ${recommended.label}。", "", true, allowRunningInference = false)
    }

    fun buildInstallRequest(): PendingTavernInstallRequest? {
        val choice = selectedTavernVersion ?: defaultInstallChoice()
        val target = choice.target.trim().ifBlank { TavernInstallDefaults.RELEASE_TARGET }
        val repoUrl = choice.repoUrl
            .trim()
            .ifBlank { tavernMirrorConfig.normalizedRepoUrl }
        LauncherInputGuards.validateVersionTarget(target)?.let { reason ->
            update("安装酒馆失败：目标版本无效。$reason", "", false, allowRunningInference = false)
            return null
        }
        TavernMirrorValidator.validateRepoUrl(repoUrl)?.let { reason ->
            update("安装酒馆失败：Git 源地址无效。$reason", "", false, allowRunningInference = false)
            return null
        }
        return PendingTavernInstallRequest(
            choice = choice.copy(target = target, repoUrl = repoUrl),
            target = target,
            repoUrl = repoUrl,
        )
    }

    fun queueInstallTask(request: PendingTavernInstallRequest) {
        pendingAptConfigTask = PendingAptConfigTask(
            task = AptConfigTask.InstallTavern,
            installTarget = request.target,
            installRepoUrl = request.repoUrl,
        )
    }

    fun clearInstallRiskDialog() {
        showInstallRiskDialog = false
        pendingInstallRiskRequest = null
        installRiskConfirmation = null
    }

    fun clearStartPreflightDialog() {
        pendingStartPreflight = null
    }

    fun confirmInstallRiskDialog() {
        val request = pendingInstallRiskRequest ?: run {
            clearInstallRiskDialog()
            return
        }
        clearInstallRiskDialog()
        queueInstallTask(request)
    }

    fun finishInstallPreflight(
        request: PendingTavernInstallRequest,
        probeStatus: TavernMirrorProbeStatus,
        versions: TavernOfficialVersions,
    ) {
        val result = TavernInstallPreflight.evaluate(
            request = request,
            officialVersions = versions,
            mirrorProbeStatus = probeStatus,
        )
        when {
            !result.ok -> {
                releaseBusy()
                update(result.blockingMessage ?: "安装前检查失败。", "", false, allowRunningInference = false)
            }

            result.confirmation != null -> {
                releaseBusy()
                pendingInstallRiskRequest = request
                installRiskConfirmation = result.confirmation
                showInstallRiskDialog = true
                update("安装前还有一步确认。", "", true, allowRunningInference = false)
            }

            else -> {
                releaseBusy()
                queueInstallTask(request)
            }
        }
    }

    fun requestInstallPreflight(request: PendingTavernInstallRequest) {
        if (!beginBusy("安装前检查", 60000L)) return
        val requestToken = busyToken
        val requestMirrorConfig = TavernMirrorConfig(
            repoUrl = request.repoUrl,
            npmRegistry = tavernMirrorConfig.normalizedNpmRegistry,
        )
        val requestUsesCurrentMirror =
            TavernMirrorProbeStatus.signatureOf(requestMirrorConfig) ==
                TavernMirrorProbeStatus.signatureOf(tavernMirrorConfig)

        fun isRequestActive(): Boolean {
            return busyToken == requestToken && busyLabel != null
        }

        fun finishWithVersions(
            probeStatus: TavernMirrorProbeStatus,
            versions: TavernOfficialVersions,
        ) {
            if (!isRequestActive()) return
            finishInstallPreflight(request, probeStatus, versions)
        }

        fun fetchVersions(probeStatus: TavernMirrorProbeStatus) {
            if (!isRequestActive()) return
            if (!TavernInstallPreflight.needsOfficialVersionRefresh(request.choice, officialVersions, request.repoUrl)) {
                finishWithVersions(probeStatus, officialVersions)
                return
            }
            update("正在读取目标版本列表。", "", false, allowRunningInference = false)
            onFetchOfficialTavernVersions(requestMirrorConfig) { result ->
                if (!isRequestActive()) return@onFetchOfficialTavernVersions
                if (!result.ok || !result.versions.hasData) {
                    releaseBusy()
                    update(result.message, "", false, allowRunningInference = false)
                    return@onFetchOfficialTavernVersions
                }
                if (requestUsesCurrentMirror) {
                    applyOfficialVersions(result.versions)
                }
                finishWithVersions(probeStatus, result.versions)
            }
        }

        val probeStatus = if (requestUsesCurrentMirror) {
            currentMirrorProbeStatus()
        } else {
            TavernMirrorProbeStatus.unknown(requestMirrorConfig)
        }
        if (
            probeStatus.checkedAtMillis > 0L &&
            !probeStatus.checking &&
            probeStatus.overallLevel != MirrorProbeLevel.Failed
        ) {
            fetchVersions(probeStatus)
            return
        }

        update("正在检测镜像源。", "", false, allowRunningInference = false)
        if (requestUsesCurrentMirror) {
            mirrorProbeStatus = TavernMirrorProbeStatus.checking(requestMirrorConfig)
        }
        onCheckTavernMirror(requestMirrorConfig) { result ->
            if (!isRequestActive()) return@onCheckTavernMirror
            if (requestUsesCurrentMirror) {
                mirrorProbeStatus = result
            }
            fetchVersions(result)
        }
    }

    fun installSelectedTavern() {
        when {
            !termuxInstalled -> {
                update("先安装 Termux。", "", false, allowRunningInference = false)
                return
            }
            termuxExternalAppsBlocked || !runCommandPermissionGranted -> {
                update("先打开 Termux 调用权限。", "", false, allowRunningInference = false)
                return
            }
            actionInProgress -> {
                update("正在处理，完成后再安装酒馆。", "", false, allowRunningInference = false)
                return
            }
        }
        if (blockIfPendingTaskExists("安装酒馆")) return
        val request = buildInstallRequest() ?: return
        requestInstallPreflight(request)
    }

    fun executeInstallSelectedTavern(
        targetFromTask: String,
        repoUrlFromTask: String,
        configPolicy: AptConfigPolicy,
    ) {
        val target = targetFromTask.ifBlank {
            selectedTavernVersion?.target?.trim().orEmpty().ifBlank { TavernInstallDefaults.RELEASE_TARGET }
        }
        val repoUrl = repoUrlFromTask.ifBlank {
            selectedTavernVersion?.repoUrl?.trim().orEmpty().ifBlank { tavernMirrorConfig.normalizedRepoUrl }
        }
        LauncherInputGuards.validateVersionTarget(target)?.let { reason ->
            update("安装酒馆失败：目标版本无效。$reason", "", false, allowRunningInference = false)
            return
        }
        TavernMirrorValidator.validateRepoUrl(repoUrl)?.let { reason ->
            update("安装酒馆失败：Git 源地址无效。$reason", "", false, allowRunningInference = false)
            return
        }
        val commandArgument = TavernInstallCommandCodec.encode(target, repoUrl, configPolicy)
        runPendingGuardedCommand(
            task = PendingLauncherTask(
                kind = PendingLauncherTaskKind.InstallTavern,
                commandName = "tavern-install",
                detail = "正在安装酒馆",
                startedAtMillis = System.currentTimeMillis(),
                targetLabel = target,
                profileId = tavernPathConfig.activeProfile.id,
            ),
            label = "安装酒馆",
            timeoutMs = 900000L,
        ) { guardedUpdate ->
            onCommand("tavern-install::$commandArgument", guardedUpdate)
        }
    }

    fun openTermuxFromGuide() {
        if (!termuxInstalled) {
            update("先安装 Termux。", "", false, allowRunningInference = false)
            return
        }
        if (actionInProgress) {
            update("正在处理，完成后再打开 Termux。", "", false, allowRunningInference = false)
            return
        }
        val woke = onOpenTermuxOnly()
        update(
            if (woke) "已打开 Termux。执行完命令后手动回启动器。" else "打开 Termux 失败。",
            "",
            woke,
            allowRunningInference = false,
        )
    }

    fun dismissApplyBackupPreview() {
        showApplyBackupPreviewDialog = false
        applyBackupPreview = null
    }

    fun openApplyBackupPreview(path: String): Boolean {
        val normalized = path.trim()
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效：$reason", "", false, allowRunningInference = false)
            return false
        }
        applyBackupPath = normalized
        applyBackupPreview = BackupRestorePreviewResolver.resolve(
            context = context,
            archivePath = normalized,
            restoreTargetDir = tavernPathConfig.displayTavernDir,
        )
        showApplyBackupPreviewDialog = true
        return true
    }

    fun requestApplyBackup(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            applyBackupPath = ""
            showApplyBackupPathDialog = true
            return
        }
        openApplyBackupPreview(normalized)
    }

    fun applySelectedBackup() {
        if (blockIfPendingTaskExists("应用备份")) {
            dismissApplyBackupPreview()
            return
        }
        val archivePath = applyBackupPath.trim()
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            update("备份路径无效，不能应用：$reason", "", false, allowRunningInference = false)
            return
        }
        if (!BackupLibraryFiles.canReadLibrarySource(context, archivePath)) {
            update(
                "应用备份失败：启动器读不到这个备份。请先刷新备份库，或重新导入。",
                "",
                false,
                allowRunningInference = false,
            )
            return
        }
        if (termuxStoragePermissionBlocked && isSharedStorageBackupPath(archivePath)) {
            storagePermissionRetryArchivePath = archivePath
            dismissApplyBackupPreview()
            showTermuxStoragePermissionDialog = true
            update("应用备份前需要先给 Termux 存储权限。", "", false, allowRunningInference = false)
            return
        }
        dismissApplyBackupPreview()
        runPendingGuardedCommand(
            task = PendingLauncherTask(
                kind = PendingLauncherTaskKind.RestoreBackup,
                commandName = "tavern-restore",
                detail = "正在应用酒馆备份",
                startedAtMillis = System.currentTimeMillis(),
                archivePath = archivePath,
                profileId = tavernPathConfig.activeProfile.id,
            ),
            label = "应用酒馆备份",
            timeoutMs = 600000L,
        ) { guardedUpdate ->
            onCommand("tavern-restore::$archivePath", guardedUpdate)
        }
    }

    fun requestTermuxStoragePermission() {
        if (actionInProgress) {
            update("正在处理，完成后再授权。", "", false, allowRunningInference = false)
            return
        }
        showTermuxStoragePermissionDialog = false
        runGuarded("请求 Termux 存储权限", 90000L, allowRunningInference = false) { guardedUpdate ->
            onCommand("termux-storage-permission", guardedUpdate)
        }
    }

    fun retryApplyAfterTermuxStoragePermission() {
        val retryPath = storagePermissionRetryArchivePath.ifBlank { applyBackupPath }.trim()
        if (retryPath.isBlank()) {
            showTermuxStoragePermissionDialog = false
            update("没有找到要继续应用的备份。", "", false, allowRunningInference = false)
            return
        }
        termuxStoragePermissionBlocked = false
        showTermuxStoragePermissionDialog = false
        applyBackupPath = retryPath
        applySelectedBackup()
    }

    fun requestDeleteBackup(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            update("没有选中要删除的备份。", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能删除：$reason", "", false, allowRunningInference = false)
            return
        }
        selectedBackupPath = normalized
        showDeleteBackupDialog = true
    }

    fun exportBackupArchive(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            update("没有选中要导出的备份。", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能导出：$reason", "", false, allowRunningInference = false)
            return
        }

        if (actionInProgress) {
            update("正在处理，完成后再导出备份。", "", false, allowRunningInference = false)
            return
        }
        update("请选择导出位置，文件名会自动整理为 .tar.gz。", "", true, allowRunningInference = false)
        onPickBackupExportDestination(normalized, normalized.substringAfterLast('/')) { result ->
            if (!result.ok) {
                update(result.message, "", false, allowRunningInference = false)
                return@onPickBackupExportDestination
            }
            update(result.message, "", true, allowRunningInference = false)
        }
    }

    fun copyBackupLibraryPath(target: BackupLibraryPathTarget) {
        val path = when (target) {
            BackupLibraryPathTarget.Manual -> "/storage/emulated/0/Download/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}"
            BackupLibraryPathTarget.Auto -> "/storage/emulated/0/Download/${BackupLibraryFiles.AUTO_RELATIVE_DIR}"
        }
        val copied = onCopyText("露科亚备份库地址", path)
        update(
            if (copied) "已复制文件地址。" else "复制失败，请手动记下 $path。",
            "",
            copied,
            allowRunningInference = false,
        )
    }

    fun requestCopyBackup(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            update("没有选中要复制的备份。", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能复制：$reason", "", false, allowRunningInference = false)
            return
        }
        selectedBackupPath = normalized
        showCopyBackupDialog = true
    }

    fun requestRenameBackup(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            update("没有选中要重命名的备份。", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能重命名：$reason", "", false, allowRunningInference = false)
            return
        }
        selectedBackupPath = normalized
        renameBackupName = normalized
            .substringAfterLast('/')
            .removeSuffix(".tar.gz")
            .take(48)
        showRenameBackupDialog = true
    }

    fun copyBackupArchive(path: String) {
        val normalized = path.trim()
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能复制：$reason", "", false, allowRunningInference = false)
            return
        }
        showCopyBackupDialog = false
        selectedBackupPath = ""
        runLocalBackupLibraryOperation("复制酒馆备份") {
            val copied = BackupLibraryFiles.copyLibraryArchive(context, normalized)
            val paths = readLocalBackupLibrary()
            paths to "已复制备份：${copied.fileName}。"
        }
    }

    fun renameBackupArchive(path: String, newName: String) {
        val normalized = path.trim()
        val normalizedName = newName.trim()
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能重命名：$reason", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupRequiredName(normalizedName)?.let { reason ->
            update("备份新名称无效：$reason", "", false, allowRunningInference = false)
            return
        }
        val targetFileName = LauncherInputGuards.backupFileNameForLabel(normalizedName)
        val duplicatePath = targetFileName?.let { fileName ->
            backupHistory.firstOrNull { existingPath ->
                existingPath.trim() != normalized &&
                    existingPath.substringAfterLast('/') == fileName
            }
        }
        if (duplicatePath != null) {
            update(
                "已有同名备份：$targetFileName。请换个名字。",
                "",
                false,
                allowRunningInference = false,
            )
            return
        }
        showRenameBackupDialog = false
        selectedBackupPath = ""
        renameBackupName = ""
        runLocalBackupLibraryOperation("重命名酒馆备份") {
            val renamed = BackupLibraryFiles.renameLibraryArchive(context, normalized, normalizedName)
            val paths = readLocalBackupLibrary()
            paths to "已重命名为：${renamed.fileName}。"
        }
    }

    fun importBackupArchive(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            update("请先填写要导入的 .tar.gz 备份路径。", "", false, allowRunningInference = false)
            return
        }
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            update("备份路径无效，不能导入：$reason", "", false, allowRunningInference = false)
            return
        }
        showImportBackupDialog = false
        importBackupPath = ""
        update("请点“导入到备份库”，用文件管理器选择外部备份。", "", false, allowRunningInference = false)
    }

    fun addImportedBackupToLibrary(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) return
        replaceBackupHistory(listOf(normalized) + backupHistory)
    }

    fun pickAndImportExternalBackup() {
        if (actionInProgress) {
            update("正在处理，完成后再导入备份。", "", false, allowRunningInference = false)
            return
        }
        onPickExternalBackup { result ->
            val importedPath = result.termuxReadablePath.trim()
            if (result.ok && importedPath.isNotBlank()) {
                addImportedBackupToLibrary(importedPath)
                runLocalBackupLibraryOperation("刷新酒馆备份列表") {
                    val paths = readLocalBackupLibrary()
                    val importedFileName = importedPath.replace('\\', '/').substringAfterLast('/')
                    val mergedPaths = if (paths.any { it.replace('\\', '/').substringAfterLast('/') == importedFileName }) {
                        paths
                    } else {
                        BackupHistoryReducer.sanitize(listOf(importedPath) + paths)
                    }
                    mergedPaths to "${result.message}，备份库已刷新。"
                }
            } else {
                update(result.message, "", result.ok, allowRunningInference = false)
            }
        }
    }

    fun checkGithubUpdate(repositoryOverride: String? = null, manual: Boolean = true) {
        val repository = repositoryOverride ?: githubRepository
        if (githubUpdateState.checking || githubUpdateState.downloading) {
            if (manual) update("GitHub 更新处理中，请稍等。", "", false, allowRunningInference = false)
            return
        }
        if (repository.isBlank()) {
            selectedTab = LauncherTab.Settings
            update("请先填写 GitHub 仓库。", "", false, allowRunningInference = false)
            githubUpdateState = githubUpdateState.copy(
                repository = repository,
                channel = githubUpdateChannel,
                message = "未配置 GitHub 仓库。",
                checking = false,
                downloading = false,
            )
            return
        }

        githubUpdateState = githubUpdateState.copy(
            repository = repository,
            channel = githubUpdateChannel,
            checking = true,
            downloading = false,
            message = "正在检查${githubUpdateChannel.label}更新。",
        )
        if (manual) update("正在检查${githubUpdateChannel.label}更新。", "", false, allowRunningInference = false)

        onCheckGithubUpdate(repository, githubUpdateChannel) { result ->
            val nextLatest = result.info ?: githubUpdateState.latest
            val nextCurrentRelease = result.currentInfo ?: githubUpdateState.currentRelease
            val shouldPrompt = result.info?.isNewer == true &&
                (manual || result.info.tagName != ignoredUpdateTag)
            githubUpdateState = githubUpdateState.copy(
                repository = repository,
                channel = githubUpdateChannel,
                checking = false,
                downloading = false,
                latest = nextLatest,
                currentRelease = nextCurrentRelease,
                message = result.message,
                lastCheckedText = "刚刚检查",
            )
            if (shouldPrompt) {
                showUpdateDialog = true
            }
            if (manual || shouldPrompt || !result.ok) {
                update(result.message, "", result.ok, allowRunningInference = false)
            }
        }
    }

    fun installGithubUpdate() {
        val latest = githubUpdateState.latest
        when {
            githubUpdateState.checking || githubUpdateState.downloading -> {
                update("GitHub 更新处理中，请稍等。", "", false, allowRunningInference = false)
            }
            latest == null -> {
                checkGithubUpdate(manual = true)
            }
            !latest.isNewer -> {
                update("当前已经是最新版本。", "", true, allowRunningInference = false)
            }
            latest.apkDownloadUrl.isBlank() -> {
                val result = onOpenGithubRelease(latest)
                update(
                    "发现新版，但没有 APK。\n${result.message}",
                    "",
                    result.ok,
                    allowRunningInference = false,
                )
            }
            else -> {
                githubUpdateState = githubUpdateState.copy(
                    downloading = true,
                    checking = false,
                    message = "正在下载新版 v${latest.versionName}。",
                )
                update("正在下载新版 v${latest.versionName}。", "", false, allowRunningInference = false)
                onInstallGithubUpdate(latest) { result ->
                    githubUpdateState = githubUpdateState.copy(
                        downloading = false,
                        checking = false,
                        message = result.message,
                    )
                    update(result.message, "", result.ok, allowRunningInference = false)
                }
            }
        }
    }

    fun saveGithubRepository() {
        if (githubUpdateState.checking || githubUpdateState.downloading) {
            update("GitHub 更新处理中，结束后再改仓库。", "", false, allowRunningInference = false)
            return
        }

        val result = onSaveGithubRepository(githubRepositoryInput)
        githubRepository = result.repository
        githubRepositoryInput = result.repository
        ignoredUpdateTag = ""
        githubUpdateState = GithubUpdateUiState(
            repository = result.repository,
            channel = githubUpdateChannel,
            message = result.message,
        )
        update(result.message, "", result.saved, allowRunningInference = false)
        if (result.saved && result.repository.isNotBlank()) {
            checkGithubUpdate(repositoryOverride = result.repository, manual = true)
        }
    }

    fun saveGithubUpdateChannel(channel: GithubReleaseChannel) {
        if (githubUpdateState.checking || githubUpdateState.downloading) {
            update("GitHub 更新处理中，结束后再切换通道。", "", false, allowRunningInference = false)
            return
        }
        val result = onSaveGithubUpdateChannel(channel)
        githubUpdateChannel = result.channel
        ignoredUpdateTag = ""
        githubUpdateState = GithubUpdateUiState(
            repository = githubRepository,
            channel = result.channel,
            message = result.message,
        )
        update(result.message, "", result.saved, allowRunningInference = false)
        if (result.saved && githubRepository.isNotBlank()) {
            checkGithubUpdate(repositoryOverride = githubRepository, manual = true)
        }
    }

    fun saveTavernMirrorConfig(repoUrl: String = tavernRepoInput, npmRegistry: String = npmRegistryInput) {
        val previousRepoUrl = tavernMirrorConfig.normalizedRepoUrl
        val nextConfig = TavernMirrorConfig(
            repoUrl = repoUrl.trim(),
            npmRegistry = npmRegistry.trim(),
        )
        val result = onSaveTavernMirrorConfig(nextConfig)
        val repoChanged = result.saved && !sameRepoUrl(previousRepoUrl, result.config.normalizedRepoUrl)
        tavernMirrorConfig = result.config
        tavernRepoInput = result.config.normalizedRepoUrl
        npmRegistryInput = result.config.normalizedNpmRegistry
        mirrorProbeStatus = TavernMirrorProbeStatus.unknown(result.config)
        if (repoChanged) {
            officialVersions = TavernOfficialVersions()
            selectedTavernVersion = selectedTavernVersion
                ?.takeIf { it.kind == TavernVersionKind.Custom }
                ?.copy(repoUrl = result.config.normalizedRepoUrl)
            pendingTavernVersionActionConfirmation = null
        }
        update(
            if (result.saved) {
                buildString {
                    append(result.message)
                    append("\n后续安装、读取官方版本、更新和回退会使用这个源。")
                    if (repoChanged) {
                        append("\n旧版本列表已清空，请按新源重新读取官方版本。")
                    }
                }
            } else {
                result.message
            },
            "",
            result.saved,
            allowRunningInference = false,
        )
    }

    fun saveTavernPathConfig(
        path: String = tavernPathInput,
        portText: String = tavernPortInput,
    ) {
        val safePort = portText.trim().toIntOrNull() ?: tavernPathConfig.normalizedPort
        val nextConfig = tavernPathConfig.withUpdatedActiveProfile(
            tavernDir = path.trim(),
            port = safePort,
        )
        val result = onSaveTavernPathConfig(nextConfig)
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "${result.config.activeProfileLabel}已保存，后续启动、停止、版本读取和备份都会使用这个目录和端口。",
            )
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun chooseDetectedTavernDirectory(path: String) {
        tavernDirectoryCandidates
            .firstOrNull { it.path == path }
            ?.takeIf { !it.selectable }
            ?.let { blocked ->
                update(blocked.reason.ifBlank { "这个目录当前不能直接分配给这个实例。" }, "", false, allowRunningInference = false)
                return
            }
        dismissTavernDirectoryChoiceDialog()
        val result = onSaveTavernPathConfig(
            tavernPathConfig.withUpdatedActiveProfilePathOnly(path),
        )
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "${result.config.activeProfileLabel}已切换到检测到的目录，端口保持 ${result.config.normalizedPort} 不变。",
            )
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun restoreDefaultTavernPath() {
        val result = onRestoreDefaultTavernPath()
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已恢复${result.config.activeProfileLabel}的默认路径和默认端口。")
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun selectTavernProfile(profileId: String) {
        val result = onSaveTavernPathConfig(tavernPathConfig.withActiveProfile(profileId))
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已切换到${result.config.activeProfileLabel}。")
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun addTavernProfile() {
        val result = onSaveTavernPathConfig(tavernPathConfig.addSuggestedProfile())
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState("已新建并切换到${result.config.activeProfileLabel}。")
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun requestRemoveCurrentTavernProfile() {
        when (
            val decision = TavernProfileRemovalGuard.evaluate(
                config = tavernPathConfig,
                tavernRunning = tavernRunning,
                tavernStarting = tavernStarting,
                actionsLocked = actionInProgress,
            )
        ) {
            is TavernProfileRemovalDecision.Blocked -> {
                update(decision.message, "", false, allowRunningInference = false)
            }

            is TavernProfileRemovalDecision.Confirm -> {
                pendingTavernProfileRemovalConfirmation = decision.confirmation
            }
        }
    }

    fun requestTavernPathMigration(
        targetPath: String,
        targetKind: TavernProfileMigrationTargetKind,
    ): Boolean {
        return when (
            val decision = TavernProfileMigrationGuard.evaluate(
                config = tavernPathConfig,
                targetPath = targetPath,
                targetKind = targetKind,
                tavernRunning = tavernRunning,
                tavernStarting = tavernStarting,
                actionsLocked = actionInProgress,
            )
        ) {
            is TavernProfileMigrationDecision.Blocked -> {
                update(decision.message, "", false, allowRunningInference = false)
                false
            }

            is TavernProfileMigrationDecision.Confirm -> {
                pendingTavernProfileMigrationConfirmation = decision.confirmation
                true
            }
        }
    }

    fun requestMigrateToManagedTavernPath() {
        val pathInfo = TavernProfilePathPolicy.describe(tavernPathConfig.activeProfile)
        requestTavernPathMigration(
            targetPath = pathInfo.launcherManagedDefaultPath,
            targetKind = TavernProfileMigrationTargetKind.LauncherManaged,
        )
    }

    fun requestMigrateToTraditionalTavernPath() {
        val pathInfo = TavernProfilePathPolicy.describe(tavernPathConfig.activeProfile)
        requestTavernPathMigration(
            targetPath = pathInfo.traditionalDefaultPath,
            targetKind = TavernProfileMigrationTargetKind.TraditionalDefault,
        )
    }

    fun openCustomTavernPathMigrationDialog() {
        customMigrationPathInput = ""
        showCustomTavernPathMigrationDialog = true
    }

    fun confirmCustomTavernPathMigrationDialog() {
        if (requestTavernPathMigration(customMigrationPathInput, TavernProfileMigrationTargetKind.Custom)) {
            showCustomTavernPathMigrationDialog = false
        }
    }

    fun confirmMigrateCurrentTavernPath() {
        val confirmation = pendingTavernProfileMigrationConfirmation ?: return
        if (blockIfPendingTaskExists("迁移酒馆目录")) {
            return
        }
        pendingTavernProfileMigrationConfirmation = null
        val encodedTargetPath = TavernProfileMigrationCommandCodec.encode(confirmation.targetPath)
        runProfileMutationPendingCommand(
            task = PendingLauncherTask(
                kind = PendingLauncherTaskKind.MigrateTavernDirectory,
                commandName = "tavern-migrate-dir",
                detail = "把${confirmation.profileName}迁移到${confirmation.targetPath}",
                startedAtMillis = System.currentTimeMillis(),
                profileId = confirmation.profileId,
                targetPath = confirmation.targetPath,
            ),
            label = "迁移酒馆目录",
            timeoutMs = 900000L,
            command = "tavern-migrate-dir::$encodedTargetPath",
        )
    }

    fun confirmRemoveCurrentTavernProfile() {
        val confirmation = pendingTavernProfileRemovalConfirmation ?: return
        pendingTavernProfileRemovalConfirmation = null
        if (confirmation.deletesProfileDirectory) {
            if (blockIfPendingTaskExists("删除实例")) {
                pendingTavernProfileRemovalConfirmation = confirmation
                return
            }
            runProfileMutationPendingCommand(
                task = PendingLauncherTask(
                    kind = PendingLauncherTaskKind.RemoveManagedProfileDirectory,
                    commandName = "tavern-delete-managed-profile-dir",
                    detail = "删除${confirmation.profileName}的托管目录",
                    startedAtMillis = System.currentTimeMillis(),
                    profileId = confirmation.profileId,
                    targetPath = confirmation.deletedDirectoryPath,
                ),
                label = "删除分身实例托管目录",
                timeoutMs = 900000L,
                command = "tavern-delete-managed-profile-dir",
            )
            return
        }
        val result = onSaveTavernPathConfig(tavernPathConfig.removeProfile(confirmation.profileId))
        applyTavernPathSaveResult(result)
        if (result.saved) {
            refreshActiveProfileState(
                "已移除${confirmation.profileName}，现在切换到${result.config.activeProfileLabel}继续管理。原目录和备份都还保留着。",
            )
        } else {
            update(result.message, "", false, allowRunningInference = false)
        }
    }

    fun useOfficialTavernMirror() {
        saveTavernMirrorConfig(
            repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
            npmRegistry = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
        )
    }

    fun useGithubProxyTavernMirror() {
        saveTavernMirrorConfig(
            repoUrl = TavernMirrorDefaults.GITHUB_PROXY_REPO,
            npmRegistry = TavernMirrorDefaults.NPMMIRROR_REGISTRY,
        )
    }

    fun useNpmMirrorOnly() {
        saveTavernMirrorConfig(
            repoUrl = tavernRepoInput,
            npmRegistry = TavernMirrorDefaults.NPMMIRROR_REGISTRY,
        )
    }

    fun readTermuxPackageMirrorStatus() {
        if (actionInProgress) {
            update("正在处理，完成后再读取 Termux 包源。", "", false, allowRunningInference = false)
            return
        }
        if (!termuxInstalled) {
            update("先安装 Termux。", "", false, allowRunningInference = false)
            return
        }
        if (!runCommandPermissionGranted) {
            update("先打开 Termux 调用权限。", "", false, allowRunningInference = false)
            return
        }
        if (!beginBusy("读取 Termux 包源", 20000L)) return
        update("正在读取当前 Termux 包源。", "", false, allowRunningInference = false)
        onCommand("termux-repo-status") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            if (!isTransientStatus(newStatus)) {
                releaseBusy()
            }
        }
    }

    fun applyCustomTermuxPackageMirror() {
        val url = customTermuxRepoInput.trim().trimEnd('/')
        TavernMirrorValidator.validateTermuxAptUrl(url)?.let { reason ->
            update("自定义 Termux 包源无效：$reason", "", false, allowRunningInference = false)
            return
        }
        if (actionInProgress) {
            update("正在处理，完成后再切换 Termux 包源。", "", false, allowRunningInference = false)
            return
        }
        if (!termuxInstalled) {
            update("先安装 Termux。", "", false, allowRunningInference = false)
            return
        }
        if (!runCommandPermissionGranted) {
            update("先打开 Termux 调用权限。", "", false, allowRunningInference = false)
            return
        }
        if (!beginBusy("切换自定义 Termux 包源", 90000L)) return
        customTermuxRepoInput = url
        update("正在切换自定义 Termux 包源。", "", false, allowRunningInference = false)
        onCommand("termux-repo-custom::$url") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            if (!isTransientStatus(newStatus)) {
                releaseBusy()
            }
        }
    }

    fun restoreDefaultGithubRepository() {
        if (githubUpdateState.checking || githubUpdateState.downloading) {
            update("GitHub 更新处理中，结束后再恢复。", "", false, allowRunningInference = false)
            return
        }

        val result = onSaveGithubRepository(GithubUpdateDefaults.REPOSITORY)
        githubRepository = result.repository
        githubRepositoryInput = result.repository
        ignoredUpdateTag = ""
        githubUpdateState = GithubUpdateUiState(
            repository = result.repository,
            channel = githubUpdateChannel,
            message = if (result.saved) {
                if (result.repository.isBlank()) {
                    "已恢复默认仓库。当前未填写启动器更新仓库。"
                } else {
                    "已恢复默认仓库。"
                }
            } else {
                result.message
            },
        )
        update(
            if (result.saved) {
                if (result.repository.isBlank()) {
                    "已恢复默认仓库。当前未填写启动器更新仓库。"
                } else {
                    "已恢复默认仓库。"
                }
            } else {
                result.message
            },
            "",
            result.saved,
            allowRunningInference = false,
        )
        if (result.saved && result.repository.isNotBlank()) {
            checkGithubUpdate(repositoryOverride = result.repository, manual = true)
        }
    }

    fun clearCurrentGithubUpdateBadge() {
        val latest = githubUpdateState.latest ?: return
        ignoredUpdateTag = latest.tagName
        onIgnoreGithubUpdate(latest.tagName)
        showUpdateDialog = false
        update(
            "已清除 v${latest.versionName} 的更新红点，这个版本不会再自动弹出提醒。",
            "",
            true,
            allowRunningInference = false,
        )
    }

    fun termuxKnownMissing(): Boolean {
        if (!termuxInstalled) return true
        val combined = "$status\n$summary"
        return combined.contains("未检测到 Termux") ||
            combined.contains("请先安装并打开 Termux")
    }

    fun openTermuxDownload(url: String, label: String) {
        val opened = onOpenExternalUrl(url)
        update(
            if (opened) "已打开 $label。装好后回来重新检测。" else "打开失败，请检查浏览器。",
            "",
            opened,
            allowRunningInference = false,
        )
    }

    fun recheckTermuxInstalled() {
        val installed = onCheckTermuxInstalled()
        termuxInstalled = installed
        runCommandPermissionGranted = installed && onCheckRunCommandPermission()
        termuxExternalAppsBlocked = false
        update(
            if (installed) "已检测到 Termux。先打开 Termux 一次。" else "还是没检测到 Termux，请先安装。",
            "",
            installed,
            allowRunningInference = false,
        )
        if (installed) {
            tavernInstallDetected = null
            tavernVersionCheckInFlight = false
            if (runCommandPermissionGranted) {
                runStartupRefresh()
            }
        }
    }

    fun termuxPermissionBlocked(): Boolean {
        if (termuxExternalAppsBlocked) return true
        if (termuxInstalled && !runCommandPermissionGranted) return true
        val combined = "$status\n$summary"
        return combined.contains("缺少 RUN_COMMAND 权限") ||
            combined.contains("Termux 拒绝调用") ||
            TermuxPermissionSignals.externalAppsBlocked(combined)
    }

    fun requestRunCommandPermission() {
        onRequestRunCommandPermission()
        val granted = onCheckRunCommandPermission()
        runCommandPermissionGranted = granted
        if (granted) {
            termuxExternalAppsBlocked = false
        }
        update(
            if (granted) {
                "RUN_COMMAND 权限已授予。"
            } else {
                "已请求权限。没弹窗就去权限设置打开。"
            },
            "",
            granted,
            allowRunningInference = false,
        )
    }

    fun recheckRunCommandPermission() {
        val granted = onCheckRunCommandPermission()
        runCommandPermissionGranted = granted
        if (granted) {
            termuxExternalAppsBlocked = false
        }
        update(
            if (granted) "RUN_COMMAND 权限已确认。" else "还没有 RUN_COMMAND 权限。",
            "",
            granted,
            allowRunningInference = false,
        )
        if (granted) {
            runStartupRefresh()
        }
    }

    fun openLauncherPermissionSettings() {
        val opened = onOpenLauncherPermissionSettings()
        update(
            if (opened) "已打开权限设置。允许后回来重新检测。" else "打开权限设置失败。",
            "",
            opened,
            allowRunningInference = false,
        )
    }

    fun openAllFilesAccessSettings() {
        val opened = onOpenAllFilesAccessSettings()
        update(
            if (opened) "已打开文件权限设置。允许后回启动器即可。" else "打开文件权限设置失败。",
            "",
            opened,
            allowRunningInference = false,
        )
    }

    fun openUnknownAppSourcesSettings() {
        val opened = onOpenUnknownAppSourcesSettings()
        update(
            if (opened) "已打开安装未知来源权限页。允许后回启动器即可。" else "打开安装未知来源权限页失败。",
            "",
            opened,
            allowRunningInference = false,
        )
    }

    fun copyTermuxPermissionCommand() {
        val copied = onCopyText("Termux 外部调用配置", TERMUX_EXTERNAL_APPS_COMMAND)
        update(
            if (copied) "已复制命令。打开 Termux 粘贴执行。" else "复制失败，请手动输入命令。",
            "",
            copied,
            allowRunningInference = false,
        )
    }

    fun hasTermuxDependencyError(): Boolean {
        val recent = "$status\n$summary\n${termuxLog.takeLast(5000)}"
        return recent.contains("git command not found", ignoreCase = true) ||
            recent.contains("npm command not found", ignoreCase = true) ||
            recent.contains("node command not found", ignoreCase = true) ||
            recent.contains("pkg command not found", ignoreCase = true) ||
            recent.contains("apt update failed", ignoreCase = true) ||
            recent.contains("No mirror or mirror group selected", ignoreCase = true) ||
            recent.contains("SSL_set_quic_tls_transport_params", ignoreCase = true) ||
            recent.contains("cannot link executable", ignoreCase = true) ||
            recent.contains("dependency.git=missing", ignoreCase = true) ||
            recent.contains("dependency.node=missing", ignoreCase = true) ||
            recent.contains("dependency.npm=missing", ignoreCase = true) ||
            recent.contains("Termux packages are still missing", ignoreCase = true) ||
            recent.contains("exitCode=69", ignoreCase = true) ||
            recent.contains("\"exitCode\": 69", ignoreCase = true)
    }

    fun termuxSetupRecommended(): Boolean {
        return !termuxBootstrapCompleted && termuxInstalled && !termuxPermissionBlocked() && hasTermuxDependencyError()
    }

    fun cachedStartPreflight(nowMillis: Long = System.currentTimeMillis()): TavernStartPreflightResult? {
        val report = healthCheckReport ?: return null
        val doctorReport = report.doctorReport ?: return null
        if (report.checkedAtMillis <= 0L) return null
        if (nowMillis - report.checkedAtMillis > START_PREFLIGHT_CACHE_WINDOW_MS) return null
        if (lastLaunchReadinessSnapshotAtMillis > report.checkedAtMillis) return null
        return TavernStartPreflight.evaluate(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
            doctorReport = doctorReport,
            activeProfile = tavernPathConfig.activeProfile,
        )
    }

    fun canFastPathStart(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (tavernRunning || tavernStarting) return false
        if (termuxKnownMissing() || termuxPermissionBlocked()) return false
        if (termuxSetupRecommended()) return false
        if (tavernInstallDetected != true) return false
        if (!tavernVersionInfo.hasData || tavernVersionInfo.notInstalled) return false
        if (lastLaunchReadinessSnapshotAtMillis <= 0L) return false
        return nowMillis - lastLaunchReadinessSnapshotAtMillis <= START_FAST_PATH_WINDOW_MS
    }

    fun prepareTermuxEnvironment() {
        when {
            !termuxInstalled -> {
                update("先安装 Termux。", "", false, allowRunningInference = false)
                return
            }
            termuxPermissionBlocked() -> {
                update("先打开 Termux 调用权限。", "", false, allowRunningInference = false)
                return
            }
            actionInProgress -> {
                update("正在处理，完成后再准备环境。", "", false, allowRunningInference = false)
                return
            }
        }
        pendingAptConfigTask = PendingAptConfigTask(task = AptConfigTask.Bootstrap)
    }

    fun executePrepareTermuxEnvironment(configPolicy: AptConfigPolicy) {
        if (!beginBusy("准备 Termux 环境", 1_200_000L)) return
        update(
            "正在准备 Termux 环境。",
            """
            等待 Termux 回传...
            正在安装或升级 git、node、npm 等基础依赖。
            首次准备可能需要几分钟，完成后这里会显示完整日志。
            如果这里暂时没有新内容，不代表卡死；看上方“正在处理”的计时。
            """.trimIndent(),
            false,
            allowRunningInference = false,
        )
        onCommand("termux-bootstrap::${configPolicy.wireValue}") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            if (!isTransientStatus(newStatus)) {
                if (ok) {
                    termuxBootstrapCompleted = true
                }
                releaseBusy()
            }
        }
    }

    fun runHealthCheck() {
        if (healthCheckInFlight) {
            update("正在体检，等这次完成再点。", "", false, allowRunningInference = false)
            return
        }
        if (!beginBusy("一键体检", 45_000L)) return
        healthCheckInFlight = true
        healthCheckToken += 1
        val token = healthCheckToken

        scope.launch {
            delay(45_000L)
            if (healthCheckInFlight && healthCheckToken == token) {
                healthCheckToken += 1
                healthCheckInFlight = false
                releaseBusy()
                update("体检超时，请重试一次。", "", false, allowRunningInference = false)
            }
        }

        fun finishHealthCheck(doctorReport: TavernDoctorReport?, mirrorStatus: TavernMirrorProbeStatus) {
            if (healthCheckToken != token) return
            val report = buildHealthCheckReport(
                doctorReport = doctorReport,
                mirrorStatus = mirrorStatus,
            )
            healthCheckReport = report
            healthCheckInFlight = false
            releaseBusy()
            val ok = report.errorCount == 0
            val message = if (ok && report.warningCount == 0) {
                "体检已完成：当前环境基本正常。"
            } else {
                "体检已完成：${report.summaryTitle}"
            }
            update(message, "", ok, allowRunningInference = false)
        }

        if (!termuxInstalled || !runCommandPermissionGranted || termuxExternalAppsBlocked) {
            finishHealthCheck(
                doctorReport = null,
                mirrorStatus = currentMirrorProbeStatus(),
            )
            return
        }

        onCommand("tavern-doctor") { newStatus, termuxOutput, ok ->
            if (healthCheckToken != token) return@onCommand
            if (isTransientStatus(newStatus) && termuxOutput.isBlank()) {
                update(newStatus, termuxOutput, ok, allowRunningInference = false)
                return@onCommand
            }
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            val doctorReport = TavernDoctorParser.parse(termuxOutput)
            mirrorProbeStatus = TavernMirrorProbeStatus.checking(tavernMirrorConfig)
            onCheckTavernMirror(tavernMirrorConfig) { mirrorResult ->
                if (healthCheckToken != token) return@onCheckTavernMirror
                mirrorProbeStatus = mirrorResult
                finishHealthCheck(doctorReport, mirrorResult)
            }
        }
    }

    fun runLauncherQuickFixAction(action: LauncherQuickFixAction) {
        when (action.type) {
            LauncherQuickFixActionType.RunHealthCheck -> {
                selectedTab = LauncherTab.Settings
                runHealthCheck()
            }

            LauncherQuickFixActionType.RequestRunPermission -> {
                requestRunCommandPermission()
            }

            LauncherQuickFixActionType.CopyExternalAppsCommand -> {
                copyTermuxPermissionCommand()
            }

            LauncherQuickFixActionType.PrepareTermuxEnvironment -> {
                prepareTermuxEnvironment()
            }

            LauncherQuickFixActionType.OpenPathSettings -> {
                selectedTab = LauncherTab.Settings
                update("已切到设置里的路径分区。先确认酒馆目录。", "", true, allowRunningInference = false)
            }

            LauncherQuickFixActionType.OpenNetworkSettings -> {
                selectedTab = LauncherTab.Settings
                update("已切到设置里的网络分区。先检查 Git 源、npm 源和 Termux 包源。", "", true, allowRunningInference = false)
            }

            LauncherQuickFixActionType.RecheckTavernVersion -> {
                selectedTab = LauncherTab.Version
                checkTavernInstall()
            }

            LauncherQuickFixActionType.RequestTermuxStoragePermission -> {
                requestTermuxStoragePermission()
            }
        }
    }

    fun continuePendingLauncherTask() {
        val task = pendingLauncherTask ?: return
        showPendingTaskDialog = false
        selectedTab = PendingLauncherTaskSupport.defaultTab(task)
        val latest = PendingLauncherTaskSupport.latestResult(task, onLatestTermuxResult())
        if (latest != null) {
            if (latest.key != lastSyncedTermuxResultKey) {
                syncTermuxResult(latest)
                lastSyncedTermuxResultKey = latest.key
            }
            val resolved = applyProfileMutationTaskSideEffect(task, latest.ok, latest.output)
                ?: PendingLauncherTaskSupport.resolveLatestResult(task, latest)
            runPendingTaskFollowUpRefresh(resolved.refreshTargets)
            clearPendingLauncherTask()
            update(
                resolved.message,
                "",
                resolved.ok,
                allowRunningInference = false,
            )
            return
        }

        if (task.kind == PendingLauncherTaskKind.ManualBackup) {
            scope.launch {
                val recovered = recoverPendingManualBackup(task)
                if (recovered != null) {
                    clearPendingLauncherTask()
                    refreshBackupList()
                    update(
                        "已继续检查上次创建备份：备份已经生成，但 Termux 没把最终返回带回来。\n备份在：${recovered.archivePath}",
                        "",
                        true,
                        allowRunningInference = false,
                    )
                    return@launch
                }

                val waitingRefreshTargets = PendingLauncherTaskSupport.waitingRefreshTargets(task)
                runPendingTaskFollowUpRefresh(waitingRefreshTargets)
                update(
                    PendingLauncherTaskSupport.waitingMessage(task),
                    "",
                    false,
                    allowRunningInference = false,
                )
            }
            return
        }

        val waitingRefreshTargets = PendingLauncherTaskSupport.waitingRefreshTargets(task)
        runPendingTaskFollowUpRefresh(waitingRefreshTargets)
        update(
            PendingLauncherTaskSupport.waitingMessage(task),
            "",
            false,
            allowRunningInference = false,
        )
    }

    fun abandonPendingLauncherTask() {
        showPendingTaskDialog = false
        clearPendingLauncherTask()
        OperationLockStore.release(context)
        update(
            "已放弃记录这次未完成任务。不会删除已经生成的备份和现有文件。",
            "",
            true,
            allowRunningInference = false,
        )
    }

    fun runHealthCheckPrimaryAction() {
        val action = healthCheckReport?.primaryAction
        when (action?.type) {
            LauncherHealthActionType.DownloadTermux -> {
                openTermuxDownload(TERMUX_FDROID_URL, "F-Droid 的 Termux 页面")
            }

            LauncherHealthActionType.RequestRunPermission -> {
                requestRunCommandPermission()
            }

            LauncherHealthActionType.CopyExternalAppsCommand -> {
                copyTermuxPermissionCommand()
            }

            LauncherHealthActionType.PrepareTermuxEnvironment -> {
                prepareTermuxEnvironment()
            }

            LauncherHealthActionType.ChooseDetectedDirectory -> {
                val candidates = healthCheckReport?.doctorReport?.candidateDirectories.orEmpty()
                if (candidates.isEmpty()) {
                    selectedTab = LauncherTab.Settings
                    update("请到设置里的路径分区确认酒馆目录。", "", false, allowRunningInference = false)
                } else {
                    openTavernDirectoryChoice(candidates)
                }
            }

            LauncherHealthActionType.OpenPathSettings -> {
                selectedTab = LauncherTab.Settings
                update("请到设置里的路径分区确认酒馆目录。", "", false, allowRunningInference = false)
            }

            LauncherHealthActionType.OpenNetworkSettings -> {
                selectedTab = LauncherTab.Settings
                update("请到设置里的网络分区检查 Git 源、npm 源和 Termux 包源。", "", false, allowRunningInference = false)
            }

            LauncherHealthActionType.RequestBackgroundRunPermission -> {
                val opened = onRequestBackgroundRunPermission()
                update(
                    if (opened) {
                        "已打开后台运行权限页面。允许后回启动器即可。"
                    } else {
                        "打开后台运行权限页面失败，请到系统设置里允许后台运行。"
                    },
                    "",
                    opened,
                    allowRunningInference = false,
                )
            }

            LauncherHealthActionType.RequestTermuxBackgroundRunPermission -> {
                val opened = onRequestTermuxBackgroundRunPermission()
                update(
                    if (opened) {
                        "已打开 Termux 后台常驻权限页面。允许后回启动器即可。"
                    } else {
                        "打开 Termux 后台常驻权限页面失败，请到系统设置里放行 Termux。"
                    },
                    "",
                    opened,
                    allowRunningInference = false,
                )
            }

            LauncherHealthActionType.OpenAllFilesAccessSettings -> {
                openAllFilesAccessSettings()
            }

            LauncherHealthActionType.OpenUnknownAppSourcesSettings -> {
                openUnknownAppSourcesSettings()
            }

            LauncherHealthActionType.StopTavern -> {
                when {
                    actionInProgress -> update("正在处理，完成后再停止酒馆。", "", false, allowRunningInference = false)
                    !tavernRunning -> update("酒馆当前未运行。", "", false, allowRunningInference = false)
                    else -> showStopConfirmDialog = true
                }
            }

            null -> {
                update("当前没有需要优先处理的快捷修复项。", "", true, allowRunningInference = false)
            }
        }
    }

    fun performForegroundStart() {
        if (tavernStarting) {
            update("酒馆正在启动中，请稍等。", "", false)
            return
        }
        if (!beginBusy("启动酒馆", 15000L)) return

        showStopConfirmDialog = false
        onForegroundStart { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok)
            if (isTransientStatus(newStatus)) {
                return@onForegroundStart
            }
            releaseBusy()
            if (ok) {
                tavernStarting = true
                tavernRunning = false
                launchAttemptToken += 1
                val token = launchAttemptToken
                scope.launch {
                    delay(60_000L)
                    if (launchAttemptToken == token && tavernStarting && !tavernRunning) {
                        tavernStarting = false
                        update(
                            "启动太久了，请看下方 Termux 返回。",
                            "",
                            false,
                            allowRunningInference = false,
                        )
                    }
                }
            } else {
                tavernStarting = false
            }
        }
    }

    fun continueStartAfterFirstGuideIfNeeded() {
        if (maybeShowFirstTavernStartGuideBeforeStart()) return
        performForegroundStart()
    }

    fun requestStartTavern() {
        if (tavernStarting) {
            update("酒馆正在启动中，请稍等。", "", false, allowRunningInference = false)
            return
        }
        if (termuxKnownMissing()) {
            update("请先安装并打开 Termux，再启动酒馆。", "", false, allowRunningInference = false)
            return
        }
        if (termuxPermissionBlocked()) {
            update("请先把 Termux 调用权限修好，再启动酒馆。", "", false, allowRunningInference = false)
            return
        }
        cachedStartPreflight()?.let { preflight ->
            if (!preflight.ok) {
                pendingStartPreflight = preflight
                update(preflight.summary, "", false, allowRunningInference = false)
                return
            }
            pendingStartPreflight = null
            continueStartAfterFirstGuideIfNeeded()
            return
        }
        if (canFastPathStart()) {
            pendingStartPreflight = null
            continueStartAfterFirstGuideIfNeeded()
            return
        }
        if (!beginBusy("启动前预检", 20000L)) return

        onCommand("tavern-doctor") { newStatus, termuxOutput, ok ->
            if (isTransientStatus(newStatus) && termuxOutput.isBlank()) {
                update(newStatus, termuxOutput, ok, allowRunningInference = false)
                return@onCommand
            }
            update(newStatus, termuxOutput, ok, allowRunningInference = false)
            val doctorReport = TavernDoctorParser.parse(termuxOutput)
            healthCheckReport = buildHealthCheckReport(doctorReport)
            val preflight = TavernStartPreflight.evaluate(
                termuxInstalled = termuxInstalled,
                runCommandPermissionGranted = runCommandPermissionGranted,
                termuxExternalAppsBlocked = termuxExternalAppsBlocked,
                doctorReport = doctorReport,
                activeProfile = tavernPathConfig.activeProfile,
            )
            releaseBusy()
            if (!preflight.ok) {
                pendingStartPreflight = preflight
                update(preflight.summary, "", false, allowRunningInference = false)
                return@onCommand
            }
            pendingStartPreflight = null
            continueStartAfterFirstGuideIfNeeded()
        }
    }

    fun returnToTavern() {
        when {
            actionInProgress -> update("正在处理，稍后再打开酒馆。", "", false, allowRunningInference = false)
            tavernStarting -> update("酒馆还在启动，等状态变成运行中再打开。", "", false, allowRunningInference = false)
            !tavernRunning -> update("酒馆未运行，请先启动酒馆。", "", false, allowRunningInference = false)
            else -> onOpenTavern { newStatus, termuxOutput, ok ->
                update(newStatus, termuxOutput, ok, allowRunningInference = false)
            }
        }
    }

    fun currentForceCleanupSuggestion(
        doctorReport: TavernDoctorReport? = healthCheckReport?.doctorReport,
        statusText: String = status,
        summaryText: String = summary,
    ): TavernForceCleanupSuggestion? {
        return TavernForceCleanupSupport.detect(
            doctorReport = doctorReport,
            status = statusText,
            summary = summaryText,
        )
    }

    fun requestForceCleanup(
        suggestionOverride: TavernForceCleanupSuggestion? = null,
        doctorReportOverride: TavernDoctorReport? = null,
        statusOverride: String = status,
        summaryOverride: String = summary,
    ) {
        if (actionInProgress) {
            update("正在处理，完成后再强制清理残留进程。", "", false, allowRunningInference = false)
            return
        }
        val suggestion = suggestionOverride ?: currentForceCleanupSuggestion(
            doctorReport = doctorReportOverride ?: healthCheckReport?.doctorReport,
            statusText = statusOverride,
            summaryText = summaryOverride,
        )
        if (suggestion == null) {
            update("当前没有检测到需要强制清理的残留进程。", "", false, allowRunningInference = false)
            return
        }
        showStopConfirmDialog = false
        pendingTavernForceCleanupConfirmation = TavernForceCleanupSupport.buildConfirmation(
            profile = tavernPathConfig.activeProfile,
            suggestion = suggestion,
        )
    }

    fun confirmStartPreflightDialog() {
        val result = pendingStartPreflight ?: run {
            clearStartPreflightDialog()
            return
        }
        clearStartPreflightDialog()
        when (result.action?.type) {
            TavernStartPreflightActionType.DownloadTermux -> {
                openTermuxDownload(TERMUX_FDROID_URL, "F-Droid 的 Termux 页面")
            }

            TavernStartPreflightActionType.RequestRunPermission -> {
                requestRunCommandPermission()
            }

            TavernStartPreflightActionType.CopyExternalAppsCommand -> {
                copyTermuxPermissionCommand()
            }

            TavernStartPreflightActionType.PrepareTermuxEnvironment -> {
                prepareTermuxEnvironment()
            }

            TavernStartPreflightActionType.ChooseDetectedDirectory -> {
                val candidates = result.doctorReport?.candidateDirectories.orEmpty()
                if (candidates.isEmpty()) {
                    selectedTab = LauncherTab.Settings
                    update("请到设置里的路径分区确认酒馆目录。", "", false, allowRunningInference = false)
                } else {
                    openTavernDirectoryChoice(candidates)
                }
            }

            TavernStartPreflightActionType.OpenPathSettings -> {
                selectedTab = LauncherTab.Settings
                update("请到设置里的路径分区确认酒馆目录。", "", false, allowRunningInference = false)
            }

            TavernStartPreflightActionType.ForceCleanupDetectedProcess -> {
                requestForceCleanup(
                    doctorReportOverride = result.doctorReport,
                    statusOverride = result.title,
                    summaryOverride = result.summary,
                )
            }

            TavernStartPreflightActionType.ReturnToTavern -> {
                returnToTavern()
            }

            TavernStartPreflightActionType.Retry -> {
                requestStartTavern()
            }

            null -> Unit
        }
    }

    fun requestStopTavern() {
        if (actionInProgress) {
            update("正在处理，稍后再停止酒馆。", "", false)
            return
        }

        if (!tavernRunning) {
            update("酒馆当前未运行。", "", false, allowRunningInference = false)
            return
        }
        showStopConfirmDialog = true
    }

    fun dismissForceCleanupDialog() {
        pendingTavernForceCleanupConfirmation = null
    }

    fun confirmStopTavern() {
        showStopConfirmDialog = false
        if (!beginBusy("停止酒馆", 20000L)) return
        tavernStarting = false
        launchAttemptToken += 1
        onCommand("stop") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok)
            val stopResult = "$newStatus\n$termuxOutput"
            if (ok && inferTavernRunning(stopResult) == false) {
                tavernRunning = false
                tavernStarting = false
            }
            if (!isTransientStatus(newStatus)) {
                releaseBusy()
            }
        }
    }

    fun confirmForceCleanupDialog() {
        val confirmation = pendingTavernForceCleanupConfirmation ?: return
        dismissForceCleanupDialog()
        if (!beginBusy(confirmation.suggestion.buttonLabel, 20000L)) return
        tavernStarting = false
        launchAttemptToken += 1
        onCommand("tavern-force-cleanup") { newStatus, termuxOutput, ok ->
            update(newStatus, termuxOutput, ok)
            val cleanupResult = "$newStatus\n$termuxOutput"
            if (ok && inferTavernRunning(cleanupResult) == false) {
                tavernRunning = false
                tavernStarting = false
            }
            if (!isTransientStatus(newStatus)) {
                releaseBusy()
            }
        }
    }

    fun dismissTavernVersionActionDialog() {
        pendingTavernVersionActionConfirmation = null
    }

    fun confirmTavernVersionActionDialog() {
        val confirmation = pendingTavernVersionActionConfirmation ?: return
        dismissTavernVersionActionDialog()
        runSelectedVersionCommandWithSafetyBackup(
            baseCommand = confirmation.kind.baseCommand,
            emptyMessage = "请先选择一个版本。",
            busyText = confirmation.kind.busyText,
            taskKind = confirmation.kind.taskKind,
            safetyBackupPrefix = confirmation.kind.safetyBackupPrefix,
        )
    }

    fun requestTavernVersionAction(kind: TavernVersionActionKind) {
        val actionLabel = when (kind) {
            TavernVersionActionKind.Update -> "更新"
            TavernVersionActionKind.Rollback -> "回退"
        }
        if (actionInProgress) {
            update("正在处理，完成后再${actionLabel}。", "", false, allowRunningInference = false)
            return
        }
        if (blockIfPendingTaskExists("${actionLabel}酒馆")) return

        val actionState = TavernVersionActionGuards.evaluate(
            current = tavernVersionInfo,
            target = selectedTavernVersion,
            officialVersions = officialVersions,
            currentRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
        )
        val disabledReason = when (kind) {
            TavernVersionActionKind.Update -> actionState.updateDisabledReason
            TavernVersionActionKind.Rollback -> actionState.rollbackDisabledReason
        }
        if (disabledReason != null) {
            update("不能${actionLabel}：$disabledReason", "", false, allowRunningInference = false)
            return
        }
        val selectedVersion = selectedTavernVersion ?: run {
            update("请先选择一个版本。", "", false, allowRunningInference = false)
            return
        }
        pendingTavernVersionActionConfirmation = TavernVersionActionConfirmationBuilder.build(
            kind = kind,
            current = tavernVersionInfo,
            target = selectedVersion,
            fallbackRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
        )
    }

    fun requestTavernRollback() {
        requestTavernVersionAction(TavernVersionActionKind.Rollback)
    }

    fun requestTavernUpdate() {
        requestTavernVersionAction(TavernVersionActionKind.Update)
    }

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab.ordinal) {
            pagerState.animateScrollToPage(
                page = selectedTab.ordinal,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
        if (selectedTab == LauncherTab.Backup) {
            val result = withContext(Dispatchers.IO) {
                runCatching { readLocalBackupLibrary() }
            }
            result.onSuccess { paths ->
                val nextBackupHistory = BackupHistoryReducer.sanitize(paths)
                if (nextBackupHistory != backupHistory) {
                    replaceBackupHistory(nextBackupHistory)
                }
            }.onFailure { error ->
                update(
                    "备份库刷新失败：${error.message ?: error.javaClass.simpleName}",
                    "",
                    false,
                    allowRunningInference = false,
                )
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val pagerTab = LauncherTab.entries[pagerState.settledPage]
        pagerInteractionLocked = false
        pagerAxisGuard.reset()
        if (selectedTab != pagerTab) {
            selectedTab = pagerTab
        }
    }

    LaunchedEffect(termuxInstalled, runCommandPermissionGranted, termuxExternalAppsBlocked) {
        while (termuxInstalled && runCommandPermissionGranted && !termuxExternalAppsBlocked) {
            if (!logRefreshInFlight) {
                logRefreshInFlight = true
                onRefreshLogs(::updateTermuxLogOnly)
            }
            delay(900)
        }
        logRefreshInFlight = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            val latest = onLatestTermuxResult()
            if (latest != null && latest.key != lastSyncedTermuxResultKey) {
                if (latest.command == "log") {
                    lastSyncedTermuxResultKey = latest.key
                } else if (busyLabel == null && !logRefreshInFlight) {
                    val signature = latest.output.take(360)
                    if (signature.isBlank()) {
                        lastSyncedTermuxResultKey = latest.key
                    } else if (!termuxLog.contains(signature)) {
                        syncTermuxResult(latest)
                        lastSyncedTermuxResultKey = latest.key
                    } else {
                        lastSyncedTermuxResultKey = latest.key
                    }
                }
            }
            delay(800)
        }
    }

    LaunchedEffect(startupRefreshSignal) {
        if (startupRefreshSignal > 0) {
            delay(900)
            runStartupRefresh()
        }
    }

    LaunchedEffect(Unit) {
        delay(1200)
        if (startupRefreshSignal <= 0 && githubRepository.isNotBlank()) {
            checkGithubUpdate(repositoryOverride = githubRepository, manual = false)
        }
    }

    LaunchedEffect(startupGithubCheckPending) {
        if (startupGithubCheckPending) {
            startupGithubCheckPending = false
            if (githubRepository.isNotBlank()) {
                checkGithubUpdate(repositoryOverride = githubRepository, manual = false)
            }
        }
    }

    fun clearPendingAptConfigTask() {
        pendingAptConfigTask = null
    }

    fun chooseAptConfigPolicy(policy: AptConfigPolicy) {
        val task = pendingAptConfigTask
        pendingAptConfigTask = null
        when (task?.task) {
            AptConfigTask.Bootstrap -> executePrepareTermuxEnvironment(policy)
            AptConfigTask.InstallTavern -> executeInstallSelectedTavern(
                task.installTarget,
                task.installRepoUrl,
                policy,
            )
            null -> update("没有待执行的安装任务。", "", false, allowRunningInference = false)
        }
    }

    pendingAptConfigTask?.let { task ->
        AptConfigPolicyDialog(
            pendingTask = task,
            actionsLocked = actionInProgress,
            onChoose = ::chooseAptConfigPolicy,
            onDismiss = ::clearPendingAptConfigTask,
        )
    }

    pendingLauncherTask?.takeIf { showPendingTaskDialog }?.let { task ->
        PendingTaskResumeDialog(
            task = task,
            activeLockLabel = OperationLockStore.activeLabel(context),
            onContinueCheck = ::continuePendingLauncherTask,
            onAbandon = ::abandonPendingLauncherTask,
            onDismiss = { showPendingTaskDialog = false },
        )
    }

    pendingFirstTavernStartGuide?.takeIf { showFirstTavernStartGuideDialog }?.let { guide ->
        FirstTavernStartGuideDialog(
            guide = guide,
            onPrimaryAction = {
                hideFirstTavernStartGuideDialog()
                markFirstTavernStartGuideSeen()
                when (guide.kind) {
                    FirstTavernStartGuideKind.IQooBackgroundPermission -> {
                        val opened = onRequestTermuxBackgroundRunPermission()
                        update(
                            if (opened) {
                                "已打开 Termux 后台权限页面。允许后再回来启动酒馆会更稳。"
                            } else {
                                "打开 Termux 后台权限页面失败，请到系统设置里放行 Termux。"
                            },
                            "",
                            opened,
                            allowRunningInference = false,
                        )
                    }

                    FirstTavernStartGuideKind.KeepTermuxInSmallWindow -> {
                        val result = onWakeTermux(maxOf(termuxReturnDelayMs, 1_500L))
                        update(
                            if (result.ok) {
                                "${result.message}\n回来后确认 Termux 没被系统关掉，再点“继续启动”。"
                            } else {
                                result.message
                            },
                            "",
                            result.ok,
                            allowRunningInference = false,
                        )
                    }
                }
            },
            onContinueStart = {
                hideFirstTavernStartGuideDialog()
                markFirstTavernStartGuideSeen()
                performForegroundStart()
            },
            onDismiss = ::hideFirstTavernStartGuideDialog,
        )
    }

    if (showInstallRiskDialog) {
        installRiskConfirmation?.let { confirmation ->
            InstallRiskConfirmDialog(
                confirmation = confirmation,
                onConfirm = ::confirmInstallRiskDialog,
                onDismiss = ::clearInstallRiskDialog,
            )
        }
    }

    pendingStartPreflight?.let { result ->
        StartPreflightConfirmDialog(
            result = result,
            activeProfile = tavernPathConfig.activeProfile,
            onConfirm = ::confirmStartPreflightDialog,
            onDismiss = ::clearStartPreflightDialog,
        )
    }

    if (showTavernDirectoryChoiceDialog && tavernDirectoryCandidates.isNotEmpty()) {
        TavernDirectoryChoiceDialog(
            currentPath = tavernPathConfig.displayTavernDir,
            candidates = tavernDirectoryCandidates,
            onChoose = ::chooseDetectedTavernDirectory,
            onDismiss = ::dismissTavernDirectoryChoiceDialog,
        )
    }

    if (showStopConfirmDialog) {
        StopTavernConfirmDialog(
            profile = tavernPathConfig.activeProfile,
            actionsLocked = actionInProgress,
            onConfirm = ::confirmStopTavern,
            onDismiss = { showStopConfirmDialog = false },
        )
    }

    pendingTavernForceCleanupConfirmation?.let { confirmation ->
        ForceCleanupTavernConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionInProgress,
            onConfirm = ::confirmForceCleanupDialog,
            onDismiss = ::dismissForceCleanupDialog,
        )
    }

    pendingTavernVersionActionConfirmation?.let { confirmation ->
        TavernVersionActionConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionInProgress,
            onConfirm = ::confirmTavernVersionActionDialog,
            onDismiss = ::dismissTavernVersionActionDialog,
        )
    }

    if (showExportDialog) {
        ExportLogDialog(
            onExportTermux = { exportWithMode(ExportLogMode.TermuxOnly) },
            onExportApp = { exportWithMode(ExportLogMode.AppOnly) },
            onExportBoth = { exportWithMode(ExportLogMode.Both) },
            onDismiss = { showExportDialog = false },
        )
    }

    if (showClearLogScopeDialog) {
        ClearLogScopeDialog(
            onClearTermux = { prepareClearLogs(ExportLogMode.TermuxOnly) },
            onClearApp = { prepareClearLogs(ExportLogMode.AppOnly) },
            onClearBoth = { prepareClearLogs(ExportLogMode.Both) },
            onDismiss = {
                showClearLogScopeDialog = false
                clearLogConfirmText = ""
            },
        )
    }

    if (showClearLogDangerDialog) {
        ClearLogDangerDialog(
            mode = selectedClearLogMode,
            confirmText = clearLogConfirmText,
            onConfirmTextChange = { clearLogConfirmText = it },
            onConfirm = { clearLogs(selectedClearLogMode) },
            onBack = {
                showClearLogDangerDialog = false
                showClearLogScopeDialog = true
                clearLogConfirmText = ""
            },
            onDismiss = {
                showClearLogDangerDialog = false
                clearLogConfirmText = ""
            },
        )
    }

    if (showManualBackupDialog) {
        ManualBackupConfirmDialog(
            backupName = manualBackupName,
            onBackupNameChange = { manualBackupName = it },
            onConfirm = {
                val backupName = manualBackupName.trim()
                LauncherInputGuards.validateManualBackupName(backupName)?.let { reason ->
                    update("备份名称无效：$reason", "", false, allowRunningInference = false)
                    return@ManualBackupConfirmDialog
                }
                if (blockIfPendingTaskExists("创建备份")) {
                    showManualBackupDialog = false
                    return@ManualBackupConfirmDialog
                }
                showManualBackupDialog = false
                manualBackupName = ""
                runPendingGuardedCommand(
                    task = PendingLauncherTask(
                        kind = PendingLauncherTaskKind.ManualBackup,
                        commandName = "tavern-backup",
                        detail = if (backupName.isBlank()) {
                            "正在创建酒馆备份"
                        } else {
                            "备份名：$backupName"
                        },
                        startedAtMillis = System.currentTimeMillis(),
                        targetLabel = backupName,
                        profileId = tavernPathConfig.activeProfile.id,
                    ),
                    label = "创建酒馆备份",
                    timeoutMs = 600000L,
                ) { guardedUpdate ->
                    if (backupName.isBlank()) {
                        onCommand("tavern-backup-manual", guardedUpdate)
                    } else {
                        onCommand("tavern-backup-manual::$backupName", guardedUpdate)
                    }
                }
            },
            onDismiss = {
                showManualBackupDialog = false
                manualBackupName = ""
            },
        )
    }

    if (showAutoBackupSettingsDialog) {
        AutoBackupSettingsDialog(
            enabled = autoBackupEnabled,
            intervalMinutes = autoBackupIntervalMinutes,
            keepCount = autoBackupKeepCount,
            actionsLocked = actionInProgress,
            onDecreaseInterval = {
                val next = (autoBackupIntervalMinutes - AUTO_BACKUP_INTERVAL_STEP_MINUTES)
                    .coerceAtLeast(MIN_AUTO_BACKUP_INTERVAL_MINUTES)
                updateBackupSettings(
                    intervalMinutes = next,
                    resetCountdown = autoBackupEnabled,
                    message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
                )
            },
            onIncreaseInterval = {
                val next = (autoBackupIntervalMinutes + AUTO_BACKUP_INTERVAL_STEP_MINUTES)
                    .coerceAtMost(MAX_AUTO_BACKUP_INTERVAL_MINUTES)
                updateBackupSettings(
                    intervalMinutes = next,
                    resetCountdown = autoBackupEnabled,
                    message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
                )
            },
            onDecreaseIntervalLarge = {
                val next = (autoBackupIntervalMinutes - 60)
                    .coerceAtLeast(MIN_AUTO_BACKUP_INTERVAL_MINUTES)
                updateBackupSettings(
                    intervalMinutes = next,
                    resetCountdown = autoBackupEnabled,
                    message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
                )
            },
            onIncreaseIntervalLarge = {
                val next = (autoBackupIntervalMinutes + 60)
                    .coerceAtMost(MAX_AUTO_BACKUP_INTERVAL_MINUTES)
                updateBackupSettings(
                    intervalMinutes = next,
                    resetCountdown = autoBackupEnabled,
                    message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
                )
            },
            onDecreaseKeep = {
                updateBackupSettings(
                    keepCount = (autoBackupKeepCount - 1).coerceAtLeast(1),
                    message = "自动备份保留数量已设为 ${(autoBackupKeepCount - 1).coerceAtLeast(1)} 个。",
                )
            },
            onIncreaseKeep = {
                updateBackupSettings(
                    keepCount = (autoBackupKeepCount + 1).coerceAtMost(50),
                    message = "自动备份保留数量已设为 ${(autoBackupKeepCount + 1).coerceAtMost(50)} 个。",
                )
            },
            onDismiss = { showAutoBackupSettingsDialog = false },
        )
    }

    if (showBackgroundRunPermissionDialog) {
        BackgroundRunPermissionDialog(
            granted = backgroundRunPermissionGranted,
            onOpenPermission = {
                showBackgroundRunPermissionDialog = false
                val opened = onRequestBackgroundRunPermission()
                update(
                    if (opened) {
                        "已打开后台运行权限页面。允许后回启动器即可。"
                    } else {
                        "打开后台运行权限页面失败，请到系统设置里允许后台运行。"
                    },
                    "",
                    opened,
                    allowRunningInference = false,
                )
            },
            onDismiss = { showBackgroundRunPermissionDialog = false },
        )
    }

    if (showApplyBackupPathDialog) {
        ApplyBackupPathDialog(
            path = applyBackupPath,
            onPathChange = { applyBackupPath = it },
            onNext = {
                if (openApplyBackupPreview(applyBackupPath)) {
                    showApplyBackupPathDialog = false
                }
            },
            onDismiss = { showApplyBackupPathDialog = false },
        )
    }

    val activeApplyBackupPreview = applyBackupPreview
    if (showApplyBackupPreviewDialog && activeApplyBackupPreview != null) {
        ApplyBackupPreviewDialog(
            preview = activeApplyBackupPreview,
            onConfirm = ::applySelectedBackup,
            onDismiss = ::dismissApplyBackupPreview,
        )
    }

    if (showTermuxStoragePermissionDialog) {
        TermuxStoragePermissionDialog(
            archivePath = storagePermissionRetryArchivePath.ifBlank { applyBackupPath }.trim(),
            actionsLocked = actionInProgress,
            onGrantPermission = ::requestTermuxStoragePermission,
            onRetryApply = ::retryApplyAfterTermuxStoragePermission,
            onDismiss = { showTermuxStoragePermissionDialog = false },
        )
    }

    if (showCopyBackupDialog) {
        CopyBackupConfirmDialog(
            archivePath = selectedBackupPath,
            onConfirm = {
                copyBackupArchive(selectedBackupPath)
            },
            onDismiss = {
                showCopyBackupDialog = false
                selectedBackupPath = ""
            },
        )
    }

    if (showRenameBackupDialog) {
        RenameBackupDialog(
            archivePath = selectedBackupPath,
            newName = renameBackupName,
            backupHistory = backupHistory,
            onNameChange = { renameBackupName = it },
            onConfirm = {
                renameBackupArchive(selectedBackupPath, renameBackupName)
            },
            onDismiss = {
                showRenameBackupDialog = false
                selectedBackupPath = ""
                renameBackupName = ""
            },
        )
    }

    if (showDeleteBackupDialog) {
        DeleteBackupConfirmDialog(
            archivePath = selectedBackupPath,
            onConfirm = {
                val path = selectedBackupPath.trim()
                LauncherInputGuards.validateBackupArchivePath(path)?.let { reason ->
                    update("备份路径无效，不能删除：$reason", "", false, allowRunningInference = false)
                    return@DeleteBackupConfirmDialog
                }
                showDeleteBackupDialog = false
                selectedBackupPath = ""
                runLocalBackupLibraryOperation("删除酒馆备份") {
                    val deleted = BackupLibraryFiles.deleteLibraryArchive(context, path)
                    val paths = readLocalBackupLibrary()
                    paths to "已删除备份：${deleted.fileName}。"
                }
            },
            onDismiss = {
                showDeleteBackupDialog = false
                selectedBackupPath = ""
            },
        )
    }

    pendingTavernProfileRemovalConfirmation?.let { confirmation ->
        DeleteTavernProfileConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionInProgress,
            onConfirm = ::confirmRemoveCurrentTavernProfile,
            onDismiss = { pendingTavernProfileRemovalConfirmation = null },
        )
    }

    pendingTavernProfileMigrationConfirmation?.let { confirmation ->
        TavernProfileMigrationConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionInProgress,
            onConfirm = ::confirmMigrateCurrentTavernPath,
            onDismiss = { pendingTavernProfileMigrationConfirmation = null },
        )
    }

    if (showCustomTavernPathMigrationDialog) {
        CustomTavernPathMigrationDialog(
            currentPath = tavernPathConfig.displayTavernDir,
            pathInput = customMigrationPathInput,
            pathError = customMigrationPathInputError(),
            actionsLocked = actionInProgress,
            onPathChange = { customMigrationPathInput = it },
            onConfirm = ::confirmCustomTavernPathMigrationDialog,
            onDismiss = {
                showCustomTavernPathMigrationDialog = false
                customMigrationPathInput = ""
            },
        )
    }

    if (showImportBackupDialog) {
        ImportBackupDialog(
            path = importBackupPath,
            onPathChange = { importBackupPath = it },
            onConfirm = { importBackupArchive(importBackupPath) },
            onDismiss = {
                showImportBackupDialog = false
                importBackupPath = ""
            },
        )
    }

    if (showUpdateDialog) {
        githubUpdateState.latest?.let { latest ->
            UpdateAvailableDialog(
                updateInfo = latest,
                currentVersionName = versionInfo.versionName,
                downloading = githubUpdateState.downloading,
                onInstall = {
                    showUpdateDialog = false
                    installGithubUpdate()
                },
                onOpenRelease = {
                    val result = onOpenGithubRelease(latest)
                    update(result.message, "", result.ok, allowRunningInference = false)
                },
                onClearBadge = ::clearCurrentGithubUpdateBadge,
                onDismiss = { showUpdateDialog = false },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LukoaColors.Background),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .nestedScroll(pagerAxisGuard)
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> pagerAxisGuard.reset()
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL,
                        MotionEvent.ACTION_OUTSIDE -> {
                            pagerInteractionLocked = false
                            pagerAxisGuard.reset()
                        }
                    }
                    false
                },
            beyondViewportPageCount = 1,
            pageSpacing = 6.dp,
            contentPadding = PaddingValues(horizontal = 2.dp),
            userScrollEnabled = !pagerInteractionLocked,
        ) { page ->
            val tab = LauncherTab.entries[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue.coerceIn(0f, 1f)
            val pageScale = 0.992f + ((1f - pageOffset) * 0.008f)
            val pageAlpha = 0.94f + ((1f - pageOffset) * 0.06f)
            val pageScrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = pageAlpha
                        scaleX = pageScale
                        scaleY = pageScale
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScrollState)
                        .padding(start = 16.dp, top = 42.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header(
                        tavernRunning = tavernRunning,
                        tavernStarting = tavernStarting,
                        showVersionUpdateBadge = showGithubUpdateBadge,
                        onVersionClick = {
                            if (githubUpdateState.hasUpdate) {
                                showUpdateDialog = true
                            } else {
                                checkGithubUpdate(manual = true)
                            }
                        },
                    )

                    if (tab != LauncherTab.Launch) {
                        if (busyLabel != null) {
                            BusyPanel(label = busyLabel.orEmpty(), startedAtMillis = busyStartedAtMillis)
                        }
                    }

                    pendingLauncherTask?.takeIf { !showPendingTaskDialog }?.let { task ->
                        PendingTaskNoticePanel(
                            task = task,
                            activeLockLabel = OperationLockStore.activeLabel(context),
                            actionsLocked = actionInProgress,
                            onContinueCheck = ::continuePendingLauncherTask,
                            onAbandon = ::abandonPendingLauncherTask,
                        )
                    }

                    when (tab) {
                        LauncherTab.Docs -> DocumentationSection(
                            onPagerLockChange = { pagerInteractionLocked = it },
                        )
                        LauncherTab.Version -> VersionManagementSection(
                            actionsLocked = actionInProgress,
                            tavernVersionInfo = tavernVersionInfo,
                            officialVersions = officialVersions,
                            currentRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
                            selectedVersion = selectedTavernVersion,
                            onRefreshOfficialVersions = ::refreshOfficialVersions,
                            onSelectVersion = {
                                selectedTavernVersion = it
                                pendingTavernVersionActionConfirmation = null
                            },
                            onTavernVersion = {
                                runGuarded("重新检测酒馆版本", 18000L, allowRunningInference = false) { guardedUpdate ->
                                    onCommand("tavern-version", guardedUpdate)
                                }
                            },
                            onTavernUpdate = ::requestTavernUpdate,
                            onTavernRollback = ::requestTavernRollback,
                            onPagerLockChange = { pagerInteractionLocked = it },
                        )
                        LauncherTab.Launch -> {
                            val launchPermissionReminder = PermissionStatusSummary.launchReminder(
                                termuxInstalled = termuxInstalled,
                                launcherBackgroundRunPermissionGranted = backgroundRunPermissionGranted,
                                termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
                                termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
                            )
                            OverviewPanel(
                                summary = summary,
                                status = status,
                                verified = verified,
                                tavernRunning = tavernRunning,
                                tavernStarting = tavernStarting,
                                syncActive = termuxInstalled && runCommandPermissionGranted,
                            )
                            launchPermissionReminder?.let { reminder ->
                                NoticeCard(
                                    title = reminder.title,
                                    detail = reminder.detail,
                                    accentColor = LukoaColors.Amber,
                                    actionLabel = "去权限页处理",
                                    onAction = {
                                        selectedTab = LauncherTab.Settings
                                        update("请到设置里的权限分区补齐后台常驻或备份相关权限。", "", false, allowRunningInference = false)
                                    },
                                )
                            }
                            if (busyLabel != null) {
                                BusyPanel(label = busyLabel.orEmpty(), startedAtMillis = busyStartedAtMillis)
                            }
                            val setupRecommended = termuxSetupRecommended()
                            val showQuickStartGuide = !termuxInstalled ||
                                termuxPermissionBlocked() ||
                                setupRecommended ||
                                tavernInstallDetected != true
                            if (showQuickStartGuide) {
                                QuickStartGuideSection(
                                    termuxInstalled = termuxInstalled,
                                    runCommandPermissionGranted = runCommandPermissionGranted,
                                    externalAppsBlocked = termuxExternalAppsBlocked,
                                    tavernInstallDetected = tavernInstallDetected,
                                    tavernVersionChecking = tavernVersionCheckInFlight,
                                    termuxSetupRecommended = setupRecommended,
                                    officialVersions = officialVersions,
                                    selectedVersion = selectedTavernVersion,
                                    mirrorRepoUrl = tavernMirrorConfig.normalizedRepoUrl,
                                    commandText = TERMUX_EXTERNAL_APPS_COMMAND,
                                    actionsLocked = actionInProgress,
                                    onOpenTermuxDownload = {
                                        openTermuxDownload(TERMUX_FDROID_URL, "F-Droid 的 Termux 页面")
                                    },
                                    onOpenTermuxGithub = {
                                        openTermuxDownload(TERMUX_GITHUB_RELEASES_URL, "Termux GitHub Releases")
                                    },
                                    onRecheckTermux = ::recheckTermuxInstalled,
                                    onRequestPermission = ::requestRunCommandPermission,
                                    onOpenPermissionSettings = ::openLauncherPermissionSettings,
                                    onCopyPermissionCommand = ::copyTermuxPermissionCommand,
                                    onOpenTermux = ::openTermuxFromGuide,
                                    onRecheckPermission = ::recheckRunCommandPermission,
                                    onPrepareTermux = ::prepareTermuxEnvironment,
                                    onCheckTavern = ::checkTavernInstall,
                                    onShowInstall = ::enterTavernInstallFlow,
                                    onRefreshOfficialVersions = ::refreshOfficialVersions,
                                    onSelectVersion = { selectedTavernVersion = it },
                                    onUseRecommendedVersion = ::useRecommendedTavernVersion,
                                    onInstallTavern = ::installSelectedTavern,
                                )
                            }
                            val forceCleanupSuggestion = currentForceCleanupSuggestion()
                            TavernControlSection(
                                tavernRunning = tavernRunning,
                                tavernStarting = tavernStarting,
                                actionInProgress = actionInProgress,
                                busyLabel = busyLabel,
                                wakeEnabled = termuxInstalled,
                                primaryEnabled = !tavernStarting &&
                                    !termuxKnownMissing() &&
                                    !termuxPermissionBlocked() &&
                                    (tavernRunning || tavernInstallDetected == true),
                                primaryDisabledReason = when {
                                    termuxKnownMissing() -> "请先安装并打开 Termux。"
                                    termuxPermissionBlocked() -> "请先打开 Termux 调用权限。"
                                    tavernStarting -> "酒馆正在启动，请稍等。"
                                    tavernInstallDetected == null -> "请先检测酒馆或安装。"
                                    !tavernRunning && tavernInstallDetected == false -> "没检测到酒馆，请先安装。"
                                    else -> null
                                },
                                forceCleanupSuggestion = forceCleanupSuggestion,
                                onWakeTermux = {
                                    if (beginBusy("唤醒 Termux", 6000L)) {
                                        val result = onWakeTermux(termuxReturnDelayMs)
                                        releaseBusy()
                                        update(
                                            result.message,
                                            "",
                                            result.ok,
                                        )
                                    }
                                },
                                onPrimaryAction = {
                                    if (tavernRunning) requestStopTavern() else requestStartTavern()
                                },
                                onForceCleanup = {
                                    requestForceCleanup(suggestionOverride = forceCleanupSuggestion)
                                },
                                onOpenTavern = ::returnToTavern,
                                onExportLog = { showExportDialog = true },
                            )
                            IssueAnalysisPanel(
                                issues = issueAnalysis,
                                actionsLocked = actionInProgress,
                                onQuickFixAction = ::runLauncherQuickFixAction,
                            )
                            LogPanel(
                                title = "Termux 调用返回",
                                content = termuxLog,
                                accentColor = LukoaColors.Accent,
                            )
                            LogPanel(
                                title = "App 操作反馈",
                                content = appLog,
                                accentColor = LukoaColors.Muted,
                            )
                        }
                        LauncherTab.Backup -> BackupSection(
                            actionsLocked = actionInProgress,
                            backupListRefreshing = backupListRefreshing,
                            autoBackupEnabled = autoBackupEnabled,
                            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
                            autoBackupKeepCount = autoBackupKeepCount,
                            backupHistory = backupHistory,
                            onCreateManualBackup = {
                                manualBackupName = ""
                                showManualBackupDialog = true
                            },
                            onToggleAutoBackup = ::toggleAutoBackup,
                            onRefreshBackups = ::refreshBackupList,
                            onOpenAutoBackupSettings = { showAutoBackupSettingsDialog = true },
                            onApplyBackup = ::requestApplyBackup,
                            onCopyBackup = ::requestCopyBackup,
                            onRenameBackup = ::requestRenameBackup,
                            onDeleteBackup = ::requestDeleteBackup,
                            onExportBackup = ::exportBackupArchive,
                            onImportBackup = ::pickAndImportExternalBackup,
                            onCopyBackupLibraryPath = ::copyBackupLibraryPath,
                            onPagerLockChange = { pagerInteractionLocked = it },
                        )
                        LauncherTab.Settings -> SettingsSection(
                            termuxReturnDelayMs = termuxReturnDelayMs,
                            termuxInstalled = termuxInstalled,
                            runCommandPermissionGranted = runCommandPermissionGranted,
                            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
                            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
                            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
                            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
                            allFilesAccessGranted = allFilesAccessGranted,
                            installUnknownAppsGranted = installUnknownAppsGranted,
                            tavernMirrorConfig = tavernMirrorConfig,
                            tavernPathConfig = tavernPathConfig,
                            tavernRepoInput = tavernRepoInput,
                            npmRegistryInput = npmRegistryInput,
                            tavernPathInput = tavernPathInput,
                            tavernPortInput = tavernPortInput,
                            mirrorProbeStatus = currentMirrorProbeStatus(),
                            termuxRepoStatus = termuxRepoStatus,
                            customTermuxRepoInput = customTermuxRepoInput,
                            repositoryInput = githubRepositoryInput,
                            githubUpdateState = githubUpdateState,
                            healthCheckReport = healthCheckReport,
                            healthCheckInFlight = healthCheckInFlight,
                            actionsLocked = actionInProgress,
                            onTavernRepoInputChange = { tavernRepoInput = it },
                            onNpmRegistryInputChange = { npmRegistryInput = it },
                            onTavernPathInputChange = { tavernPathInput = it },
                            onTavernPortInputChange = { tavernPortInput = it },
                            onSelectTavernProfile = ::selectTavernProfile,
                            onAddTavernProfile = ::addTavernProfile,
                            onRemoveCurrentTavernProfile = ::requestRemoveCurrentTavernProfile,
                            onMigrateToManagedTavernPath = ::requestMigrateToManagedTavernPath,
                            onMigrateToTraditionalTavernPath = ::requestMigrateToTraditionalTavernPath,
                            onMigrateToCustomTavernPath = ::openCustomTavernPathMigrationDialog,
                            onCustomTermuxRepoInputChange = { customTermuxRepoInput = it },
                            onSaveTavernPath = { saveTavernPathConfig() },
                            onRestoreDefaultTavernPath = ::restoreDefaultTavernPath,
                            onSaveTavernMirror = { saveTavernMirrorConfig() },
                            onUseOfficialMirror = ::useOfficialTavernMirror,
                            onUseGithubProxyMirror = ::useGithubProxyTavernMirror,
                            onUseNpmMirror = ::useNpmMirrorOnly,
                            onCheckTavernMirror = {
                                if (actionInProgress) {
                                    update("正在处理，完成后再检测镜像源。", "", false, allowRunningInference = false)
                                } else {
                                    mirrorProbeStatus = TavernMirrorProbeStatus.checking(tavernMirrorConfig)
                                    onCheckTavernMirror(tavernMirrorConfig) { result ->
                                        mirrorProbeStatus = result
                                        val ok = result.overallLevel != MirrorProbeLevel.Failed
                                        val message = when (result.overallLevel) {
                                            MirrorProbeLevel.Healthy -> "镜像源检测完成，当前源可用。"
                                            MirrorProbeLevel.Warning -> "镜像源检测完成，有提醒项，安装前建议看一眼。"
                                            MirrorProbeLevel.Failed -> "镜像源检测失败，请先换源或检查网络。"
                                            MirrorProbeLevel.Unknown -> "镜像源还没检测完成。"
                                        }
                                        update(message, "", ok, allowRunningInference = false)
                                    }
                                }
                            },
                            onReadTermuxRepoStatus = ::readTermuxPackageMirrorStatus,
                            onApplyCustomTermuxMirror = ::applyCustomTermuxPackageMirror,
                            onRequestBackgroundRunPermission = {
                                val opened = onRequestBackgroundRunPermission()
                                update(
                                    if (opened) {
                                        "已打开后台运行权限页面。允许后回启动器即可。"
                                    } else {
                                        "打开后台运行权限页面失败，请到系统设置里允许后台运行。"
                                    },
                                    "",
                                    opened,
                                    allowRunningInference = false,
                                )
                            },
                            onRequestTermuxBackgroundRunPermission = {
                                val opened = onRequestTermuxBackgroundRunPermission()
                                update(
                                    if (opened) {
                                        "已打开 Termux 后台常驻权限页面。允许后回启动器即可。"
                                    } else {
                                        "打开 Termux 后台常驻权限页面失败，请到系统设置里放行 Termux。"
                                    },
                                    "",
                                    opened,
                                    allowRunningInference = false,
                                )
                            },
                            onRequestRunCommandPermission = ::requestRunCommandPermission,
                            onOpenPermissionSettings = ::openLauncherPermissionSettings,
                            onCopyExternalAppsCommand = ::copyTermuxPermissionCommand,
                            onOpenTermuxOnly = {
                                val opened = onOpenTermuxOnly()
                                update(
                                    if (opened) "已打开 Termux。" else "没找到 Termux。",
                                    "",
                                    opened,
                                    allowRunningInference = false,
                                )
                            },
                            onOpenAllFilesAccessSettings = ::openAllFilesAccessSettings,
                            onOpenUnknownAppSourcesSettings = ::openUnknownAppSourcesSettings,
                            onShowTermuxStoragePermissionGuide = {
                                showTermuxStoragePermissionDialog = true
                            },
                            onRepositoryInputChange = { githubRepositoryInput = it },
                            onSaveRepository = ::saveGithubRepository,
                            onRestoreDefaultRepository = ::restoreDefaultGithubRepository,
                            onSaveUpdateChannel = ::saveGithubUpdateChannel,
                            onCheckUpdate = { checkGithubUpdate(manual = true) },
                            onInstallUpdate = {
                                if (githubUpdateState.hasUpdate) {
                                    showUpdateDialog = true
                                } else {
                                    checkGithubUpdate(manual = true)
                                }
                            },
                            onOpenRelease = {
                                githubUpdateState.latest?.let { latest ->
                                    val result = onOpenGithubRelease(latest)
                                    update(result.message, "", result.ok, allowRunningInference = false)
                                }
                            },
                            onRunHealthCheck = ::runHealthCheck,
                            onRunHealthCheckPrimaryAction = ::runHealthCheckPrimaryAction,
                            onClearLogs = ::requestClearLogs,
                            onExportDiagnostic = ::exportDiagnosticLog,
                            onDecreaseTermuxReturnDelay = {
                                updateTermuxReturnDelay(termuxReturnDelayMs - 100L)
                            },
                            onIncreaseTermuxReturnDelay = {
                                updateTermuxReturnDelay(termuxReturnDelayMs + 100L)
                            },
                            onPagerLockChange = { pagerInteractionLocked = it },
                        )
                    }
                }
            }
        }

        LauncherBottomBar(
            selectedTab = selectedTab,
            onSelectTab = { selectedTab = it },
        )
    }
}

private class PagerAxisGuardConnection(
    private val touchSlop: Float,
) : NestedScrollConnection {
    var interactionLocked: Boolean = false

    private var accumulatedX = 0f
    private var accumulatedY = 0f
    private var allowHorizontal: Boolean? = null

    fun reset() {
        accumulatedX = 0f
        accumulatedY = 0f
        allowHorizontal = null
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (interactionLocked || source != NestedScrollSource.UserInput) return Offset.Zero
        if (allowHorizontal == null) {
            accumulatedX += available.x
            accumulatedY += available.y
            if (hypot(accumulatedX.toDouble(), accumulatedY.toDouble()) >= touchSlop.toDouble()) {
                val horizontalDistance = abs(accumulatedX)
                val verticalDistance = abs(accumulatedY)
                allowHorizontal =
                    horizontalDistance >= touchSlop * 1.15f &&
                    horizontalDistance >= verticalDistance * 2f
            }
        }
        return if (allowHorizontal == false) Offset(x = available.x, y = 0f) else Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val consumed = if (!interactionLocked && allowHorizontal == false) {
            Velocity(x = available.x, y = 0f)
        } else {
            Velocity.Zero
        }
        reset()
        return consumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        reset()
        return Velocity.Zero
    }
}

private const val TERMUX_FDROID_URL = "https://f-droid.org/packages/com.termux/"
private const val TERMUX_GITHUB_RELEASES_URL = "https://github.com/termux/termux-app/releases"
private const val TERMUX_EXTERNAL_APPS_COMMAND =
    "mkdir -p ~/.termux && { grep -qxF 'allow-external-apps=true' ~/.termux/termux.properties 2>/dev/null || printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties; }; termux-reload-settings 2>/dev/null || true"

private fun hasTermuxStoragePermissionProblem(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains("termux-storage-permission") ||
        lower.contains("termux cannot read the backup archive") ||
        (
            lower.contains("permission denied") &&
                (
                    lower.contains("/download/") ||
                        lower.contains("/storage/emulated/0") ||
                        lower.contains("storage/downloads")
                    )
            )
}

private fun isSharedStorageBackupPath(path: String): Boolean {
    val normalized = path.trim().replace('\\', '/').lowercase()
    val sharedStorageRoots = listOf(
        Environment.getExternalStorageDirectory().path,
        System.getenv("EXTERNAL_STORAGE").orEmpty(),
        "/storage/emulated/0",
    ).map { it.trim().replace('\\', '/').trimEnd('/').lowercase() }
        .filter { it.isNotBlank() }
        .distinct()

    return sharedStorageRoots.any { normalized == it || normalized.startsWith("$it/") } ||
        normalized.contains("/storage/downloads/") ||
        normalized.contains("/storage/shared/")
}
