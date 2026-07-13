package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernComparablePathTest {
    @Test
    fun `normalize collapses dot segments and duplicate separators`() {
        assertEquals(
            "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern",
            TavernComparablePath.normalize("~/LukoaLauncher/../LukoaLauncher//SillyTavern/"),
        )
    }

    @Test
    fun `home aliases still compare as same directory after normalization`() {
        assertTrue(
            TavernComparablePath.same(
                "\$HOME/LukoaLauncher/./SillyTavern",
                "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern",
            ),
        )
    }
}
