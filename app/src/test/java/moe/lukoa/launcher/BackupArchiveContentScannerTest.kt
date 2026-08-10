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
            "SillyTavern/data/default-user/worlds/MintWorld.json",
            "SillyTavern/public/scripts/extensions/third-party/MintTools/manifest.json",
        )

        val summary = BackupArchiveContentScanner.scan(ByteArrayInputStream(archive))

        assertEquals(listOf("Mint"), summary.group(BackupArchiveContentKind.CharacterCards)?.names)
        assertEquals(listOf("CoolPreset"), summary.group(BackupArchiveContentKind.Presets)?.names)
        assertEquals(listOf("CleanRegex"), summary.group(BackupArchiveContentKind.RegexScripts)?.names)
        assertEquals(listOf("Mint"), summary.group(BackupArchiveContentKind.Chats)?.names)
        assertEquals(listOf("MintWorld"), summary.group(BackupArchiveContentKind.WorldBooks)?.names)
        assertEquals(listOf("MintTools"), summary.group(BackupArchiveContentKind.Extensions)?.names)
        assertEquals(6, summary.groups.sumOf { it.entryCount })
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
        assertEquals(
            listOf(
                "启动整理",
                "嵌套脚本",
                "旧版脚本",
                "旧版嵌套",
                "预设脚本",
                "角色脚本",
            ),
            summary.group(BackupArchiveContentKind.TavernHelperScripts)?.names,
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
