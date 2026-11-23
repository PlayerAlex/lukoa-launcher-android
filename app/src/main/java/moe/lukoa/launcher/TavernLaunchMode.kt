package moe.lukoa.launcher

import android.content.Context

enum class TavernLaunchMode(
    val wireValue: String,
    val shortLabel: String,
    val label: String,
    val summary: String,
) {
    ForegroundConsole(
        wireValue = "foreground",
        shortLabel = "前台窗口",
        label = "前台窗口启动",
        summary = "打开 Termux 查看实时启动日志，就绪后自动拉起浏览器。",
    ),
    BackgroundSilent(
        wireValue = "background",
        shortLabel = "后台静默",
        label = "后台静默启动",
        summary = "在后台静默启动酒馆，就绪后直接打开浏览器，不再弹出 Termux 窗口。",
    ),
    ;

    fun toggled(): TavernLaunchMode = when (this) {
        ForegroundConsole -> BackgroundSilent
        BackgroundSilent -> ForegroundConsole
    }

    companion object {
        fun fromWireValue(value: String?): TavernLaunchMode {
            return entries.firstOrNull { it.wireValue == value?.trim()?.lowercase() }
                ?: ForegroundConsole
        }
    }
}

class TavernLaunchModeStore(private val context: Context) {
    fun load(): TavernLaunchMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return TavernLaunchMode.fromWireValue(prefs.getString(KEY_LAUNCH_MODE, null))
    }

    fun save(mode: TavernLaunchMode): TavernLaunchMode {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAUNCH_MODE, mode.wireValue)
            .apply()
        return mode
    }

    private companion object {
        const val PREFS_NAME = "lukoa_tavern_launch_mode"
        const val KEY_LAUNCH_MODE = "launch_mode"
    }
}
