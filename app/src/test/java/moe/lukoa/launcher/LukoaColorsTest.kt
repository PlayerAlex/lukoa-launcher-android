package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun materialTheme_keepsTheSixtyThirtyTenSurfaceHierarchy() {
        assertEquals(LukoaColors.Background, LukoaDarkColorScheme.background)
        assertEquals(LukoaColors.Background, LukoaDarkColorScheme.surfaceContainerLowest)
        assertEquals(LukoaColors.Surface, LukoaDarkColorScheme.surfaceContainerLow)
        assertEquals(LukoaColors.Surface, LukoaDarkColorScheme.surfaceContainer)
        assertEquals(LukoaColors.Elevated, LukoaDarkColorScheme.surfaceContainerHigh)
        assertEquals(LukoaColors.Elevated, LukoaDarkColorScheme.surfaceContainerHighest)
        assertEquals(LukoaColors.Primary, LukoaDarkColorScheme.primary)
        assertEquals(LukoaColors.Accent, LukoaDarkColorScheme.tertiary)
    }

    @Test
    fun textAndActionColors_keepReadableContrast() {
        listOf(
            LukoaColors.Background,
            LukoaColors.Surface,
            LukoaColors.Elevated,
        ).forEach { background ->
            assertTrue(contrastRatio(LukoaColors.TextPrimary, background) >= 4.5f)
            assertTrue(contrastRatio(LukoaColors.TextSecondary, background) >= 4.5f)
        }
        assertTrue(contrastRatio(LukoaColors.OnPrimary, LukoaColors.Primary) >= 4.5f)
        assertTrue(contrastRatio(LukoaColors.Accent, LukoaColors.Background) >= 4.5f)
        assertTrue(contrastRatio(LukoaColors.Primary, LukoaColors.PrimarySoft) >= 4.5f)
        assertTrue(contrastRatio(LukoaColors.Accent, LukoaColors.AccentSoft) >= 4.5f)
        assertTrue(contrastRatio(LukoaColors.Danger, LukoaColors.DangerSoft) >= 4.5f)
        assertTrue(contrastRatio(LukoaColors.Stop, LukoaColors.Background) >= 4.5f)
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
