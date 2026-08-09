package moe.lukoa.launcher

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherModuleBoundaryTest {
    private val settingsSource by lazy {
        File("src/main/java/moe/lukoa/launcher/LauncherSettingsSection.kt").readText(Charsets.UTF_8)
    }
    private val screenSource by lazy {
        File("src/main/java/moe/lukoa/launcher/LukoaLauncherScreen.kt").readText(Charsets.UTF_8)
    }
    private val commonControlsSource by lazy {
        File("src/main/java/moe/lukoa/launcher/LauncherCommonControls.kt").readText(Charsets.UTF_8)
    }

    @Test
    fun `settings dialogs share one saveable destination`() {
        assertTrue(settingsSource.contains("enum class SettingsDialogDestination"))
        assertTrue(settingsSource.contains("var activeDialog by rememberSaveable"))
        assertFalse(Regex("var show[A-Za-z]+Dialog by").containsMatchIn(settingsSource))
    }

    @Test
    fun `screen does not recreate shallow profile coordinator forwarding functions`() {
        assertTrue(screenSource.contains("profileCoordinator::saveTavernDirectory"))
        assertTrue(screenSource.contains("profileCoordinator::requestMigrateToManagedTavernPath"))
        assertFalse(Regex("fun [A-Za-z]+\\([^)]*\\) = profileCoordinator\\.").containsMatchIn(screenSource))
    }

    @Test
    fun `info icon keeps a large touch target without a rectangular indication`() {
        assertTrue(commonControlsSource.contains("sizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
        assertTrue(commonControlsSource.contains("indication = null"))
        assertTrue(commonControlsSource.contains("modifier = Modifier.size(20.dp)"))
    }
}
