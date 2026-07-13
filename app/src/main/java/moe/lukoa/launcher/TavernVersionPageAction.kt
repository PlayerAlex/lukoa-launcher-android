package moe.lukoa.launcher

internal enum class TavernVersionPageActionKind {
    Install,
    Update,
    Rollback,
    Switch,
    None,
}

internal data class TavernVersionPageAction(
    val kind: TavernVersionPageActionKind,
    val currentLabel: String,
    val targetLabel: String,
    val badgeLabel: String,
    val buttonLabel: String,
    val enabled: Boolean,
    val disabledReason: String? = null,
)

internal object TavernVersionPageActionResolver {
    private const val ACTION_LOCKED_REASON = "当前有任务正在处理，请等任务结束后再操作。"

    fun resolve(
        actionsLocked: Boolean,
        current: TavernVersionInfo,
        selectedVersion: TavernVersionChoice?,
        actionState: TavernVersionActionState,
    ): TavernVersionPageAction {
        val currentLabel = when {
            current.notInstalled -> "未安装"
            current.hasData -> current.displayVersion
            else -> "未读取"
        }
        val targetLabel = selectedVersion?.let(::targetLabel).orEmpty().ifBlank { "未选择" }

        val base = when {
            selectedVersion == null -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.None,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "待选择",
                buttonLabel = "先选择目标版本",
                enabled = false,
                disabledReason = "请先选择一个官方版本，或填写自定义版本。",
            )

            current.notInstalled -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.Install,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "安装",
                buttonLabel = "安装 $targetLabel",
                enabled = !actionState.instanceActive,
                disabledReason = if (actionState.instanceActive) {
                    TavernVersionActionGuards.ACTIVE_INSTANCE_DISABLED_REASON
                } else {
                    null
                },
            )

            !current.hasData -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.None,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "待检测",
                buttonLabel = "先检测当前版本",
                enabled = false,
                disabledReason = "先重新检测当前安装，才能判断应该更新、回退还是切换。",
            )

            actionState.relation == TavernTargetRelation.Newer -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.Update,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "更新",
                buttonLabel = "更新到 $targetLabel",
                enabled = actionState.updateAvailable,
                disabledReason = actionState.updateDisabledReason,
            )

            actionState.relation == TavernTargetRelation.Older -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.Rollback,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "回退",
                buttonLabel = "回退到 $targetLabel",
                enabled = actionState.rollbackAvailable,
                disabledReason = actionState.rollbackDisabledReason,
            )

            actionState.relation == TavernTargetRelation.Unknown -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.Switch,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "切换",
                buttonLabel = "切换到 $targetLabel",
                enabled = actionState.updateAvailable,
                disabledReason = actionState.updateDisabledReason,
            )

            else -> TavernVersionPageAction(
                kind = TavernVersionPageActionKind.None,
                currentLabel = currentLabel,
                targetLabel = targetLabel,
                badgeLabel = "当前",
                buttonLabel = "已经是 $targetLabel",
                enabled = false,
                disabledReason = "当前安装已经是这个版本。",
            )
        }

        return if (actionsLocked && base.kind != TavernVersionPageActionKind.None) {
            base.copy(enabled = false, disabledReason = ACTION_LOCKED_REASON)
        } else {
            base
        }
    }

    private fun targetLabel(choice: TavernVersionChoice): String {
        return choice.name.trim().ifBlank { choice.target.trim() }
    }
}
