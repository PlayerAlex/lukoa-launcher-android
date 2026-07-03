package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BackgroundRunAccess {
    fun isGranted(
        context: Context,
        packageName: String = context.packageName,
    ): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    fun request(
        context: Context,
        packageName: String = context.packageName,
    ): Boolean {
        if (packageName != context.packageName && !isPackageInstalled(context, packageName)) {
            return false
        }
        if (isGranted(context, packageName)) return true
        val intents = buildList {
            add(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"),
                ),
            )
            add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            add(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                ),
            )
        }
        return intents.any { intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun isTermuxGranted(context: Context): Boolean {
        return isPackageInstalled(context, TermuxCommandRunner.TERMUX_PACKAGE) &&
            isGranted(context, TermuxCommandRunner.TERMUX_PACKAGE)
    }

    fun requestTermux(context: Context): Boolean {
        return request(context, TermuxCommandRunner.TERMUX_PACKAGE)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
