package moe.lukoa.launcher

import java.util.Locale

internal fun formatStorageKilobytes(kilobytes: Long): String {
    val safeKilobytes = kilobytes.coerceAtLeast(0L)
    return when {
        safeKilobytes >= 1024L * 1024L -> String.format(
            Locale.ROOT,
            "%.1f GB",
            safeKilobytes / 1024.0 / 1024.0,
        )
        safeKilobytes >= 1024L -> String.format(
            Locale.ROOT,
            "%.1f MB",
            safeKilobytes / 1024.0,
        )
        else -> "$safeKilobytes KB"
    }
}
