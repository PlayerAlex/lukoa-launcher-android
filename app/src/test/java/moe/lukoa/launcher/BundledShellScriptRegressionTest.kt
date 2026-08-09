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
    fun `install uses the bundled script with guarded dependency recovery`() {
        assertTrue(runnerSource.contains("scriptCommand = \"install\""))
        assertTrue(runnerSource.contains("scriptArgs = listOf(args.target, args.repoUrl, args.configPolicy.wireValue)"))
        assertTrue(script.contains("wait_for_apt_locks()"))
        assertTrue(script.contains("LUKOA_APT_CONFIG_POLICY=\"\${3:-keep}\""))
        assertTrue(script.contains("installDependencyDpkgConfigureExitCode"))
        assertTrue(script.contains("installDependencyFixBrokenExitCode"))
        assertTrue(script.contains("installDependencyRetryExitCode"))
    }

    @Test
    fun `update uses the bundled script and keeps managed upload changes safe`() {
        assertTrue(runnerSource.contains("scriptCommand = \"update\""))
        assertTrue(runnerSource.contains("scriptArgs = listOf(args.target, args.repoUrl)"))
        assertTrue(script.contains("OFFICIAL_REPO=\"\${2:-\$OFFICIAL_REPO}\""))
        assertTrue(script.contains("upload_limit_prepare_update"))
        assertTrue(script.contains("uploadLimit.updateAction=restored-after-failure"))
    }

    @Test
    fun `rollback uses the bundled script and restores managed upload changes`() {
        assertTrue(runnerSource.contains("scriptCommand = \"rollback\""))
        assertTrue(runnerSource.contains("scriptArgs = listOf(args.target, args.repoUrl)"))
        assertTrue(script.contains("Managed upload limit could not be safely removed before rollback"))
        assertTrue(script.contains("uploadLimit.rollbackAction=reapplied"))
        assertTrue(script.contains("git fetch --all --tags --prune"))
        assertFalse(runnerSource.contains("buildTavernInstallCommand"))
        assertFalse(runnerSource.contains("buildTavernUpdateCommand"))
        assertFalse(runnerSource.contains("buildTavernRollbackCommand"))
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
    fun `extension mutations are limited to stopped tavern direct child directories`() {
        assertTrue(script.contains("run_tavern_extension_action()"))
        assertTrue(script.contains("delete|disable|enable|install|update|rollback)"))
        assertTrue(script.contains("repair_require_stopped || return"))
        assertTrue(script.contains(".lukoa-disabled-third-party"))
        assertTrue(script.contains("path.dirname(target) !== root"))
        assertTrue(script.contains("targetStat.isSymbolicLink()"))
        assertTrue(script.contains("fs.renameSync(source, target)"))
        assertTrue(script.contains("Extension destination already exists"))
        assertTrue(script.contains("Disabled extension root must not be a symbolic link"))
        assertTrue(script.contains("directoryKilobytes(directory)"))
        assertTrue(script.contains("if (entry.isSymbolicLink()) continue"))
        assertTrue(script.contains("sizeScanBudget.entries >= 50000"))
        assertTrue(script.contains("Date.now() - sizeScanBudget.startedAt >= 5000"))
        assertTrue(script.contains("extensions-delete|tavern-extensions-delete"))
        assertTrue(script.contains("extensions-disable|tavern-extensions-disable"))
        assertTrue(script.contains("extensions-enable|tavern-extensions-enable"))
        assertFalse(script.contains("fs.rmSync(extensionRoot"))
    }

    @Test
    fun `extension install validates a staged clone before atomic placement`() {
        val extensionBlock = script.substringAfter("run_tavern_extension_action() {")
            .substringBefore("run_tavern_user_action() {")
        val resetThemeBlock = script.substringAfter("cmd_reset_theme() {")
            .substringBefore("cmd_node_memory_set() {")

        assertTrue(script.contains("extensions-install|tavern-extensions-install"))
        assertTrue(script.contains(".lukoa-extension-staging"))
        assertTrue(script.contains("spawnSync('git', ['clone', '--depth', '1'"))
        assertTrue(script.contains("Invalid extension repository URL"))
        assertTrue(script.contains("manifest.json"))
        assertTrue(script.contains("manifestStat.isSymbolicLink()"))
        assertTrue(script.contains("GIT_TERMINAL_PROMPT: '0'"))
        assertTrue(extensionBlock.contains("git command not found in Termux"))
        assertFalse(resetThemeBlock.contains("git command not found in Termux"))
        assertTrue(script.contains("Extension destination already exists"))
        assertTrue(script.contains("fs.renameSync(stagingDirectory, target)"))
        assertFalse(script.contains("fs.renameSync(stagingDirectory, extensionRoot)"))
    }

    @Test
    fun `extension update check is read only and time bounded`() {
        val extensionBlock = script.substringAfter("run_tavern_extension_action() {")
            .substringBefore("run_tavern_user_action() {")

        assertTrue(script.contains("extensions-check-updates|tavern-extensions-check-updates"))
        assertTrue(script.contains("'ls-remote', '--heads'"))
        assertTrue(script.contains("15 * 1000"))
        assertTrue(script.contains("Date.now() - remoteCheckStartedAt"))
        assertTrue(script.contains("update_available"))
        assertFalse(extensionBlock.contains("git merge"))
        assertFalse(extensionBlock.contains("git pull"))
    }

    @Test
    fun `extension update and rollback use validated atomic directory swaps`() {
        val extensionBlock = script.substringAfter("run_tavern_extension_action() {")
            .substringBefore("run_tavern_user_action() {")

        assertTrue(extensionBlock.contains("delete|disable|enable|install|update|rollback)"))
        assertTrue(script.contains("extensions-update|tavern-extensions-update"))
        assertTrue(script.contains("extensions-rollback|tavern-extensions-rollback"))
        assertTrue(extensionBlock.contains(".lukoa-extension-rollbacks"))
        assertTrue(extensionBlock.contains("updateStatus !== 'update_available'"))
        assertTrue(extensionBlock.contains("Extension has local changes or no verified update"))
        assertTrue(extensionBlock.contains("validateExtensionManifest"))
        assertTrue(extensionBlock.contains("fs.renameSync(target, rollbackTarget)"))
        assertTrue(extensionBlock.contains("fs.renameSync(stagingDirectory, target)"))
        assertTrue(extensionBlock.contains("fs.renameSync(rollbackTarget, target)"))
        assertTrue(extensionBlock.contains("Extension target is not a regular directory"))
        assertTrue(extensionBlock.contains("Extension target escaped the extension root"))
        assertFalse(extensionBlock.contains("git pull"))
        assertFalse(extensionBlock.contains("git merge"))
    }

    @Test
    fun `selective restore only swaps the resolved user data directory`() {
        val restoreBlock = script.substringAfter("cmd_restore() {")
            .substringBefore("cmd_migrate_dir() {")

        assertTrue(restoreBlock.contains("restore_mode=\"${'$'}{2:-full}\""))
        assertTrue(restoreBlock.contains("full|user-data"))
        assertTrue(restoreBlock.contains("resolve_tavern_data_root"))
        assertTrue(restoreBlock.contains("restoreUserDataOnly=1"))
        assertTrue(restoreBlock.contains("mv \"${'$'}current_data_root\" \"${'$'}data_rollback_dir\""))
        assertTrue(restoreBlock.contains("mv \"${'$'}restore_data_source\" \"${'$'}current_data_root\""))
        assertTrue(restoreBlock.contains("restoreMode=user-data"))
        assertTrue(restoreBlock.contains("restoreMode=full"))
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
