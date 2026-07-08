package moe.lukoa.launcher

object TavernComparablePath {
    fun normalize(value: String): String {
        val normalized = value.trim().replace('\\', '/').trimEnd('/')
        if (normalized.isBlank()) return ""
        return when {
            normalized == "~" || normalized == "\$HOME" -> TavernPathDefaults.TERMUX_HOME_DIR
            normalized.startsWith("~/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${normalized.removePrefix("~/")}".trimEnd('/')

            normalized.startsWith("\$HOME/") ->
                "${TavernPathDefaults.TERMUX_HOME_DIR}/${normalized.removePrefix("\$HOME/")}".trimEnd('/')

            else -> normalized
        }
    }

    fun same(left: String, right: String): Boolean {
        return normalize(left) == normalize(right)
    }
}
