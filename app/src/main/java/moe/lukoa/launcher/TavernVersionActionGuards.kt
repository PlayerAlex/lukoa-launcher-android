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
            TavernTargetRelation.Newer -> "目标比当前新。"
            TavernTargetRelation.Older -> "目标比当前旧，切换前建议先备份。"
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
        // Local tracked changes no longer block here: launcher-managed files are handled by the
        // script, and user edits are surfaced in the confirmation dialog with an explicit
        // "stash and continue" consent.
        return when {
            instanceActive -> ACTIVE_INSTANCE_DISABLED_REASON
            current.notInstalled -> "先安装酒馆。"
            !current.hasData -> "先检测当前版本。"
            target == null -> "先选目标版本。"
            else -> officialSelectionReason(target, officialVersions, currentRepoUrl)
        } ?: when {
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
            target == null -> "先选目标版本。"
            else -> officialSelectionReason(target, officialVersions, currentRepoUrl)
        } ?: when {
            relation == TavernTargetRelation.Same -> "当前已经是这个版本。"
            // Unknown relation (commit hash / non-semver branch) is allowed on both sides; the
            // confirmation dialog already tells the user to back up when the direction is unclear.
            else -> null
        }
    }

    // The version list used to gate both buttons. The script fetches before checking out, and a
    // missing ref fails cleanly, so an old or unrefreshed list no longer blocks anything.
    @Suppress("UNUSED_PARAMETER")
    private fun officialSelectionReason(
        target: TavernVersionChoice?,
        officialVersions: TavernOfficialVersions,
        currentRepoUrl: String,
    ): String? = null
}
