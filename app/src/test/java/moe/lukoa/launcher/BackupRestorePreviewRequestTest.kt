package moe.lukoa.launcher

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestorePreviewRequestTest {
    @Test
    fun `new request replaces old request`() {
        val coordinator = BackupRestorePreviewRequestCoordinator()
        val oldRequest = coordinator.begin("/storage/old.tar.gz")
        val newRequest = coordinator.begin("/storage/new.tar.gz")

        assertFalse(coordinator.accepts(oldRequest, "/storage/old.tar.gz"))
        assertTrue(coordinator.accepts(newRequest, "/storage/new.tar.gz"))
    }

    @Test
    fun `old path preview is rejected after selection changes`() {
        val coordinator = BackupRestorePreviewRequestCoordinator()
        val request = coordinator.begin("/storage/old.tar.gz")

        assertFalse(coordinator.accepts(request, "/storage/new.tar.gz"))
    }

    @Test
    fun `cancelled request cannot enter confirmation flow`() {
        val coordinator = BackupRestorePreviewRequestCoordinator()
        val request = coordinator.begin("/storage/backup.tar.gz")

        coordinator.cancel()

        assertFalse(coordinator.accepts(request, "/storage/backup.tar.gz"))
    }

    @Test
    fun `preview time and size formatting stay stable`() {
        assertEquals(
            "2026-07-10 00:00:00",
            formatBackupRestorePreviewTime(1_783_641_600_000L, ZoneId.of("UTC")),
        )
        assertEquals("1.5 KB", formatBackupRestorePreviewSize(1536L))
        assertEquals("未读取", formatBackupRestorePreviewTime(null, ZoneId.of("UTC")))
        assertEquals("未读取", formatBackupRestorePreviewSize(null))
    }
}
