package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernServerPluginRecord(
    val directoryName: String,
    val displayName: String,
    val version: String,
    val description: String,
    val directoryKilobytes: Long,
    val repositoryUrl: String,
    val revision: String,
    val isGitRepository: Boolean,
)

data class TavernServerPluginSnapshot(
    val rootDirectory: String = "",
    val enabledInConfig: Boolean = false,
    val plugins: List<TavernServerPluginRecord> = emptyList(),
)

data class TavernServerPluginState(
    val snapshot: TavernServerPluginSnapshot? = null,
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的服务器插件。",
)

object TavernServerPluginCommandCodec {
    private val DIR_REGEX = Regex("^[A-Za-z0-9._-]+$")
    private val URL_REGEX = Regex("^https://[^\\s]+$", RegexOption.IGNORE_CASE)

    fun validateDirectoryName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() || trimmed == "." || trimmed == ".." || trimmed.startsWith(".") ->
                "插件目录名无效。"
            !DIR_REGEX.matches(trimmed) -> "插件目录名包含不支持的字符。"
            else -> null
        }
    }

    fun validateRepositoryUrl(url: String): String? {
        val trimmed = url.trim()
        return when {
            trimmed.isBlank() -> "请输入公开的 HTTPS Git 仓库地址。"
            !URL_REGEX.matches(trimmed) -> "仓库地址必须以 https:// 开头且不能包含空格。"
            else -> null
        }
    }

    fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.trim().toByteArray(StandardCharsets.UTF_8))

    fun decode(value: String?): String? {
        val trimmed = value.orEmpty().trim()
        if (trimmed.isBlank()) return null
        return try {
            String(Base64.getUrlDecoder().decode(trimmed), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

object TavernServerPluginOutputParser {
    private const val HEADER = "==== SillyTavern server plugins ===="
    private const val FOOTER = "==== end SillyTavern server plugins ===="

    fun parse(output: String): TavernServerPluginSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var rootDirectory = ""
        var enabledInConfig = false
        val plugins = mutableListOf<TavernServerPluginRecord>()
        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("plugin.root=") -> {
                    rootDirectory = TavernServerPluginCommandCodec.decode(line.substringAfter('=')) ?: rootDirectory
                }
                line.startsWith("plugin.enabledInConfig=") -> {
                    enabledInConfig = line.substringAfter('=') == "true"
                }
                line.startsWith("plugin.record=") -> {
                    val fields = line.substringAfter('=').split('|')
                    if (fields.size < 8) return@forEach
                    val dirName = TavernServerPluginCommandCodec.decode(fields[0]) ?: return@forEach
                    if (TavernServerPluginCommandCodec.validateDirectoryName(dirName) != null) return@forEach
                    plugins += TavernServerPluginRecord(
                        directoryName = dirName,
                        displayName = TavernServerPluginCommandCodec.decode(fields[1]).orEmpty().ifBlank { dirName },
                        version = TavernServerPluginCommandCodec.decode(fields[2]).orEmpty(),
                        description = TavernServerPluginCommandCodec.decode(fields[3]).orEmpty(),
                        directoryKilobytes = fields[4].toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                        repositoryUrl = TavernServerPluginCommandCodec.decode(fields[5]).orEmpty(),
                        revision = fields[6].trim(),
                        isGitRepository = fields[7].trim() == "true",
                    )
                }
            }
        }
        return TavernServerPluginSnapshot(
            rootDirectory = rootDirectory,
            enabledInConfig = enabledInConfig,
            plugins = plugins.sortedBy { it.displayName.lowercase() },
        )
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }
}
