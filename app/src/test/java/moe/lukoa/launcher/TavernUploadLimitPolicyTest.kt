package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernUploadLimitPolicyTest {
    @Test
    fun `presets and any value inside the range are accepted`() {
        TavernUploadLimitPolicy.presetMegabytes.forEach { assertTrue(TavernUploadLimitPolicy.isAllowed(it)) }
        assertTrue(TavernUploadLimitPolicy.isAllowed(TavernUploadLimitPolicy.MIN_MEGABYTES))
        assertTrue(TavernUploadLimitPolicy.isAllowed(1500))
        assertTrue(TavernUploadLimitPolicy.isAllowed(TavernUploadLimitPolicy.MAX_MEGABYTES))
    }

    @Test
    fun `values outside the range are rejected`() {
        assertFalse(TavernUploadLimitPolicy.isAllowed(null))
        assertFalse(TavernUploadLimitPolicy.isAllowed(0))
        assertFalse(TavernUploadLimitPolicy.isAllowed(TavernUploadLimitPolicy.MIN_MEGABYTES - 1))
        assertFalse(TavernUploadLimitPolicy.isAllowed(TavernUploadLimitPolicy.MAX_MEGABYTES + 1))
    }

    @Test
    fun `labels make large limits clear`() {
        assertEquals("500MB", TavernUploadLimitPolicy.label(500))
        assertEquals("1GB", TavernUploadLimitPolicy.label(1024))
        assertEquals("2GB", TavernUploadLimitPolicy.label(2048))
        assertEquals("1500MB", TavernUploadLimitPolicy.label(1500))
    }

    @Test
    fun `custom input validation explains what is wrong`() {
        assertNull(TavernUploadLimitPolicy.validateCustomInput(" 1500 "))
        assertNotNull(TavernUploadLimitPolicy.validateCustomInput(""))
        assertNotNull(TavernUploadLimitPolicy.validateCustomInput("1.5GB"))
        assertNotNull(TavernUploadLimitPolicy.validateCustomInput("10"))
        assertNotNull(TavernUploadLimitPolicy.validateCustomInput("99999"))
    }
}
