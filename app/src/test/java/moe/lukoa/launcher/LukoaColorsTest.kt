package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LukoaColorsTest {
    @Test
    fun palette_exposesTheRequestedSemanticColors() {
        assertEquals(Color(0xFF071311), LukoaColors.Background)
        assertEquals(Color(0xFF0D1D1A), LukoaColors.Surface)
        assertEquals(Color(0xFF142824), LukoaColors.Elevated)
        assertEquals(Color(0xFF5EE6C1), LukoaColors.Primary)
        assertEquals(Color(0xFF82F2D2), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF071311), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFECFBF6), LukoaColors.TextPrimary)
        assertEquals(Color(0xFF9BBAB2), LukoaColors.TextSecondary)
        assertEquals(Color(0xFF21423B), LukoaColors.Border)
        assertEquals(Color(0xFFFFC857), LukoaColors.Accent)
    }

    @Test
    fun primaryColor_readsAsBrightCoolMintInsteadOfMutedGreen() {
        assertTrue(LukoaColors.Primary.green >= 0.85f)
        assertTrue(LukoaColors.Primary.blue >= 0.72f)
        assertTrue(LukoaColors.Primary.red <= 0.40f)
        assertTrue(LukoaColors.Primary.green - LukoaColors.Primary.red >= 0.45f)
        assertTrue(LukoaColors.Primary.blue - LukoaColors.Primary.red >= 0.30f)
    }

    @Test
    fun stopAction_usesTheClearerSoftRed() {
        assertEquals(Color(0xFFFF7A8B), LukoaColors.Stop)
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
