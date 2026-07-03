package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernVersionActionConfirmationTest {
    @Test
    fun `update confirmation uses stable summary and source label`() {
        val confirmation = TavernVersionActionConfirmationBuilder.build(
            kind = TavernVersionActionKind.Update,
            current = TavernVersionInfo(hasData = true, packageVersion = "1.13.0"),
            target = TavernVersionChoice(
                kind = TavernVersionKind.Stable,
                name = "1.14.0",
                target = "refs/tags/1.14.0",
                repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
            ),
            fallbackRepoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
        )

        assertEquals("会把当前酒馆切到你选中的目标版本。", confirmation.summary)
        assertEquals("1.13.0", confirmation.currentVersion)
        assertEquals("1.14.0", confirmation.targetVersion)
        assertEquals("官方 GitHub", confirmation.sourceLabel)
        assertTrue(confirmation.detail.contains("自动创建一份安全备份"))
        assertTrue(confirmation.riskTip.contains("不要切换路径"))
    }

    @Test
    fun `rollback confirmation warns for custom target`() {
        val confirmation = TavernVersionActionConfirmationBuilder.build(
            kind = TavernVersionActionKind.Rollback,
            current = TavernVersionInfo(hasData = true, packageVersion = "1.14.0"),
            target = TavernVersionChoice(
                kind = TavernVersionKind.Custom,
                name = "fix-branch",
                target = "fix-branch",
            ),
            fallbackRepoUrl = TavernMirrorDefaults.GITHUB_PROXY_REPO,
        )

        assertEquals("确认回退酒馆", confirmation.kind.dialogTitle)
        assertEquals("fix-branch 自定义", confirmation.targetVersion)
        assertEquals("GitHub 加速", confirmation.sourceLabel)
        assertTrue(confirmation.riskTip.contains("自定义版本"))
    }
}
