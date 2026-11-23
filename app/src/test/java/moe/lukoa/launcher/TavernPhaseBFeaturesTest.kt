package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class TavernPhaseBFeaturesTest {
    private fun b64(v: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `storage codec allowlists cleanup categories and parser reads breakdown`() {
        val encoded = TavernStorageCommandCodec.encodeCategories(
            setOf(TavernStorageCleanupCategory.OldLogs, TavernStorageCleanupCategory.NpmCache),
        )
        assertEquals("old_logs,npm_cache", encoded)
        assertEquals(
            listOf(TavernStorageCleanupCategory.OldLogs, TavernStorageCleanupCategory.NpmCache),
            TavernStorageCommandCodec.decodeCategories(encoded),
        )
        assertNull(TavernStorageCommandCodec.decodeCategories("chats,characters"))
        assertNull(TavernStorageCommandCodec.decodeCategories(""))

        val output = """
            ==== Lukoa storage ====
            storage.freedKb=12800
            storage.tavernTotalKb=512000
            storage.dataTotalKb=120000
            storage.chatsKb=45000
            storage.charactersKb=60000
            storage.thumbnailsKb=4096
            storage.chatBackupsKb=10000
            storage.nodeModulesKb=320000
            storage.extensionsKb=15000
            storage.extensionRollbacksKb=2048
            storage.logsKb=4096
            storage.oldLogKb=1024
            storage.npmCacheKb=20480
            storage.leftoverDirsKb=5120
            storage.leftoverDirsCount=2
            storage.configBackupsKb=16
            storage.configBackupsCount=3
            ==== end Lukoa storage ====
        """.trimIndent()
        val parsed = TermuxStructuredOutputParser.parse(output).storageSnapshot
        assertNotNull(parsed)
        parsed!!
        assertEquals(12800L, parsed.lastFreedKb)
        assertEquals(512000L, parsed.tavernTotalKb)
        assertEquals(45000L, parsed.chatsKb)
        assertEquals(2, parsed.leftoverDirsCount)
        assertEquals(3, parsed.configBackupsCount)
        assertEquals(4096L + 2048L + 1024L + 20480L + 5120L + 16L, parsed.totalCleanableKb)
    }

    @Test
    fun `server plugins codec validates inputs and parser reads records`() {
        assertNull(TavernServerPluginCommandCodec.validateDirectoryName("my-plugin_1"))
        assertNotNull(TavernServerPluginCommandCodec.validateDirectoryName("../escape"))
        assertNotNull(TavernServerPluginCommandCodec.validateDirectoryName(".hidden"))
        assertNull(TavernServerPluginCommandCodec.validateRepositoryUrl("https://github.com/org/plugin"))
        assertNotNull(TavernServerPluginCommandCodec.validateRepositoryUrl("http://insecure.example/repo"))

        val output = """
            ==== SillyTavern server plugins ====
            plugin.root=${b64("/home/SillyTavern/plugins")}
            plugin.enabledInConfig=true
            plugin.record=${b64("sample-plugin")}|${b64("Sample Plugin")}|${b64("1.2.0")}|${b64("A test server plugin")}|512|${b64("https://github.com/org/sample-plugin.git")}|abc1234|true
            ==== end SillyTavern server plugins ====
        """.trimIndent()
        val parsed = TermuxStructuredOutputParser.parse(output).serverPluginsSnapshot
        assertNotNull(parsed)
        parsed!!
        assertEquals("/home/SillyTavern/plugins", parsed.rootDirectory)
        assertTrue(parsed.enabledInConfig)
        assertEquals(1, parsed.plugins.size)
        assertEquals("Sample Plugin", parsed.plugins[0].displayName)
        assertEquals("1.2.0", parsed.plugins[0].version)
        assertEquals("abc1234", parsed.plugins[0].revision)
        assertTrue(parsed.plugins[0].isGitRepository)
    }

    @Test
    fun `git stash parser reads stash entries and recognizes lukoa managed stashes`() {
        assertTrue(TavernGitStashCommandCodec.isValidIndex(0))
        assertFalse(TavernGitStashCommandCodec.isValidIndex(-1))
        assertFalse(TavernGitStashCommandCodec.isValidIndex(1000))

        val output = """
            ==== SillyTavern git stash ====
            stash.available=true
            stash.entry=stash@{0}	On release: lukoa-before-update-20261123	2026-11-23 19:00:00 +0800
            stash.entry=stash@{1}	WIP on release: manual tweaks	2026-11-22 10:00:00 +0800
            ==== end SillyTavern git stash ====
        """.trimIndent()
        val parsed = TermuxStructuredOutputParser.parse(output).gitStashSnapshot
        assertNotNull(parsed)
        val (available, entries) = parsed!!
        assertTrue(available)
        assertEquals(2, entries.size)
        assertEquals(0, entries[0].index)
        assertTrue(entries[0].isLukoaManaged)
        assertEquals(1, entries[1].index)
        assertFalse(entries[1].isLukoaManaged)
    }

    @Test
    fun `quick import codec sanitizes filenames and parser extracts paths and result`() {
        assertEquals("Alice_Card.png", TavernQuickImportCommandCodec.sanitizeFileName("../Alice:Card.png", "png"))
        assertEquals("default-user", TavernQuickImportCommandCodec.normalizeUserHandle("bad handle!"))
        assertEquals("alice-1", TavernQuickImportCommandCodec.normalizeUserHandle("alice-1"))

        val encoded = TavernQuickImportCommandCodec.encode(
            kind = TavernQuickImportKind.Character,
            stagedPath = "/storage/emulated/0/Download/LukoaLauncher/imports/card.png",
            userHandle = "default-user",
        )
        val decoded = TavernQuickImportCommandCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(TavernQuickImportKind.Character, decoded!!.first)
        assertEquals("default-user", decoded.second)
        assertEquals("/storage/emulated/0/Download/LukoaLauncher/imports/card.png", decoded.third)

        val pathsOutput = """
            ==== SillyTavern quick import paths ====
            import.dataRoot=/home/SillyTavern/data
            import.userRoot=/home/SillyTavern/data/default-user
            import.dir.character=/home/SillyTavern/data/default-user/characters
            import.dir.world=/home/SillyTavern/data/default-user/worlds
            import.dir.preset=/home/SillyTavern/data/default-user/OpenAI Settings
            import.dir.theme=/home/SillyTavern/data/default-user/themes
            import.dir.chats=/home/SillyTavern/data/default-user/chats
            ==== end SillyTavern quick import paths ====
            ==== SillyTavern quick import ====
            import.kind=character
            import.targetFile=/home/SillyTavern/data/default-user/characters/card.png
            ==== end SillyTavern quick import ====
        """.trimIndent()
        val structured = TermuxStructuredOutputParser.parse(pathsOutput)
        assertEquals("/home/SillyTavern/data/default-user/characters", structured.quickImportPaths?.characterDir)
        assertEquals("/home/SillyTavern/data/default-user/OpenAI Settings", structured.quickImportPaths?.presetDir)
        assertEquals("/home/SillyTavern/data/default-user/characters/card.png", structured.quickImportedFile)
    }

    @Test
    fun `phase b commands have centralized presentation and timeout policies`() {
        listOf(
            "tavern-storage-scan",
            "tavern-storage-clean",
            "tavern-server-plugins-list",
            "tavern-server-plugins-install",
            "tavern-server-plugins-update",
            "tavern-server-plugins-delete",
            "tavern-git-stash-list",
            "tavern-git-stash-apply",
            "tavern-git-stash-drop",
            "tavern-quick-import-paths",
            "tavern-quick-import",
        ).forEach { cmd ->
            assertEquals(TermuxCommandPresentation.Background, TermuxCommandPresentationPolicy.forCommand(cmd))
            assertTrue(TermuxCommandTimeoutPolicy.hasExplicitTimeout(cmd))
        }
        assertEquals(
            TermuxCommandPresentation.Foreground,
            TermuxCommandPresentationPolicy.forCommand("tavern-extensions-update-all"),
        )
        assertTrue(TermuxCommandTimeoutPolicy.hasExplicitTimeout("tavern-extensions-update-all"))
    }
}
