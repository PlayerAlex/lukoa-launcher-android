package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
