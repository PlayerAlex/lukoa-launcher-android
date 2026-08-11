package moe.lukoa.launcher

enum class TermuxCommandPresentation(val runsInBackground: Boolean) {
    Background(true),
    Foreground(false),
}

object TermuxCommandPresentationPolicy {
    private val backgroundCommands = setOf(
        "selftest",
        "install-script",
        "log",
        "status",
        "start",
        "stop",
        "tavern-version",
        "tavern-doctor",
        "tavern-repair-dependencies",
        "tavern-reset-theme",
        "tavern-node-memory",
        "tavern-upload-limit-status",
        "tavern-upload-limit-set",
        "tavern-upload-limit-reset",
        "tavern-users-list",
        "tavern-extensions-list",
        "tavern-extensions-check-updates",
        "tavern-official-versions",
        "termux-repo-status",
        "tavern-backup",
        "tavern-backup-manual",
        "tavern-backup-auto",
        "tavern-backup-list",
        "return-launcher",
    )

    fun forCommand(displayCommand: String): TermuxCommandPresentation {
        return if (displayCommand in backgroundCommands) {
            TermuxCommandPresentation.Background
        } else {
            TermuxCommandPresentation.Foreground
        }
    }
}
