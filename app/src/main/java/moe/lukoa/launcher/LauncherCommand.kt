package moe.lukoa.launcher

data class LauncherCommand(
    val name: String,
    val argument: String? = null,
)

object LauncherCommandCodec {
    private const val SEPARATOR = "::"

    fun decode(value: String): LauncherCommand {
        val separatorIndex = value.indexOf(SEPARATOR)
        if (separatorIndex < 0) {
            return LauncherCommand(name = value)
        }
        return LauncherCommand(
            name = value.substring(0, separatorIndex),
            argument = value.substring(separatorIndex + SEPARATOR.length)
                .takeIf { it.isNotBlank() },
        )
    }

    fun encode(name: String, argument: String? = null): String {
        require(name.isNotBlank()) { "command name cannot be blank" }
        require(!name.contains(SEPARATOR)) { "command name cannot contain separator" }
        require(name.none { it == '\n' || it == '\r' || it.code < 32 }) {
            "command name cannot contain control characters"
        }
        return argument?.takeIf { it.isNotBlank() }
            ?.let { "$name$SEPARATOR$it" }
            ?: name
    }
}
