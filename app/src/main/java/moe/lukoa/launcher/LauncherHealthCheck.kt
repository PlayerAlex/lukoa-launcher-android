package moe.lukoa.launcher

enum class LauncherHealthLevel {
    Good,
    Warning,
    Error,
    Unknown,
}

enum class LauncherHealthActionType {
    DownloadTermux,
    RequestRunPermission,
    CopyExternalAppsCommand,
    PrepareTermuxEnvironment,
    ChooseDetectedDirectory,
    OpenPathSettings,
    OpenNetworkSettings,
    RequestBackgroundRunPermission,
    OpenAllFilesAccessSettings,
    OpenUnknownAppSourcesSettings,
    StopTavern,
}

data class LauncherHealthAction(
    val type: LauncherHealthActionType,
    val label: String,
)

data class LauncherHealthItem(
    val title: String,
    val detail: String,
    val level: LauncherHealthLevel,
)

data class LauncherHealthReport(
    val checkedAtMillis: Long = 0L,
    val summaryTitle: String = "还没体检",
    val summaryDetail: String = "点“一键体检”后，启动器会把权限、路径、镜像源和酒馆环境一起看一遍。",
    val items: List<LauncherHealthItem> = emptyList(),
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val primaryAction: LauncherHealthAction? = null,
    val doctorReport: TavernDoctorReport? = null,
) {
    val hasData: Boolean
        get() = checkedAtMillis > 0L
}

