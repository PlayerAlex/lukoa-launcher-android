package moe.lukoa.launcher

import androidx.compose.runtime.Composable

@Composable
fun LauncherDirectoryChoiceDialogHost(
    state: LauncherPathSettingsState,
    onChoose: (String) -> Unit,
) {
    if (!state.showDirectoryChoiceDialog) return
    TavernDirectoryChoiceDialog(
        currentPath = state.config.activeProfile.normalizedTavernDir,
        candidates = state.directoryCandidates,
        onChoose = onChoose,
        onDismiss = state::clearDirectoryChoice,
    )
}

@Composable
fun LauncherProfileMutationDialogHost(
    state: LauncherPathSettingsState,
    actionsLocked: Boolean,
    onConfirmRemoval: () -> Unit,
    onConfirmMigration: () -> Unit,
    onConfirmCustomMigration: () -> Unit,
) {
    state.pendingRemovalConfirmation?.let { confirmation ->
        DeleteTavernProfileConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionsLocked,
            onConfirm = onConfirmRemoval,
            onDismiss = { state.pendingRemovalConfirmation = null },
        )
    }
    state.pendingMigrationConfirmation?.let { confirmation ->
        TavernProfileMigrationConfirmDialog(
            confirmation = confirmation,
            actionsLocked = actionsLocked,
            onConfirm = onConfirmMigration,
            onDismiss = { state.pendingMigrationConfirmation = null },
        )
    }
    if (state.showCustomMigrationDialog) {
        CustomTavernPathMigrationDialog(
            currentPath = state.config.displayTavernDir,
            pathInput = state.customMigrationPathInput,
            pathError = LauncherPathSettingsPolicy.customMigrationError(state.customMigrationPathInput),
            actionsLocked = actionsLocked,
            onPathChange = { state.customMigrationPathInput = it },
            onConfirm = onConfirmCustomMigration,
            onDismiss = {
                state.showCustomMigrationDialog = false
                state.customMigrationPathInput = ""
            },
        )
    }
}
