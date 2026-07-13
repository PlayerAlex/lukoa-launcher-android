package moe.lukoa.launcher

object TavernComparablePath {
    fun normalize(value: String): String {
        val trimmed = value.trim().replace('\\', '/')
        if (trimmed.isBlank()) return ""
        val expanded = when {
            trimmed == "~" || trimmed == "\$HOME" -> TavernPathDefaults.TERMUX_HOME_DIR
            trimmed.startsWith("~/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${trimmed.removePrefix("~/")}"

            trimmed.startsWith("\$HOME/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${trimmed.removePrefix("\$HOME/")}"

            else -> trimmed
        }
        val collapsed = expanded.replace(Regex("/+"), "/")
        val absolute = collapsed.startsWith("/")
        val segments = mutableListOf<String>()
        collapsed.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> {
                    if (segments.isNotEmpty() && segments.last() != "..") {
                        segments.removeAt(segments.lastIndex)
                    } else if (!absolute) {
                        segments += segment
                    }
                }

                else -> segments += segment
            }
        }
        val joined = segments.joinToString("/")
        return when {
            absolute && joined.isBlank() -> "/"
            absolute -> "/$joined"
            else -> joined
        }
    }

    fun same(left: String, right: String): Boolean {
        return normalize(left) == normalize(right)
    }
}
