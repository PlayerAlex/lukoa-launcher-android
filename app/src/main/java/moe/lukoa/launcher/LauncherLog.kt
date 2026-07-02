package moe.lukoa.launcher

import java.time.LocalTime

fun logEntry(source: String, text: String): String {
    return "[${LocalTime.now().withNano(0)}] $source\n${text.trim()}"
}

fun appendLog(current: String, source: String, text: String): String {
    val entry = logEntry(source, text)
    val next = if (current.startsWith("暂无 ")) entry else "$current\n\n$entry"
    return next.takeLast(12000)
}
