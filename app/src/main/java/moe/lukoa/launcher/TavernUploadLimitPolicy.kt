package moe.lukoa.launcher

object TavernUploadLimitPolicy {
    val allowedMegabytes = listOf(500, 1024, 2048)

    fun isAllowed(megabytes: Int?): Boolean = megabytes != null && megabytes in allowedMegabytes

    fun label(megabytes: Int): String = when (megabytes) {
        1024 -> "1GB"
        2048 -> "2GB"
        else -> "${megabytes}MB"
    }
}
