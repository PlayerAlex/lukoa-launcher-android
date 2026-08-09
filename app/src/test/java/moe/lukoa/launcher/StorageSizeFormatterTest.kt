package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSizeFormatterTest {
    @Test
    fun `formats user and extension directory sizes consistently`() {
        assertEquals("0 KB", formatStorageKilobytes(-1L))
        assertEquals("1023 KB", formatStorageKilobytes(1023L))
        assertEquals("1.0 MB", formatStorageKilobytes(1024L))
        assertEquals("1.5 MB", formatStorageKilobytes(1536L))
        assertEquals("1.0 GB", formatStorageKilobytes(1024L * 1024L))
    }
}