object LauncherHealthCheck {
    fun build(
        checkedAtMillis: Long = System.currentTimeMillis(),
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsBlocked: Boolean,
        backgroundRunPermissionGranted: Boolean,
        allFilesAccessGranted: Boolean,
        installUnknownAppsGranted: Boolean,
        termuxStoragePermissionBlocked: Boolean,
        tavernRunning: Boolean,
        mirrorProbeStatus: TavernMirrorProbeStatus,
        doctorReport: TavernDoctorReport?,
    ): LauncherHealthReport {
        val termuxCallReady = termuxInstalled && runCommandPermissionGranted && !termuxExternalAppsBlocked
        val missingCoreTools = buildList {
            if (doctorReport?.gitAvailable == false) add("git")
            if (doctorReport?.nodeAvailable == false) add("node")
            if (doctorReport?.npmAvailable == false) add("npm")
        }
        val extraPermissionWarnings = buildList {
            if (!backgroundRunPermissionGranted) add("后台运行")
            if (!allFilesAccessGranted) add("文件权限")
            if (!installUnknownAppsGranted) add("安装未知应用")
            if (termuxStoragePermissionBlocked) add("Termux 存储")
        }
        val hasReadableTermuxRepo = doctorReport?.let {
            it.termuxRepoUri.isNotBlank() || (
                it.termuxRepoLabel.isNotBlank() &&
                    it.termuxRepoLabel != "未读取"
            )
        } == true
        val pathProblem = doctorReport != null && (
            doctorReport.tavernDirExists == false ||
                doctorReport.packageJsonExists == false ||
                doctorReport.startEntryExists == false ||
                doctorReport.gitRepo == false
            )

        val items = buildList {
            add(
                when {
                    !termuxInstalled -> LauncherHealthItem(
                        title = "Termux",
                        detail = "手机里还没检测到 Termux。启动酒馆前要先安装并打开一次。",
                        level = LauncherHealthLevel.Error,
                    )

                    else -> LauncherHealthItem(
                        title = "Termux",
                        detail = "已检测到 Termux。",
                        level = LauncherHealthLevel.Good,
                    )
                },
            )

            add(
                when {
                    !termuxInstalled -> LauncherHealthItem(
                        title = "Termux 调用",
                        detail = "先装好 Termux，才能继续检查调用权限。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    !runCommandPermissionGranted -> LauncherHealthItem(
                        title = "Termux 调用",
                        detail = "启动器还没拿到 RUN_COMMAND 权限，当前不能直接调用 Termux。",
                        level = LauncherHealthLevel.Error,
                    )

                    termuxExternalAppsBlocked -> LauncherHealthItem(
                        title = "Termux 调用",
                        detail = "Termux 还没开启外部调用。复制权限命令到 Termux 执行一次即可。",
                        level = LauncherHealthLevel.Error,
                    )

                    else -> LauncherHealthItem(
                        title = "Termux 调用",
                        detail = "RUN_COMMAND 和外部调用都已就绪。",
                        level = LauncherHealthLevel.Good,
                    )
                },
            )

            add(
                if (extraPermissionWarnings.isEmpty()) {
                    LauncherHealthItem(
                        title = "系统权限",
                        detail = "后台运行、文件和安装相关权限都已基本就绪。",
                        level = LauncherHealthLevel.Good,
                    )
                } else {
                    LauncherHealthItem(
                        title = "系统权限",
                        detail = "还有这些权限没准备好：${extraPermissionWarnings.joinToString("、")}。",
                        level = LauncherHealthLevel.Warning,
                    )
                },
            )

            add(
                when {
                    !termuxCallReady -> LauncherHealthItem(
                        title = "Termux 环境",
                        detail = "先把 Termux 调用打通，启动器才能继续读 git、node、npm。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport == null -> LauncherHealthItem(
                        title = "Termux 环境",
                        detail = "这次没读到体检结果，请重试一次。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    missingCoreTools.isNotEmpty() -> LauncherHealthItem(
                        title = "Termux 环境",
                        detail = "缺少 ${missingCoreTools.joinToString("、")}。点“准备 Termux”会自动补齐。",
                        level = LauncherHealthLevel.Error,
                    )

                    doctorReport.curlAvailable == false -> LauncherHealthItem(
                        title = "Termux 环境",
                        detail = "git、node、npm 已有，但缺少 curl。网页状态检测会不够准。",
                        level = LauncherHealthLevel.Warning,
                    )

                    else -> LauncherHealthItem(
                        title = "Termux 环境",
                        detail = "git、node、npm、curl 都已就绪。",
                        level = LauncherHealthLevel.Good,
                    )
                },
            )

            add(
                when {
                    !termuxCallReady -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "先把 Termux 调用修好，启动器才能确认酒馆目录。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport == null -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "这次没读到酒馆目录结果，请重试一次。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport.tavernDirExists == false -> {
                        val candidateMessage = when (doctorReport.candidateDirectories.size) {
                            0 -> "当前路径没有找到酒馆目录。"
                            1 -> "当前路径没找到酒馆，但发现了 1 个候选目录。"
                            else -> "当前路径没找到酒馆，但发现了 ${doctorReport.candidateDirectories.size} 个候选目录。"
                        }
                        LauncherHealthItem(
                            title = "酒馆路径",
                            detail = "$candidateMessage 当前配置是：${doctorReport.tavernDir.ifBlank { "~/SillyTavern" }}",
                            level = LauncherHealthLevel.Error,
                        )
                    }

                    doctorReport.packageJsonExists == false -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "当前目录里没有 package.json，看起来不像酒馆根目录。",
                        level = LauncherHealthLevel.Error,
                    )

                    doctorReport.startEntryExists == false -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "当前目录里没有 start.sh 或 server.js，启动会失败。",
                        level = LauncherHealthLevel.Error,
                    )

                    doctorReport.gitRepo == false -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "当前目录不是 Git 仓库，后续更新和回退会失败。",
                        level = LauncherHealthLevel.Error,
                    )

                    else -> LauncherHealthItem(
                        title = "酒馆路径",
                        detail = "当前目录看起来是完整的酒馆目录：${doctorReport.tavernDir.ifBlank { "~/SillyTavern" }}",
                        level = LauncherHealthLevel.Good,
                    )
                },
            )

            add(
                when (mirrorProbeStatus.overallLevel) {
                    MirrorProbeLevel.Healthy -> LauncherHealthItem(
                        title = "镜像源",
                        detail = "Git 源、版本列表和 npm 源都可用。",
                        level = LauncherHealthLevel.Good,
                    )

                    MirrorProbeLevel.Warning -> LauncherHealthItem(
                        title = "镜像源",
                        detail = mirrorMessages(mirrorProbeStatus),
                        level = LauncherHealthLevel.Warning,
                    )

                    MirrorProbeLevel.Failed -> LauncherHealthItem(
                        title = "镜像源",
                        detail = mirrorMessages(mirrorProbeStatus),
                        level = LauncherHealthLevel.Error,
                    )

                    MirrorProbeLevel.Unknown -> LauncherHealthItem(
                        title = "镜像源",
                        detail = "镜像源还没检测完成。",
                        level = LauncherHealthLevel.Unknown,
                    )
                },
            )

            add(
                when {
                    !termuxCallReady -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = "先把 Termux 调用修好，启动器才能确认当前端口状态。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport == null -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = "这次没读到运行状态，请重试一次。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport.portConflict == true -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = "端口 ${doctorReport.port} 已被别的进程占用，直接启动大概率会失败。",
                        level = LauncherHealthLevel.Error,
                    )

                    doctorReport.httpOk == true -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = "端口 ${doctorReport.port} 正常响应，酒馆网页可访问。",
                        level = LauncherHealthLevel.Good,
                    )

                    doctorReport.processDetected == true -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = if (doctorReport.curlAvailable == false) {
                            "检测到酒馆进程，但缺少 curl，暂时没法确认网页状态。"
                        } else {
                            "检测到酒馆进程，但网页当前没有响应。"
                        },
                        level = LauncherHealthLevel.Warning,
                    )

                    else -> LauncherHealthItem(
                        title = "运行与端口",
                        detail = "当前未运行，端口 ${doctorReport.port} 看起来是空闲的。",
                        level = LauncherHealthLevel.Good,
                    )
                },
            )

            add(
                when {
                    !termuxCallReady -> LauncherHealthItem(
                        title = "Termux 包源",
                        detail = "先把 Termux 调用修好，启动器才能读取当前包源。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    doctorReport == null -> LauncherHealthItem(
                        title = "Termux 包源",
                        detail = "这次没读到当前包源。",
                        level = LauncherHealthLevel.Unknown,
                    )

                    !hasReadableTermuxRepo -> LauncherHealthItem(
                        title = "Termux 包源",
                        detail = "这次没读到当前 Termux 包源，不一定代表你没有包源。更像是读取失败，去网络设置里点一次“读取当前 Termux 包源”再确认。",
                        level = LauncherHealthLevel.Warning,
                    )

                    else -> LauncherHealthItem(
                        title = "Termux 包源",
                        detail = buildString {
                            append("当前使用 ${doctorReport.termuxRepoLabel}。")
                            if (doctorReport.termuxRepoUri.isNotBlank()) {
                                append(" 地址是 ${doctorReport.termuxRepoUri}。")
                            }
                        },
                        level = LauncherHealthLevel.Good,
                    )
                },
            )
        }

        val errorCount = items.count { it.level == LauncherHealthLevel.Error }
        val warningCount = items.count { it.level == LauncherHealthLevel.Warning }
        val primaryAction = choosePrimaryAction(
            termuxInstalled = termuxInstalled,
            runCommandPermissionGranted = runCommandPermissionGranted,
            termuxExternalAppsBlocked = termuxExternalAppsBlocked,
            backgroundRunPermissionGranted = backgroundRunPermissionGranted,
            allFilesAccessGranted = allFilesAccessGranted,
            installUnknownAppsGranted = installUnknownAppsGranted,
            tavernRunning = tavernRunning,
            mirrorProbeStatus = mirrorProbeStatus,
            doctorReport = doctorReport,
            missingCoreTools = missingCoreTools,
            pathProblem = pathProblem,
        )

        val (summaryTitle, summaryDetail) = when {
            !termuxInstalled -> "先安装 Termux" to "手机里还没检测到 Termux。先装好再继续。"
            !runCommandPermissionGranted -> "先开 RUN_COMMAND 权限" to "启动器现在还不能调用 Termux。"
            termuxExternalAppsBlocked -> "先开外部调用" to "把权限命令复制到 Termux 执行一次，就能继续。"
            missingCoreTools.isNotEmpty() -> "先准备 Termux 环境" to "缺少 ${missingCoreTools.joinToString("、")}，启动、安装和版本管理都会受影响。"
            mirrorProbeStatus.overallLevel == MirrorProbeLevel.Failed -> "先修镜像源" to mirrorMessages(mirrorProbeStatus)
            pathProblem && doctorReport?.candidateDirectories?.isNotEmpty() == true ->
                "先确认酒馆目录" to "当前路径没找到酒馆，但已经识别到候选目录。"

            pathProblem -> "先确认酒馆目录" to "当前路径不像完整的 SillyTavern 根目录。"
            doctorReport?.portConflict == true -> "先处理端口占用" to "酒馆端口已经被别的进程占用，直接启动会撞车。"
            errorCount == 0 && warningCount == 0 -> "当前环境基本正常" to "启动、版本管理和镜像源检查都没有发现明显问题。"
            errorCount == 0 -> "有提醒项，启动前建议看一眼" to "当前没有致命问题，但还有 $warningCount 个提醒项。"
            else -> "发现 $errorCount 个需要先处理的问题" to "先修最上面的优先项，再重新体检一次。"
        }

        return LauncherHealthReport(
            checkedAtMillis = checkedAtMillis,
            summaryTitle = summaryTitle,
            summaryDetail = summaryDetail,
            items = items,
            errorCount = errorCount,
            warningCount = warningCount,
            primaryAction = primaryAction,
            doctorReport = doctorReport,
        )
    }

    private fun mirrorMessages(status: TavernMirrorProbeStatus): String {
        return listOf(
            status.repoStatus.message,
            status.versionStatus.message,
            status.npmStatus.message,
        )
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" ")
            .ifBlank { "镜像源检测没有返回明确信息。" }
    }

    private fun choosePrimaryAction(
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsBlocked: Boolean,
        backgroundRunPermissionGranted: Boolean,
        allFilesAccessGranted: Boolean,
        installUnknownAppsGranted: Boolean,
        tavernRunning: Boolean,
        mirrorProbeStatus: TavernMirrorProbeStatus,
        doctorReport: TavernDoctorReport?,
        missingCoreTools: List<String>,
        pathProblem: Boolean,
    ): LauncherHealthAction? {
        return when {
            !termuxInstalled -> LauncherHealthAction(
                type = LauncherHealthActionType.DownloadTermux,
                label = "下载 Termux",
            )

            !runCommandPermissionGranted -> LauncherHealthAction(
                type = LauncherHealthActionType.RequestRunPermission,
                label = "请求权限",
            )

            termuxExternalAppsBlocked -> LauncherHealthAction(
                type = LauncherHealthActionType.CopyExternalAppsCommand,
                label = "复制权限命令",
            )

            missingCoreTools.isNotEmpty() -> LauncherHealthAction(
                type = LauncherHealthActionType.PrepareTermuxEnvironment,
                label = "准备 Termux",
            )

            mirrorProbeStatus.overallLevel == MirrorProbeLevel.Failed -> LauncherHealthAction(
                type = LauncherHealthActionType.OpenNetworkSettings,
                label = "查看网络设置",
            )

            pathProblem && !doctorReport?.candidateDirectories.isNullOrEmpty() -> LauncherHealthAction(
                type = LauncherHealthActionType.ChooseDetectedDirectory,
                label = "选择目录",
            )

            pathProblem -> LauncherHealthAction(
                type = LauncherHealthActionType.OpenPathSettings,
                label = "去设置改路径",
            )

            doctorReport?.portConflict == true && tavernRunning -> LauncherHealthAction(
                type = LauncherHealthActionType.StopTavern,
                label = "停止酒馆",
            )

            !backgroundRunPermissionGranted -> LauncherHealthAction(
                type = LauncherHealthActionType.RequestBackgroundRunPermission,
                label = "开后台权限",
            )

            !allFilesAccessGranted -> LauncherHealthAction(
                type = LauncherHealthActionType.OpenAllFilesAccessSettings,
                label = "开文件权限",
            )

            !installUnknownAppsGranted -> LauncherHealthAction(
                type = LauncherHealthActionType.OpenUnknownAppSourcesSettings,
                label = "开安装权限",
            )

            else -> null
        }
    }
}
