package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherBackupUiState(initialState: LauncherUiState) {
    var autoBackupEnabled by mutableStateOf(initialState.autoBackupEnabled)
    var autoBackupIntervalMinutes by mutableIntStateOf(
        initialState.autoBackupIntervalMinutes.coerceIn(
            MIN_AUTO_BACKUP_INTERVAL_MINUTES,
            MAX_AUTO_BACKUP_INTERVAL_MINUTES,
        ),
    )
    var autoBackupKeepCount by mutableIntStateOf(initialState.autoBackupKeepCount.coerceIn(1, 50))
    var backupHistory by mutableStateOf(initialState.backupHistory)
    var backupListRefreshing by mutableStateOf(false)

    var showManualBackupDialog by mutableStateOf(false)
    var showAutoBackupSettingsDialog by mutableStateOf(false)
    var showApplyBackupPathDialog by mutableStateOf(false)
    var showApplyBackupPreviewDialog by mutableStateOf(false)
    var showTermuxStoragePermissionDialog by mutableStateOf(false)
    var showCopyBackupDialog by mutableStateOf(false)
    var showRenameBackupDialog by mutableStateOf(false)
    var showDeleteBackupDialog by mutableStateOf(false)

    var manualBackupName by mutableStateOf("")
    var applyBackupPath by mutableStateOf("")
    var applyBackupPreview by mutableStateOf<BackupRestorePreview?>(null)
    var applyBackupPreviewRequest by mutableStateOf<BackupRestorePreviewRequest?>(null)
    var storagePermissionRetryArchivePath by mutableStateOf("")
    var selectedBackupPath by mutableStateOf("")
    var renameBackupName by mutableStateOf("")
}
