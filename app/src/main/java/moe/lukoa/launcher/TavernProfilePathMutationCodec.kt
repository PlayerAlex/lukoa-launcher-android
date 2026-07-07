package moe.lukoa.launcher

import java.util.Base64

data class TavernProfileMigrationCommandArgs(
    val targetPath: String,
)

object TavernProfileMigrationCommandCodec {
    fun encode(targetPath: String): String {
        return Base64.getUrlEncoder().encodeToString(targetPath.trim().toByteArray(Charsets.UTF_8))
    }

    fun decode(value: String?): TavernProfileMigrationCommandArgs? {
        val encoded = value?.trim().orEmpty()
        if (encoded.isBlank()) return null
        return runCatching {
            TavernProfileMigrationCommandArgs(
                targetPath = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8).trim(),
            )
        }.getOrNull()
    }
}

object TavernProfilePathMutationOutputParser {
    fun migratedTargetPath(output: String): String? = lineValue(output, "migrated.to")

    fun deletedProfilePath(output: String): String? = lineValue(output, "deleted.profileDir")

    private fun lineValue(output: String, key: String): String? {
        return output.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
