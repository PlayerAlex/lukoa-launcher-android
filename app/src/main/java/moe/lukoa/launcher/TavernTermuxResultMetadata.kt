package moe.lukoa.launcher

data class TavernTermuxResultMetadata(
    val profileId: String = "",
    val runtimeStateDir: String = "",
)

object TavernTermuxResultMetadataParser {
    private val profileIdJsonRegex = Regex(""""profileId"\s*:\s*"([^"]+)"""")
    private val runtimeStateDirJsonRegex = Regex(""""runtimeStateDir"\s*:\s*"([^"]+)"""")

    fun parse(output: String): TavernTermuxResultMetadata {
        if (output.isBlank()) return TavernTermuxResultMetadata()
        return TavernTermuxResultMetadata(
            profileId = output.firstValue(
                jsonRegex = profileIdJsonRegex,
                linePrefix = "profile_id=",
            ),
            runtimeStateDir = output.firstValue(
                jsonRegex = runtimeStateDirJsonRegex,
                linePrefix = "runtime_state_dir=",
            ),
        )
    }

    private fun String.firstValue(
        jsonRegex: Regex,
        linePrefix: String,
    ): String {
        jsonRegex.find(this)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        return lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(linePrefix) }
            ?.substringAfter(linePrefix)
            ?.trim()
            .orEmpty()
    }
}

object TavernRuntimeStateProfileKey {
    fun sanitize(profileId: String): String {
        val normalized = buildString {
            profileId.ifBlank { TavernProfileDefaults.MAIN_PROFILE_ID }.forEach { char ->
                append(
                    if (char.isLetterOrDigit() || char == '.' || char == '_' || char == '-') {
                        char
                    } else {
                        '_'
                    },
                )
            }
        }
        return normalized.ifBlank { TavernProfileDefaults.MAIN_PROFILE_ID }
    }

    fun matchesRuntimeStateDir(
        profileId: String,
        runtimeStateDir: String,
    ): Boolean {
        val normalizedRuntimeDir = runtimeStateDir.trim().replace('\\', '/').trimEnd('/')
        if (normalizedRuntimeDir.isBlank()) return false
        return normalizedRuntimeDir.endsWith("/profiles/${sanitize(profileId)}")
    }
}
