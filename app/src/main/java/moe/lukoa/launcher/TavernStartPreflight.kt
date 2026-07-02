package moe.lukoa.launcher

enum class TavernStartPreflightActionType {
    DownloadTermux,
    RequestRunPermission,
    CopyExternalAppsCommand,
    PrepareTermuxEnvironment,
    ChooseDetectedDirectory,
    OpenPathSettings,
    StopDetectedProcess,
    ReturnToTavern,
    Retry,
}

data class TavernStartPreflightAction(
    val type: TavernStartPreflightActionType,
    val label: String,
)

data class TavernStartPreflightResult(
    val ok: Boolean,
    val title: String = "",
    val summary: String = "",
    val details: List<String> = emptyList(),
    val action: TavernStartPreflightAction? = null,
    val doctorReport: TavernDoctorReport? = null,
)

object TavernStartPreflight {
    fun evaluate(
        termuxInstalled: Boolean,
        runCommandPermissionGranted: Boolean,
        termuxExternalAppsBlocked: Boolean,
        doctorReport: TavernDoctorReport?,
    ): TavernStartPreflightResult {
        if (!termuxInstalled) {
            return blocked(
                summary = "还没检测到 Termux，先安装并打开一次，再启动酒馆。",
                details = listOf("启动酒馆前需要先装好 Termux。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.DownloadTermux,
                    label = "下载 Termux",
                ),
            )
        }

        if (!runCommandPermissionGranted) {
            return blocked(
                summary = "启动器还没有拿到 RUN_COMMAND 权限，当前不能直接启动酒馆。",
                details = listOf("先把 Termux 调用权限打开，再重新点启动。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.RequestRunPermission,
                    label = "请求权限",
                ),
            )
        }

        if (termuxExternalAppsBlocked) {
            return blocked(
                summary = "Termux 还没开启外部调用，先放行再启动更稳。",
                details = listOf("把权限命令复制到 Termux 执行一次即可。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.CopyExternalAppsCommand,
                    label = "复制权限命令",
                ),
            )
        }

        if (doctorReport == null) {
            return blocked(
                summary = "这次没拿到完整的启动前预检结果，请重试一次。",
                details = listOf("Termux 返回不完整时，直接硬启动更容易让你分不清卡在哪。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.Retry,
                    label = "重试预检",
                ),
            )
        }

        val missingCoreTools = buildList {
            if (doctorReport.gitAvailable == false) add("git")
            if (doctorReport.nodeAvailable == false) add("node")
            if (doctorReport.npmAvailable == false) add("npm")
        }
        if (missingCoreTools.isNotEmpty()) {
            return blocked(
                summary = "Termux 里缺少启动酒馆要用的基础依赖，先补齐再启动更稳。",
                details = listOf(
                    "缺少：${missingCoreTools.joinToString("、")}",
                    "点“准备 Termux”后，启动器会自动补齐这些依赖。",
                ),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.PrepareTermuxEnvironment,
                    label = "准备 Termux",
                ),
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.tavernDirExists == false) {
            val details = buildList {
                add("当前配置路径：${doctorReport.tavernDir.ifBlank { "~/SillyTavern" }}")
                if (doctorReport.candidateDirectories.isNotEmpty()) {
                    add("已发现 ${doctorReport.candidateDirectories.size} 个候选目录，可以直接选。")
                } else {
                    add("当前路径下没有找到酒馆目录。")
                }
            }
            val action = if (doctorReport.candidateDirectories.isNotEmpty()) {
                TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.ChooseDetectedDirectory,
                    label = "选择目录",
                )
            } else {
                TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.OpenPathSettings,
                    label = "去设置路径",
                )
            }
            return blocked(
                summary = "当前路径没找到酒馆，先把目录确认对，再启动。",
                details = details,
                action = action,
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.packageJsonExists == false ||
            doctorReport.startEntryExists == false ||
            doctorReport.gitRepo == false
        ) {
            val details = buildList {
                add("当前配置路径：${doctorReport.tavernDir.ifBlank { "~/SillyTavern" }}")
                if (doctorReport.packageJsonExists == false) {
                    add("目录里缺少 package.json。")
                }
                if (doctorReport.startEntryExists == false) {
                    add("目录里缺少 start.sh 或 server.js。")
                }
                if (doctorReport.gitRepo == false) {
                    add("目录不是 Git 仓库，后续更新和回退会出问题。")
                }
            }
            return blocked(
                summary = "当前目录不像完整的 SillyTavern 根目录，先改对路径更稳。",
                details = details,
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.OpenPathSettings,
                    label = "去设置路径",
                ),
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.httpOk == true) {
            return blocked(
                summary = "酒馆看起来已经在运行了，不用重复启动。",
                details = listOf("端口 ${doctorReport.port} 当前已经能正常响应。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.ReturnToTavern,
                    label = "返回酒馆",
                ),
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.processDetected == true) {
            return blocked(
                summary = if (doctorReport.curlAvailable == false) {
                    "已经检测到酒馆进程，但这次没法确认网页状态，先别重复启动。"
                } else {
                    "已经检测到酒馆进程，但网页当前没有正常响应，先处理现有进程更稳。"
                },
                details = listOf("端口 ${doctorReport.port} 当前疑似已有旧进程占着。"),
                action = TavernStartPreflightAction(
                    type = TavernStartPreflightActionType.StopDetectedProcess,
                    label = "尝试停止现有进程",
                ),
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.portConflict == true) {
            return blocked(
                summary = "酒馆端口已经被别的进程占用，先处理端口占用再启动。",
                details = listOf("端口 ${doctorReport.port} 当前不是空闲状态。"),
                doctorReport = doctorReport,
            )
        }

        if (doctorReport.summaryLevel == TavernDoctorLevel.Failed) {
            return blocked(
                summary = doctorReport.summaryMessage.ifBlank { "启动前预检没通过，请先处理上面的环境问题。" },
                details = listOf("这次预检没有通过，先修好再启动更稳。"),
                doctorReport = doctorReport,
            )
        }

        return TavernStartPreflightResult(ok = true, doctorReport = doctorReport)
    }

    private fun blocked(
        summary: String,
        details: List<String>,
        action: TavernStartPreflightAction? = null,
        doctorReport: TavernDoctorReport? = null,
    ): TavernStartPreflightResult {
        return TavernStartPreflightResult(
            ok = false,
            title = "启动前发现问题",
            summary = summary,
            details = details,
            action = action,
            doctorReport = doctorReport,
        )
    }
}
