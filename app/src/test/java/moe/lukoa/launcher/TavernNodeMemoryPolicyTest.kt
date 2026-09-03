package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernNodeMemoryPolicyTest {
    @Test
    fun `presets and any value inside the range are accepted`() {
        TavernNodeMemoryPolicy.presetMegabytes.forEach { assertTrue(TavernNodeMemoryPolicy.isAllowed(it)) }
        assertTrue(TavernNodeMemoryPolicy.isAllowed(TavernNodeMemoryPolicy.MIN_MEGABYTES))
        assertTrue(TavernNodeMemoryPolicy.isAllowed(3072))
        assertTrue(TavernNodeMemoryPolicy.isAllowed(TavernNodeMemoryPolicy.MAX_MEGABYTES))
    }

    @Test
    fun `values outside the range are rejected`() {
        assertFalse(TavernNodeMemoryPolicy.isAllowed(null))
        assertFalse(TavernNodeMemoryPolicy.isAllowed(0))
        assertFalse(TavernNodeMemoryPolicy.isAllowed(TavernNodeMemoryPolicy.MIN_MEGABYTES - 1))
        assertFalse(TavernNodeMemoryPolicy.isAllowed(TavernNodeMemoryPolicy.MAX_MEGABYTES + 1))
    }

    @Test
    fun `labels use gigabytes only for whole gigabytes`() {
        assertEquals("2GB", TavernNodeMemoryPolicy.label(2048))
        assertEquals("6GB", TavernNodeMemoryPolicy.label(6144))
        assertEquals("3000MB", TavernNodeMemoryPolicy.label(3000))
    }

    @Test
    fun `custom input validation explains what is wrong`() {
        assertNull(TavernNodeMemoryPolicy.validateCustomInput("3072"))
        assertNotNull(TavernNodeMemoryPolicy.validateCustomInput(""))
        assertNotNull(TavernNodeMemoryPolicy.validateCustomInput("4GB"))
        assertNotNull(TavernNodeMemoryPolicy.validateCustomInput("100"))
        assertNotNull(TavernNodeMemoryPolicy.validateCustomInput("65536"))
    }
}
