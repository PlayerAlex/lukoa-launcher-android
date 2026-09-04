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

    @Test
    fun `local tracked changes no longer block update or rollback`() {
        val changed = current.copy(
            localChanges = "1",
            changedFiles = listOf("public/index.html"),
        )

        assertNull(evaluate(newerTarget, current = changed).updateDisabledReason)
        assertNull(evaluate(olderTarget, current = changed).rollbackDisabledReason)
    }

    @Test
    fun `custom target with unknown relation stays available in both directions`() {
        val customTarget = TavernVersionChoice(
            kind = TavernVersionKind.Custom,
            name = "fix-branch",
            target = "fix-branch",
        )

        val state = evaluate(customTarget)

        assertEquals(TavernTargetRelation.Unknown, state.relation)
        assertNull(state.updateDisabledReason)
        assertNull(state.rollbackDisabledReason)
        assertEquals("无法判断新旧，执行前先备份。", TavernVersionActionGuards.relationHint(state, customTarget))
    }

    private fun evaluate(
        target: TavernVersionChoice,
        tavernRunning: Boolean = false,
        tavernStarting: Boolean = false,
        current: TavernVersionInfo = this.current,
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
