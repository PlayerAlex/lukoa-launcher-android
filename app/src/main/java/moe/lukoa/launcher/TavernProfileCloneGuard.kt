package moe.lukoa.launcher

data class TavernProfileCloneConfirmation(
    val sourceProfile: TavernProfile,
    val targetProfile: TavernProfile,
)

sealed interface TavernProfileCloneDecision {
    data class Blocked(val message: String) : TavernProfileCloneDecision
    data class Confirm(val confirmation: TavernProfileCloneConfirmation) : TavernProfileCloneDecision
}

object TavernProfileCloneGuard {
    fun evaluate(
        config: TavernPathConfig,
        tavernRunning: Boolean,
        tavernStarting: Boolean,
        actionsLocked: Boolean,
    ): TavernProfileCloneDecision {
        if (actionsLocked) return TavernProfileCloneDecision.Blocked("当前有其他任务正在处理，请完成后再克隆实例。")
        if (tavernRunning || tavernStarting) {
            return TavernProfileCloneDecision.Blocked("克隆实例前必须先停止当前酒馆。")
        }
        val source = config.activeProfile
        val target = runCatching { TavernProfileDefaults.suggestedClone(config.availableProfiles) }
            .getOrElse { return TavernProfileCloneDecision.Blocked(it.message ?: "没有可用的分身槽位。") }
        if (!TavernProfilePathPolicy.isLauncherManagedPath(target.normalizedTavernDir)) {
            return TavernProfileCloneDecision.Blocked("新分身没有分配到安全的启动器托管目录。")
        }
        if (TavernComparablePath.same(source.normalizedTavernDir, target.normalizedTavernDir)) {
            return TavernProfileCloneDecision.Blocked("新分身目录与当前实例相同，不能克隆。")
        }
        return TavernProfileCloneDecision.Confirm(
            TavernProfileCloneConfirmation(sourceProfile = source, targetProfile = target),
        )
    }
}
