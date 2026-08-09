package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernExtensionRecord(
    val directoryName: String,
    val displayName: String,
    val version: String,
    val hasManifest: Boolean,
    val author: String = "",
    val directoryKilobytes: Long? = null,
    val enabled: Boolean = true,
)

data class TavernExtensionSnapshot(
    val rootDirectory: String,
    val disabledRootDirectory: String,
    val extensions: List<TavernExtensionRecord>,
)

data class TavernExtensionManagementState(
    val rootDirectory: String = "",
    val disabledRootDirectory: String = "",
    val extensions: List<TavernExtensionRecord> = emptyList(),
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的扩展。",
)

object TavernExtensionOutputParser {
    private const val HEADER = "==== SillyTavern extensions ===="
    private const val FOOTER = "==== end SillyTavern extensions ===="

    fun parse(output: String): TavernExtensionSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var rootDirectory = ""
        var disabledRootDirectory = ""
        val extensions = mutableListOf<TavernExtensionRecord>()
        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("extension.root=") -> {
                    rootDirectory = decode(line.substringAfter('=')) ?: rootDirectory
                }

                line.startsWith("extension.disabledRoot=") -> {
                    disabledRootDirectory = decode(line.substringAfter('=')) ?: disabledRootDirectory
                }

                line.startsWith("extension.record=") -> {
                    val fields = line.substringAfter('=').split('|')
                    if (fields.size !in 4..7) return@forEach
                    val directoryName = decode(fields[0]) ?: return@forEach
                    if (TavernExtensionCommandCodec.validateDirectoryName(directoryName) != null) return@forEach
                    extensions += TavernExtensionRecord(
                        directoryName = directoryName,
                        displayName = decode(fields[1]).orEmpty().ifBlank { directoryName },
                        version = decode(fields[2]).orEmpty(),
                        hasManifest = fields[3] == "true",
                        author = fields.getOrNull(4)?.let(::decode).orEmpty(),
                        directoryKilobytes = fields.getOrNull(5)
                            ?.toLongOrNull()
                            ?.takeIf { it >= 0L },
                        enabled = fields.getOrNull(6) != "false",
                    )
                }
            }
        }
        return TavernExtensionSnapshot(
            rootDirectory = rootDirectory,
            disabledRootDirectory = disabledRootDirectory,
            extensions = extensions.sortedWith(
                compareByDescending<TavernExtensionRecord> { it.enabled }
                    .thenBy { it.displayName.lowercase() },
            ),
        )
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }

    private fun decode(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

object TavernExtensionCommandCodec {
    fun encodeDirectoryName(directoryName: String): String {
        require(validateDirectoryName(directoryName) == null) { "unsafe extension directory name" }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(directoryName.toByteArray(StandardCharsets.UTF_8))
    }

    fun decodeDirectoryName(encoded: String): String? {
        val decoded = try {
            String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return decoded.takeIf { validateDirectoryName(it) == null }
    }

    fun validateDirectoryName(value: String): String? = when {
        value.isBlank() -> "扩展目录名不能为空。"
        value != value.trim() -> "扩展目录名首尾不能包含空格。"
        value == "." || value == ".." -> "扩展目录名不能指向上级目录。"
        value.length > 128 -> "扩展目录名不能超过 128 个字符。"
        value.any { it == '/' || it == '\\' } -> "扩展目录名不能包含路径分隔符。"
        value.any { it.code < 32 || it.code == 127 } -> "扩展目录名不能包含控制字符。"
        else -> null
    }
}
