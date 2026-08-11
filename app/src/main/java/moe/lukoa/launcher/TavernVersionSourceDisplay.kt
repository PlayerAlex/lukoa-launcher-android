package moe.lukoa.launcher

internal object TavernVersionSourceDisplay {
    fun label(repoUrl: String): String {
        val normalized = repoUrl.trim()
        if (normalized.isBlank() || normalized.equals("unknown", ignoreCase = true)) return "未读取"
        return if (normalized.contains("github.com", ignoreCase = true)) {
            "GitHub"
        } else {
            normalized
        }
    }
}
