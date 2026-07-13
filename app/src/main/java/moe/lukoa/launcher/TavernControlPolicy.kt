package moe.lukoa.launcher

const val TAVERN_START_BUSY_LABEL = "启动酒馆"

fun shouldOfferStopTavern(tavernRunning: Boolean, tavernStarting: Boolean): Boolean {
    return tavernRunning || tavernStarting
}

fun canInterruptActiveTavernStart(
    actionInProgress: Boolean,
    tavernStarting: Boolean,
    busyLabel: String?,
): Boolean {
    return actionInProgress && tavernStarting && busyLabel == TAVERN_START_BUSY_LABEL
}
