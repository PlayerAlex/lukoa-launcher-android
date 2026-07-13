package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernVersionPageActionResolverTest {
    private val repoUrl = TavernMirrorDefaults.OFFICIAL_REPO
    private val current = TavernVersionInfo(
        hasData = true,
        packageVersion = "1.0.0",
        branch = "release",
    )

    @Test
    fun `not installed target becomes install action`() {
        val target = stable("1.1.0")
        val notInstalled = TavernVersionInfo(notInstalled = true)

        val action = resolve(current = notInstalled, target = target)

        assertEquals(TavernVersionPageActionKind.Install, action.kind)
        assertEquals("安装 1.1.0", action.buttonLabel)
        assertTrue(action.enabled)
    }

    @Test
    fun `newer official target becomes update action`() {
        val target = stable("1.1.0")

        val action = resolve(target = target)

        assertEquals(TavernVersionPageActionKind.Update, action.kind)
        assertEquals("更新到 1.1.0", action.buttonLabel)
        assertTrue(action.enabled)
    }

    @Test
    fun `older official target becomes rollback action`() {
        val target = stable("0.9.0")

        val action = resolve(target = target)

        assertEquals(TavernVersionPageActionKind.Rollback, action.kind)
        assertEquals("回退到 0.9.0", action.buttonLabel)
        assertTrue(action.enabled)
    }

    @Test
    fun `unknown custom target becomes switch action through update path`() {
        val target = TavernVersionChoice(
            kind = TavernVersionKind.Custom,
            name = "feature-branch",
            target = "feature-branch",
        )

        val action = resolve(target = target)

        assertEquals(TavernVersionPageActionKind.Switch, action.kind)
        assertEquals("切换到 feature-branch", action.buttonLabel)
        assertTrue(action.enabled)
    }

    @Test
    fun `same target has no executable action`() {
        val target = stable("1.0.0")

        val action = resolve(target = target)

        assertEquals(TavernVersionPageActionKind.None, action.kind)
        assertEquals("当前", action.badgeLabel)
        assertFalse(action.enabled)
    }

    @Test
    fun `operation lock disables otherwise available action`() {
        val target = stable("1.1.0")

        val action = resolve(target = target, actionsLocked = true)

        assertEquals(TavernVersionPageActionKind.Update, action.kind)
        assertFalse(action.enabled)
        assertTrue(action.disabledReason.orEmpty().contains("任务"))
    }

    private fun stable(version: String) = TavernVersionChoice(
        kind = TavernVersionKind.Stable,
        name = version,
        target = version,
        repoUrl = repoUrl,
    )

    private fun resolve(
        current: TavernVersionInfo = this.current,
        target: TavernVersionChoice,
        actionsLocked: Boolean = false,
    ): TavernVersionPageAction {
        val officialVersions = if (target.kind == TavernVersionKind.Custom) {
            TavernOfficialVersions()
        } else {
            TavernOfficialVersions(stable = listOf(target))
        }
        val actionState = TavernVersionActionGuards.evaluate(
            current = current,
            target = target,
            officialVersions = officialVersions,
            currentRepoUrl = repoUrl,
        )
        return TavernVersionPageActionResolver.resolve(
            actionsLocked = actionsLocked,
            current = current,
            selectedVersion = target,
            actionState = actionState,
        )
    }
}
