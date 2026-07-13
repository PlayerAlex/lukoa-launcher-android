package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupCommandCodecTest {
    @Test
    fun `rename args round trip keeps spaces and chinese`() {
        val encoded = BackupCommandCodec.encodeRename(
            archivePath = "/sdcard/Download/LukoaLauncher/backups/sd/demo backup.tar.gz",
            newName = "更新前备份",
        )

        val decoded = BackupCommandCodec.decodeRename(encoded)
        requireNotNull(decoded)
        assertEquals("/sdcard/Download/LukoaLauncher/backups/sd/demo backup.tar.gz", decoded.archivePath)
        assertEquals("更新前备份", decoded.newName)
    }

    @Test
    fun `export args round trip keeps destination`() {
        val encoded = BackupCommandCodec.encodeExportTo(
            archivePath = "/data/data/com.termux/files/home/a.tar.gz",
            destinationPath = "/storage/emulated/0/Download/lukoa/out.tar.gz",
        )

        val decoded = BackupCommandCodec.decodeExportTo(encoded)
        requireNotNull(decoded)
        assertEquals("/data/data/com.termux/files/home/a.tar.gz", decoded.archivePath)
        assertEquals("/storage/emulated/0/Download/lukoa/out.tar.gz", decoded.destinationPath)
    }

    @Test
    fun `invalid encoded payload returns null`() {
        assertNull(BackupCommandCodec.decodeRename("broken"))
        assertNull(BackupCommandCodec.decodeExportTo("still-broken"))
        assertNull(BackupCommandCodec.decodeRename(null))
    }
}
