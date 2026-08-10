package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdatePromptPolicyTest {
    @Test
    fun `automatic check prompts for a newer unignored version`() {
        assertTrue(
            updateState(tagName = "v0.9.3-beta40", isNewer = true)
                .shouldPromptUpdate(ignoredTag = "", manual = false),
        )
    }

    @Test
    fun `automatic check does not prompt for the ignored version`() {
        assertFalse(
            updateState(tagName = "v0.9.3-beta40", isNewer = true)
                .shouldPromptUpdate(ignoredTag = "v0.9.3-beta40", manual = false),
        )
    }

    @Test
    fun `a newly published tag prompts after an older tag was ignored`() {
        assertTrue(
            updateState(tagName = "v0.9.3-beta41", isNewer = true)
                .shouldPromptUpdate(ignoredTag = "v0.9.3-beta40", manual = false),
        )
    }

    @Test
    fun `manual check can reopen an ignored update`() {
        assertTrue(
            updateState(tagName = "v0.9.3-beta40", isNewer = true)
                .shouldPromptUpdate(ignoredTag = "v0.9.3-beta40", manual = true),
        )
    }

    @Test
    fun `current or missing release never prompts`() {
        assertFalse(updateState(tagName = "v0.9.3-beta39", isNewer = false).shouldPromptUpdate("", false))
        assertFalse(GithubUpdateUiState().shouldPromptUpdate("", false))
    }

    private fun updateState(tagName: String, isNewer: Boolean): GithubUpdateUiState {
        return GithubUpdateUiState(
            latest = GithubUpdateInfo(
                repository = "PlayerAlex/lukoa-launcher-android",
                tagName = tagName,
                versionName = tagName.removePrefix("v"),
                releaseName = tagName,
                releaseUrl = "https://example.com/$tagName",
                apkName = "lukoa.apk",
                apkDownloadUrl = "https://example.com/lukoa.apk",
                publishedAt = "2026-08-10T00:00:00Z",
                body = "",
                prerelease = true,
                isNewer = isNewer,
            ),
        )
    }
}
