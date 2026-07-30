package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherPathSettingsStateTest {
    @Test
    fun `rejected save keeps attempted path so user can correct it`() {
        val initialConfig = TavernPathConfig()
        val state = LauncherPathSettingsState(initialConfig)
        state.pathInput = "~/already-used"

        state.applySaveResult(
            TavernPathSaveResult(
                saved = false,
                config = initialConfig,
                message = "实例目录不能重复。",
            ),
        )

        assertEquals("~/already-used", state.pathInput)
    }

    @Test
    fun `rejected save keeps attempted port so user can correct it`() {
        val initialConfig = TavernPathConfig()
        val state = LauncherPathSettingsState(initialConfig)
        state.portInput = "8001"

        state.applySaveResult(
            TavernPathSaveResult(
                saved = false,
                config = initialConfig,
                message = "实例端口不能重复。",
            ),
        )

        assertEquals("8001", state.portInput)
    }
}
