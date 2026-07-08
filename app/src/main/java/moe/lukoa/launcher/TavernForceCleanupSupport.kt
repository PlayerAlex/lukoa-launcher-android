package moe.lukoa.launcher

enum class TavernForceCleanupKind(
    val buttonLabel: String,
    val dialogTitle: String,
) {
    StopDidNotExit(
        buttonLabel = "强制释放端口",
        dialogTitle = "确认强制释放端口",
    ),
    ResidualProcess(
        buttonLabel = "强制清理残留进程",
        dialogTitle = "确认强制清理残留进程",
    ),
    PortConflict(
        buttonLabel = "强制释放端口",
        dialogTitle = "确认强制释放端口",
    ),
}

data class TavernForceCleanupSuggestion(
    val kind: TavernForceCleanupKind,
    val summary: String,
    val reasonDetail: String,
    val buttonHint: String,
    val riskTip: String,
) {
    val buttonLabel: String
        get() = kind.buttonLabel
}

data class TavernForceCleanupConfirmation(
    val suggestion: TavernForceCleanupSuggestion,
    val profileName: String,
    val profilePath: String,
    val profilePort: Int,
)

object TavernForceCleanupSupport {
    fun detect(
        doctorReport: TavernDoctorReport?,
        status: String,
        summary: String,
    ): TavernForceCleanupSuggestion? {
        val normalizedStatus = status.trim()
        val normalizedSummary = summary.trim()
        val merged = listOf(normalizedStatus, normalizedSummary)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        if (
            normalizedSummary.contains("强制清理已完成") ||
            normalizedSummary.contains("酒馆当前未运行") ||
            normalizedSummary.contains("停止酒馆成功")
        ) {
            return null
        }
        return when {
            normalizedSummary.contains("停止酒馆失败：酒馆网页仍在响应") ||
            normalizedSummary.contains("普通停止后酒馆还没完全停下") ||
                merged.contains("Use force cleanup", ignoreCase = true) -> {
                TavernForceCleanupSuggestion(
                    kind = TavernForceCleanupKind.StopDidNotExit,
                    summary = "普通停止后，当前实例还没有完全停下。",
                    reasonDetail = "已经尝试按正常方式停止，但当前实例对应的网页或进程还没退干净。",
                    buttonHint = "如果你确认要处理的就是这个实例，可以再手动强制释放端口。",
                    riskTip = "这一步会先再尝试温和停止当前实例；如果还是不退出，才会强制结束仍占着当前目录和端口的残留进程。不会删除聊天、角色、世界书或备份文件。",
                )
            }

            doctorReport?.processDetected == true && doctorReport.httpOk != true -> {
                TavernForceCleanupSuggestion(
                    kind = TavernForceCleanupKind.ResidualProcess,
                    summary = "检测到当前实例疑似有残留进程。",
                    reasonDetail = "启动前预检或体检发现当前目录下疑似还有旧进程，但网页没有正常响应。",
                    buttonHint = "只有在你确认这是当前实例留下的旧进程时，才建议继续强制清理。",
                    riskTip = "这一步会针对当前实例目录和端口对应的残留进程做清理。不会删除聊天、角色、世界书或备份文件。",
                )
            }

            doctorReport?.portConflict == true ||
                normalizedSummary.contains("酒馆端口已被别的进程占用") ||
                normalizedStatus.contains("端口被别的进程占用") -> {
                TavernForceCleanupSuggestion(
                    kind = TavernForceCleanupKind.PortConflict,
                    summary = "检测到当前端口被别的进程占用。",
                    reasonDetail = "当前端口不是空闲状态。只有在你确认占用它的是当前实例残留进程时，才建议继续。",
                    buttonHint = "不确定是谁占用时，优先先手动关闭相关应用，或重启 Termux / 手机后再试。",
                    riskTip = "这一步会尝试清理当前实例对应的残留进程，并在温和停止无效时强制结束它们。不会删除聊天、角色、世界书或备份文件。",
                )
            }

            else -> null
        }
    }

    fun buildConfirmation(
        profile: TavernProfile,
        suggestion: TavernForceCleanupSuggestion,
    ): TavernForceCleanupConfirmation {
        return TavernForceCleanupConfirmation(
            suggestion = suggestion,
            profileName = profile.normalizedName,
            profilePath = profile.displayTavernDir,
            profilePort = profile.normalizedPort,
        )
    }
}
