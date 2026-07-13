package moe.lukoa.launcher

class RuntimeLogSessionGate(discardFirstSnapshot: Boolean) {
    private var discardNextSnapshot = discardFirstSnapshot

    fun shouldAppendSnapshot(): Boolean {
        if (!discardNextSnapshot) return true
        discardNextSnapshot = false
        return false
    }

    fun discardNextSnapshot() {
        discardNextSnapshot = true
    }
}