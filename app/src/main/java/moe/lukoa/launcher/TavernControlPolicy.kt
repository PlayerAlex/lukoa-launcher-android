package moe.lukoa.launcher

fun shouldOfferStopTavern(tavernRunning: Boolean, tavernStarting: Boolean): Boolean {
    return tavernRunning || tavernStarting
}