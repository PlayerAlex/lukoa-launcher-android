package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherPathSettingsState(initialConfig: TavernPathConfig) {
    var config by mutableStateOf(initialConfig)
    var pathInput by mutableStateOf(initialConfig.displayTavernDir)
    var portInput by mutableStateOf(initialConfig.normalizedPort.toString())
    var directoryCandidates by mutableStateOf<List<TavernDirectoryCandidateOption>>(emptyList())
    var showDirectoryChoiceDialog by mutableStateOf(false)
    var pendingRemovalConfirmation by mutableStateOf<TavernProfileRemovalConfirmation?>(null)
    var pendingMigrationConfirmation by mutableStateOf<TavernProfileMigrationConfirmation?>(null)
    var showCustomMigrationDialog by mutableStateOf(false)
    var customMigrationPathInput by mutableStateOf("")

    fun applySaveResult(result: TavernPathSaveResult) {
        config = result.config
        pathInput = result.config.displayTavernDir
        portInput = result.config.normalizedPort.toString()
    }

    fun openDirectoryChoice(candidates: List<String>) {
        val resolved = TavernDirectoryCandidateGuard.resolve(config, candidates)
        if (resolved.isEmpty()) return
        directoryCandidates = resolved
        showDirectoryChoiceDialog = true
    }

    fun clearDirectoryChoice() {
        showDirectoryChoiceDialog = false
        directoryCandidates = emptyList()
    }

    fun clearTransientPathUi() {
        clearDirectoryChoice()
        pendingMigrationConfirmation = null
        showCustomMigrationDialog = false
        customMigrationPathInput = ""
    }
}
