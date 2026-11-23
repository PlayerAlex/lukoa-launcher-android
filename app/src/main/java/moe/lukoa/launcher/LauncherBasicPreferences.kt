package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

data class InstalledBrowserOption(
    val packageName: String,
    val label: String,
)

data class LauncherBasicPrefs(
    val preferredBrowserPackage: String = "",
    val keepScreenOn: Boolean = true,
    val autoStartTavernOnLaunch: Boolean = false,
)

object LauncherBasicPrefsStore {
    private const val PREFS_NAME = "lukoa_basic_prefs"
    private const val KEY_PREFERRED_BROWSER = "preferred_browser_package"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_AUTO_START_TAVERN = "auto_start_tavern_on_launch"

    fun load(context: Context): LauncherBasicPrefs {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LauncherBasicPrefs(
            preferredBrowserPackage = prefs.getString(KEY_PREFERRED_BROWSER, "").orEmpty().trim(),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            autoStartTavernOnLaunch = prefs.getBoolean(KEY_AUTO_START_TAVERN, false),
        )
    }

    fun save(context: Context, value: LauncherBasicPrefs): LauncherBasicPrefs {
        val normalized = value.copy(
            preferredBrowserPackage = value.preferredBrowserPackage.trim(),
        )
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFERRED_BROWSER, normalized.preferredBrowserPackage)
            .putBoolean(KEY_KEEP_SCREEN_ON, normalized.keepScreenOn)
            .putBoolean(KEY_AUTO_START_TAVERN, normalized.autoStartTavernOnLaunch)
            .apply()
        return normalized
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
