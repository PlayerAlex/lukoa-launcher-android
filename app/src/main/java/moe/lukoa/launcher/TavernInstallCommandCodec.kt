package moe.lukoa.launcher

import java.util.Base64

data class TavernInstallCommandArgs(
    val target: String,
    val repoUrl: String,
    val configPolicy: AptConfigPolicy,
)

object TavernInstallCommandCodec {
    private const val SEPARATOR = "."

    fun encode(
        target: String,
        repoUrl: String,
        configPolicy: AptConfigPolicy,
    ): String {
        return listOf(
            encodePart(target.trim()),
            encodePart(repoUrl.trim()),
            encodePart(configPolicy.wireValue),
        ).joinToString(SEPARATOR)
    }

    fun decode(value: String?): TavernInstallCommandArgs? {
        val parts = value.orEmpty().split(SEPARATOR, limit = 3)
        if (parts.size != 3 || parts[0].isBlank() || parts[1].isBlank()) return null
        return try {
            TavernInstallCommandArgs(
                target = decodePart(parts[0]).trim(),
                repoUrl = decodePart(parts[1]).trim(),
                configPolicy = AptConfigPolicyCodec.normalize(decodePart(parts[2])),
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
