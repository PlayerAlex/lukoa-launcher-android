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

fun appendRawLog(current: String, text: String): String {
    val entry = text.trim()
    if (entry.isBlank()) return current
    val next = if (current.startsWith("暂无 ")) entry else "$current\n\n$entry"
    return next.takeLast(120_000)
}

// Live deltas are consecutive slices of the same log, so they join with a single newline instead
// of the blank line that separates unrelated entries.
fun appendLiveRuntimeLog(current: String, delta: String): String {
    val entry = delta.trim('\n')
    if (entry.isBlank()) return current
    val next = if (current.isBlank() || current.startsWith("暂无 ")) entry else "${current.trimEnd('\n')}\n$entry"
    return next.takeLast(120_000)
}
