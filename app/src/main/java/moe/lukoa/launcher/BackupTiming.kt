package moe.lukoa.launcher

const val MIN_AUTO_BACKUP_INTERVAL_MINUTES = 10
const val MAX_AUTO_BACKUP_INTERVAL_MINUTES = 12 * 60
const val AUTO_BACKUP_INTERVAL_STEP_MINUTES = 10

fun formatBackupInterval(minutes: Int): String {
    val safeMinutes = minutes.coerceIn(
        MIN_AUTO_BACKUP_INTERVAL_MINUTES,
        MAX_AUTO_BACKUP_INTERVAL_MINUTES,
    )
    val hours = safeMinutes / 60
    val restMinutes = safeMinutes % 60
    return when {
        hours == 0 -> "${safeMinutes} 分钟"
        restMinutes == 0 -> "${hours} 小时"
        else -> "${hours} 小时 ${restMinutes} 分钟"
    }
}
