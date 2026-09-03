package moe.lukoa.launcher

/**
 * Node.js 堆内存上限（MB，对应 --max-old-space-size）。预设 2/4/6GB，允许在范围内手填。
 * 这个值只是写进启动器自己的 env 文件，下次启动酒馆时才会生效，所以不需要先停酒馆。
 */
object TavernNodeMemoryPolicy {
    val presetMegabytes = listOf(2048, 4096, 6144)
    const val MIN_MEGABYTES = 512
    const val MAX_MEGABYTES = 16384

    fun isAllowed(megabytes: Int?): Boolean =
        megabytes != null && megabytes in MIN_MEGABYTES..MAX_MEGABYTES

    fun label(megabytes: Int): String = when {
        megabytes >= 1024 && megabytes % 1024 == 0 -> "${megabytes / 1024}GB"
        else -> "${megabytes}MB"
    }

    /** 返回 null 表示输入合法，否则是给用户看的错误说明。 */
    fun validateCustomInput(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "请输入数字，单位是 MB。"
        val value = trimmed.toIntOrNull() ?: return "只能输入整数，单位是 MB。"
        return when {
            value < MIN_MEGABYTES -> "最小 ${MIN_MEGABYTES}MB，再小酒馆起不来。"
            value > MAX_MEGABYTES -> "最大 ${label(MAX_MEGABYTES)}。"
            else -> null
        }
    }
}
