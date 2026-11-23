package moe.lukoa.launcher

/**
 * Decides whether a persisted Termux command result carries meaningful state or log updates for
 * the UI. Commands like `foreground-console` or `start` often emit only a status JSON envelope
 * plus a `recent log` block; stripping both leaves `commandText` empty, so relying on
 * `commandText` alone would silently drop crash/exit/error results.
 */
object TermuxResultSyncPolicy {
    fun shouldSync(
        display: TermuxResultDisplay,
        extracted: TermuxDisplayContent,
        currentTermuxLog: String,
    ): Boolean {
        if (display.output.isBlank()) return false
        if (display.command == "log") return false

        val commandSignature = extracted.commandText.take(360)
        if (commandSignature.isNotBlank()) {
            return !currentTermuxLog.contains(commandSignature)
        }
        if (extracted.tavernRuntimeLogText.isNotBlank()) {
            return true
        }
        if (!display.ok) {
            return true
        }
        return inferTavernRunning(display.output) != null ||
            inferTavernStarting(display.output) ||
            inferTavernPortConflict(display.output) ||
            TermuxPermissionSignals.externalAppsBlocked(display.output) ||
            TavernDirectoryCandidateParser.parse(display.output).isNotEmpty()
    }
}
