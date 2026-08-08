package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSizeFormatterTest {
    @Test
    fun `formats user and extension directory sizes consistently`() {
        assertEquals("0KB", formatStorageKilobytes(-1L))
        assertEquals("1023KB", formatStorageKilobytes(1023L))
        assertEquals("1.0MB", formatStorageKilobytes(1024L))
        assertEquals("1.5MB", formatStorageKilobytes(1536L))
        assertEquals("1.0GB", formatStorageKilobytes(1024L * 1024L))
    }
}
