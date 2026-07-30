package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernPathConfigTest {
    @Test
    fun `main profile defaults to traditional SillyTavern directory`() {
        val config = TavernPathConfig()

        assertEquals("~/SillyTavern", config.activeProfile.tavernDir)
        assertEquals("\$HOME/SillyTavern", config.normalizedTavernDir)
        assertTrue(config.isActiveProfileDefault)
    }

    @Test
    fun `existing main profile path is preserved instead of rewritten to new default`() {
        val config = TavernPathConfig(
            tavernDir = "~/LukoaLauncher/SillyTavern",
        )

        assertEquals("~/LukoaLauncher/SillyTavern", config.activeProfile.tavernDir)
        assertEquals("~/LukoaLauncher/SillyTavern", config.displayTavernDir)
        assertFalse(config.isActiveProfileDefault)
    }

    @Test
    fun `blank tavern path is rejected instead of becoming a default path`() {
        assertEquals(
            "酒馆目录不能为空。",
            TavernPathValidator.validate("   "),
        )
    }

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
    fun `absolute traditional path still counts as active profile default`() {
        val config = TavernPathConfig().withUpdatedActiveProfile(
            tavernDir = "/data/data/com.termux/files/home/SillyTavern",
        )

        assertTrue(config.isActiveProfileDefault)
    }

    @Test
    fun `restoring main profile path only uses traditional default and keeps saved port`() {
        val config = TavernPathConfig().withUpdatedActiveProfile(
            tavernDir = "~/custom-main",
            port = 9005,
        )

        val restored = config.restoreActiveProfileDefaultPathOnly()

        assertEquals("~/SillyTavern", restored.activeProfile.tavernDir)
        assertEquals(9005, restored.activeProfile.port)
    }

    @Test
    fun `blank path normalization falls back to traditional default`() {
        assertEquals(
            "\$HOME/SillyTavern",
            TavernPathNormalizer.normalize("   "),
        )
    }

    @Test
    fun `updating active profile path only keeps saved port`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "~/custom-before-detect",
                port = 9005,
            )

        val updated = config.withUpdatedActiveProfilePathOnly(
            tavernDir = "~/detected-dir",
        )

        assertEquals("~/detected-dir", updated.activeProfile.tavernDir)
        assertEquals(9005, updated.activeProfile.port)
    }

    @Test
    fun `updating active profile port only keeps saved path`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "~/custom-clone",
                port = 9005,
            )

        val updated = config.withUpdatedActiveProfilePortOnly(port = 9010)

        assertEquals("~/custom-clone", updated.activeProfile.tavernDir)
        assertEquals(9010, updated.activeProfile.port)
    }

    @Test
    fun `restoring active profile path only keeps saved port`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "~/custom-clone",
                port = 9005,
            )

        val restored = config.restoreActiveProfileDefaultPathOnly()

        assertEquals("~/LukoaLauncher/SillyTavern2", restored.activeProfile.tavernDir)
        assertEquals(9005, restored.activeProfile.port)
    }

    @Test
    fun `restoring active profile port only keeps saved path`() {
        val config = TavernPathConfig()
            .addSuggestedProfile()
            .withUpdatedActiveProfile(
                tavernDir = "~/custom-clone",
                port = 9005,
            )

        val restored = config.restoreActiveProfileDefaultPortOnly()

        assertEquals("~/custom-clone", restored.activeProfile.tavernDir)
        assertEquals(8001, restored.activeProfile.port)
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
