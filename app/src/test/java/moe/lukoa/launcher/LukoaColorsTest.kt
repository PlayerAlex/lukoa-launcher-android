package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class LukoaColorsTest {
    @Test
    fun palette_exposesTheRequestedSemanticColors() {
        assertEquals(Color(0xFF0D1412), LukoaColors.Background)
        assertEquals(Color(0xFF151D1A), LukoaColors.Surface)
        assertEquals(Color(0xFF1B2622), LukoaColors.Elevated)
        assertEquals(Color(0xFF6DB5A4), LukoaColors.Primary)
        assertEquals(Color(0xFF8AC9B8), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF0D1412), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFE3EDE8), LukoaColors.TextPrimary)
        assertEquals(Color(0xFF8DA39A), LukoaColors.TextSecondary)
        assertEquals(Color(0xFF25332E), LukoaColors.Border)
        assertEquals(Color(0xFFE8B86D), LukoaColors.Accent)
    }

    @Test
    fun stopAction_keepsPreviousSoftRed() {
        assertEquals(Color(0xFFFB7185), LukoaColors.Stop)
    }
}
