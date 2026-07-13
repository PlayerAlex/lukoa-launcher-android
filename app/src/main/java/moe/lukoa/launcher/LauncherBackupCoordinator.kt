package moe.lukoa.launcher

import android.content.Context
import android.os.Environment
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherBackupCoordinator(
    context: Context,
    private val scope: CoroutineScope,
    val state: LauncherBackupUiState,
    private val statusUpdate: (String, String, Boolean) -> Unit,
    private val persistCurrentState: () -> Unit,
    private val persistBackupHistory: (List<String>) -> List<String>,
    private val persistAutoBackupConfig: (Boolean, Int, Int) -> Unit,
    private val configureAutoBackupSchedule: (Boolean, Int, Boolean) -> Unit,
    private val isBackgroundRunPermissionGranted: () -> Boolean,
    private val showBackgroundRunPermissionDialog: () -> Unit,
    private val activeOperationLabel: () -> String?,
    private val beginBusy: (String, Long) -> Boolean,
    private val releaseBusy: () -> Unit,
    private val isActionInProgress: () -> Boolean,
    private val blockIfPendingTaskExists: (String) -> Boolean,
    private val runGuardedCommand: (String, Long, Boolean, (LauncherUpdate) -> Unit) -> Unit,
    private val runPendingCommand: (PendingLauncherTask, String, Long, (LauncherUpdate) -> Unit) -> Unit,
    private val onCommand: (String, LauncherUpdate) -> Unit,
    private val activeProfileId: () -> String,
    private val restoreTargetDirectory: () -> String,
    private val isTermuxStoragePermissionBlocked: () -> Boolean,
    private val setTermuxStoragePermissionBlocked: (Boolean) -> Unit,
    private val onCopyText: (String, String) -> Boolean,
    private val onPickExternalBackup: ((ExternalBackupImportResult) -> Unit) -> Unit,
    private val onPickBackupExportDestination: (String, String, (BackupExportDestinationResult) -> Unit) -> Unit,
) {
    private val appContext = context.applicationContext
    private val previewRequestCoordinator = BackupRestorePreviewRequestCoordinator()
    private var previewJob: Job? = null

    fun openManualBackupDialog() {
        state.manualBackupName = ""
        state.showManualBackupDialog = true
    }

    fun dismissManualBackupDialog() {
        state.showManualBackupDialog = false
        state.manualBackupName = ""
    }

    fun confirmManualBackup() {
        val backupName = state.manualBackupName.trim()
        LauncherInputGuards.validateManualBackupName(backupName)?.let { reason ->
            statusUpdate("备份名称无效：$reason", "", false)
            return
        }
        if (blockIfPendingTaskExists("创建备份")) {
            state.showManualBackupDialog = false
            return
        }
        state.showManualBackupDialog = false
        state.manualBackupName = ""
        runPendingCommand(
            PendingLauncherTask(
                kind = PendingLauncherTaskKind.ManualBackup,
                commandName = "tavern-backup",
                detail = if (backupName.isBlank()) "正在创建酒馆备份" else "备份名：$backupName",
                startedAtMillis = System.currentTimeMillis(),
                targetLabel = backupName,
                profileId = activeProfileId(),
            ),
            "创建酒馆备份",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-backup-manual"),
        ) { guardedUpdate ->
            if (backupName.isBlank()) {
                onCommand("tavern-backup-manual", guardedUpdate)
            } else {
                onCommand(LauncherCommandCodec.encode("tavern-backup-manual", backupName), guardedUpdate)
            }
        }
    }

    fun openAutoBackupSettings() {
        state.showAutoBackupSettingsDialog = true
    }

    fun dismissAutoBackupSettings() {
        state.showAutoBackupSettingsDialog = false
    }

    fun toggleAutoBackup() {
        val enabled = !state.autoBackupEnabled
        updateBackupSettings(
            enabled = enabled,
            resetCountdown = enabled,
            message = if (enabled) {
                "自动备份已开启：每 ${formatBackupInterval(state.autoBackupIntervalMinutes)} 一次，保留 ${state.autoBackupKeepCount} 个。"
            } else {
                "自动备份已关闭。已有备份不会被删除。"
            },
        )
        if (enabled && !isBackgroundRunPermissionGranted()) {
            showBackgroundRunPermissionDialog()
        }
    }

    fun decreaseAutoBackupInterval(stepMinutes: Int = AUTO_BACKUP_INTERVAL_STEP_MINUTES) {
        val next = (state.autoBackupIntervalMinutes - stepMinutes)
            .coerceAtLeast(MIN_AUTO_BACKUP_INTERVAL_MINUTES)
        updateBackupSettings(
            intervalMinutes = next,
            resetCountdown = state.autoBackupEnabled,
            message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
        )
    }

    fun increaseAutoBackupInterval(stepMinutes: Int = AUTO_BACKUP_INTERVAL_STEP_MINUTES) {
        val next = (state.autoBackupIntervalMinutes + stepMinutes)
            .coerceAtMost(MAX_AUTO_BACKUP_INTERVAL_MINUTES)
        updateBackupSettings(
            intervalMinutes = next,
            resetCountdown = state.autoBackupEnabled,
            message = "自动备份间隔已设为 ${formatBackupInterval(next)}。",
        )
    }

    fun decreaseAutoBackupKeepCount() {
        val next = (state.autoBackupKeepCount - 1).coerceAtLeast(1)
        updateBackupSettings(keepCount = next, message = "自动备份保留数量已设为 $next 个。")
    }

    fun increaseAutoBackupKeepCount() {
        val next = (state.autoBackupKeepCount + 1).coerceAtMost(50)
        updateBackupSettings(keepCount = next, message = "自动备份保留数量已设为 $next 个。")
    }

    fun refreshBackupList(
        minimumDisplayMillis: Long = MINIMUM_REFRESH_DISPLAY_MILLIS,
        reportBusy: Boolean = true,
    ) {
        if (state.backupListRefreshing) return
        val activeLabel = activeOperationLabel()
        if (activeLabel != null) {
            if (reportBusy) {
                statusUpdate("正在处理：$activeLabel。请稍等。", "", false)
            }
            return
        }
        state.backupListRefreshing = true
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val result = withContext(Dispatchers.IO) {
                runCatching { readLocalBackupLibrary() }
            }
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            val safeMinimumDisplayMillis = minimumDisplayMillis.coerceAtLeast(0L)
            if (elapsed < safeMinimumDisplayMillis) {
                delay(safeMinimumDisplayMillis - elapsed)
            }
            result.onSuccess { paths -> persistBackupHistory(paths) }.onFailure { error ->
                statusUpdate(
                    "刷新备份库失败：${error.message ?: error.javaClass.simpleName}",
                    "",
                    false,
                )
            }
            state.backupListRefreshing = false
        }
    }

    fun dismissApplyBackupPathDialog() {
        state.showApplyBackupPathDialog = false
    }

    fun dismissApplyBackupPreview() {
        state.showApplyBackupPreviewDialog = false
        state.applyBackupPreview = null
    }

    fun cancelApplyBackupPreviewLoading() {
        previewRequestCoordinator.cancel()
        previewJob?.cancel()
        previewJob = null
        state.applyBackupPreviewRequest = null
    }

    fun openApplyBackupPreview(path: String): Boolean {
        val normalized = path.trim()
        LauncherInputGuards.validateBackupArchivePath(normalized)?.let { reason ->
            statusUpdate("备份路径无效：$reason", "", false)
            return false
        }

        previewJob?.cancel()
        val request = previewRequestCoordinator.begin(normalized)
        state.applyBackupPath = normalized
        state.applyBackupPreview = null
        state.showApplyBackupPreviewDialog = false
        state.applyBackupPreviewRequest = request
        val targetDirectory = restoreTargetDirectory()
        previewJob = scope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    BackupRestorePreviewResolver.resolve(
                        context = appContext,
                        archivePath = normalized,
                        restoreTargetDir = targetDirectory,
                    )
                }
                if (!previewRequestCoordinator.accepts(request, state.applyBackupPath)) return@launch
                previewRequestCoordinator.finish(request)
                state.applyBackupPreviewRequest = null
                previewJob = null
                state.applyBackupPreview = preview
                state.showApplyBackupPreviewDialog = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (!previewRequestCoordinator.accepts(request, state.applyBackupPath)) return@launch
                previewRequestCoordinator.finish(request)
                state.applyBackupPreviewRequest = null
                previewJob = null
                statusUpdate("读取备份信息失败：${error.message ?: "请刷新备份库后重试。"}", "", false)
            }
        }
        return true
    }

    fun requestApplyBackup(path: String) {
        val normalized = path.trim()
        if (normalized.isBlank()) {
            state.applyBackupPath = ""
            state.showApplyBackupPathDialog = true
            return
        }
        openApplyBackupPreview(normalized)
    }

    fun applySelectedBackup() {
        if (blockIfPendingTaskExists("应用备份")) {
            dismissApplyBackupPreview()
            return
        }
        val archivePath = state.applyBackupPath.trim()
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            statusUpdate("备份路径无效，不能应用：$reason", "", false)
            return
        }
        if (!BackupLibraryFiles.canReadLibrarySource(appContext, archivePath)) {
            statusUpdate("应用备份失败：启动器读不到这个备份。请先刷新备份库，或重新导入。", "", false)
            return
        }
        if (isTermuxStoragePermissionBlocked() && isSharedStorageBackupPath(archivePath)) {
            state.storagePermissionRetryArchivePath = archivePath
            dismissApplyBackupPreview()
            state.showTermuxStoragePermissionDialog = true
            statusUpdate("应用备份前需要先给 Termux 存储权限。", "", false)
            return
        }
        dismissApplyBackupPreview()
        runPendingCommand(
            PendingLauncherTask(
                kind = PendingLauncherTaskKind.RestoreBackup,
                commandName = "tavern-restore",
                detail = "正在应用酒馆备份",
                startedAtMillis = System.currentTimeMillis(),
                archivePath = archivePath,
                profileId = activeProfileId(),
            ),
            "应用酒馆备份",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-restore"),
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-restore", archivePath), guardedUpdate)
        }
    }

    fun dismissTermuxStoragePermissionDialog() {
        state.showTermuxStoragePermissionDialog = false
    }

    fun requestTermuxStoragePermission() {
        if (isActionInProgress()) {
            statusUpdate("正在处理，完成后再授权。", "", false)
            return
        }
        state.showTermuxStoragePermissionDialog = false
        runGuardedCommand(
            "请求 Termux 存储权限",
            TermuxCommandTimeoutPolicy.operationLockMillis("termux-storage-permission").coerceAtLeast(90_000L),
            false,
        ) { guardedUpdate ->
            onCommand("termux-storage-permission", guardedUpdate)
        }
    }

    fun retryApplyAfterTermuxStoragePermission() {
        val retryPath = state.storagePermissionRetryArchivePath.ifBlank { state.applyBackupPath }.trim()
        if (retryPath.isBlank()) {
            state.showTermuxStoragePermissionDialog = false
            statusUpdate("没有找到要继续应用的备份。", "", false)
            return
        }
        setTermuxStoragePermissionBlocked(false)
        state.showTermuxStoragePermissionDialog = false
        state.applyBackupPath = retryPath
        applySelectedBackup()
    }

    fun requestDeleteBackup(path: String) {
        selectBackupForAction(path, "删除") {
            state.selectedBackupPath = it
            state.showDeleteBackupDialog = true
        }
    }

    fun dismissDeleteBackupDialog() {
        state.showDeleteBackupDialog = false
        state.selectedBackupPath = ""
    }

    fun deleteSelectedBackup() {
        val path = state.selectedBackupPath.trim()
        LauncherInputGuards.validateBackupArchivePath(path)?.let { reason ->
            statusUpdate("备份路径无效，不能删除：$reason", "", false)
            return
        }
        dismissDeleteBackupDialog()
        runLocalBackupLibraryOperation("删除酒馆备份") {
            val deleted = BackupLibraryFiles.deleteLibraryArchive(appContext, path)
            val paths = readLocalBackupLibrary()
            paths to "已删除备份：${deleted.fileName}。"
        }
    }

    fun exportBackupArchive(path: String) {
        val normalized = validatedBackupPath(path, "导出") ?: return
        if (isActionInProgress()) {
            statusUpdate("正在处理，完成后再导出备份。", "", false)
            return
        }
        statusUpdate("请选择导出位置，文件名会自动整理为 .tar.gz。", "", true)
        onPickBackupExportDestination(normalized, normalized.substringAfterLast('/')) { result ->
            statusUpdate(result.message, "", result.ok)
        }
    }

    fun copyBackupLibraryPath(target: BackupLibraryPathTarget) {
        val path = when (target) {
            BackupLibraryPathTarget.Manual -> "/storage/emulated/0/Download/${BackupLibraryFiles.MANUAL_RELATIVE_DIR}"
            BackupLibraryPathTarget.Auto -> "/storage/emulated/0/Download/${BackupLibraryFiles.AUTO_RELATIVE_DIR}"
        }
        val copied = onCopyText("露科亚备份库地址", path)
        statusUpdate(if (copied) "已复制文件地址。" else "复制失败，请手动记下 $path。", "", copied)
    }

    fun requestCopyBackup(path: String) {
        selectBackupForAction(path, "复制") {
            state.selectedBackupPath = it
            state.showCopyBackupDialog = true
        }
    }

    fun dismissCopyBackupDialog() {
        state.showCopyBackupDialog = false
        state.selectedBackupPath = ""
    }

    fun copySelectedBackup() {
        val normalized = validatedBackupPath(state.selectedBackupPath, "复制") ?: return
        dismissCopyBackupDialog()
        runLocalBackupLibraryOperation("复制酒馆备份") {
            val copied = BackupLibraryFiles.copyLibraryArchive(appContext, normalized)
            val paths = readLocalBackupLibrary()
            paths to "已复制备份：${copied.fileName}。"
        }
    }

    fun requestRenameBackup(path: String) {
        selectBackupForAction(path, "重命名") { normalized ->
            state.selectedBackupPath = normalized
            state.renameBackupName = LauncherBackupUiPolicy.defaultRenameName(normalized)
            state.showRenameBackupDialog = true
        }
    }

    fun dismissRenameBackupDialog() {
        state.showRenameBackupDialog = false
        state.selectedBackupPath = ""
        state.renameBackupName = ""
    }

    fun renameSelectedBackup() {
        val normalized = validatedBackupPath(state.selectedBackupPath, "重命名") ?: return
        val normalizedName = state.renameBackupName.trim()
        LauncherInputGuards.validateBackupRequiredName(normalizedName)?.let { reason ->
            statusUpdate("备份新名称无效：$reason", "", false)
            return
        }
        val targetFileName = LauncherInputGuards.backupFileNameForLabel(normalizedName)
        val duplicatePath = LauncherBackupUiPolicy.duplicateRenamePath(
            archivePath = normalized,
            newName = normalizedName,
            backupHistory = state.backupHistory,
        )
        if (duplicatePath != null) {
            statusUpdate("已有同名备份：$targetFileName。请换个名字。", "", false)
            return
        }
        dismissRenameBackupDialog()
        runLocalBackupLibraryOperation("重命名酒馆备份") {
            val renamed = BackupLibraryFiles.renameLibraryArchive(appContext, normalized, normalizedName)
            val paths = readLocalBackupLibrary()
            paths to "已重命名为：${renamed.fileName}。"
        }
    }

    fun pickAndImportExternalBackup() {
        if (isActionInProgress()) {
            statusUpdate("正在处理，完成后再导入备份。", "", false)
            return
        }
        onPickExternalBackup { result ->
            val importedPath = result.termuxReadablePath.trim()
            if (result.ok && importedPath.isNotBlank()) {
                persistBackupHistory(listOf(importedPath) + state.backupHistory)
                runLocalBackupLibraryOperation("刷新酒馆备份列表") {
                    val paths = readLocalBackupLibrary()
                    val importedFileName = importedPath.replace('\\', '/').substringAfterLast('/')
                    val mergedPaths = if (paths.any {
                            it.replace('\\', '/').substringAfterLast('/') == importedFileName
                        }
                    ) {
                        paths
                    } else {
                        BackupHistoryReducer.sanitize(listOf(importedPath) + paths)
                    }
                    mergedPaths to "${result.message}，备份库已刷新。"
                }
            } else {
                statusUpdate(result.message, "", result.ok)
            }
        }
    }

    private fun updateBackupSettings(
        enabled: Boolean = state.autoBackupEnabled,
        intervalMinutes: Int = state.autoBackupIntervalMinutes,
        keepCount: Int = state.autoBackupKeepCount,
        resetCountdown: Boolean = false,
        message: String? = null,
    ) {
        val safeIntervalMinutes = intervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        )
        val safeKeepCount = keepCount.coerceIn(1, 50)
        state.autoBackupEnabled = enabled
        state.autoBackupIntervalMinutes = safeIntervalMinutes
        state.autoBackupKeepCount = safeKeepCount
        persistAutoBackupConfig(enabled, safeIntervalMinutes, safeKeepCount)
        configureAutoBackupSchedule(enabled, safeIntervalMinutes, resetCountdown)
        if (message != null) {
            statusUpdate(message, "", true)
        } else {
            persistCurrentState()
        }
    }

    private fun readLocalBackupLibrary(): List<String> {
        return BackupHistoryReducer.sanitize(
            AutoBackupRetentionManager.enforceConfiguredLimit(
                context = appContext,
                reason = "backup-library-refresh",
            ),
        )
    }

    private fun runLocalBackupLibraryOperation(
        label: String,
        operation: () -> Pair<List<String>, String>,
    ) {
        if (!beginBusy(label, LOCAL_BACKUP_OPERATION_TIMEOUT_MILLIS)) return
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { operation() } }
            releaseBusy()
            result.onSuccess { (paths, message) ->
                persistBackupHistory(paths)
                statusUpdate(message, "", true)
            }.onFailure { error ->
                statusUpdate("$label 失败：${error.message ?: error.javaClass.simpleName}", "", false)
            }
        }
    }

    private fun selectBackupForAction(path: String, actionLabel: String, onValid: (String) -> Unit) {
        val selection = LauncherBackupUiPolicy.selectArchive(path, actionLabel)
        selection.errorMessage?.let { message ->
            statusUpdate(message, "", false)
            return
        }
        onValid(selection.normalizedPath)
    }

    private fun validatedBackupPath(path: String, actionLabel: String): String? {
        val selection = LauncherBackupUiPolicy.selectArchive(path, actionLabel)
        selection.errorMessage?.let { message ->
            statusUpdate(message, "", false)
            return null
        }
        return selection.normalizedPath
    }

    private companion object {
        const val MINIMUM_REFRESH_DISPLAY_MILLIS = 450L
        const val LOCAL_BACKUP_OPERATION_TIMEOUT_MILLIS = 30 * 60 * 1000L
    }
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
