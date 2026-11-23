package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LukoaColorsTest {
    @After
    fun restoreDefaultThemeStyle() {
        LukoaColors.applyStyle(LukoaThemeStyle.Monochrome)
    }

    @Test
    fun defaultMonochromePalette_isPureNeutralAndHighContrast() {
        LukoaColors.applyStyle(LukoaThemeStyle.Monochrome)

        assertEquals(LukoaThemeStyle.Monochrome, LukoaColors.currentStyle)
        assertEquals(Color(0xFF09090B), LukoaColors.Background)
        assertEquals(Color(0xFF121215), LukoaColors.Surface)
        assertEquals(Color(0xFF1B1B20), LukoaColors.Elevated)
        assertEquals(Color(0xFF2D2D35), LukoaColors.Border)
        assertEquals(Color(0xFFE4E4E9), LukoaColors.Primary)
        assertEquals(Color(0xFFFFFFFF), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF09090B), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFE8E8EC), LukoaColors.TextPrimary)
        assertEquals(Color(0xFF9C9CA6), LukoaColors.TextSecondary)
        assertEquals(Color(0xFFD2D2DA), LukoaColors.Accent)
        assertEquals(Color(0xFFEA7A82), LukoaColors.Stop)

        listOf(
            LukoaColors.Background,
            LukoaColors.Surface,
            LukoaColors.Elevated,
            LukoaColors.Border,
            LukoaColors.Primary,
            LukoaColors.Accent,
            LukoaColors.TextPrimary,
            LukoaColors.TextSecondary,
        ).forEach { color ->
            val spread = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            assertTrue("极简黑白配色不应出现偏色，实际通道差为 $spread", spread <= 0.05f)
        }
        assertReadableContrastForCurrentPalette()
    }

    @Test
    fun classicTealPalette_preservesOriginalCoolSemanticColors() {
        LukoaColors.applyStyle(LukoaThemeStyle.ClassicTeal)

        assertEquals(Color(0xFF071311), LukoaColors.Background)
        assertEquals(Color(0xFF0D1D1A), LukoaColors.Surface)
        assertEquals(Color(0xFF142824), LukoaColors.Elevated)
        assertEquals(Color(0xFF4EB89F), LukoaColors.Primary)
        assertEquals(Color(0xFF66CBB1), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF071311), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFCADAD5), LukoaColors.TextPrimary)
        assertEquals(Color(0xFF8FA9A2), LukoaColors.TextSecondary)
        assertEquals(Color(0xFF21423B), LukoaColors.Border)
        assertEquals(Color(0xFFFFC857), LukoaColors.Accent)
        assertEquals(Color(0xFFFF7A8B), LukoaColors.Stop)

        assertTrue(LukoaColors.Primary.luminance() in 0.34f..0.45f)
        assertTrue(LukoaColors.Primary.green in 0.70f..0.78f)
        assertTrue(LukoaColors.Primary.blue >= 0.60f)
        assertTrue(LukoaColors.Primary.red <= 0.34f)
        assertTrue(LukoaColors.Primary.green - LukoaColors.Primary.red >= 0.40f)
        assertTrue(LukoaColors.Primary.blue - LukoaColors.Primary.red >= 0.30f)
        assertTrue(LukoaColors.TextPrimary.luminance() in 0.60f..0.75f)
        assertTrue(LukoaColors.TextSecondary.luminance() <= 0.42f)
        assertReadableContrastForCurrentPalette()
    }

    @Test
    fun materialTheme_keepsTheSixtyThirtyTenSurfaceHierarchyForBothStyles() {
        LukoaThemeStyle.entries.forEach { style ->
            LukoaColors.applyStyle(style)
            val scheme = lukoaDarkColorScheme()
            assertEquals(LukoaColors.Background, scheme.background)
            assertEquals(LukoaColors.Background, scheme.surfaceContainerLowest)
            assertEquals(LukoaColors.Surface, scheme.surfaceContainerLow)
            assertEquals(LukoaColors.Surface, scheme.surfaceContainer)
            assertEquals(LukoaColors.Elevated, scheme.surfaceContainerHigh)
            assertEquals(LukoaColors.Elevated, scheme.surfaceContainerHighest)
            assertEquals(LukoaColors.Primary, scheme.primary)
            assertEquals(LukoaColors.Accent, scheme.tertiary)
        }
    }

    @Test
    fun themeStyle_togglesAndParsesWireValues() {
        assertEquals(LukoaThemeStyle.ClassicTeal, LukoaThemeStyle.Monochrome.toggled())
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.ClassicTeal.toggled())
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue(null))
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue("unknown"))
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue("monochrome"))
        assertEquals(LukoaThemeStyle.ClassicTeal, LukoaThemeStyle.fromWireValue("classic_teal"))
    }

    private fun assertReadableContrastForCurrentPalette() {
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
