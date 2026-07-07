package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernPathConfigTest {
    @Test
    fun `add suggested profile creates editable clone slot`() {
        val config = TavernPathConfig().addSuggestedProfile()

        assertEquals(2, config.availableProfiles.size)
        assertEquals("profile-2", config.activeProfile.id)
        assertEquals("~/LukoaLauncher/SillyTavern2", config.activeProfile.tavernDir)
        assertEquals(8001, config.activeProfile.port)
    }

    @Test
    fun `restore active profile default keeps active slot but resets path and port`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "/data/data/com.termux/files/home/custom-clone",
                port = 9005,
            )

        val restored = config.restoreActiveProfileDefault()

        assertEquals("profile-2", restored.activeProfile.id)
        assertEquals("~/LukoaLauncher/SillyTavern2", restored.activeProfile.tavernDir)
        assertEquals(8001, restored.activeProfile.port)
        assertTrue(restored.isActiveProfileDefault)
    }

    @Test
    fun `remove active profile falls back to remaining profile`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .removeProfile("profile-2")

        assertEquals(1, config.availableProfiles.size)
        assertEquals("main", config.activeProfile.id)
        assertFalse(config.hasMultipleProfiles)
    }

    @Test
    fun `main profile is restored when legacy config no longer contains it`() {
        val config = TavernPathConfig(
            activeProfileId = "profile-2",
            profiles = listOf(
                TavernProfile(
                    id = "profile-2",
                    name = "分身实例",
                    tavernDir = "/data/data/com.termux/files/home/clone-only",
                    port = 9002,
                ),
            ),
        )

        assertEquals(1, config.availableProfiles.size)
        assertEquals("main", config.activeProfile.id)
        assertEquals("主实例", config.activeProfile.normalizedName)
        assertEquals("/data/data/com.termux/files/home/clone-only", config.activeProfile.tavernDir)
        assertEquals(9002, config.activeProfile.port)
    }

    @Test
    fun `remove profile ignores attempts to delete main profile`() {
        val config = TavernPathConfig()
            .addSuggestedProfile(makeActive = false)
            .removeProfile("main")

        assertEquals(2, config.availableProfiles.size)
        assertEquals("main", config.activeProfile.id)
        assertTrue(config.availableProfiles.any { it.id == "profile-2" })
    }
}
