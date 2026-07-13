package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernUserRecord(
    val handle: String,
    val name: String,
    val admin: Boolean,
    val enabled: Boolean,
    val directoryExists: Boolean,
    val directoryKilobytes: Long,
)

data class TavernUserManagementState(
    val users: List<TavernUserRecord> = emptyList(),
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的用户。",
)

object TavernUserOutputParser {
    fun parse(output: String): List<TavernUserRecord>? {
        if (!output.contains("==== SillyTavern users ====")) return null
        return output.lineSequence().mapNotNull { line ->
            if (!line.startsWith("user.record=")) return@mapNotNull null
            val fields = line.substringAfter('=').split('|')
            if (fields.size != 6) return@mapNotNull null
            TavernUserRecord(
                handle = decode(fields[0]) ?: return@mapNotNull null,
                name = decode(fields[1]) ?: return@mapNotNull null,
                admin = fields[2] == "true",
                enabled = fields[3] == "true",
                directoryExists = fields[4] == "true",
                directoryKilobytes = fields[5].toLongOrNull() ?: 0L,
            )
        }.toList()
    }

    private fun decode(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

object TavernUserCommandCodec {
    fun encode(vararg values: String): String = values.joinToString(".") { value ->
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun validateHandle(value: String): String? = when {
        value.isBlank() -> "登录标识不能为空。"
        value.length > 64 -> "登录标识不能超过 64 个字符。"
        !Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$").matches(value) -> "登录标识只能使用小写字母、数字和中划线。"
        else -> null
    }

    fun validateName(value: String): String? = when {
        value.isBlank() -> "显示名称不能为空。"
        value.length > 80 -> "显示名称不能超过 80 个字符。"
        value.any { it == '\n' || it == '\r' || it == '\u0000' } -> "显示名称包含不支持的字符。"
        else -> null
    }
}
