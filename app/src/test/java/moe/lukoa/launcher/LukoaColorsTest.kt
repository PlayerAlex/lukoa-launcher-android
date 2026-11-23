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
        assertEquals(Color(0xFF0D0D0D), LukoaColors.Background)
        assertEquals(Color(0xFF161616), LukoaColors.Surface)
        assertEquals(Color(0xFF202020), LukoaColors.Elevated)
        assertEquals(Color(0xFF323232), LukoaColors.Border)
        assertEquals(Color(0xFFEDEDED), LukoaColors.Primary)
        assertEquals(Color(0xFFFFFFFF), LukoaColors.PrimaryHover)
        assertEquals(Color(0xFF111111), LukoaColors.OnPrimary)
        assertEquals(Color(0xFFF2F2F2), LukoaColors.TextPrimary)
        assertEquals(Color(0xFFA6A6A6), LukoaColors.TextSecondary)
        assertEquals(Color(0xFFDCDCDC), LukoaColors.Accent)
        assertEquals(Color(0xFFEB7A7E), LukoaColors.Stop)

        assertNeutralGrayscaleRoles()
        assertReadableContrastForCurrentPalette()
    }

    @Test
    fun daylightPalette_isPureNeutralLightAndHighContrast() {
        LukoaColors.applyStyle(LukoaThemeStyle.Daylight)

        assertEquals(LukoaThemeStyle.Daylight, LukoaColors.currentStyle)
        assertTrue(LukoaColors.currentStyle.isLight)
        assertEquals(Color(0xFFF2F2F2), LukoaColors.Background)
        assertEquals(Color(0xFFFFFFFF), LukoaColors.Surface)
        assertEquals(Color(0xFFF8F8F8), LukoaColors.Elevated)
        assertEquals(Color(0xFFDDDDDD), LukoaColors.Border)
        assertEquals(Color(0xFF1A1A1A), LukoaColors.Primary)
        assertEquals(Color(0xFFFFFFFF), LukoaColors.OnPrimary)
        assertEquals(Color(0xFF141414), LukoaColors.TextPrimary)
        assertEquals(Color(0xFF5C5C5C), LukoaColors.TextSecondary)
        assertTrue(LukoaColors.Background.luminance() >= 0.85f)
        assertTrue(LukoaColors.TextPrimary.luminance() <= 0.05f)

        assertNeutralGrayscaleRoles()
        assertReadableContrastForCurrentPalette()
    }

    @Test
    fun materialTheme_keepsTheSurfaceHierarchyForBothStyles() {
        LukoaThemeStyle.entries.forEach { style ->
            LukoaColors.applyStyle(style)
            val scheme = lukoaColorScheme()
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
        assertEquals(LukoaThemeStyle.Daylight, LukoaThemeStyle.Monochrome.toggled())
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.Daylight.toggled())
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue(null))
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue("unknown"))
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue("classic_teal"))
        assertEquals(LukoaThemeStyle.Monochrome, LukoaThemeStyle.fromWireValue("monochrome"))
        assertEquals(LukoaThemeStyle.Daylight, LukoaThemeStyle.fromWireValue("daylight"))
    }

    private fun assertNeutralGrayscaleRoles() {
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
            assertEquals("纯正中性黑白不应偏冷蓝也不应偏黄褐", color.red, color.green, 0.0001f)
            assertEquals("纯正中性黑白不应偏冷蓝也不应偏黄褐", color.green, color.blue, 0.0001f)
        }
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
