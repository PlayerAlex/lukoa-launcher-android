package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBackupUiPolicyTest {
    @Test
    fun `blank backup selection returns action specific message`() {
        val selection = LauncherBackupUiPolicy.selectArchive("   ", "删除")

        assertFalse(selection.isValid)
        assertEquals("没有选中要删除的备份。", selection.errorMessage)
    }

    @Test
    fun `valid backup selection trims path`() {
        val selection = LauncherBackupUiPolicy.selectArchive(
            "  /storage/emulated/0/Download/LukoaLauncher/backups/sd/demo.tar.gz  ",
            "复制",
        )

        assertTrue(selection.isValid)
        assertEquals(
            "/storage/emulated/0/Download/LukoaLauncher/backups/sd/demo.tar.gz",
            selection.normalizedPath,
        )
        assertNull(selection.errorMessage)
    }

    @Test
    fun `rename defaults remove archive suffix and limit length`() {
        val longName = "a".repeat(60)

        assertEquals("更新前", LauncherBackupUiPolicy.defaultRenameName("/backup/更新前.tar.gz"))
        assertEquals(48, LauncherBackupUiPolicy.defaultRenameName("/backup/$longName.tar.gz").length)
    }

    @Test
    fun `rename duplicate ignores selected archive itself`() {
        val selected = "/backup/old.tar.gz"
        val history = listOf(selected, "/backup/target.tar.gz")

        assertEquals(
            "/backup/target.tar.gz",
            LauncherBackupUiPolicy.duplicateRenamePath(selected, "target", history),
        )
        assertNull(LauncherBackupUiPolicy.duplicateRenamePath(selected, "old", history))
    }
}
