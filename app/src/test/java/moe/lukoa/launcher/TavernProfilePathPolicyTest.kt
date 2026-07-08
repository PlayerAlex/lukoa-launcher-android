package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernProfilePathPolicyTest {
    @Test
    fun `main profile defaults to launcher managed root directory`() {
        val info = TavernProfilePathPolicy.describe(TavernPathConfig().activeProfile)

        assertEquals(TavernProfilePathKind.LauncherManaged, info.kind)
        assertEquals("~/LukoaLauncher/SillyTavern", info.currentDisplayPath)
        assertEquals("~/LukoaLauncher/SillyTavern", info.launcherManagedDefaultDisplayPath)
        assertTrue(info.canMigrateToTraditionalDefault)
        assertFalse(info.canDeleteDirectoryWithProfile)
    }

    @Test
    fun `clone profile defaults to launcher managed clone directory`() {
        val profile = TavernPathConfig().addSuggestedProfile().activeProfile
        val info = TavernProfilePathPolicy.describe(profile)

        assertEquals(TavernProfilePathKind.LauncherManaged, info.kind)
        assertEquals("~/LukoaLauncher/SillyTavern2", info.currentDisplayPath)
        assertTrue(info.canDeleteDirectoryWithProfile)
        assertFalse(info.canMigrateToTraditionalDefault)
    }

    @Test
    fun `traditional default path is recognized separately`() {
        val profile = TavernProfileDefaults.profileForId("main").copy(
            tavernDir = TavernPathDefaults.LEGACY_DEFAULT_TAVERN_DIR,
        )

        val info = TavernProfilePathPolicy.describe(profile)

        assertEquals(TavernProfilePathKind.TraditionalDefault, info.kind)
        assertEquals("~/SillyTavern", info.currentDisplayPath)
    }

    @Test
    fun `absolute launcher managed path is still recognized as launcher managed`() {
        val profile = TavernProfileDefaults.profileForId("main").copy(
            tavernDir = "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern",
        )

        val info = TavernProfilePathPolicy.describe(profile)

        assertEquals(TavernProfilePathKind.LauncherManaged, info.kind)
        assertTrue(TavernProfilePathPolicy.isLauncherManagedPath(profile.tavernDir))
    }

    @Test
    fun `custom path does not auto qualify for directory deletion`() {
        val profile = TavernProfileDefaults.profileForId("profile-2").copy(
            tavernDir = "~/LukoaLauncher/custom-silly",
        )

        val info = TavernProfilePathPolicy.describe(profile)

        assertEquals(TavernProfilePathKind.Custom, info.kind)
        assertFalse(info.canDeleteDirectoryWithProfile)
    }
}
