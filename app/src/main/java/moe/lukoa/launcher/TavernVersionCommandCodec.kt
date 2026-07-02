package moe.lukoa.launcher

import java.util.Base64

data class TavernVersionCommandArgs(
    val target: String,
    val repoUrl: String,
    val commit: String = "",
)

object TavernVersionCommandCodec {
    private const val SEPARATOR = "."

    fun encode(target: String, repoUrl: String, commit: String = ""): String {
        return listOf(target.trim(), repoUrl.trim(), commit.trim())
            .joinToString(SEPARATOR) { encodePart(it) }
    }

    fun decode(value: String?): TavernVersionCommandArgs? {
        val parts = value.orEmpty().split(SEPARATOR, limit = 3)
        if (parts.size != 3 || parts[0].isBlank() || parts[1].isBlank()) return null
        return try {
            TavernVersionCommandArgs(
                target = decodePart(parts[0]).trim(),
                repoUrl = decodePart(parts[1]).trim(),
                commit = decodePart(parts[2]).trim(),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun encodePart(value: String): String {
        return Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodePart(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }
}
