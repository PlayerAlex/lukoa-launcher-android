package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TavernVersionSourceDisplayTest {
    @Test
    fun `direct github source uses the github name`() {
        assertEquals(
            "GitHub",
            TavernVersionSourceDisplay.label("https://github.com/SillyTavern/SillyTavern.git"),
        )
    }

    @Test
    fun `non github source keeps its real address`() {
        assertEquals(
            "https://mirror.example.com/SillyTavern.git",
            TavernVersionSourceDisplay.label(" https://mirror.example.com/SillyTavern.git "),
        )
    }

    @Test
    fun `missing source stays explicit`() {
        assertEquals("未读取", TavernVersionSourceDisplay.label("unknown"))
    }
}
