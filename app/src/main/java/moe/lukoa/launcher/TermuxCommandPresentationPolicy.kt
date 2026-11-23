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
        "restart",
        "tavern-chat-guard-status",
        "tavern-chat-guard-set",
        "tavern-chat-guard-clear-history",
        "tavern-config-read",
        "tavern-config-set",
        "tavern-storage-scan",
        "tavern-storage-clean",
        "tavern-server-plugins-list",
        "tavern-server-plugins-install",
        "tavern-server-plugins-update",
        "tavern-server-plugins-delete",
        "tavern-git-stash-list",
        "tavern-git-stash-apply",
        "tavern-git-stash-drop",
        "tavern-quick-import-paths",
        "tavern-quick-import",
        "tavern-quick-import-batch",
        "tavern-proxy-test",
        "tavern-version",
        "tavern-doctor",
        "tavern-repair-dependencies",
        "tavern-reset-theme",
        "tavern-node-memory",
        "tavern-node-memory-status",
        "tavern-node-memory-reset",
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
        "tavern-backup-manual-data",
        "tavern-backup-auto",
        "tavern-backup-list",
        "return-launcher",
        "wake",
    )

    fun forCommand(displayCommand: String): TermuxCommandPresentation {
        return if (displayCommand in backgroundCommands) {
            TermuxCommandPresentation.Background
        } else {
            TermuxCommandPresentation.Foreground
        }
    }
}
