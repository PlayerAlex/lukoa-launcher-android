package moe.lukoa.launcher

data class TavernProfileRemovalConfirmation(
    val profileId: String,
    val profileName: String,
    val profilePath: String,
    val profilePort: Int,
    val nextProfileName: String,
    val remainingProfileCount: Int,
    val deletesProfileDirectory: Boolean,
    val deletedDirectoryPath: String,
)

sealed interface TavernProfileRemovalDecision {
    data class Blocked(val message: String) : TavernProfileRemovalDecision

    data class Confirm(val confirmation: TavernProfileRemovalConfirmation) : TavernProfileRemovalDecision
}

object TavernProfileRemovalGuard {
    fun evaluate(
        config: TavernPathConfig,
        tavernRunning: Boolean,
        tavernStarting: Boolean,
        actionsLocked: Boolean,
    ): TavernProfileRemovalDecision {
        if (actionsLocked) {
            return TavernProfileRemovalDecision.Blocked(
                "当前还有别的操作在处理，先等它完成，再删除实例。",
            )
        }
        if (config.isActiveProfileMain) {
            return TavernProfileRemovalDecision.Blocked(
                "主实例暂时不能删除。先切换到要删的分身实例，再删除它。",
            )
        }
        if (!config.hasMultipleProfiles) {
            return TavernProfileRemovalDecision.Blocked("当前至少要保留一个实例。")
        }
        if (tavernStarting) {
            return TavernProfileRemovalDecision.Blocked(
                "当前实例还在启动中。请先等它启动完，或先停止，再删除这个实例。",
            )
        }
        if (tavernRunning) {
            return TavernProfileRemovalDecision.Blocked(
                "当前实例还在运行。请先停止这个实例，再删除它，避免删掉配置后找不回它的运行状态。",
            )
        }

        val profile = config.activeProfile
        val pathInfo = TavernProfilePathPolicy.describe(profile)
        val remainingProfiles = config.availableProfiles.filterNot { it.id == profile.id }
        val nextProfile = remainingProfiles.firstOrNull()
            ?: TavernProfileDefaults.profileForId(TavernProfileDefaults.MAIN_PROFILE_ID)
        return TavernProfileRemovalDecision.Confirm(
            TavernProfileRemovalConfirmation(
                profileId = profile.id,
                profileName = profile.normalizedName,
                profilePath = profile.displayTavernDir,
                profilePort = profile.normalizedPort,
                nextProfileName = nextProfile.normalizedName,
                remainingProfileCount = remainingProfiles.size,
                deletesProfileDirectory = pathInfo.canDeleteDirectoryWithProfile,
                deletedDirectoryPath = if (pathInfo.canDeleteDirectoryWithProfile) {
                    profile.displayTavernDir
                } else {
                    ""
                },
            ),
        )
    }
}
