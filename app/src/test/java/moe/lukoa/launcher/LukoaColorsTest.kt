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
    fun defaultMonochromePalette_isWarmNeutralAndHighContrast() {
        LukoaColors.applyStyle(LukoaThemeStyle.Monochrome)

        assertEquals(LukoaThemeStyle.Monochrome, LukoaColors.currentStyle)
        assertEquals(Color(0xFF0E0D0C), LukoaColors.Background)
        assertEquals(Color(0xFF171614), LukoaColors.Surface)
        assertEquals(Color(0xFF211F1C), LukoaColors.Elevated)
        assertEquals(Color(0xFF34312C), LukoaColors.Border)
        assertEquals(Color(0xFFEBE7DF), LukoaColors.Primary)
        assertEquals(Color(0xFFFAF7F2), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF12110F), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFEEEBE5), LukoaColors.TextPrimary)
        assertEquals(Color(0xFFABA69F), LukoaColors.TextSecondary)
        assertEquals(Color(0xFFDBD7D0), LukoaColors.Accent)
        assertEquals(Color(0xFFEB847E), LukoaColors.Stop)

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
            assertTrue("暖调黑白不应偏冷蓝，实际 R=${color.red} G=${color.green} B=${color.blue}", color.red >= color.green && color.green >= color.blue)
            val spread = color.red - color.blue
            assertTrue("暖调黑白应保持中性克制，实际通道差为 $spread", spread <= 0.055f)
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
