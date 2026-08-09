package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernVersionOperationSummaryTest {
    @Test
    fun `parser builds a verified update result with safety backup`() {
        val summary = TavernVersionOperationSummaryParser.parse(
            output = """
                unrelated
                ==== SillyTavern update ====
                directory=/data/data/com.termux/files/home/SillyTavern
                target=release
                before=abc1234
                after=def5678
                exitCode=0
                npmExitCode=0
                ==== end SillyTavern update ====
            """.trimIndent(),
            kind = TavernVersionActionKind.Update,
            safetyBackupPath = "/storage/emulated/0/Lukoa/backup-before-update.tar.gz",
        )

        requireNotNull(summary)
        assertTrue(summary.succeeded)
        assertTrue(summary.revisionChanged)
        assertEquals("abc1234", summary.beforeRevision)
        assertEquals("def5678", summary.afterRevision)
        assertEquals("release", summary.target)
        assertEquals(0, summary.npmExitCode)
        assertEquals("/storage/emulated/0/Lukoa/backup-before-update.tar.gz", summary.safetyBackupPath)
    }

    @Test
    fun `parser keeps failed rollback details and backup entry`() {
        val summary = TavernVersionOperationSummaryParser.parse(
            output = """
                ==== SillyTavern rollback ====
                target=1.12.0
                before=def5678
                after=def5678
                exitCode=74
                npmExitCode=0
                ==== end SillyTavern rollback ====
            """.trimIndent(),
            kind = TavernVersionActionKind.Rollback,
            safetyBackupPath = "/backups/safe.tar.gz",
        )

        requireNotNull(summary)
        assertFalse(summary.succeeded)
        assertFalse(summary.revisionChanged)
        assertEquals(74, summary.exitCode)
        assertEquals("/backups/safe.tar.gz", summary.safetyBackupPath)
    }

    @Test
    fun `parser rejects incomplete mismatched or unsafe result blocks`() {
        assertNull(
            TavernVersionOperationSummaryParser.parse(
                output = "==== SillyTavern update ====\nexitCode=0",
                kind = TavernVersionActionKind.Update,
                safetyBackupPath = "/backup.tar.gz",
            ),
        )
        assertNull(
            TavernVersionOperationSummaryParser.parse(
                output = "==== SillyTavern rollback ====\nexitCode=0\n==== end SillyTavern rollback ====",
                kind = TavernVersionActionKind.Update,
                safetyBackupPath = "/backup.tar.gz",
            ),
        )
        assertNull(
            TavernVersionOperationSummaryParser.parse(
                output = "==== SillyTavern update ====\nexitCode=oops\n==== end SillyTavern update ====",
                kind = TavernVersionActionKind.Update,
                safetyBackupPath = "/backup.tar.gz",
            ),
        )
    }
}
