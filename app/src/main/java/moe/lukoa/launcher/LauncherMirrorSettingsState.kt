package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherMirrorSettingsState(initialConfig: TavernMirrorConfig) {
    var config by mutableStateOf(initialConfig)
    var tavernRepoInput by mutableStateOf(initialConfig.normalizedRepoUrl)
    var npmRegistryInput by mutableStateOf(initialConfig.normalizedNpmRegistry)
    var probeStatus by mutableStateOf(TavernMirrorProbeStatus.unknown(initialConfig))
    var termuxRepoStatus by mutableStateOf(TermuxRepoStatus())
    var customTermuxRepoInput by mutableStateOf("")

    fun currentProbeStatus(): TavernMirrorProbeStatus {
        return probeStatus.takeIf { it.matches(config) }
            ?: TavernMirrorProbeStatus.unknown(config)
    }

    fun applySavedConfig(config: TavernMirrorConfig) {
        this.config = config
        tavernRepoInput = config.normalizedRepoUrl
        npmRegistryInput = config.normalizedNpmRegistry
        probeStatus = TavernMirrorProbeStatus.unknown(config)
    }

    fun applyTermuxRepoStatus(status: TermuxRepoStatus) {
        termuxRepoStatus = status
        if (customTermuxRepoInput.isBlank() || status.label == "自定义") {
            customTermuxRepoInput = status.uri
        }
    }
}
