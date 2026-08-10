package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TermuxCommandPresentationPolicyTest {
    @Test
    fun `commands that should not leave launcher stay in background`() {
        listOf(
            "selftest",
            "install-script",
            "log",
            "status",
            "start",
            "stop",
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
        ).forEach { command ->
            assertEquals(
                command,
                TermuxCommandPresentation.Background,
                TermuxCommandPresentationPolicy.forCommand(command),
            )
        }
    }

    @Test
    fun `user initiated progress and mutations open in foreground`() {
        listOf(
            "tavern-force-cleanup",
            "tavern-doctor",
            "tavern-repair-dependencies",
            "tavern-install",
            "tavern-update",
            "tavern-rollback",
            "tavern-backup-manual",
            "tavern-backup-delete",
            "tavern-backup-export-to",
            "tavern-backup-import",
            "tavern-restore",
            "tavern-extensions-install",
            "tavern-user-delete",
            "tavern-migrate-dir",
        ).forEach { command ->
            assertEquals(
                command,
                TermuxCommandPresentation.Foreground,
                TermuxCommandPresentationPolicy.forCommand(command),
            )
        }
    }

    @Test
    fun `unknown commands default to foreground instead of silently hiding work`() {
        assertEquals(
            TermuxCommandPresentation.Foreground,
            TermuxCommandPresentationPolicy.forCommand("future-user-operation"),
        )
    }
}
