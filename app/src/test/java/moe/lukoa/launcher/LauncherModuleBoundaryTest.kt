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
    private val backupCoordinatorSource by lazy {
        File("src/main/java/moe/lukoa/launcher/LauncherBackupCoordinator.kt").readText(Charsets.UTF_8)
    }
    private val backupSectionSource by lazy {
        File("src/main/java/moe/lukoa/launcher/BackupSection.kt").readText(Charsets.UTF_8)
    }
    private val launcherPanelsSource by lazy {
        File("src/main/java/moe/lukoa/launcher/LauncherPanels.kt").readText(Charsets.UTF_8)
    }
    private val repairToolsSource by lazy {
        File("src/main/java/moe/lukoa/launcher/RepairToolsSection.kt").readText(Charsets.UTF_8)
    }
    private val githubUpdateStoreSource by lazy {
        File("src/main/java/moe/lukoa/launcher/GithubUpdateStore.kt").readText(Charsets.UTF_8)
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
    fun `continuous polling is limited to a visible lifecycle`() {
        val lifecycleGates = Regex(
            "repeatOnLifecycle\\(Lifecycle\\.State\\.STARTED\\)",
        ).findAll(screenSource).count()

        assertTrue("日志和结果轮询都必须受可见生命周期约束", lifecycleGates >= 2)
    }

    @Test
    fun `refreshing the backup library remains read only`() {
        assertTrue(backupCoordinatorSource.contains("BackupLibraryFiles.listLibraryArchives"))
        assertFalse(backupCoordinatorSource.contains("AutoBackupRetentionManager.enforceConfiguredLimit"))
    }

    @Test
    fun `backup actions use the shared button without a forwarding wrapper`() {
        assertFalse(backupSectionSource.contains("private fun BackupActionButton("))
        assertFalse(backupSectionSource.contains("BackupActionButton("))
        assertTrue(backupSectionSource.contains("SecondaryActionButton("))
    }

    @Test
    fun `header protects the launcher title from long version labels`() {
        val titleBlock = launcherPanelsSource
            .substringAfter("text = \"露科亚启动器\"")
            .substringBefore("                    )")

        assertTrue(titleBlock.contains("MaterialTheme.typography.titleLarge"))
        assertTrue(titleBlock.contains("maxLines = 1"))
        assertTrue(titleBlock.contains("softWrap = false"))
        assertTrue(launcherPanelsSource.contains(".widthIn(max = 112.dp)"))
    }

    @Test
    fun `repair tools compact summary does not inherit the two line truncation`() {
        val compactEntry = repairToolsSource
            .substringAfter("title = \"检查与修复\"")
            .substringBefore("onClick = { showDialog = true }")

        assertTrue(compactEntry.contains("detailMaxLines = Int.MAX_VALUE"))
    }

    @Test
    fun `default github repository flow has no unreachable blank success branch`() {
        val restoreDefaultBlock = screenSource
            .substringAfter("fun restoreDefaultGithubRepository()")
            .substringBefore("fun ignoreCurrentGithubUpdate()")

        assertFalse(githubUpdateStoreSource.contains("if (normalized.isBlank())"))
        assertFalse(restoreDefaultBlock.contains("if (result.repository.isBlank())"))
    }

    @Test
    fun `info icon keeps a large touch target without a rectangular indication`() {
        assertTrue(commonControlsSource.contains("sizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
        assertTrue(commonControlsSource.contains("indication = null"))
        assertTrue(commonControlsSource.contains("modifier = Modifier.size(20.dp)"))
    }
}
