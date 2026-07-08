package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingManualBackupRecoveryTest {
    @Test
    fun `custom named backup can be recovered by expected label`() {
        val result = PendingManualBackupRecovery.recover(
            startedAtMillis = 1_000_000L,
            expectedLabel = "我的备份",
            archives = listOf(
                archive("我的备份.tar.gz", "/storage/emulated/0/Download/LukoaLauncher/backups/sd/我的备份.tar.gz", 1_020_000L),
                archive("zd-20260708.tar.gz", "/storage/emulated/0/Download/LukoaLauncher/backups/zd/zd-20260708.tar.gz", 1_030_000L),
            ),
        )

        assertEquals("我的备份.tar.gz", result?.fileName)
    }

    @Test
    fun `single recent manual backup can be recovered without label`() {
        val result = PendingManualBackupRecovery.recover(
            startedAtMillis = 2_000_000L,
            expectedLabel = "",
            archives = listOf(
                archive("sd-20260708.tar.gz", "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-20260708.tar.gz", 2_010_000L),
            ),
        )

        assertEquals("/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-20260708.tar.gz", result?.archivePath)
    }

    @Test
    fun `multiple recent manual backups without label are not guessed`() {
        val result = PendingManualBackupRecovery.recover(
            startedAtMillis = 3_000_000L,
            expectedLabel = "",
            archives = listOf(
                archive("sd-1.tar.gz", "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-1.tar.gz", 3_010_000L),
                archive("sd-2.tar.gz", "/storage/emulated/0/Download/LukoaLauncher/backups/sd/sd-2.tar.gz", 3_020_000L),
            ),
        )

        assertNull(result)
    }

    private fun archive(fileName: String, path: String, modifiedAtMillis: Long): BackupLibraryArchiveDetails {
        return BackupLibraryArchiveDetails(
            fileName = fileName,
            termuxReadablePath = path,
            size = 123L,
            modifiedAtMillis = modifiedAtMillis,
        )
    }
}
