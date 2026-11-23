package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

enum class TavernBrowserOpenMode(
    val wireValue: String,
    val shortLabel: String,
    val title: String,
    val summary: String,
) {
    SwitchToBrowser(
        wireValue = "switch_to_browser",
        shortLabel = "切回浏览器",
        title = "优先切回浏览器（防重复开窗口）",
        summary = "首次打开酒馆网址，之后点“返回酒馆”直接把浏览器切回前台，不再重复新建标签页；若不小心关了标签，长按“返回酒馆”即可重新打开网址。",
    ),
    CustomTab(
        wireValue = "custom_tab",
        shortLabel = "独立窗口",
        title = "独立应用内窗口",
        summary = "在独立窗口打开酒馆网页，不会在主浏览器里堆积任何酒馆标签页。",
    ),
    AlwaysOpenUrl(
        wireValue = "always_open_url",
        shortLabel = "每次开网址",
        title = "每次重新打开网址",
        summary = "每次点击都向浏览器发送打开网址请求。",
    ),
    ;

    companion object {
        fun fromWireValue(value: String?): TavernBrowserOpenMode =
            entries.firstOrNull { it.wireValue == value?.trim()?.lowercase() } ?: SwitchToBrowser
    }
}

data class InstalledBrowserOption(
    val packageName: String,
    val label: String,
)

data class LauncherBasicPrefs(
    val preferredBrowserPackage: String = "",
    val browserOpenMode: TavernBrowserOpenMode = TavernBrowserOpenMode.SwitchToBrowser,
    val keepScreenOn: Boolean = true,
    val autoStartTavernOnLaunch: Boolean = false,
)

data class LastOpenedTavernBrowser(
    val port: Int = 0,
    val browserPackage: String = "",
    val openedAtMillis: Long = 0L,
)

object LauncherBasicPrefsStore {
    private const val PREFS_NAME = "lukoa_basic_prefs"
    private const val KEY_PREFERRED_BROWSER = "preferred_browser_package"
    private const val KEY_BROWSER_OPEN_MODE = "browser_open_mode"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_AUTO_START_TAVERN = "auto_start_tavern_on_launch"
    private const val KEY_LAST_OPENED_PORT = "last_opened_tavern_port"
    private const val KEY_LAST_OPENED_BROWSER = "last_opened_tavern_browser"
    private const val KEY_LAST_OPENED_AT = "last_opened_tavern_at"

    fun load(context: Context): LauncherBasicPrefs {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LauncherBasicPrefs(
            preferredBrowserPackage = prefs.getString(KEY_PREFERRED_BROWSER, "").orEmpty().trim(),
            browserOpenMode = TavernBrowserOpenMode.fromWireValue(prefs.getString(KEY_BROWSER_OPEN_MODE, null)),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            autoStartTavernOnLaunch = prefs.getBoolean(KEY_AUTO_START_TAVERN, false),
        )
    }

    fun save(context: Context, value: LauncherBasicPrefs): LauncherBasicPrefs {
        val normalized = value.copy(
            preferredBrowserPackage = value.preferredBrowserPackage.trim(),
        )
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousBrowser = prefs.getString(KEY_PREFERRED_BROWSER, "").orEmpty().trim()
        val editor = prefs.edit()
            .putString(KEY_PREFERRED_BROWSER, normalized.preferredBrowserPackage)
            .putString(KEY_BROWSER_OPEN_MODE, normalized.browserOpenMode.wireValue)
            .putBoolean(KEY_KEEP_SCREEN_ON, normalized.keepScreenOn)
            .putBoolean(KEY_AUTO_START_TAVERN, normalized.autoStartTavernOnLaunch)
        if (previousBrowser != normalized.preferredBrowserPackage) {
            editor.remove(KEY_LAST_OPENED_PORT)
                .remove(KEY_LAST_OPENED_BROWSER)
                .remove(KEY_LAST_OPENED_AT)
        }
        editor.apply()
        return normalized
    }

    fun recordOpenedTavern(
        context: Context,
        port: Int,
        browserPackage: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_OPENED_PORT, port)
            .putString(KEY_LAST_OPENED_BROWSER, browserPackage.trim())
            .putLong(KEY_LAST_OPENED_AT, nowMillis)
            .apply()
    }

    fun readLastOpenedTavern(context: Context): LastOpenedTavernBrowser {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LastOpenedTavernBrowser(
            port = prefs.getInt(KEY_LAST_OPENED_PORT, 0),
            browserPackage = prefs.getString(KEY_LAST_OPENED_BROWSER, "").orEmpty().trim(),
            openedAtMillis = prefs.getLong(KEY_LAST_OPENED_AT, 0L),
        )
    }
}

object LauncherBrowserResolver {
    private val preferredOrder = listOf(
        "com.android.chrome",
        "com.microsoft.emmx",
        "mark.via.gp",
        "mark.via",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.kiwibrowser.browser",
        "com.lemurbrowser.exts",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.quark.browser",
        "com.UCMobile",
        "com.yandex.browser",
    )

    fun queryInstalledBrowsers(context: Context): List<InstalledBrowserOption> {
        val pm = context.packageManager
        val probeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolved = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    probeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(probeIntent, PackageManager.MATCH_ALL)
            }
        }.getOrDefault(emptyList())

        val selfPackage = context.packageName
        val deduped = linkedMapOf<String, InstalledBrowserOption>()
        for (info in resolved) {
            val activityInfo = info.activityInfo ?: continue
            val pkg = activityInfo.packageName?.trim().orEmpty()
            if (pkg.isEmpty() || pkg == selfPackage || deduped.containsKey(pkg)) continue
            val label = runCatching {
                info.loadLabel(pm)?.toString()?.trim().orEmpty()
            }.getOrDefault("").ifBlank { pkg }
            deduped[pkg] = InstalledBrowserOption(packageName = pkg, label = label)
        }
        return sortBrowserOptions(deduped.values.toList())
    }

    fun resolveDefaultBrowserPackage(context: Context): String {
        val pm = context.packageManager
        val probeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(
                    probeIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(probeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        }.getOrNull() ?: return ""
        val pkg = info.activityInfo?.packageName?.trim().orEmpty()
        return if (pkg.isBlank() || pkg == "android" || pkg == context.packageName) "" else pkg
    }

    internal fun sortBrowserOptions(options: List<InstalledBrowserOption>): List<InstalledBrowserOption> {
        return options.sortedWith(
            compareBy<InstalledBrowserOption>(
                { pkgRank(it.packageName) },
                { it.label.lowercase() },
                { it.packageName },
            ),
        )
    }

    fun displayLabelFor(
        preferredPackage: String,
        installedBrowsers: List<InstalledBrowserOption>,
    ): String {
        val pkg = preferredPackage.trim()
        if (pkg.isEmpty()) return "系统默认"
        val match = installedBrowsers.firstOrNull { it.packageName == pkg }
        return match?.label ?: "已指定（未安装，将回退默认）"
    }

    private fun pkgRank(packageName: String): Int {
        val index = preferredOrder.indexOf(packageName)
        return if (index >= 0) index else Int.MAX_VALUE
    }
}
