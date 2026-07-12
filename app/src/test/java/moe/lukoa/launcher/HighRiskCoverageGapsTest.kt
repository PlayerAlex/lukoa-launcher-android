package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighRiskCoverageGapsTest {
    @Test
    fun `install command codec preserves structured values and rejects malformed input`() {
        val encoded = TavernInstallCommandCodec.encode(
            target = " feature/test ",
            repoUrl = " https://example.com/repo.git ",
            configPolicy = AptConfigPolicy.KeepCurrent,
        )

        assertEquals(
            TavernInstallCommandArgs(
                target = "feature/test",
                repoUrl = "https://example.com/repo.git",
                configPolicy = AptConfigPolicy.KeepCurrent,
            ),
            TavernInstallCommandCodec.decode(encoded),
        )
        assertNull(TavernInstallCommandCodec.decode(null))
        assertNull(TavernInstallCommandCodec.decode("not.a.valid.extra.payload"))
    }

    @Test
    fun `version command codec preserves optional commit and rejects incomplete input`() {
        val encoded = TavernVersionCommandCodec.encode(
            target = " v1.2.3 ",
            repoUrl = " https://example.com/repo.git/ ",
            commit = " abc123 ",
        )

        assertEquals(
            TavernVersionCommandArgs("v1.2.3", "https://example.com/repo.git/", "abc123"),
            TavernVersionCommandCodec.decode(encoded),
        )
        assertNull(TavernVersionCommandCodec.decode("broken"))
        assertNull(TavernVersionCommandCodec.decode(".."))
    }

    @Test
    fun `mirror validators block unsafe schemes whitespace and separators`() {
        assertNull(TavernMirrorValidator.validateRepoUrl("https://example.com/repo.git"))
        assertNull(TavernMirrorValidator.validateNpmRegistry("https://registry.example.com/"))
        assertNull(TavernMirrorValidator.validateTermuxAptUrl("http://packages.example.com/apt"))

        assertTrue(TavernMirrorValidator.validateRepoUrl("http://example.com/repo.git") != null)
        assertTrue(TavernMirrorValidator.validateNpmRegistry("file:///tmp/npm") != null)
        assertTrue(TavernMirrorValidator.validateTermuxAptUrl("https://one::https://two") != null)
        assertTrue(TavernMirrorValidator.validateTermuxAptUrl("https://example.com/a b") != null)
        assertTrue(TavernMirrorValidator.validateTermuxAptUrl("https://example.com\nnext") != null)
    }

    @Test
    fun `repository comparison normalizes case whitespace and trailing slash`() {
        assertTrue(sameRepoUrl(" HTTPS://Example.com/Repo.git/ ", "https://example.com/repo.git"))
        assertFalse(sameRepoUrl("", TavernMirrorDefaults.OFFICIAL_REPO))
        assertFalse(sameRepoUrl("https://example.com/a", "https://example.com/b"))
    }

    @Test
    fun `github source parser supports direct and encoded proxy repositories`() {
        val direct = TavernGithubSourceParser.parse("https://github.com/SillyTavern/SillyTavern.git/")
        assertEquals("SillyTavern/SillyTavern", direct?.repository)
        assertEquals("", direct?.proxyPrefix)

        val proxied = TavernGithubSourceParser.parse(
            "https://gh-proxy.com/https%3A%2F%2Fgithub.com%2FSillyTavern%2FSillyTavern.git",
        )
        assertEquals("SillyTavern/SillyTavern", proxied?.repository)
        assertEquals("https://gh-proxy.com/", proxied?.proxyPrefix)
        assertTrue(proxied?.tagsApiUrl?.startsWith("https://gh-proxy.com/https://api.github.com/repos/") == true)
        assertNull(TavernGithubSourceParser.parse("https://gitlab.com/example/repo.git"))
    }

    @Test
    fun `directory candidate parser only reads bounded candidate section and removes duplicates`() {
        val output = """
            candidate.0=/outside
            ==== SillyTavern directory candidates ====
            candidate.1=~/SillyTavern
            ignored=value
            candidate.2= /storage/emulated/0/SillyTavern
            candidate.3=~/SillyTavern
            ==== end SillyTavern directory candidates ====
            candidate.4=/also-outside
        """.trimIndent()

        assertEquals(
            listOf("~/SillyTavern", "/storage/emulated/0/SillyTavern"),
            TavernDirectoryCandidateParser.parse(output),
        )
        assertTrue(TavernDirectoryCandidateParser.parse("candidate.1=/unsafe").isEmpty())
    }

    @Test
    fun `termux permission signal recognizes known denial messages only`() {
        assertTrue(TermuxPermissionSignals.externalAppsBlocked("RunCommandService requires permission"))
        assertTrue(TermuxPermissionSignals.externalAppsBlocked("Set allow-external-apps=true"))
        assertTrue(TermuxPermissionSignals.externalAppsBlocked("Edit TERMUX.PROPERTIES"))
        assertFalse(TermuxPermissionSignals.externalAppsBlocked("command completed successfully"))
    }

    @Test
    fun `termux history policy retains newest bounded diagnostic text`() {
        val result = termuxResult(
            stdout = "a".repeat(12_100) + "stdout-tail",
            stderr = "b".repeat(3_100) + "stderr-tail",
            raw = "c".repeat(1_100) + "raw-tail",
        )

        val bounded = TermuxResultHistoryPolicy.forHistory(result)

        assertEquals(12_000, bounded.stdout.length)
        assertEquals(3_000, bounded.stderr.length)
        assertEquals(1_000, bounded.raw.length)
        assertTrue(bounded.stdout.endsWith("stdout-tail"))
        assertTrue(bounded.stderr.endsWith("stderr-tail"))
        assertTrue(bounded.raw.endsWith("raw-tail"))
        assertEquals(16_000, TermuxResultHistoryPolicy.textCharacterCount(bounded))
    }

    @Test
    fun `version selection removes current release and recommends next stable choice`() {
        val currentChoice = TavernVersionChoice(TavernVersionKind.Stable, "v1.2.3", "v1.2.3")
        val nextChoice = TavernVersionChoice(TavernVersionKind.Stable, "v1.2.4", "v1.2.4")
        val testChoice = TavernVersionChoice(TavernVersionKind.Test, "staging", "staging")
        val versions = TavernOfficialVersions(
            stable = listOf(currentChoice, nextChoice),
            test = listOf(testChoice),
        )
        val current = TavernVersionInfo(hasData = true, packageVersion = "1.2.3")

        val choices = TavernVersionSelection.versionManagementChoices(versions, current)

        assertEquals(listOf(nextChoice), choices.stable)
        assertEquals(nextChoice, TavernVersionSelection.normalizeForVersionManagement(versions, current, currentChoice))
        assertEquals(nextChoice, TavernVersionSelection.recommendedInstallChoice(choices))
    }

    @Test
    fun `custom version selection survives catalog refresh`() {
        val custom = TavernVersionChoice(TavernVersionKind.Custom, "feature/foo", "feature/foo")

        assertEquals(
            custom,
            TavernVersionSelection.normalizeForInstall(TavernOfficialVersions(), custom),
        )
    }

    private fun termuxResult(stdout: String, stderr: String, raw: String): TermuxCommandResult {
        return TermuxCommandResult(
            executionId = 1,
            command = "status",
            nonce = "nonce",
            hasResultBundle = true,
            timeMillis = 1L,
            stdout = stdout,
            stderr = stderr,
            exitCode = 0,
            errCode = null,
            errMessage = "",
            stdoutOriginalLength = stdout.length.toString(),
            stderrOriginalLength = stderr.length.toString(),
            raw = raw,
        )
    }
}
