package moe.lukoa.launcher

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledShellScriptRegressionTest {
    private val script by lazy {
        File("src/main/assets/lukoa-tavern.sh").readText(Charsets.UTF_8)
    }
    private val runnerSource by lazy {
        File("src/main/java/moe/lukoa/launcher/TermuxCommandRunner.kt").readText(Charsets.UTF_8)
    }

    @Test
    fun `shell defaults main profile to traditional path and clones to managed paths`() {
        assertTrue(script.contains("profile_default_tavern_dir()"))
        assertTrue(script.contains("DEFAULT_TAVERN_DIR=\"\$(profile_default_tavern_dir \"\${TAVERN_PROFILE_ID:-main}\")\""))
        assertTrue(script.contains("printf \"%s\" \"\$LEGACY_DEFAULT_TAVERN_DIR\""))
        assertTrue(script.contains("launcher_managed_profile_dir \"\$profile_id\""))

        assertTrue(runnerSource.contains("profile_default_tavern_dir()"))
        assertTrue(runnerSource.contains("DEFAULT_TAVERN_DIR=\"\${'$'}(profile_default_tavern_dir"))
        assertTrue(runnerSource.contains("printf \"%s\" \"\${'$'}LEGACY_DEFAULT_TAVERN_DIR\""))
        assertTrue(runnerSource.contains("launcher_managed_profile_dir \"\${'$'}profile_id\""))
    }

    @Test
    fun `node memory configuration does not source executable state`() {
        assertFalse(script.contains(". \"\$NODE_MEMORY_FILE\""))
        assertTrue(script.contains("2048|4096|6144"))
        assertTrue(script.contains("\${NODE_OPTIONS:+\$NODE_OPTIONS }--max-old-space-size="))
    }

    @Test
    fun `case blocks do not retain an if terminator`() {
        assertFalse(Regex("""esac\s*\nfi\b""").containsMatchIn(script))
    }

    @Test
    fun `upload patch protects update and rollback`() {
        assertTrue(script.contains("Managed upload limit could not be safely removed before update"))
        assertTrue(script.contains("Managed upload limit could not be safely removed before rollback"))
        assertTrue(script.contains("upload_limit_reapply_after_update"))
    }

    @Test
    fun `upload limit reset reads version default and preserves unrelated file changes`() {
        assertTrue(script.contains("cmd_upload_limit_reset"))
        assertTrue(script.contains("git show HEAD:src/server-main.js"))
        assertTrue(script.contains("current.slice(0, currentMatches[0].index)"))
        assertTrue(script.contains("upload-limit-reset|tavern-upload-limit-reset"))
    }

    @Test
    fun `user management uses SillyTavern storage module and protects data`() {
        assertTrue(script.contains("util.setConfigFilePath(configFile)"))
        assertTrue(script.indexOf("util.setConfigFilePath(configFile)") < script.indexOf("await import(pathToFileURL(path.resolve('src/users.js')).href)"))
        assertTrue(script.contains("await users.initUserStorage(dataRoot)"))
        assertTrue(script.contains("handle === 'default-user'"))
        assertTrue(script.contains("await storage.removeItem(users.toKey(handle))"))
        assertFalse(script.contains("fs.rmSync(users.getUserDirectories(handle).root"))
    }

    @Test
    fun `extension deletion is limited to a stopped tavern direct child directory`() {
        assertTrue(script.contains("run_tavern_extension_action()"))
        assertTrue(script.contains("if (action === 'delete')"))
        assertTrue(script.contains("repair_require_stopped || return"))
        assertTrue(script.contains("path.dirname(target) !== extensionRoot"))
        assertTrue(script.contains("targetStat.isSymbolicLink()"))
        assertTrue(script.contains("extensions-delete|tavern-extensions-delete"))
        assertFalse(script.contains("fs.rmSync(extensionRoot"))
    }

    @Test
    fun `bundled script transport supports background stdin and bounded foreground compression`() {
        assertTrue(runnerSource.contains("putExtra(EXTRA_STDIN, it)"))
        assertTrue(runnerSource.contains("stdin = plan.stdin"))
        assertTrue(runnerSource.contains("TermuxScriptTransport.Stdin"))
        assertTrue(runnerSource.contains("TermuxScriptTransport.CompressedArgument"))
        assertFalse(runnerSource.contains("LUKOA_LAUNCHER_SCRIPT_EOF"))
    }
}
