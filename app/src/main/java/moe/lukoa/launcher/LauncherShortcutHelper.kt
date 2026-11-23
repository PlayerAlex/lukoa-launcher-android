package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

object LauncherShortcutHelper {
    const val ACTION_QUICK_START_TAVERN = "moe.lukoa.launcher.action.QUICK_START_TAVERN"
    private const val SHORTCUT_ID_QUICK_START = "lukoa_quick_start_tavern"

    fun publishDynamicShortcuts(context: Context) {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(ShortcutManager::class.java) ?: return
        runCatching {
            manager.dynamicShortcuts = listOf(buildQuickStartShortcut(appContext))
        }
    }

    fun requestPinQuickStartShortcut(context: Context): Pair<Boolean, String> {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(ShortcutManager::class.java)
            ?: return false to "当前系统不支持创建桌面快捷方式。"
        if (!manager.isRequestPinShortcutSupported) {
            return false to "当前手机桌面暂不支持固定快捷方式；可长按启动器图标使用“一键启动酒馆”。"
        }
        return runCatching {
            val accepted = manager.requestPinShortcut(buildQuickStartShortcut(appContext), null)
            if (accepted) {
                true to "已向桌面请求添加“一键启动酒馆”快捷方式，请在系统弹窗中确认。"
            } else {
                false to "系统未接受创建桌面快捷方式，请检查桌面权限设置。"
            }
        }.getOrElse { error ->
            false to "创建桌面快捷方式失败：${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun buildQuickStartShortcut(context: Context): ShortcutInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_START_TAVERN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return ShortcutInfo.Builder(context, SHORTCUT_ID_QUICK_START)
            .setShortLabel("启动酒馆")
            .setLongLabel("一键启动酒馆")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_lukoa_launcher))
            .setIntent(intent)
            .build()
    }
}
