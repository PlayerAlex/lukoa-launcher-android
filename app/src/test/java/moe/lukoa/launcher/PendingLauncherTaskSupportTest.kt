package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingLauncherTaskSupportTest {
    @Test
    fun `default tab maps backup and restore to backup tab`() {
        assertEquals(
            LauncherTab.Backup,
            PendingLauncherTaskSupport.defaultTab(
                PendingLauncherTask(
                    kind = PendingLauncherTaskKind.ManualBackup,
                    commandName = "tavern-backup",
                    detail = "",
                    startedAtMillis = 1L,
                ),
            ),
        )
        assertEquals(
            LauncherTab.Backup,
            PendingLauncherTaskSupport.defaultTab(
                PendingLauncherTask(
                    kind = PendingLauncherTaskKind.RestoreBackup,
                    commandName = "tavern-restore",
                    detail = "",
                    startedAtMillis = 1L,
                ),
            ),
        )
    }

    @Test
    fun `default tab maps version tasks to version tab`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.UpdateTavern,
            commandName = "tavern-update",
            detail = "",
            startedAtMillis = 1L,
        )
        assertEquals(LauncherTab.Version, PendingLauncherTaskSupport.defaultTab(task))
    }

    @Test
    fun `safety backup waiting stage does not request extra refresh`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.UpdateTavern,
            commandName = "tavern-backup",
            detail = "正在自动创建安全备份",
            startedAtMillis = 1L,
        )
        val targets = PendingLauncherTaskSupport.waitingRefreshTargets(task)
        assertFalse(targets.backupList)
        assertFalse(targets.startupState)
        assertTrue(PendingLauncherTaskSupport.waitingMessage(task).contains("自动安全备份"))
    }

    @Test
    fun `successful safety backup result reports backup path and refreshes backup list`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RollbackTavern,
            commandName = "tavern-backup",
            detail = "正在自动创建安全备份",
            startedAtMillis = 1L,
        )
        val latest = TermuxResultDisplay(
            key = "k1",
            command = "tavern-backup",
            ok = true,
            output = """
                ==== SillyTavern backup ====
                archive=/storage/emulated/0/Download/LukoaLauncher/backups/sd/safe.tar.gz
            """.trimIndent(),
        )

        val result = PendingLauncherTaskSupport.resolveLatestResult(task, latest)
        assertTrue(result.ok)
        assertTrue(result.message.contains("自动安全备份已经生成"))
        assertTrue(result.refreshTargets.backupList)
        assertFalse(result.refreshTargets.startupState)
    }

    @Test
    fun `completed restore keeps startup and backup refresh`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RestoreBackup,
            commandName = "tavern-restore",
            detail = "正在应用酒馆备份",
            startedAtMillis = 1L,
            safetyBackupPath = "/safe/backup.tar.gz",
        )
        val latest = TermuxResultDisplay(
            key = "k2",
            command = "tavern-restore",
            ok = true,
            output = "restore.completed=1",
        )

        val result = PendingLauncherTaskSupport.resolveLatestResult(task, latest)
        assertTrue(result.ok)
        assertTrue(result.message.contains("自动安全备份已保留"))
        assertTrue(result.refreshTargets.backupList)
        assertTrue(result.refreshTargets.startupState)
    }

    @Test
    fun `latest result ignores same command from another profile`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RestoreBackup,
            commandName = "tavern-restore",
            detail = "正在应用酒馆备份",
            startedAtMillis = 1L,
            profileId = "profile-2",
        )
        val latest = TermuxResultDisplay(
            key = "k3",
            command = "tavern-restore",
            ok = true,
            output = """{"profileId":"main"}""",
            timeMillis = 2L,
            profileId = "main",
        )

        assertNull(PendingLauncherTaskSupport.latestResult(task, listOf(latest)))
    }

    @Test
    fun `latest result accepts matching runtime state dir for pending profile`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RestoreBackup,
            commandName = "tavern-restore",
            detail = "正在应用酒馆备份",
            startedAtMillis = 1L,
            profileId = "profile-2",
        )
        val latest = TermuxResultDisplay(
            key = "k4",
            command = "tavern-restore",
            ok = true,
            output = "runtime_state_dir=/data/data/com.termux/files/home/.local/state/lukoa-launcher/profiles/profile-2",
            timeMillis = 2L,
            runtimeStateDir = "/data/data/com.termux/files/home/.local/state/lukoa-launcher/profiles/profile-2",
        )

        assertEquals(latest, PendingLauncherTaskSupport.latestResult(task, listOf(latest)))
    }

    @Test
    fun `latest result ignores matching command received before task started`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.ManualBackup,
            commandName = "tavern-backup",
            detail = "正在创建酒馆备份",
            startedAtMillis = 100L,
        )
        val stale = TermuxResultDisplay(
            key = "stale",
            command = "tavern-backup",
            output = "archive=/old.tar.gz",
            ok = true,
            timeMillis = 99L,
        )

        assertNull(PendingLauncherTaskSupport.latestResult(task, listOf(stale)))
    }

    @Test
    fun `latest result searches history past unrelated newest result`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RestoreBackup,
            commandName = "tavern-restore",
            detail = "正在应用酒馆备份",
            startedAtMillis = 100L,
        )
        val unrelated = TermuxResultDisplay(
            key = "newest",
            command = "status",
            output = "running=false",
            ok = true,
            timeMillis = 120L,
        )
        val matching = TermuxResultDisplay(
            key = "matching",
            command = "tavern-restore",
            output = "restore.completed=1",
            ok = true,
            timeMillis = 110L,
        )

        assertEquals(
            matching,
            PendingLauncherTaskSupport.latestResult(task, listOf(unrelated, matching)),
        )
    }
}
