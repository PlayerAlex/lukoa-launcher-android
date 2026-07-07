package moe.lukoa.launcher

enum class TavernProfileMigrationTargetKind(
    val label: String,
) {
    LauncherManaged("启动器托管目录"),
    TraditionalDefault("传统默认目录"),
    Custom("自定义地址"),
}

data class TavernProfileMigrationConfirmation(
    val profileId: String,
    val profileName: String,
    val sourcePath: String,
    val targetPath: String,
    val targetKind: TavernProfileMigrationTargetKind,
    val targetKindLabel: String,
    val riskNote: String,
)

sealed interface TavernProfileMigrationDecision {
    data class Blocked(val message: String) : TavernProfileMigrationDecision

    data class Confirm(val confirmation: TavernProfileMigrationConfirmation) : TavernProfileMigrationDecision
}

object TavernProfileMigrationGuard {
    fun evaluate(
        config: TavernPathConfig,
        targetPath: String,
        targetKind: TavernProfileMigrationTargetKind,
        tavernRunning: Boolean,
        tavernStarting: Boolean,
        actionsLocked: Boolean,
    ): TavernProfileMigrationDecision {
        if (actionsLocked) {
            return TavernProfileMigrationDecision.Blocked(
                "当前还有别的操作在处理，先等它完成，再迁移酒馆目录。",
            )
        }
        if (tavernStarting) {
            return TavernProfileMigrationDecision.Blocked(
                "当前实例还在启动中。请先等它启动完，或先停止，再迁移目录。",
            )
        }
        if (tavernRunning) {
            return TavernProfileMigrationDecision.Blocked(
                "当前实例还在运行。请先停止这个实例，再迁移目录，避免一边运行一边搬目录。",
            )
        }

        val profile = config.activeProfile
        val normalizedTargetPath = TavernPathNormalizer.normalize(targetPath)
        TavernPathValidator.validate(normalizedTargetPath)?.let { reason ->
            return TavernProfileMigrationDecision.Blocked(reason)
        }
        if (normalizedTargetPath == profile.normalizedTavernDir) {
            return TavernProfileMigrationDecision.Blocked("目标目录和当前目录一样，不需要迁移。")
        }
        if (targetKind == TavernProfileMigrationTargetKind.TraditionalDefault &&
            profile.id != TavernProfileDefaults.MAIN_PROFILE_ID
        ) {
            return TavernProfileMigrationDecision.Blocked(
                "传统默认目录 ~/SillyTavern 只建议留给主实例。分身实例请迁移到启动器托管目录，或自己填写一个自定义地址。",
            )
        }

        val occupiedProfile = config.availableProfiles
            .firstOrNull {
                it.id != profile.id &&
                    it.normalizedTavernDir == normalizedTargetPath
            }
        if (occupiedProfile != null) {
            return TavernProfileMigrationDecision.Blocked(
                "这个目录已经被${occupiedProfile.normalizedName}使用了，不能直接迁过去。",
            )
        }

        val targetDisplayPath = TavernPathNormalizer.toDisplayPath(normalizedTargetPath)
        val riskNote = when (targetKind) {
            TavernProfileMigrationTargetKind.LauncherManaged -> {
                "如果目标目录里已经有旧文件，迁移时会先把旧目录挪到安全备份名，再把当前实例迁过去。开始前仍建议先手动备份一次。"
            }

            TavernProfileMigrationTargetKind.TraditionalDefault -> {
                "这会把当前实例迁到传统默认目录 $targetDisplayPath。那里如果已经有另一套酒馆，会先被挪到安全备份名，再继续迁移。为了避免覆盖错内容，开始前建议先手动备份。"
            }

            TavernProfileMigrationTargetKind.Custom -> {
                "这是你自己填写的目录，不属于启动器推荐的默认位置。迁移过去后，这个实例后续的风险判断、删除实例时的目录删除和路径识别，都不再按托管目录规则处理；请确认你能自己承担这类问题。"
            }
        }

        return TavernProfileMigrationDecision.Confirm(
            TavernProfileMigrationConfirmation(
                profileId = profile.id,
                profileName = profile.normalizedName,
                sourcePath = profile.displayTavernDir,
                targetPath = targetDisplayPath,
                targetKind = targetKind,
                targetKindLabel = targetKind.label,
                riskNote = riskNote,
            ),
        )
    }
}
