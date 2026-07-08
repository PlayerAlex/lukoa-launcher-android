package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernProfileMigrationGuardTest {
    @Test
    fun `running instance blocks migration`() {
        val decision = TavernProfileMigrationGuard.evaluate(
            config = TavernPathConfig(),
            targetPath = "~/target",
            targetKind = TavernProfileMigrationTargetKind.Custom,
            tavernRunning = true,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Blocked)
        assertEquals(
            "当前实例还在运行。请先停止这个实例，再迁移目录，避免一边运行一边搬目录。",
            (decision as TavernProfileMigrationDecision.Blocked).message,
        )
    }

    @Test
    fun `same path blocks migration`() {
        val config = TavernPathConfig()

        val decision = TavernProfileMigrationGuard.evaluate(
            config = config,
            targetPath = config.displayTavernDir,
            targetKind = TavernProfileMigrationTargetKind.LauncherManaged,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Blocked)
        assertEquals("目标目录和当前目录一样，不需要迁移。", (decision as TavernProfileMigrationDecision.Blocked).message)
    }

    @Test
    fun `clone cannot migrate to traditional default directory`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val decision = TavernProfileMigrationGuard.evaluate(
            config = config,
            targetPath = TavernPathDefaults.LEGACY_DEFAULT_TAVERN_DIR,
            targetKind = TavernProfileMigrationTargetKind.TraditionalDefault,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Blocked)
        assertTrue((decision as TavernProfileMigrationDecision.Blocked).message.contains("传统默认目录 ~/SillyTavern 只建议留给主实例"))
    }

    @Test
    fun `target path already used by another profile is blocked`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val decision = TavernProfileMigrationGuard.evaluate(
            config = config,
            targetPath = TavernPathDefaults.DEFAULT_TAVERN_DIR,
            targetKind = TavernProfileMigrationTargetKind.LauncherManaged,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Blocked)
        assertTrue((decision as TavernProfileMigrationDecision.Blocked).message.contains("会一直保留给主实例"))
    }

    @Test
    fun `clone cannot migrate into main launcher managed directory even when main moved away`() {
        val config = TavernPathConfig()
            .withUpdatedActiveProfile(tavernDir = "~/SillyTavern")
            .addSuggestedProfile()

        val decision = TavernProfileMigrationGuard.evaluate(
            config = config,
            targetPath = "~/LukoaLauncher/SillyTavern",
            targetKind = TavernProfileMigrationTargetKind.LauncherManaged,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Blocked)
        assertTrue((decision as TavernProfileMigrationDecision.Blocked).message.contains("会一直保留给主实例"))
    }

    @Test
    fun `custom path migration asks for confirmation with risk note`() {
        val config = TavernPathConfig().addSuggestedProfile()

        val decision = TavernProfileMigrationGuard.evaluate(
            config = config,
            targetPath = "~/MySillyTavern",
            targetKind = TavernProfileMigrationTargetKind.Custom,
            tavernRunning = false,
            tavernStarting = false,
            actionsLocked = false,
        )

        assertTrue(decision is TavernProfileMigrationDecision.Confirm)
        val confirmation = (decision as TavernProfileMigrationDecision.Confirm).confirmation
        assertEquals("分身实例", confirmation.profileName)
        assertEquals("~/MySillyTavern", confirmation.targetPath)
        assertTrue(confirmation.riskNote.contains("不属于启动器推荐的默认位置"))
    }
}
