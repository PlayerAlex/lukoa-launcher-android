package moe.lukoa.launcher

object LauncherLaunchSessionPolicy {
    fun isFreshTaskLaunch(hasSavedInstanceState: Boolean): Boolean {
        return !hasSavedInstanceState
    }
}