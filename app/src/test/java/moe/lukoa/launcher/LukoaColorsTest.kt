package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class LukoaColorsTest {
    @Test
    fun normalAccent_keepsOriginalMintPalette() {
        assertEquals(Color(0xFF2DD4BF), LukoaColors.Accent)
        assertEquals(Color(0xFF0E332E), LukoaColors.AccentSoft)
        assertEquals(LukoaColors.Accent, LukoaColors.Info)
        assertEquals(LukoaColors.AccentSoft, LukoaColors.InfoSoft)
    }

    @Test
    fun stopAction_keepsPreviousSoftRed() {
        assertEquals(Color(0xFFFB7185), LukoaColors.Stop)
    }
}
