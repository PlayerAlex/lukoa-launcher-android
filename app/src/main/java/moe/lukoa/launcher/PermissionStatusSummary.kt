package moe.lukoa.launcher

enum class PermissionNoticeTone {
    Info,
    Warning,
}

data class PermissionStatusNotice(
    val title: String,
    val detail: String,
    val pendingItems: List<String> = emptyList(),
    val tone: PermissionNoticeTone = PermissionNoticeTone.Info,
)

object PermissionStatusSummary {
    fun settingsNotice(
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsReady: Boolean,
        launcherBackgroundRunPermissionGranted: Boolean,
        termuxBackgroundRunPermissionGranted: Boolean,
        allFilesAccessGranted: Boolean,
        installUnknownAppsGranted: Boolean,
        termuxStoragePermissionBlocked: Boolean,
    ): PermissionStatusNotice {
        if (!termuxInstalled) {
            return PermissionStatusNotice(
                title = "先装好 Termux，再回来补权限",
                detail = "装好并打开一次 Termux 后，启动页会继续提示你补齐调用权限和后台常驻相关设置。",
                tone = PermissionNoticeTone.Info,
            )
        }

        val pendingItems = pendingItems(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsReady = termuxExternalAppsReady,
            launcherBackgroundRunPermissionGranted = launcherBackgroundRunPermissionGranted,
            termuxBackgroundRunPermissionGranted = termuxBackgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            termuxStoragePermissionBlocked = termuxStoragePermissionBlocked,
        )
        if (pendingItems.isEmpty()) {
            return PermissionStatusNotice(
                title = "权限基本就绪",
                detail = "这里保留的是处理入口。真正的当前运行状态看启动页；要逐项核对授权时，再点下方“查看权限详情”。",
                tone = PermissionNoticeTone.Info,
            )
        }

        val backgroundPending = pendingItems.filter { it.contains("后台") || it.contains("常驻") }
        val title = if (backgroundPending.isNotEmpty()) {
            "后台常驻还没完全放行"
        } else {
            "还有 ${pendingItems.size} 项待处理"
        }
        val detail = buildString {
            append("待处理：")
            append(pendingItems.joinToString("、"))
            append("。")
            if (backgroundPending.isNotEmpty()) {
                append(" 首次启动酒馆、长任务、自动备份和前台日志同步切到后台后更容易被系统打断。")
            }
            append(" 点下方查看权限详情后，按顺序补齐即可。")
        }
        return PermissionStatusNotice(
            title = title,
            detail = detail,
            pendingItems = pendingItems,
            tone = PermissionNoticeTone.Warning,
        )
    }

    fun launchReminder(
        termuxInstalled: Boolean,
        launcherBackgroundRunPermissionGranted: Boolean,
        termuxBackgroundRunPermissionGranted: Boolean,
        termuxStoragePermissionBlocked: Boolean,
    ): PermissionStatusNotice? {
        if (!termuxInstalled) return null
        val backgroundTargets = buildList {
            if (!launcherBackgroundRunPermissionGranted) add("露科亚启动器")
            if (!termuxBackgroundRunPermissionGranted) add("Termux")
        }
        if (backgroundTargets.isNotEmpty()) {
            val title = if (backgroundTargets.size == 1) {
                "${backgroundTargets.first()} 后台常驻待处理"
            } else {
                "后台常驻还没完全放行"
            }
            val detail = if (backgroundTargets.size == 1) {
                "${backgroundTargets.first()} 还可能被系统省电限制。首次启动酒馆、长任务、自动备份或前台日志同步切到后台后更容易中断。"
            } else {
                "露科亚启动器和 Termux 都还可能被系统省电限制。首次启动酒馆、长任务、自动备份或前台日志同步切到后台后更容易中断。"
            }
            return PermissionStatusNotice(
                title = title,
                detail = detail,
                pendingItems = backgroundTargets,
                tone = PermissionNoticeTone.Warning,
            )
        }
        if (termuxStoragePermissionBlocked) {
            return PermissionStatusNotice(
                title = "备份权限提醒",
                detail = "最近一次应用备份时，Termux 存储权限没有放行。下次恢复前建议先去权限页补齐。",
                pendingItems = listOf("Termux 存储"),
                tone = PermissionNoticeTone.Warning,
            )
        }
        return null
    }

    fun pendingItems(
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsReady: Boolean,
        launcherBackgroundRunPermissionGranted: Boolean,
        termuxBackgroundRunPermissionGranted: Boolean,
        allFilesAccessGranted: Boolean,
        installUnknownAppsGranted: Boolean,
        termuxStoragePermissionBlocked: Boolean,
    ): List<String> {
        return buildList {
            if (termuxInstalled && !runCommandPermissionGranted) add("RUN_COMMAND")
            if (termuxInstalled && !termuxExternalAppsReady) add("Termux 外部调用")
            if (!launcherBackgroundRunPermissionGranted) add("启动器后台运行")
            if (termuxInstalled && !termuxBackgroundRunPermissionGranted) add("Termux 后台常驻")
            if (!allFilesAccessGranted) add("文件管理")
            if (!installUnknownAppsGranted) add("安装未知来源")
            if (termuxStoragePermissionBlocked) add("Termux 存储")
        }
    }
}
