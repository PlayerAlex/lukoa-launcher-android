package moe.lukoa.launcher

data class TavernVersionActionState(
    val updateDisabledReason: String?,
    val rollbackDisabledReason: String?,
    val relation: TavernTargetRelation,
) {
    val updateAvailable: Boolean
        get() = updateDisabledReason == null

    val rollbackAvailable: Boolean
        get() = rollbackDisabledReason == null
}

object TavernVersionActionGuards {
    fun evaluate(
        current: TavernVersionInfo,
        target: TavernVersionChoice?,
    ): TavernVersionActionState {
        val relation = TavernVersionComparator.relation(current, target)
        return TavernVersionActionState(
            updateDisabledReason = updateDisabledReason(current, target, relation),
            rollbackDisabledReason = rollbackDisabledReason(current, target, relation),
            relation = relation,
        )
    }

    fun relationHint(state: TavernVersionActionState, target: TavernVersionChoice?): String? {
        if (target == null || state.updateDisabledReason != null && state.rollbackDisabledReason != null) return null
        return when (state.relation) {
            TavernTargetRelation.Newer -> "目标比当前新，只能更新。"
            TavernTargetRelation.Older -> "目标比当前旧，只能回退。"
            TavernTargetRelation.Same -> "已经是这个版本。"
            TavernTargetRelation.Unknown -> "无法判断新旧，执行前先备份。"
        }
    }

    private fun updateDisabledReason(
        current: TavernVersionInfo,
        target: TavernVersionChoice?,
        relation: TavernTargetRelation,
    ): String? {
        return when {
            current.notInstalled -> "先安装酒馆。"
            !current.hasData -> "先检测当前版本。"
            current.hasLocalChanges -> "源码有本地改动，先处理。"
            target == null -> "先选目标版本。"
            relation == TavernTargetRelation.Older -> "目标更旧，不能更新。"
            relation == TavernTargetRelation.Same -> "当前已经是这个版本。"
            else -> null
        }
    }

    private fun rollbackDisabledReason(
        current: TavernVersionInfo,
        target: TavernVersionChoice?,
        relation: TavernTargetRelation,
    ): String? {
        return when {
            current.notInstalled -> "先安装酒馆。"
            !current.hasData -> "先检测当前版本。"
            current.hasLocalChanges -> "源码有本地改动，先处理。"
            target == null -> "先选目标版本。"
            relation == TavernTargetRelation.Newer -> "目标更新，不能回退。"
            relation == TavernTargetRelation.Same -> "当前已经是这个版本。"
            relation == TavernTargetRelation.Unknown ->
                "无法判断目标是不是更旧，不能直接回退。"
            else -> null
        }
    }
}
