package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernVersionCatalogTest {
    @Test
    fun `requires official catalog skips custom targets`() {
        assertFalse(
            TavernVersionCatalog.requiresOfficialCatalog(
                TavernVersionChoice(
                    kind = TavernVersionKind.Custom,
                    name = "feature/foo",
                    target = "feature/foo",
                ),
            ),
        )
    }

    @Test
    fun `contains choice only accepts current official list entries`() {
        val repoUrl = TavernMirrorDefaults.OFFICIAL_REPO
        val target = TavernVersionChoice(
            kind = TavernVersionKind.Stable,
            name = "v1.0.0",
            target = "v1.0.0",
            repoUrl = repoUrl,
        )
        val versions = TavernOfficialVersions(stable = listOf(target))

        assertTrue(TavernVersionCatalog.containsChoice(versions, target))
        assertFalse(
            TavernVersionCatalog.containsChoice(
                versions,
                target.copy(name = "v1.0.1", target = "v1.0.1"),
            ),
        )
    }

    @Test
    fun `matches current mirror rejects stale selection source`() {
        val target = TavernVersionChoice(
            kind = TavernVersionKind.Test,
            name = "release",
            target = "release",
            repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
        )

        assertFalse(
            TavernVersionCatalog.matchesCurrentMirror(
                target = target,
                currentRepoUrl = TavernMirrorDefaults.GITHUB_PROXY_REPO,
            ),
        )
    }
}
