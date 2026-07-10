package moe.lukoa.launcher

private val TavernRunningFieldRegex = Regex(""""running"\s*:\s*(true|false)""")
private val TavernStatusFieldRegex = Regex(""""status"\s*:\s*"([^"]+)"""")
private val TavernRunningFalseRegex = Regex(""""running"\s*:\s*false""")
private val TavernStoppedStatusRegex = Regex(""""status"\s*:\s*"stopped"""")
private val TavernErrorStatusRegex = Regex(""""status"\s*:\s*"error"""")
private val TavernPortConflictRegex = Regex("""Address 127\.0\.0\.1:\d+ is already in use""")
private val TavernPortConflictFieldRegex = Regex("""doctor\.process\.portConflict=(1|true|ok)""", RegexOption.IGNORE_CASE)
private val tavernStartingMarkers = listOf(
    "SillyTavern is starting",
    "SillyTavern launch command accepted",
    "starting in Termux foreground log mode",
    "正在启动酒馆",
)
private val tavernStoppedMarkers = listOf(
    "process is not running",
    "SillyTavern was not running",
    "SillyTavern foreground session exited",
    "SillyTavern stopped",
    "SillyTavern force cleanup completed",
    "SillyTavern process exited immediately",
)
private val tavernFatalStartMarkers = listOf(
    "node command not found",
    "SillyTavern directory not found",
    "no start.sh or server.js found",
)

fun inferExplicitTavernRunning(text: String): Boolean? {
    val latestStatus = TavernStatusFieldRegex.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)
    if (latestStatus == "starting") return null
    return when (TavernRunningFieldRegex.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)) {
        "true" -> true
        "false" -> false
        else -> null
    }
}

fun inferTavernStarting(text: String): Boolean {
    val latestStatus = TavernStatusFieldRegex.findAll(text).lastOrNull()?.groupValues?.getOrNull(1)
    val tail = TavernLogSignals.stripAnsi(text.takeLast(4000))
    return latestStatus == "starting" ||
        tavernStartingMarkers.any { tail.contains(it, ignoreCase = true) }
}

private fun tavernStatusEnvelope(text: String): String {
    return text.substringBefore("\n====").trim()
}

fun isTavernLogStatusReport(text: String): Boolean {
    val envelope = tavernStatusEnvelope(text)
    return TavernStatusFieldRegex.findAll(envelope).lastOrNull()?.groupValues?.getOrNull(1) == "log"
}

fun inferTavernRunningFromLogSnapshot(text: String): Boolean? {
    if (!isTavernLogStatusReport(text)) return inferTavernRunning(text)
    val envelope = tavernStatusEnvelope(text)
    return when (inferExplicitTavernRunning(envelope)) {
        false -> false
        true -> if (envelope.contains("HTTP endpoint is not responding", ignoreCase = true)) null else true
        null -> null
    }
}

fun inferTavernStartingFromLogSnapshot(text: String): Boolean {
    if (!isTavernLogStatusReport(text)) return false
    val envelope = tavernStatusEnvelope(text)
    return inferExplicitTavernRunning(envelope) == true &&
        envelope.contains("HTTP endpoint is not responding", ignoreCase = true)
}

fun inferTavernPortConflict(text: String): Boolean {
    val tail = TavernLogSignals.stripAnsi(text.takeLast(4000))
    return TavernPortConflictRegex.containsMatchIn(tail) ||
        TavernPortConflictFieldRegex.containsMatchIn(tail) ||
        tail.contains("酒馆端口已经被别的进程占用") ||
        tail.contains("端口已被别的进程占用")
}

fun inferTavernRunning(text: String): Boolean? {
    inferExplicitTavernRunning(text)?.let { return it }
    val tail = TavernLogSignals.stripAnsi(text.takeLast(4000))
    return when {
        TavernRunningFalseRegex.containsMatchIn(tail) -> false
        TavernStoppedStatusRegex.containsMatchIn(tail) -> false
        tavernStoppedMarkers.any { tail.contains(it, ignoreCase = true) } -> false
        tavernFatalStartMarkers.any { tail.contains(it, ignoreCase = true) } -> false
        inferTavernPortConflict(tail) -> false
        inferTavernStarting(text) -> null
        TavernLogSignals.hasRecentLiveSignal(tail) -> true
        TavernErrorStatusRegex.containsMatchIn(tail) -> false
        tail.contains("SillyTavern is listening") -> true
        tail.contains("SillyTavern is already running") -> true
        else -> null
    }
}
