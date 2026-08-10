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
        "tavern-version",
        "tavern-upload-limit-status",
        "tavern-users-list",
        "tavern-extensions-list",
        "tavern-extensions-check-updates",
        "tavern-official-versions",
        "termux-repo-status",
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
