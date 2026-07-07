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
        assertEquals("~/SillyTavern-2", config.activeProfile.tavernDir)
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
        assertEquals("~/SillyTavern-2", restored.activeProfile.tavernDir)
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
}
