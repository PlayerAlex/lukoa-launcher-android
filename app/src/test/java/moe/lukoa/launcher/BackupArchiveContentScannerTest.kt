package moe.lukoa.launcher

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupArchiveContentScannerTest {
    @Test
    fun `scanner groups named user content without returning the raw archive listing`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/characters/Mint.png",
            "SillyTavern/data/default-user/OpenAI Settings/CoolPreset.json",
            "SillyTavern/data/default-user/regex/CleanRegex.json",
            "SillyTavern/data/default-user/chats/Mint/2026-08-10.jsonl",
            "SillyTavern/data/default-user/chats/Mint/2026-08-11.jsonl",
            "SillyTavern/data/default-user/chats/Aqua/first.jsonl",
            "SillyTavern/data/default-user/group chats/Friends.jsonl",
            "SillyTavern/data/default-user/worlds/MintWorld.json",
            "SillyTavern/public/scripts/extensions/third-party/MintTools/manifest.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(listOf("Mint"), summary.group(BackupArchiveContentKind.CharacterCards)?.names)
        assertEquals(listOf("CoolPreset"), summary.group(BackupArchiveContentKind.Presets)?.names)
        assertEquals(listOf("CleanRegex"), summary.group(BackupArchiveContentKind.RegexScripts)?.names)
        assertEquals(
            listOf(
                BackupArchiveContentNode(
                    title = "Mint",
                    entryCount = 2,
                    names = listOf("2026-08-10", "2026-08-11"),
                ),
                BackupArchiveContentNode(
                    title = "Aqua",
                    entryCount = 1,
                    names = listOf("first"),
                ),
                BackupArchiveContentNode(
                    title = "群聊",
                    entryCount = 1,
                    names = listOf("Friends"),
                ),
            ),
            summary.group(BackupArchiveContentKind.Chats)?.children,
        )
        assertEquals(listOf("MintWorld"), summary.group(BackupArchiveContentKind.WorldBooks)?.names)
        assertEquals(listOf("MintTools"), summary.group(BackupArchiveContentKind.Extensions)?.names)
        assertEquals(9, summary.groups.sumOf { it.entryCount })
    }

    @Test
    fun `scanner summarizes user data extensions config and manifest without extracting`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/chats/chat.jsonl",
            "SillyTavern/data/default-user/characters/Mint.png",
            "SillyTavern/public/scripts/extensions/third-party/Mint/manifest.json",
            "SillyTavern/config.yaml",
            "LUKOA_BACKUP_MANIFEST.txt",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(5, summary.entryCount)
        assertTrue(summary.hasUserData)
        assertTrue(summary.hasExtensions)
        assertTrue(summary.hasConfiguration)
        assertTrue(summary.hasLukoaManifest)
        assertFalse(summary.truncated)
    }

    @Test
    fun `scanner does not mistake nested character assets for cards or presets`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/characters/真正的角色卡.png",
            "SillyTavern/data/default-user/characters/真正的角色卡/emotions/joy.png",
            "SillyTavern/data/default-user/characters/真正的角色卡/emotions/neutral.webp",
            "SillyTavern/data/default-user/characters/真正的角色卡/context/Alpaca.json",
            "SillyTavern/data/default-user/instruct/真正的预设.json",
            "SillyTavern/data/default-user/instruct/资源目录/ChatML.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("真正的角色卡"),
            summary.group(BackupArchiveContentKind.CharacterCards)?.names,
        )
        assertEquals(null, summary.group(BackupArchiveContentKind.Presets))
        assertEquals(
            listOf("真正的预设"),
            summary.group(BackupArchiveContentKind.PromptTemplates)?.names,
        )
    }

    @Test
    fun `scanner keeps bundled generation and prompt templates out of user presets`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/OpenAI Settings/我保存的预设.json",
            "SillyTavern/data/default-user/NovelAI Settings/Asper-Kayra.json",
            "SillyTavern/data/default-user/TextGen Settings/Divine Intellect.json",
            "SillyTavern/data/default-user/KoboldAI Settings/GUI KoboldAI.json",
            "SillyTavern/data/default-user/instruct/ChatML.json",
            "SillyTavern/data/default-user/context/Default.json",
            "SillyTavern/data/default-user/sysprompt/Default.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("我保存的预设"),
            summary.group(BackupArchiveContentKind.Presets)?.names,
        )
        assertEquals(
            listOf("Asper-Kayra", "Divine Intellect", "GUI KoboldAI"),
            summary.group(BackupArchiveContentKind.GenerationTemplates)?.names,
        )
        assertEquals(
            listOf("ChatML", "Default"),
            summary.group(BackupArchiveContentKind.PromptTemplates)?.names,
        )
    }

    @Test
    fun `scanner reads extension display names and tavern helper scripts from supported json`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/regex/CleanRegex.json" to "{}",
            "SillyTavern/data/default-user/themes/清凉薄荷.json" to "{}",
            "SillyTavern/data/default-user/user.css" to ":root { --mint: #6db5a4; }",
            "SillyTavern/public/scripts/extensions/third-party/JS-Slash-Runner/manifest.json" to
                """{"display_name":"酒馆助手","author":"KAKAA"}""",
            "SillyTavern/data/default-user/settings.json" to
                """
                {
                  "extension_settings": {
                    "tavern_helper": {
                      "script": {
                        "scripts": [
                          {"type":"script","name":"启动整理"},
                          {"type":"folder","name":"工具箱","scripts":[
                            {"type":"script","name":"嵌套脚本"}
                          ]}
                        ]
                      }
                    },
                    "TavernHelper": {
                      "script": {
                        "scriptsRepository": [
                          {"type":"script","value":{"name":"旧版脚本"}},
                          {"type":"folder","name":"旧工具箱","value":[
                            {"type":"script","value":{"name":"旧版嵌套"}}
                          ]}
                        ]
                      }
                    }
                  }
                }
                """.trimIndent(),
            "SillyTavern/data/default-user/OpenAI Settings/清凉预设.json" to
                """
                {
                  "extensions": {
                    "tavern_helper": {
                      "scripts": [
                        {"type":"script","name":"预设脚本"}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            "SillyTavern/data/default-user/characters/脚本角色.json" to
                """
                {
                  "data": {
                    "extensions": {
                      "tavern_helper": {
                        "scripts": [
                          {"type":"script","name":"角色脚本"}
                        ]
                      }
                    }
                  }
                }
                """.trimIndent(),
            "SillyTavern/node_modules/fake/package.json" to
                """{"extensions":{"tavern_helper":{"scripts":[{"type":"script","name":"依赖伪数据"}]}}}""",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("CleanRegex"),
            summary.group(BackupArchiveContentKind.RegexScripts)?.names,
        )
        assertEquals(
            listOf("酒馆助手"),
            summary.group(BackupArchiveContentKind.Extensions)?.names,
        )
        assertEquals(
            listOf("清凉薄荷", "自定义 CSS"),
            summary.group(BackupArchiveContentKind.Beautification)?.names,
        )
        val scriptGroup = summary.group(BackupArchiveContentKind.TavernHelperScripts)
        assertEquals(emptyList<String>(), scriptGroup?.names)
        assertEquals(
            listOf(
                BackupArchiveContentNode(
                    title = "全局脚本",
                    entryCount = 4,
                    names = listOf("启动整理", "嵌套脚本", "旧版脚本", "旧版嵌套"),
                ),
                BackupArchiveContentNode(
                    title = "预设脚本",
                    entryCount = 1,
                    children = listOf(
                        BackupArchiveContentNode(
                            title = "清凉预设",
                            entryCount = 1,
                            names = listOf("预设脚本"),
                        ),
                    ),
                ),
                BackupArchiveContentNode(
                    title = "局部脚本",
                    entryCount = 1,
                    children = listOf(
                        BackupArchiveContentNode(
                            title = "脚本角色",
                            entryCount = 1,
                            names = listOf("角色脚本"),
                        ),
                    ),
                ),
            ),
            scriptGroup?.children,
        )
    }

    @Test
    fun `scanner reads global tavern helper scripts from large user settings`() {
        val padding = "x".repeat(BackupArchiveContentScanner.MAX_INSPECTABLE_JSON_BYTES)
        val archive = tarGzip(
            "SillyTavern/data/default-user/settings.json" to
                """
                {
                  "extension_settings": {
                    "tavern_helper": {
                      "script": {
                        "scripts": [
                          {"type":"script","name":"Large global script"}
                        ]
                      }
                    }
                  },
                  "padding": "$padding"
                }
                """.trimIndent(),
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("Large global script"),
            summary.group(BackupArchiveContentKind.TavernHelperScripts)
                ?.children
                ?.single()
                ?.names,
        )
    }

    @Test
    fun `scanner accepts extensions as alternate global settings root`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/settings.json" to
                """
                {
                  "extensions": {
                    "tavern_helper": {
                      "script": {
                        "scripts": [
                          {"type":"script","name":"Alternate root script"}
                        ]
                      }
                    }
                  }
                }
                """.trimIndent(),
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("Alternate root script"),
            summary.group(BackupArchiveContentKind.TavernHelperScripts)
                ?.children
                ?.single()
                ?.names,
        )
    }

    @Test
    fun `scanner returns backup content groups in product display order`() {
        val archive = tarGzip(
            "SillyTavern/data/default-user/NovelAI Settings/Generation.json" to "{}",
            "SillyTavern/data/default-user/instruct/Prompt.json" to "{}",
            "SillyTavern/data/default-user/themes/Theme.json" to "{}",
            "SillyTavern/data/default-user/OpenAI Settings/Preset.json" to "{}",
            "SillyTavern/data/default-user/settings.json" to
                """{"extension_settings":{"tavern_helper":{"script":{"scripts":[{"type":"script","name":"Global"}]}}}}""",
            "SillyTavern/data/default-user/characters/Character.png" to "card",
            "SillyTavern/data/default-user/worlds/World.json" to "{}",
            "SillyTavern/data/default-user/chats/Character/Chat.jsonl" to "{}",
            "SillyTavern/data/default-user/regex/Regex.json" to "{}",
            "SillyTavern/public/scripts/extensions/third-party/Extension/manifest.json" to "{}",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf(
                BackupArchiveContentKind.RegexScripts,
                BackupArchiveContentKind.Extensions,
                BackupArchiveContentKind.GenerationTemplates,
                BackupArchiveContentKind.PromptTemplates,
                BackupArchiveContentKind.Beautification,
                BackupArchiveContentKind.Presets,
                BackupArchiveContentKind.TavernHelperScripts,
                BackupArchiveContentKind.CharacterCards,
                BackupArchiveContentKind.WorldBooks,
                BackupArchiveContentKind.Chats,
            ),
            summary.groups.map(BackupArchiveContentGroup::kind),
        )
    }

    @Test
    fun `scanner lists extension directories even when manifest is missing`() {
        val archive = tarGzip(
            "SillyTavern/public/scripts/extensions/third-party/NoManifest/index.js" to "export default {};",
            "SillyTavern/plugins/ServerPlugin/index.js" to "module.exports = {};",
            "SillyTavern/public/scripts/extensions/third-party/NamedExtension/manifest.json" to
                """{"display_name":"有名称的扩展"}""",
            "SillyTavern/public/scripts/extensions/.lukoa-disabled-third-party/PausedExtension/manifest.json" to
                """{"display_name":"暂停扩展"}""",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))
        val extensions = summary.group(BackupArchiveContentKind.Extensions)

        assertEquals(4, extensions?.entryCount)
        assertEquals(
            listOf("NoManifest", "ServerPlugin", "有名称的扩展", "暂停扩展（已停用）"),
            extensions?.names,
        )
    }

    @Test
    fun `scanner lists regex files from an external data root`() {
        val archive = tarGzip(
            "SillyTavern/config.yaml" to "dataRoot: /storage/emulated/0/TavernData",
            "TavernData/default-user/regex/清理标记.json" to "{}",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("清理标记"),
            summary.group(BackupArchiveContentKind.RegexScripts)?.names,
        )
    }

    @Test
    fun `display groups normalize summaries loaded from an older cache`() {
        val summary = BackupArchiveContentSummary(
            entryCount = 4,
            hasUserData = true,
            hasExtensions = false,
            hasConfiguration = false,
            hasLukoaManifest = true,
            truncated = false,
            groups = listOf(
                BackupArchiveContentGroup(BackupArchiveContentKind.Chats, 1),
                BackupArchiveContentGroup(BackupArchiveContentKind.CharacterCards, 1),
                BackupArchiveContentGroup(BackupArchiveContentKind.Presets, 1),
                BackupArchiveContentGroup(BackupArchiveContentKind.GenerationTemplates, 1),
            ),
        )

        assertEquals(
            listOf(
                BackupArchiveContentKind.GenerationTemplates,
                BackupArchiveContentKind.Presets,
                BackupArchiveContentKind.CharacterCards,
                BackupArchiveContentKind.Chats,
            ),
            summary.displayGroups.map(BackupArchiveContentGroup::kind),
        )
    }

    @Test
    fun `scanner falls back to extension directory and skips oversized json inspection`() {
        val oversizedJson = " ".repeat(BackupArchiveContentScanner.MAX_INSPECTABLE_JSON_BYTES + 1)
        val archive = tarGzip(
            "SillyTavern/public/scripts/extensions/third-party/PlainExtension/manifest.json" to "{}",
            "SillyTavern/data/default-user/settings.json" to oversizedJson,
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(
            listOf("PlainExtension"),
            summary.group(BackupArchiveContentKind.Extensions)?.names,
        )
        assertEquals(null, summary.group(BackupArchiveContentKind.TavernHelperScripts))
    }

    @Test
    fun `scanner keeps looking for regex and extensions after the general preview limit`() {
        val fillerEntries = (0 until BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES)
            .map { "SillyTavern/src/filler/$it.js" to "" }
        val archive = tarGzip(
            *(
                fillerEntries + listOf(
                    "SillyTavern/data/default-user/regex/后段正则.json" to "{}",
                    "SillyTavern/public/scripts/extensions/third-party/LateExtension/manifest.json" to
                        """{"display_name":"后段扩展"}""",
                )
            ).toTypedArray(),
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertTrue(summary.truncated)
        assertEquals(BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES, summary.entryCount)
        assertEquals(
            listOf("后段正则"),
            summary.group(BackupArchiveContentKind.RegexScripts)?.names,
        )
        assertEquals(
            listOf("后段扩展"),
            summary.group(BackupArchiveContentKind.Extensions)?.names,
        )
    }

    @Test
    fun `scanner rejects traversal and marks oversized listings truncated`() {
        val unsafe = tarGzip("../outside.txt")
        runCatching { BackupArchiveContentScanner.scan(ByteArrayInputStream(unsafe)) }
            .onSuccess { error("expected unsafe archive rejection") }

        val manyEntries = (0..BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES)
            .map { "SillyTavern/data/default-user/chats/$it.jsonl" }
            .toTypedArray()
        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(tarGzip(*manyEntries)))
        assertTrue(summary.truncated)
        assertEquals(BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES, summary.entryCount)
        val chatGroup = summary.group(BackupArchiveContentKind.Chats)
        assertFalse(chatGroup?.namesTruncated ?: true)
        assertEquals(
            BackupArchiveContentScanner.MAX_PREVIEW_ENTRIES,
            chatGroup?.children?.single()?.names?.size,
        )
    }

    private fun tarGzip(vararg entries: String): ByteArray {
        return tarGzip(*entries.map { it to "" }.toTypedArray())
    }

    private fun tarGzip(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        GzipCompressorOutputStream(bytes).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                entries.forEach { (name, content) ->
                    val contentBytes = content.toByteArray(Charsets.UTF_8)
                    val entry = TarArchiveEntry(name).apply { size = contentBytes.size.toLong() }
                    tar.putArchiveEntry(entry)
                    tar.write(contentBytes)
                    tar.closeArchiveEntry()
                }
                tar.finish()
            }
        }
        return bytes.toByteArray()
    }
}
