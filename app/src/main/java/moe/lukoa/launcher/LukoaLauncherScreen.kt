package moe.lukoa.launcher

import android.os.Build
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
    onRecentTermuxResults: () -> List<TermuxResultDisplay>,
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
    onExportLog: (LauncherUiState, ExportLogMode, LauncherUpdate) -> Unit,
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
        "${initialState.status}\n${initialState.summary}\n${initialState.termuxLog}\n${initialState.tavernRuntimeLog}\n${initialState.appLog}"
    val initialRunningSignal = inferTavernRunning(initialRuntimeText)
    var status by remember { mutableStateOf(initialState.status) }
    var summary by remember { mutableStateOf(initialState.summary) }
    var termuxLog by remember { mutableStateOf(initialState.termuxLog) }
    var tavernRuntimeLog by remember { mutableStateOf(initialState.tavernRuntimeLog) }
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
    val backupUiState = remember { LauncherBackupUiState(initialState) }
    var autoBackupEnabled by backupUiState::autoBackupEnabled
    var autoBackupIntervalMinutes by backupUiState::autoBackupIntervalMinutes
    var autoBackupKeepCount by backupUiState::autoBackupKeepCount
    var backupHistory by backupUiState::backupHistory
    var termuxReturnDelayMs by remember { mutableLongStateOf(initialState.termuxReturnDelayMs.coerceIn(300L, 2_000L)) }
    var logRefreshInFlight by remember { mutableStateOf(false) }
    var termuxBootstrapCompleted by remember { mutableStateOf(false) }
    var pendingTavernVersionActionConfirmation by remember {
        mutableStateOf<TavernVersionActionConfirmation?>(null)
    }
    var pendingTavernForceCleanupConfirmation by remember {
        mutableStateOf<TavernForceCleanupConfirmation?>(null)
    }
    var showExportDialog by remember { mutableStateOf(false) }
    var showClearLogScopeDialog by remember { mutableStateOf(false) }
    var showClearLogDangerDialog by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var showBackgroundRunPermissionDialog by remember { mutableStateOf(false) }
    var showFirstTavernStartGuideDialog by remember { mutableStateOf(false) }
    var backgroundPermissionPromptShown by remember { mutableStateOf(false) }
    var selectedClearLogMode by remember { mutableStateOf(ExportLogMode.Both) }
    var clearLogConfirmText by remember { mutableStateOf("") }
    var termuxStoragePermissionBlocked by remember {
        mutableStateOf(hasTermuxStoragePermissionProblem(initialRuntimeText))
    }
    var pendingAptConfigTask by remember { mutableStateOf<PendingAptConfigTask?>(null) }
    var pendingInstallRiskRequest by remember { mutableStateOf<PendingTavernInstallRequest?>(null) }
    var installRiskConfirmation by remember { mutableStateOf<TavernInstallConfirmation?>(null) }
    var pendingStartPreflight by remember { mutableStateOf<TavernStartPreflightResult?>(null) }
    val initialRestoredOperationLock = remember {
        val nowMillis = System.currentTimeMillis()
        OperationLockRecovery.restore(
            snapshot = OperationLockStore.active(context, nowMillis),
            nowMillis = nowMillis,
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        )
    }
    var busyLabel by remember { mutableStateOf(initialRestoredOperationLock?.label) }
    var busyStartedAtMillis by remember {
        mutableLongStateOf(initialRestoredOperationLock?.busyStartedAtElapsedMillis ?: 0L)
    }
    var restoredOperationLockActive by remember { mutableStateOf(initialRestoredOperationLock != null) }
    var busyToken by remember { mutableIntStateOf(0) }
    var observedOperationLockToken by remember { mutableIntStateOf(0) }
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
    val pathSettingsState = remember { LauncherPathSettingsState(initialTavernPathConfig) }
    val mirrorSettingsState = remember { LauncherMirrorSettingsState(initialTavernMirrorConfig) }
    var tavernMirrorConfig by mirrorSettingsState::config
    var tavernPathConfig by pathSettingsState::config
    var tavernRepoInput by mirrorSettingsState::tavernRepoInput
    var npmRegistryInput by mirrorSettingsState::npmRegistryInput
    var tavernPathInput by pathSettingsState::pathInput
    var tavernPortInput by pathSettingsState::portInput
    var mirrorProbeStatus by mirrorSettingsState::probeStatus
    var termuxRepoStatus by mirrorSettingsState::termuxRepoStatus
    var customTermuxRepoInput by mirrorSettingsState::customTermuxRepoInput
    var ignoredUpdateTag by remember { mutableStateOf(initialIgnoredUpdateTag) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showInstallRiskDialog by remember { mutableStateOf(false) }
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
    val issueAnalysis = TavernIssueAnalyzer.analyze("$termuxLog\n\n$tavernRuntimeLog", status)
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

    DisposableEffect(context) {
        val stopObserving = OperationLockStore.observe(context) { snapshot ->
            scope.launch {
                if (snapshot == null) {
                    if (restoredOperationLockActive) {
                        observedOperationLockToken += 1
                        busyLabel = null
                        busyStartedAtMillis = 0L
                        restoredOperationLockActive = false
                    }
                    return@launch
                }
                if (busyLabel != null) return@launch
                val nowMillis = System.currentTimeMillis()
                val restored = OperationLockRecovery.restore(
                    snapshot = snapshot,
                    nowMillis = nowMillis,
                    elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
                ) ?: return@launch
                busyLabel = restored.label
                busyStartedAtMillis = restored.busyStartedAtElapsedMillis
                restoredOperationLockActive = true
                observedOperationLockToken += 1
                val token = observedOperationLockToken
                scope.launch {
                    delay(restored.remainingMillis)
                    if (observedOperationLockToken == token && restoredOperationLockActive) {
                        OperationLockStore.active(context)
                    }
                }
            }
        }
        onDispose(stopObserving)
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
            tavernRuntimeLog = tavernRuntimeLog,
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
        pathSettingsState.applySaveResult(result)
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
        if (FirstTavernStartGuideResolver.hasSuccessfulStartHistory("$termuxLog\n\n$tavernRuntimeLog", appLog)) {
            markFirstTavernStartGuideSeen()
            return false
        }
        if (!FirstTavernStartGuideResolver.shouldShow(
                alreadyShown = firstTavernStartGuideSeen,
                tavernInstallDetected = tavernInstallDetected,
                tavernRunning = tavernRunning,
                termuxLog = "$termuxLog\n\n$tavernRuntimeLog",
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
        pathSettingsState.openDirectoryChoice(candidates)
    }

    fun clearTransientTavernPathUiState() {
        pathSettingsState.clearTransientPathUi()
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
        return mirrorSettingsState.currentProbeStatus()
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
        val waitingCommandLog = "正在读取${activeLabel}的运行状态和版本信息。"
        val waitingRuntimeLog = "正在同步${activeLabel}的运行日志。"
        val clearedVersionInfo = TavernVersionInfo()
        val customSelection = selectedTavernVersion?.takeIf { it.kind == TavernVersionKind.Custom }

        status = statusText
        summary = summaryText
        verified = true
        termuxLog = waitingCommandLog
        tavernRuntimeLog = waitingRuntimeLog
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
        pathSettingsState.pendingRemovalConfirmation = null
        clearTransientTavernPathUiState()
        launchAttemptToken += 1
        lastSyncedTermuxResultKey = onLatestTermuxResult()?.key.orEmpty()
        onPersistState(currentState().copy(
            status = statusText,
            summary = summaryText,
            termuxLog = waitingCommandLog,
            tavernRuntimeLog = waitingRuntimeLog,
            appLog = "App：$statusText",
            verified = true,
        ))
    }

    fun extractTermuxDisplayContent(output: String): TermuxDisplayContent {
        return TermuxDisplayContentExtractor.extract(output)
    }

    fun update(newStatus: String, termuxOutput: String, ok: Boolean, allowRunningInference: Boolean = true) {
        val displayContent = extractTermuxDisplayContent(termuxOutput)
        val displayTermuxOutput = displayContent.commandText
        val runtimeLogOutput = displayContent.tavernRuntimeLogText
        val newSummary = StatusSummarizer.summarize(newStatus, termuxOutput, ok)
        RuntimeLogArchive.appendApp(context, newStatus)
        if (displayTermuxOutput.isNotBlank()) {
            RuntimeLogArchive.appendTermuxCommand(context, displayTermuxOutput)
        }
        if (runtimeLogOutput.isNotBlank() && !tavernRuntimeLog.contains(runtimeLogOutput)) {
            RuntimeLogArchive.appendTavernRuntime(context, runtimeLogOutput)
        }
        val newAppLog = appendLog(appLog, "App", newStatus)
        val newTermuxLog = if (displayTermuxOutput.isNotBlank()) {
            appendRawLog(termuxLog, displayTermuxOutput)
        } else {
            termuxLog
        }
        val newTavernRuntimeLog = if (runtimeLogOutput.isNotBlank() && !tavernRuntimeLog.contains(runtimeLogOutput)) {
            appendRawLog(tavernRuntimeLog, runtimeLogOutput)
        } else {
            tavernRuntimeLog
        }
        val nextBackupHistory = BackupHistoryReducer.reduce(backupHistory, termuxOutput, ok)

        status = newStatus
        summary = newSummary
        verified = ok
        appLog = newAppLog
        termuxLog = newTermuxLog
        tavernRuntimeLog = newTavernRuntimeLog
        backupHistory = nextBackupHistory
        rememberLaunchReadinessSnapshot(termuxOutput)
        applyTavernVersionInfoFromOutput(termuxOutput)
        inferTavernInstalledFromOutput(newStatus, termuxOutput)?.let {
            tavernInstallDetected = it
        }
        TavernOfficialVersionParser.parse(termuxOutput).takeIf { it.hasData }?.let(::applyOfficialVersions)
        TermuxRepoStatusParser.parse(termuxOutput)?.let(mirrorSettingsState::applyTermuxRepoStatus)
        val permissionText = "$newStatus\n$termuxOutput"
        maybePromptTavernDirectoryChoice(permissionText)
        if (TermuxPermissionSignals.externalAppsBlocked(permissionText)) {
            termuxExternalAppsBlocked = true
        }
        if (hasTermuxStoragePermissionProblem(permissionText)) {
            termuxStoragePermissionBlocked = true
            backupUiState.storagePermissionRetryArchivePath = backupUiState.applyBackupPath.ifBlank {
                backupUiState.storagePermissionRetryArchivePath
            }
            backupUiState.showTermuxStoragePermissionDialog = true
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
            tavernRuntimeLog = newTavernRuntimeLog,
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
        val displayTermuxOutput = extractTermuxDisplayContent(display.output).commandText
        if (displayTermuxOutput.isBlank()) return
        val newStatus = "已同步 Termux：${display.command}"
        val newSummary = StatusSummarizer.summarize(newStatus, display.output, display.ok)
        RuntimeLogArchive.appendApp(context, newStatus)
        RuntimeLogArchive.appendTermuxCommand(context, displayTermuxOutput)
        val newTermuxLog = appendRawLog(termuxLog, displayTermuxOutput)
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
        TermuxRepoStatusParser.parse(display.output)?.let(mirrorSettingsState::applyTermuxRepoStatus)
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
            tavernRuntimeLog = tavernRuntimeLog,
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
        restoredOperationLockActive = false
        if (OperationLockStore.release(context)) return

        val nowMillis = System.currentTimeMillis()
        val restored = OperationLockRecovery.restore(
            snapshot = OperationLockStore.active(context, nowMillis),
            nowMillis = nowMillis,
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
        ) ?: return
        busyLabel = restored.label
        busyStartedAtMillis = restored.busyStartedAtElapsedMillis
        restoredOperationLockActive = true
        observedOperationLockToken += 1
        val token = observedOperationLockToken
        scope.launch {
            delay(restored.remainingMillis)
            if (observedOperationLockToken == token && restoredOperationLockActive) {
                OperationLockStore.active(context)
            }
        }
    }

    fun beginBusy(label: String, timeoutMs: Long = 18000L): Boolean {
        val currentLabel = busyLabel
        if (currentLabel != null) {
            update("正在处理：$currentLabel。请等一下。", "", false)
            return false
        }

        if (!OperationLockStore.acquire(context, label, timeoutMs)) {
            val activeLabel = OperationLockStore.activeLabel(context) ?: "其他操作"
            update("正在处理：$activeLabel。请等一下。", "", false)
            return false
        }
        busyLabel = label
        busyStartedAtMillis = SystemClock.elapsedRealtime()
        restoredOperationLockActive = false
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

    LaunchedEffect(initialRestoredOperationLock) {
        val restoredLock = initialRestoredOperationLock ?: return@LaunchedEffect
        delay(restoredLock.remainingMillis)
        if (restoredOperationLockActive && busyLabel == restoredLock.label) {
            releaseBusy()
            update(
                "上次操作的等待时间已结束，可以继续操作。",
                "",
                false,
                allowRunningInference = false,
            )
        }
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

        fun applyDetectedTavernState(source: String, nextRuntimeLog: String = tavernRuntimeLog) {
            val inferredRunning = inferTavernRunningFromLogSnapshot(source)
            val startingDetected = !isTavernLogStatusReport(source) && inferTavernStarting(source)
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
                nextRuntimeLog == tavernRuntimeLog &&
                newTavernRunning == tavernRunning &&
                newTavernStarting == tavernStarting
            ) {
                return
            }
            status = newStatus
            summary = newSummary
            verified = newVerified
            tavernRuntimeLog = nextRuntimeLog
            tavernRunning = newTavernRunning
            tavernStarting = newTavernStarting
            onPersistState(currentState().copy(
                status = newStatus,
                summary = newSummary,
                termuxLog = termuxLog,
                tavernRuntimeLog = nextRuntimeLog,
                verified = newVerified,
            ))
        }

        if (termuxOutput.contains("缺少 RUN_COMMAND 权限")) {
            runCommandPermissionGranted = false
            return
        }

        val runtimeLogOutput = extractTermuxDisplayContent(termuxOutput).tavernRuntimeLogText
        if (runtimeLogOutput.isBlank()) {
            applyDetectedTavernState(termuxOutput)
            return
        }

        val newRuntimeLog = if (tavernRuntimeLog.contains(runtimeLogOutput)) {
            tavernRuntimeLog
        } else {
            RuntimeLogArchive.appendTavernRuntime(context, runtimeLogOutput)
            appendRawLog(tavernRuntimeLog, runtimeLogOutput)
        }
        applyDetectedTavernState(termuxOutput, newRuntimeLog)
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
            ExportLogMode.TermuxOnly -> "Termux 前台回传和酒馆运行日志"
            ExportLogMode.AppOnly -> "App 操作反馈"
            ExportLogMode.Both -> "Termux 前台回传、酒馆运行日志和 App 操作反馈"
        }
        val newStatus = "已清除$targetText。"
        val newSummary = "日志显示已清理"
        RuntimeLogArchive.clear(context, mode)
        RuntimeLogArchive.appendApp(context, newStatus)
        val newTermuxLog = if (mode.includeTermux) "暂无 Termux 前台回传。" else termuxLog
        val newTavernRuntimeLog = if (mode.includeTermux) "暂无酒馆运行日志。" else tavernRuntimeLog
        val newAppLog = if (mode.includeApp) {
            "暂无 App 操作反馈。"
        } else {
            appendLog(appLog, "App", "已清除$targetText，酒馆文件未删。")
        }
        status = newStatus
        summary = newSummary
        verified = true
        termuxLog = newTermuxLog
        tavernRuntimeLog = newTavernRuntimeLog
        appLog = newAppLog
        onPersistState(
            LauncherUiState(
                status = newStatus,
                summary = newSummary,
                termuxLog = newTermuxLog,
                tavernRuntimeLog = newTavernRuntimeLog,
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
        return LauncherCommandCodec.encode(baseCommand, encoded)
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
        val safetyBackupCommand = LauncherCommandCodec.encode(
            "tavern-backup-manual",
            PendingLauncherTaskSupport.buildSafetyBackupLabel(safetyBackupPrefix),
        )
        if (!beginBusy(
                busyText,
                TermuxCommandTimeoutPolicy.chainedOperationLockMillis("tavern-backup-manual", baseCommand),
            )
        ) return
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
        update("正在生成运行日志。日志较大时会稍等一会儿，但不该再把界面卡死。", "", true, allowRunningInference = false)
        onExportLog(currentState(), mode, ::update)
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

    val backupCoordinator = remember(context, scope, backupUiState) {
        LauncherBackupCoordinator(
            context = context,
            scope = scope,
            state = backupUiState,
            statusUpdate = { status, output, ok ->
                update(status, output, ok, allowRunningInference = false)
            },
            persistCurrentState = { onPersistState(currentState()) },
            persistBackupHistory = ::replaceBackupHistory,
            persistAutoBackupConfig = onPersistAutoBackupConfig,
            configureAutoBackupSchedule = onConfigureAutoBackupSchedule,
            isBackgroundRunPermissionGranted = { backgroundRunPermissionGranted },
            showBackgroundRunPermissionDialog = { showBackgroundRunPermissionDialog = true },
            activeOperationLabel = { busyLabel ?: OperationLockStore.activeLabel(context) },
            beginBusy = ::beginBusy,
            releaseBusy = ::releaseBusy,
            isActionInProgress = { busyLabel != null },
            blockIfPendingTaskExists = ::blockIfPendingTaskExists,
            runGuardedCommand = { label, timeoutMs, allowRunningInference, command ->
                runGuarded(label, timeoutMs, allowRunningInference, command)
            },
            runPendingCommand = { task, label, timeoutMs, command ->
                runPendingGuardedCommand(
                    task = task,
                    label = label,
                    timeoutMs = timeoutMs,
                    action = command,
                )
            },
            onCommand = onCommand,
            activeProfileId = { tavernPathConfig.activeProfile.id },
            restoreTargetDirectory = { tavernPathConfig.displayTavernDir },
            isTermuxStoragePermissionBlocked = { termuxStoragePermissionBlocked },
            setTermuxStoragePermissionBlocked = { termuxStoragePermissionBlocked = it },
            onCopyText = onCopyText,
            onPickExternalBackup = onPickExternalBackup,
            onPickBackupExportDestination = onPickBackupExportDestination,
        )
    }

    fun runPendingTaskFollowUpRefresh(
        refreshTargets: PendingTaskRefreshTargets,
        startupDelayMs: Long = 0L,
    ) {
        if (refreshTargets.backupList) {
            backupCoordinator.refreshBackupList()
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

    val profileCoordinator = remember(pathSettingsState, mirrorSettingsState) {
        LauncherProfileCoordinator(
            pathState = pathSettingsState,
            mirrorState = mirrorSettingsState,
            statusUpdate = { message, output, ok ->
                update(message, output, ok, allowRunningInference = false)
            },
            refreshActiveProfileState = ::refreshActiveProfileState,
            blockIfPendingTaskExists = ::blockIfPendingTaskExists,
            runProfileMutationPendingCommand = ::runProfileMutationPendingCommand,
            beginBusy = ::beginBusy,
            releaseBusy = ::releaseBusy,
            isTransientStatus = ::isTransientStatus,
            isActionInProgress = { busyLabel != null },
            isTavernRunning = { tavernRunning },
            isTavernStarting = { tavernStarting },
            isTermuxInstalled = { termuxInstalled },
            isRunCommandPermissionGranted = { runCommandPermissionGranted },
            onCommand = onCommand,
            onSaveTavernMirrorConfig = onSaveTavernMirrorConfig,
            onSaveTavernPathConfig = onSaveTavernPathConfig,
            onRestoreDefaultTavernPath = onRestoreDefaultTavernPath,
            onCheckTavernMirror = onCheckTavernMirror,
            onTavernRepoChanged = { repoUrl ->
                officialVersions = TavernOfficialVersions()
                selectedTavernVersion = selectedTavernVersion
                    ?.takeIf { it.kind == TavernVersionKind.Custom }
                    ?.copy(repoUrl = repoUrl)
                pendingTavernVersionActionConfirmation = null
            },
        )
    }

    fun checkTavernInstall() {
        if (!termuxInstalled || !runCommandPermissionGranted) {
            update("请先准备好 Termux。", "", false, allowRunningInference = false)
            return
        }
        if (!beginBusy(
                "检测酒馆安装",
                TermuxCommandTimeoutPolicy.operationLockMillis("tavern-version"),
            )
        ) return
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
        if (!beginBusy(
                "读取官方版本",
                TermuxCommandTimeoutPolicy.operationLockMillis("tavern-official-versions"),
            )
        ) return
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
            timeoutMs = TermuxCommandTimeoutPolicy.operationLockMillis("tavern-install"),
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-install", commandArgument), guardedUpdate)
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

    fun saveTavernMirrorConfig() = profileCoordinator.saveTavernMirrorConfig()

    fun saveTavernPathConfig() = profileCoordinator.saveTavernPathConfig()

    fun chooseDetectedTavernDirectory(path: String) = profileCoordinator.chooseDetectedTavernDirectory(path)

    fun restoreDefaultTavernPath() = profileCoordinator.restoreDefaultTavernPath()

    fun selectTavernProfile(profileId: String) = profileCoordinator.selectTavernProfile(profileId)

    fun addTavernProfile() = profileCoordinator.addTavernProfile()

    fun requestRemoveCurrentTavernProfile() = profileCoordinator.requestRemoveCurrentTavernProfile()

    fun requestMigrateToManagedTavernPath() = profileCoordinator.requestMigrateToManagedTavernPath()

    fun requestMigrateToTraditionalTavernPath() = profileCoordinator.requestMigrateToTraditionalTavernPath()

    fun openCustomTavernPathMigrationDialog() = profileCoordinator.openCustomTavernPathMigrationDialog()

    fun confirmCustomTavernPathMigrationDialog() = profileCoordinator.confirmCustomTavernPathMigrationDialog()

    fun confirmMigrateCurrentTavernPath() = profileCoordinator.confirmMigrateCurrentTavernPath()

    fun confirmRemoveCurrentTavernProfile() = profileCoordinator.confirmRemoveCurrentTavernProfile()

    fun useOfficialTavernMirror() = profileCoordinator.useOfficialTavernMirror()

    fun useGithubProxyTavernMirror() = profileCoordinator.useGithubProxyTavernMirror()

    fun useNpmMirrorOnly() = profileCoordinator.useNpmMirrorOnly()

    fun readTermuxPackageMirrorStatus() = profileCoordinator.readTermuxPackageMirrorStatus()

    fun applyCustomTermuxPackageMirror() = profileCoordinator.applyCustomTermuxPackageMirror()
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
        if (!beginBusy(
                "准备 Termux 环境",
                TermuxCommandTimeoutPolicy.operationLockMillis("termux-bootstrap"),
            )
        ) return
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
                backupCoordinator.requestTermuxStoragePermission()
            }
        }
    }

    fun continuePendingLauncherTask() {
        val task = pendingLauncherTask ?: return
        showPendingTaskDialog = false
        selectedTab = PendingLauncherTaskSupport.defaultTab(task)
        val latest = PendingLauncherTaskSupport.latestResult(task, onRecentTermuxResults())
        if (latest != null) {
            if (latest.key != lastSyncedTermuxResultKey) {
                syncTermuxResult(latest)
                lastSyncedTermuxResultKey = latest.key
            }
            val resolved = applyProfileMutationTaskSideEffect(task, latest.ok, latest.output)
                ?: PendingLauncherTaskSupport.resolveLatestResult(task, latest)
            runPendingTaskFollowUpRefresh(resolved.refreshTargets)
            clearPendingLauncherTask()
            releaseBusy()
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
                    releaseBusy()
                    backupCoordinator.refreshBackupList()
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
        releaseBusy()
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
        if (!beginBusy(
                "启动前预检",
                TermuxCommandTimeoutPolicy.operationLockMillis("tavern-doctor"),
            )
        ) return

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
            update("当前没有检测到需要强制清理的残留进程。可以先做一次体检，或先普通停止酒馆后再回来处理。", "", false, allowRunningInference = false)
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
        if (!beginBusy(
                "停止酒馆",
                TermuxCommandTimeoutPolicy.operationLockMillis("stop"),
            )
        ) return
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
        if (!beginBusy(
                confirmation.suggestion.buttonLabel,
                TermuxCommandTimeoutPolicy.operationLockMillis("tavern-force-cleanup"),
            )
        ) return
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
            tavernRunning = tavernRunning,
            tavernStarting = tavernStarting,
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
            backupCoordinator.refreshBackupList(minimumDisplayMillis = 0L, reportBusy = false)
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
                    val signature = extractTermuxDisplayContent(latest.output).commandText.take(360)
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

    LauncherDirectoryChoiceDialogHost(
        state = pathSettingsState,
        onChoose = ::chooseDetectedTavernDirectory,
    )

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

    LauncherBackupSettingsDialogHost(
        coordinator = backupCoordinator,
        actionsLocked = actionInProgress,
    )

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

    LauncherBackupOperationDialogHost(
        coordinator = backupCoordinator,
        actionsLocked = actionInProgress,
    )

    LauncherProfileMutationDialogHost(
        state = pathSettingsState,
        actionsLocked = actionInProgress,
        onConfirmRemoval = ::confirmRemoveCurrentTavernProfile,
        onConfirmMigration = ::confirmMigrateCurrentTavernPath,
        onConfirmCustomMigration = ::confirmCustomTavernPathMigrationDialog,
    )

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
                            actionsLocked = actionInProgress && !restoredOperationLockActive,
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
                            tavernRunning = tavernRunning,
                            tavernStarting = tavernStarting,
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
                                onOpenTavern = ::returnToTavern,
                                onExportLog = { showExportDialog = true },
                            )
                            IssueAnalysisPanel(
                                issues = issueAnalysis,
                                actionsLocked = actionInProgress,
                                onQuickFixAction = ::runLauncherQuickFixAction,
                            )
                            LogPanel(
                                title = "酒馆运行日志",
                                content = tavernRuntimeLog,
                                accentColor = LukoaColors.Info,
                                maxVisibleLines = null,
                            )
                            LogPanel(
                                title = "App 操作反馈",
                                content = appLog,
                                accentColor = LukoaColors.Muted,
                            )
                        }
                        LauncherTab.Backup -> BackupSection(
                            actionsLocked = actionInProgress || backupUiState.applyBackupPreviewRequest != null,
                            backupListRefreshing = backupUiState.backupListRefreshing,
                            autoBackupEnabled = autoBackupEnabled,
                            autoBackupIntervalMinutes = autoBackupIntervalMinutes,
                            autoBackupKeepCount = autoBackupKeepCount,
                            backupHistory = backupHistory,
                            onCreateManualBackup = backupCoordinator::openManualBackupDialog,
                            onToggleAutoBackup = backupCoordinator::toggleAutoBackup,
                            onRefreshBackups = { backupCoordinator.refreshBackupList() },
                            onOpenAutoBackupSettings = backupCoordinator::openAutoBackupSettings,
                            onApplyBackup = backupCoordinator::requestApplyBackup,
                            onCopyBackup = backupCoordinator::requestCopyBackup,
                            onRenameBackup = backupCoordinator::requestRenameBackup,
                            onDeleteBackup = backupCoordinator::requestDeleteBackup,
                            onExportBackup = backupCoordinator::exportBackupArchive,
                            onImportBackup = backupCoordinator::pickAndImportExternalBackup,
                            onCopyBackupLibraryPath = backupCoordinator::copyBackupLibraryPath,
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
                            forceCleanupSuggestion = currentForceCleanupSuggestion(),
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
                            onCheckTavernMirror = profileCoordinator::checkTavernMirror,
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
                                backupUiState.showTermuxStoragePermissionDialog = true
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
                            onForceCleanup = ::requestForceCleanup,
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
