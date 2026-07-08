package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernDirectoryCandidateGuardTest {
    @Test
    fun `candidate already used by another profile is blocked`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val options = TavernDirectoryCandidateGuard.resolve(
            config = config,
            candidates = listOf(
                "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern",
                "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern2",
            ),
        )

        assertEquals(2, options.size)
        assertFalse(options[0].selectable)
        assertEquals("这个目录会一直保留给主实例，分身实例不能直接使用。", options[0].reason)
        assertTrue(options[1].selectable)
    }

    @Test
    fun `absolute termux home path is shown as tilde path`() {
        val option = TavernDirectoryCandidateOption(
            path = "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern2",
            selectable = true,
        )

        assertEquals("~/LukoaLauncher/SillyTavern2", option.displayPath)
    }
}
