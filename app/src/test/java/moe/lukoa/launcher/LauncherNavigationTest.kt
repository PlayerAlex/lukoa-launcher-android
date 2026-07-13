package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherNavigationTest {
    @Test
    fun `busy elapsed time always uses minute and second format`() {
        assertEquals("00:00", formatBusyElapsed(0))
        assertEquals("00:42", formatBusyElapsed(42))
        assertEquals("02:05", formatBusyElapsed(125))
    }
}
