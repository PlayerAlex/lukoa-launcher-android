package moe.lukoa.launcher

private val TavernRunningFieldRegex = Regex(""""running"\s*:\s*(true|false)""")
private val TavernStatusFieldRegex = Regex(""""status"\s*:\s*"([^"]+)"""")
private val TavernRunningFalseRegex = Regex(""""running"\s*:\s*false""")
private val TavernStoppedStatusRegex = Regex(""""status"\s*:\s*"stopped"""")
private val TavernErrorStatusRegex = Regex(""""status"\s*:\s*"error"""")
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

fun inferTavernRunning(text: String): Boolean? {
    inferExplicitTavernRunning(text)?.let { return it }
    val tail = TavernLogSignals.stripAnsi(text.takeLast(4000))
    return when {
        TavernRunningFalseRegex.containsMatchIn(tail) -> false
        TavernStoppedStatusRegex.containsMatchIn(tail) -> false
        tavernStoppedMarkers.any { tail.contains(it, ignoreCase = true) } -> false
        tavernFatalStartMarkers.any { tail.contains(it, ignoreCase = true) } -> false
        inferTavernStarting(text) -> null
        TavernLogSignals.hasRecentLiveSignal(tail) -> true
        TavernErrorStatusRegex.containsMatchIn(tail) -> false
        tail.contains("SillyTavern is listening") -> true
        tail.contains("SillyTavern is already running") -> true
        tail.contains("Address 127.0.0.1:8000 is already in use") -> true
        else -> null
    }
}
