package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxCommandTimeoutPolicyTest {
    @Test
    fun `long task timeouts preserve existing wait windows`() {
        assertEquals(15 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-install"))
        assertEquals(10 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("tavern-update"))
        assertEquals(20 * 60_000L, TermuxCommandTimeoutPolicy.timeoutMillis("termux-bootstrap"))
    }

    @Test
    fun `all controller routes with special behavior have explicit timeouts`() {
        val routedCommands = listOf(
            "log",
            "status",
            "stop",
            "tavern-force-cleanup",
            "tavern-version",
            "tavern-version-startup",
            "tavern-doctor",
            "tavern-official-versions",
            "termux-storage-permission",
            "termux-repo-status",
            "termux-repo",
            "termux-repo-custom",
            "termux-bootstrap",
            "tavern-install",
            "tavern-update",
            "tavern-rollback",
            "tavern-backup",
            "tavern-backup-manual",
            "tavern-backup-auto",
            "tavern-backup-list",
            "tavern-backup-delete",
            "tavern-backup-export",
            "tavern-backup-export-to",
            "tavern-backup-copy",
            "tavern-backup-import",
            "tavern-backup-rename",
            "tavern-restore",
            "tavern-migrate-dir",
            "tavern-delete-managed-profile-dir",
        )

        routedCommands.forEach { command ->
            assertTrue("missing explicit timeout for $command", TermuxCommandTimeoutPolicy.hasExplicitTimeout(command))
        }
    }

    @Test
    fun `persistent operation lock outlives result wait`() {
        val commands = listOf(
            "tavern-install",
            "tavern-update",
            "tavern-rollback",
            "tavern-backup-manual",
            "tavern-restore",
            "termux-bootstrap",
        )

        commands.forEach { command ->
            assertTrue(
                "operation lock must not expire before result wait for $command",
                TermuxCommandTimeoutPolicy.operationLockMillis(command) >
                    TermuxCommandTimeoutPolicy.timeoutMillis(command),
            )
        }
        assertEquals(
            20 * 60_000L + TermuxCommandTimeoutPolicy.OPERATION_LOCK_GRACE_MS,
            TermuxCommandTimeoutPolicy.chainedOperationLockMillis(
                "tavern-backup-manual",
                "tavern-update",
            ),
        )
    }

    @Test
    fun `unknown command keeps the safe default timeout`() {
        assertEquals(
            TermuxCommandTimeoutPolicy.DEFAULT_TIMEOUT_MS,
            TermuxCommandTimeoutPolicy.timeoutMillis("unknown"),
        )
    }
}
