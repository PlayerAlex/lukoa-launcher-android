package moe.lukoa.launcher

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object LauncherShortcutHelper {
    const val ACTION_QUICK_START_TAVERN = "moe.lukoa.launcher.action.QUICK_START_TAVERN"
    private const val SHORTCUT_ID_DYNAMIC_QUICK_START = "lukoa_quick_start_tavern"
    private const val SHORTCUT_ID_PINNED_QUICK_START = "lukoa_pin_quick_start_tavern"
    private const val PIN_ICON_TARGET_PX = 192

    fun publishDynamicShortcuts(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            val shortcut = buildQuickStartShortcut(appContext, SHORTCUT_ID_DYNAMIC_QUICK_START)
            ShortcutManagerCompat.setDynamicShortcuts(appContext, listOf(shortcut))
        }
    }

    fun requestPinQuickStartShortcut(context: Context): Pair<Boolean, String> {
        val appContext = context.applicationContext
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false to "当前手机桌面暂不支持固定快捷方式；也可长按启动器图标直接使用「启动酒馆」。"
        }
        return runCatching {
            val pinnedShortcut = buildQuickStartShortcut(appContext, SHORTCUT_ID_PINNED_QUICK_START)
            val alreadyPinned = runCatching {
                ShortcutManagerCompat.getShortcuts(
                    appContext,
                    ShortcutManagerCompat.FLAG_MATCH_PINNED,
                ).any {
                    it.id == SHORTCUT_ID_PINNED_QUICK_START || it.id == SHORTCUT_ID_DYNAMIC_QUICK_START
                }
            }.getOrDefault(false)

            if (alreadyPinned) {
                runCatching {
                    ShortcutManagerCompat.updateShortcuts(appContext, listOf(pinnedShortcut))
                }
                return@runCatching true to "桌面上已经添加过「启动酒馆」快捷方式了，已为你刷新图标与入口。"
            }

            val callbackIntent = ShortcutManagerCompat.createShortcutResultIntent(context, pinnedShortcut)
            val successCallback = PendingIntent.getBroadcast(
                context,
                0,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val accepted = ShortcutManagerCompat.requestPinShortcut(
                context,
                pinnedShortcut,
                successCallback.intentSender,
            )
            if (accepted) {
                true to "已请求添加到桌面，若系统弹出确认框请点允许（部分国产系统需在权限设置开启「创建桌面快捷方式」）。"
            } else {
                false to "未能添加到桌面：请在系统应用权限中开启「创建桌面快捷方式」，或长按启动器图标直接启动。"
            }
        }.getOrElse { error ->
            false to "创建桌面快捷方式失败（${error.message ?: error.javaClass.simpleName}），也可长按启动器图标使用「启动酒馆」。"
        }
    }

    private fun buildQuickStartShortcut(context: Context, id: String): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_START_TAVERN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel("启动酒馆")
            .setLongLabel("一键启动酒馆")
            .setIcon(buildScaledShortcutIcon(context))
            .setIntent(intent)
            .build()
    }

    private fun buildScaledShortcutIcon(context: Context): IconCompat {
        // The bundled nodpi icon is a high-res PNG; scaling it down to 192px keeps Binder IPC small
        // so OEM launchers do not silently reject or fail requestPinShortcut().
        val bitmap = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_lukoa_launcher, bounds)
            val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, PIN_ICON_TARGET_PX)
            val decoded = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_lukoa_launcher,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ) ?: return@runCatching null
            if (decoded.width > PIN_ICON_TARGET_PX || decoded.height > PIN_ICON_TARGET_PX) {
                val scaled = Bitmap.createScaledBitmap(decoded, PIN_ICON_TARGET_PX, PIN_ICON_TARGET_PX, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }
        }.getOrNull()
        return if (bitmap != null) {
            IconCompat.createWithBitmap(bitmap)
        } else {
            IconCompat.createWithResource(context, R.drawable.ic_lukoa_launcher)
        }
    }

    internal fun computeInSampleSize(width: Int, height: Int, targetPx: Int): Int {
        var sample = 1
        val maxSide = maxOf(width, height)
        while (maxSide / (sample * 2) >= targetPx) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
