package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernUploadLimitPolicyTest {
    @Test
    fun `only supported upload limits are accepted`() {
        assertTrue(TavernUploadLimitPolicy.isAllowed(500))
        assertTrue(TavernUploadLimitPolicy.isAllowed(1024))
        assertTrue(TavernUploadLimitPolicy.isAllowed(2048))
        assertFalse(TavernUploadLimitPolicy.isAllowed(null))
        assertFalse(TavernUploadLimitPolicy.isAllowed(0))
        assertFalse(TavernUploadLimitPolicy.isAllowed(4096))
    }

    @Test
    fun `labels make large limits clear`() {
        assertEquals("500MB", TavernUploadLimitPolicy.label(500))
        assertEquals("1GB", TavernUploadLimitPolicy.label(1024))
        assertEquals("2GB", TavernUploadLimitPolicy.label(2048))
    }
}
