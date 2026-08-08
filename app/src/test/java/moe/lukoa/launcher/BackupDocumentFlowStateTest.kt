package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDocumentFlowStateTest {
    @Test
    fun `import request and selected uri survive saved state round trip`() {
        val original = BackupDocumentFlowState(
            importPending = true,
            importUri = "content://downloads/backup.tar.gz",
        )

        val values = BackupDocumentFlowStateCodec.encode(original)
        val restored = BackupDocumentFlowStateCodec.decode(values::get)

        assertEquals(original, restored)
        assertFalse(restored.exportPending)
    }

    @Test
    fun `export request keeps source destination name and selected uri`() {
        val original = BackupDocumentFlowState(
            exportSourcePath = "/storage/emulated/0/Download/LukoaLauncher/backups/sd/manual.tar.gz",
            exportFileName = "manual.tar.gz",
            exportUri = "content://documents/export.tar.gz",
        )

        val restored = BackupDocumentFlowStateCodec.decode(
            BackupDocumentFlowStateCodec.encode(original)::get,
        )

        assertEquals(original, restored)
        assertTrue(restored.exportPending)
    }

    @Test
    fun `missing saved values restore an idle flow`() {
        assertEquals(
            BackupDocumentFlowState(),
            BackupDocumentFlowStateCodec.decode { null },
        )
    }
}
