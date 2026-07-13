package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernDirectoryChoicePromptPolicyTest {
    @Test
    fun `matched missing path with candidates still prompts`() {
        val output = """
            {"message":"SillyTavern directory not found: /data/data/com.termux/files/home/SillyTavern. Multiple candidates were found; choose one manually in the launcher."}
            ==== SillyTavern directory candidates ====
            candidate.1=/data/data/com.termux/files/home/LukoaLauncher/SillyTavern
            ==== end SillyTavern directory candidates ====
        """.trimIndent()

        val shouldPrompt = TavernDirectoryChoicePromptPolicy.shouldPrompt(
            text = output,
            currentPath = "~/SillyTavern",
        )

        assertTrue(shouldPrompt)
    }

    @Test
    fun `stale missing path from previous profile no longer prompts`() {
        val output = """
            {"message":"SillyTavern directory not found: /data/data/com.termux/files/home/SillyTavern. A possible directory was found; confirm it manually in the launcher."}
            ==== SillyTavern directory candidates ====
            candidate.1=/data/data/com.termux/files/home/LukoaLauncher/SillyTavern
            ==== end SillyTavern directory candidates ====
        """.trimIndent()

        val shouldPrompt = TavernDirectoryChoicePromptPolicy.shouldPrompt(
            text = output,
            currentPath = "~/LukoaLauncher/SillyTavern",
        )

        assertFalse(shouldPrompt)
    }

    @Test
    fun `missing path parser strips helper suffixes`() {
        val path = TavernDirectoryChoicePromptPolicy.missingConfiguredPath(
            """SillyTavern directory not found: /data/data/com.termux/files/home/SillyTavern. Multiple candidates were found; choose one manually in the launcher.""",
        )

        assertEquals(
            "/data/data/com.termux/files/home/SillyTavern",
            path,
        )
    }

    @Test
    fun `missing path parser tolerates trailing json fields`() {
        val path = TavernDirectoryChoicePromptPolicy.missingConfiguredPath(
            """{"message":"SillyTavern directory not found: /data/data/com.termux/files/home/SillyTavern","code":66}""",
        )

        assertEquals(
            "/data/data/com.termux/files/home/SillyTavern",
            path,
        )
    }

    @Test
    fun `no candidates means no chooser prompt`() {
        val shouldPrompt = TavernDirectoryChoicePromptPolicy.shouldPrompt(
            text = "SillyTavern directory not found: /data/data/com.termux/files/home/SillyTavern",
            currentPath = "~/SillyTavern",
        )

        assertFalse(shouldPrompt)
    }
}
