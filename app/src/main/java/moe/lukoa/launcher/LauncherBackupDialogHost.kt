package moe.lukoa.launcher

import androidx.compose.runtime.Composable

@Composable
fun LauncherBackupSettingsDialogHost(
    coordinator: LauncherBackupCoordinator,
    actionsLocked: Boolean,
) {
    val state = coordinator.state

    if (state.showManualBackupDialog) {
        ManualBackupConfirmDialog(
            backupName = state.manualBackupName,
            onBackupNameChange = { state.manualBackupName = it },
            onConfirm = coordinator::confirmManualBackup,
            onDismiss = coordinator::dismissManualBackupDialog,
        )
    }

    if (state.showAutoBackupSettingsDialog) {
        AutoBackupSettingsDialog(
            enabled = state.autoBackupEnabled,
            intervalMinutes = state.autoBackupIntervalMinutes,
            keepCount = state.autoBackupKeepCount,
            actionsLocked = actionsLocked,
            onDecreaseInterval = { coordinator.decreaseAutoBackupInterval() },
            onIncreaseInterval = { coordinator.increaseAutoBackupInterval() },
            onDecreaseIntervalLarge = { coordinator.decreaseAutoBackupInterval(60) },
            onIncreaseIntervalLarge = { coordinator.increaseAutoBackupInterval(60) },
            onDecreaseKeep = coordinator::decreaseAutoBackupKeepCount,
            onIncreaseKeep = coordinator::increaseAutoBackupKeepCount,
            onDismiss = coordinator::dismissAutoBackupSettings,
        )
    }
}

@Composable
fun LauncherBackupOperationDialogHost(
    coordinator: LauncherBackupCoordinator,
    actionsLocked: Boolean,
) {
    val state = coordinator.state

    if (state.showApplyBackupPathDialog) {
        ApplyBackupPathDialog(
            path = state.applyBackupPath,
            onPathChange = { state.applyBackupPath = it },
            onNext = {
                if (coordinator.openApplyBackupPreview(state.applyBackupPath)) {
                    coordinator.dismissApplyBackupPathDialog()
                }
            },
            onDismiss = coordinator::dismissApplyBackupPathDialog,
        )
    }

    state.applyBackupPreviewRequest?.let { request ->
        ApplyBackupPreviewLoadingDialog(
            archivePath = request.archivePath,
            onDismiss = coordinator::cancelApplyBackupPreviewLoading,
        )
    }

    val activeApplyBackupPreview = state.applyBackupPreview
    if (state.showApplyBackupPreviewDialog && activeApplyBackupPreview != null) {
        ApplyBackupPreviewDialog(
            preview = activeApplyBackupPreview,
            onConfirm = coordinator::applySelectedBackup,
            onDismiss = coordinator::dismissApplyBackupPreview,
        )
    }

    if (state.showTermuxStoragePermissionDialog) {
        TermuxStoragePermissionDialog(
            archivePath = state.storagePermissionRetryArchivePath.ifBlank { state.applyBackupPath }.trim(),
            actionsLocked = actionsLocked,
            onGrantPermission = coordinator::requestTermuxStoragePermission,
            onRetryApply = coordinator::retryApplyAfterTermuxStoragePermission,
            onDismiss = coordinator::dismissTermuxStoragePermissionDialog,
        )
    }

    if (state.showCopyBackupDialog) {
        CopyBackupConfirmDialog(
            archivePath = state.selectedBackupPath,
            onConfirm = coordinator::copySelectedBackup,
            onDismiss = coordinator::dismissCopyBackupDialog,
        )
    }

    if (state.showRenameBackupDialog) {
        RenameBackupDialog(
            archivePath = state.selectedBackupPath,
            newName = state.renameBackupName,
            backupHistory = state.backupHistory,
            onNameChange = { state.renameBackupName = it },
            onConfirm = coordinator::renameSelectedBackup,
            onDismiss = coordinator::dismissRenameBackupDialog,
        )
    }

    if (state.showDeleteBackupDialog) {
        DeleteBackupConfirmDialog(
            archivePath = state.selectedBackupPath,
            onConfirm = coordinator::deleteSelectedBackup,
            onDismiss = coordinator::dismissDeleteBackupDialog,
        )
    }
}
