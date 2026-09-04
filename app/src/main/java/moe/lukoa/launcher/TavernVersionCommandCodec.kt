package moe.lukoa.launcher

import java.util.Base64

data class TavernVersionCommandArgs(
    val target: String,
    val repoUrl: String,
    val commit: String = "",
    val discardLocalChanges: Boolean = false,
) {
    /** Wire value understood by `lukoa-tavern.sh update|rollback <target> <repo> <policy>`. */
    val localChangesPolicy: String
        get() = if (discardLocalChanges) DISCARD_POLICY else KEEP_POLICY

    companion object {
        const val DISCARD_POLICY = "discard"
        const val KEEP_POLICY = "keep"
    }
}

object TavernVersionCommandCodec {
    private const val SEPARATOR = "."
    private const val DISCARD_FLAG = "discard"

    fun encode(
        target: String,
        repoUrl: String,
        commit: String = "",
        discardLocalChanges: Boolean = false,
    ): String {
        val parts = mutableListOf(target.trim(), repoUrl.trim(), commit.trim())
        if (discardLocalChanges) parts += DISCARD_FLAG
        return parts.joinToString(SEPARATOR) { encodePart(it) }
    }

    fun decode(value: String?): TavernVersionCommandArgs? {
        // 3 parts is the historical layout; pending tasks persisted by older versions still use it.
        val parts = value.orEmpty().split(SEPARATOR, limit = 4)
        if (parts.size !in 3..4 || parts[0].isBlank() || parts[1].isBlank()) return null
        return try {
            TavernVersionCommandArgs(
                target = decodePart(parts[0]).trim(),
                repoUrl = decodePart(parts[1]).trim(),
                commit = decodePart(parts[2]).trim(),
                discardLocalChanges = parts.getOrNull(3)?.let(::decodePart) == DISCARD_FLAG,
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
