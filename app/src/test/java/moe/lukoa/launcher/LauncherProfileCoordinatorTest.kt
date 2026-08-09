package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherProfileCoordinatorTest {
    @Test
    fun `clone confirmation dispatches recoverable task without registering profile early`() {
        val pathState = LauncherPathSettingsState(TavernPathConfig())
        var dispatchedTask: PendingLauncherTask? = null
        var dispatchedCommand = ""
        val coordinator = createCoordinator(
            pathState = pathState,
            onSavePath = { config -> TavernPathSaveResult(true, config, "saved") },
            onRunProfileMutation = { task, _, _, command ->
                dispatchedTask = task
                dispatchedCommand = command
            },
        )

        coordinator.requestCloneCurrentTavernProfile()

        val confirmation = requireNotNull(pathState.pendingCloneConfirmation)
        assertEquals("profile-2", confirmation.targetProfile.id)
        assertEquals(1, pathState.config.availableProfiles.size)

        coordinator.confirmCloneCurrentTavernProfile()

        val task = requireNotNull(dispatchedTask)
        assertEquals(PendingLauncherTaskKind.CloneTavernProfile, task.kind)
        assertEquals("profile-2", task.profileId)
        assertEquals("\$HOME/LukoaLauncher/SillyTavern2", task.targetPath)
        val command = LauncherCommandCodec.decode(dispatchedCommand)
        assertEquals("tavern-clone-profile-dir", command.name)
        assertEquals(
            task.targetPath,
            TavernProfileMigrationCommandCodec.decode(command.argument)?.targetPath,
        )
        assertEquals(1, pathState.config.availableProfiles.size)
        assertEquals(null, pathState.pendingCloneConfirmation)
    }

    @Test
    fun `invalid port is rejected before persistence`() {
        val pathState = LauncherPathSettingsState(TavernPathConfig())
        pathState.portInput = "not-a-port"
        var saveCount = 0
        val statuses = mutableListOf<String>()
        val coordinator = createCoordinator(
            pathState = pathState,
            onSavePath = { config ->
                saveCount += 1
                TavernPathSaveResult(true, config, "已保存")
            },
            onStatus = { message, _, _ -> statuses += message },
        )

        val saved = coordinator.saveTavernPort()

        assertFalse(saved)
        assertEquals(0, saveCount)
        assertEquals("not-a-port", pathState.portInput)
        assertTrue(statuses.single().startsWith("酒馆端口无效："))
    }

    @Test
    fun `rejected directory save reports failure and keeps dialog input`() {
        val pathState = LauncherPathSettingsState(TavernPathConfig())
        pathState.pathInput = "~/already-used"
        var refreshCount = 0
        val statuses = mutableListOf<String>()
        val coordinator = createCoordinator(
            pathState = pathState,
            onSavePath = {
                TavernPathSaveResult(
                    saved = false,
                    config = TavernPathConfig(),
                    message = "实例目录不能重复。",
                )
            },
            onStatus = { message, _, _ -> statuses += message },
            onRefresh = { refreshCount += 1 },
        )

        val saved = coordinator.saveTavernDirectory()

        assertFalse(saved)
        assertEquals("~/already-used", pathState.pathInput)
        assertEquals(0, refreshCount)
        assertEquals(listOf("实例目录不能重复。"), statuses)
    }

    @Test
    fun `successful port save returns success and refreshes active profile`() {
        val pathState = LauncherPathSettingsState(TavernPathConfig())
        pathState.portInput = " 9000 "
        var proposedPort = 0
        val refreshMessages = mutableListOf<String>()
        val coordinator = createCoordinator(
            pathState = pathState,
            onSavePath = { config ->
                proposedPort = config.normalizedPort
                TavernPathSaveResult(true, config, "已保存")
            },
            onRefresh = refreshMessages::add,
        )

        val saved = coordinator.saveTavernPort()

        assertTrue(saved)
        assertEquals(9000, proposedPort)
        assertEquals("9000", pathState.portInput)
        assertEquals(1, refreshMessages.size)
    }

    private fun createCoordinator(
        pathState: LauncherPathSettingsState,
        onSavePath: (TavernPathConfig) -> TavernPathSaveResult,
        onStatus: (String, String, Boolean) -> Unit = { _, _, _ -> },
        onRefresh: (String) -> Unit = {},
        onRunProfileMutation: (PendingLauncherTask, String, Long, String) -> Unit = { _, _, _, _ -> },
    ): LauncherProfileCoordinator {
        return LauncherProfileCoordinator(
            pathState = pathState,
            mirrorState = LauncherMirrorSettingsState(TavernMirrorConfig()),
            statusUpdate = onStatus,
            refreshActiveProfileState = onRefresh,
            blockIfPendingTaskExists = { false },
            runProfileMutationPendingCommand = onRunProfileMutation,
            beginBusy = { _, _ -> null },
            isOperationActive = { false },
            releaseBusy = {},
            isTransientStatus = { false },
            isActionInProgress = { false },
            isTavernRunning = { false },
            isTavernStarting = { false },
            isTermuxInstalled = { true },
            isRunCommandPermissionGranted = { true },
            onCommand = { _, _ -> },
            onSaveTavernMirrorConfig = { config ->
                TavernMirrorSaveResult(true, config, "已保存")
            },
            onSaveTavernPathConfig = onSavePath,
            onCheckTavernMirror = { _, _ -> },
            onTavernRepoChanged = {},
        )
    }
}
