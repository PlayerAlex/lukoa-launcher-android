package moe.lukoa.launcher

data class TavernVersionActionState(
    val updateDisabledReason: String?,
    val rollbackDisabledReason: String?,
    val relation: TavernTargetRelation,
    val instanceActive: Boolean,
) {
    val updateAvailable: Boolean
        get() = updateDisabledReason == null

    val rollbackAvailable: Boolean
        get() = rollbackDisabledReason == null
}

object TavernVersionActionGuards {
    const val ACTIVE_INSTANCE_DISABLED_REASON = "先停止当前实例，再更新或回退。"

    fun evaluate(
        current: TavernVersionInfo,
        target: TavernVersionChoice?,
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
        tavernRunning: Boolean = false,
        tavernStarting: Boolean = false,
    ): TavernVersionActionState {
        val relation = TavernVersionComparator.relation(current, target)
        val instanceActive = tavernRunning || tavernStarting
        return TavernVersionActionState(
            updateDisabledReason = updateDisabledReason(
                current,
                target,
                relation,
                officialVersions,
                currentRepoUrl,
                instanceActive,
            ),
            rollbackDisabledReason = rollbackDisabledReason(
                current,
                target,
                relation,
                officialVersions,
                currentRepoUrl,
                instanceActive,
            ),
            relation = relation,
            instanceActive = instanceActive,
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
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
        instanceActive: Boolean,
    ): String? {
        return when {
            instanceActive -> ACTIVE_INSTANCE_DISABLED_REASON
            current.notInstalled -> "先安装酒馆。"
            !current.hasData -> "先检测当前版本。"
            current.hasLocalChanges -> "源码有本地改动，先处理。"
            target == null -> "先选目标版本。"
            else -> officialSelectionReason(target, officialVersions, currentRepoUrl)
        } ?: when {
            relation == TavernTargetRelation.Older -> "目标更旧，不能更新。"
            relation == TavernTargetRelation.Same -> "当前已经是这个版本。"
            else -> null
        }
    }

    private fun rollbackDisabledReason(
        current: TavernVersionInfo,
        target: TavernVersionChoice?,
        relation: TavernTargetRelation,
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
        instanceActive: Boolean,
    ): String? {
        return when {
            instanceActive -> ACTIVE_INSTANCE_DISABLED_REASON
            current.notInstalled -> "先安装酒馆。"
            !current.hasData -> "先检测当前版本。"
            current.hasLocalChanges -> "源码有本地改动，先处理。"
            target == null -> "先选目标版本。"
            else -> officialSelectionReason(target, officialVersions, currentRepoUrl)
        } ?: when {
            relation == TavernTargetRelation.Newer -> "目标更新，不能回退。"
            relation == TavernTargetRelation.Same -> "当前已经是这个版本。"
            relation == TavernTargetRelation.Unknown ->
                "无法判断目标是不是更旧，不能直接回退。"
            else -> null
        }
    }

    private fun officialSelectionReason(
        target: TavernVersionChoice?,
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
    ): String? {
        if (!TavernVersionCatalog.requiresOfficialCatalog(target)) return null
        if (!TavernVersionCatalog.matchesCurrentMirror(target, currentRepoUrl)) {
            return "已选版本来自旧 Git 源，请重新选择。"
        }
        if (!officialVersions.hasData) {
            return "先读取当前源的官方版本列表。"
        }
        if (!TavernVersionCatalog.listMatchesCurrentMirror(officialVersions, currentRepoUrl)) {
            return "当前版本列表不是这个 Git 源的，请先刷新。"
        }
        if (!TavernVersionCatalog.containsChoice(officialVersions, target)) {
            return "当前版本列表里没有这个目标，请先刷新。"
        }
        return null
    }
}
