package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernExtensionRecord(
    val directoryName: String,
    val displayName: String,
    val version: String,
    val hasManifest: Boolean,
)

data class TavernExtensionSnapshot(
    val rootDirectory: String,
    val extensions: List<TavernExtensionRecord>,
)

data class TavernExtensionManagementState(
    val rootDirectory: String = "",
    val extensions: List<TavernExtensionRecord> = emptyList(),
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的扩展。",
)

object TavernExtensionOutputParser {
    private const val HEADER = "==== SillyTavern extensions ===="

    fun parse(output: String): TavernExtensionSnapshot? {
        if (!output.contains(HEADER)) return null
        var rootDirectory = ""
        val extensions = mutableListOf<TavernExtensionRecord>()
        output.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("extension.root=") -> {
                    rootDirectory = decode(line.substringAfter('=')) ?: rootDirectory
                }

                line.startsWith("extension.record=") -> {
                    val fields = line.substringAfter('=').split('|')
                    if (fields.size != 4) return@forEach
                    val directoryName = decode(fields[0]) ?: return@forEach
                    if (TavernExtensionCommandCodec.validateDirectoryName(directoryName) != null) return@forEach
                    extensions += TavernExtensionRecord(
                        directoryName = directoryName,
                        displayName = decode(fields[1]).orEmpty().ifBlank { directoryName },
                        version = decode(fields[2]).orEmpty(),
                        hasManifest = fields[3] == "true",
                    )
                }
            }
        }
        return TavernExtensionSnapshot(
            rootDirectory = rootDirectory,
            extensions = extensions.sortedBy { it.displayName.lowercase() },
        )
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
