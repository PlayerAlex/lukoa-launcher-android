package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TavernVersionActionGuardsTest {
    private val repoUrl = TavernMirrorDefaults.OFFICIAL_REPO
    private val current = TavernVersionInfo(
        hasData = true,
        packageVersion = "1.0.0",
        branch = "release",
    )
    private val newerTarget = TavernVersionChoice(
        kind = TavernVersionKind.Stable,
        name = "1.1.0",
        target = "1.1.0",
        repoUrl = repoUrl,
    )
    private val olderTarget = TavernVersionChoice(
        kind = TavernVersionKind.Stable,
        name = "0.9.0",
        target = "0.9.0",
        repoUrl = repoUrl,
    )

    @Test
    fun `running instance blocks update and rollback`() {
        val state = evaluate(newerTarget, tavernRunning = true)

        assertEquals(TavernVersionActionGuards.ACTIVE_INSTANCE_DISABLED_REASON, state.updateDisabledReason)
        assertEquals(TavernVersionActionGuards.ACTIVE_INSTANCE_DISABLED_REASON, state.rollbackDisabledReason)
    }

    @Test
    fun `starting instance blocks update and rollback`() {
        val state = evaluate(olderTarget, tavernStarting = true)

        assertEquals(TavernVersionActionGuards.ACTIVE_INSTANCE_DISABLED_REASON, state.updateDisabledReason)
        assertEquals(TavernVersionActionGuards.ACTIVE_INSTANCE_DISABLED_REASON, state.rollbackDisabledReason)
    }

    @Test
    fun `stopped instance keeps existing version relation rules`() {
        val updateState = evaluate(newerTarget)
        val rollbackState = evaluate(olderTarget)

        assertNull(updateState.updateDisabledReason)
        assertNull(rollbackState.rollbackDisabledReason)
        assertEquals("目标更旧，不能更新。", rollbackState.updateDisabledReason)
        assertEquals("目标更新，不能回退。", updateState.rollbackDisabledReason)
    }

    private fun evaluate(
        target: TavernVersionChoice,
        tavernRunning: Boolean = false,
        tavernStarting: Boolean = false,
    ): TavernVersionActionState {
        return TavernVersionActionGuards.evaluate(
            current = current,
            target = target,
            officialVersions = TavernOfficialVersions(stable = listOf(target)),
            currentRepoUrl = repoUrl,
            tavernRunning = tavernRunning,
            tavernStarting = tavernStarting,
        )
    }
}
