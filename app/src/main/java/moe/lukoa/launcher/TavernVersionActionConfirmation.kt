package moe.lukoa.launcher

enum class TavernVersionActionKind(
    val title: String,
    val dialogTitle: String,
    val confirmLabel: String,
    val baseCommand: String,
    val busyText: String,
    val taskKind: PendingLauncherTaskKind,
    val safetyBackupPrefix: String,
) {
    Update(
        title = "更新酒馆",
        dialogTitle = "确认更新酒馆",
        confirmLabel = "确认更新",
        baseCommand = "tavern-update",
        busyText = "更新酒馆源码",
        taskKind = PendingLauncherTaskKind.UpdateTavern,
        safetyBackupPrefix = "更新前",
    ),
    Rollback(
        title = "回退酒馆",
        dialogTitle = "确认回退酒馆",
        confirmLabel = "确认回退",
        baseCommand = "tavern-rollback",
        busyText = "回退酒馆版本",
        taskKind = PendingLauncherTaskKind.RollbackTavern,
        safetyBackupPrefix = "回退前",
    ),
}

data class TavernVersionActionConfirmation(
    val kind: TavernVersionActionKind,
    val summary: String,
    val currentVersion: String,
    val targetVersion: String,
    val sourceLabel: String,
    val detail: String,
    val riskTip: String,
)

object TavernVersionActionConfirmationBuilder {
    fun build(
        kind: TavernVersionActionKind,
        current: TavernVersionInfo,
        target: TavernVersionChoice,
        fallbackRepoUrl: String,
    ): TavernVersionActionConfirmation {
        val repoLabel = repoLabelFor(target.repoUrl.ifBlank { fallbackRepoUrl })
        val targetVersion = target.label.ifBlank { target.target }
        val detail = when (kind) {
            TavernVersionActionKind.Update ->
                "开始前会先自动创建一份安全备份。更新只切换程序版本，不会删除聊天、角色、世界书和插件。"

            TavernVersionActionKind.Rollback ->
                "开始前会先自动创建一份安全备份。回退只切换程序版本，不会删除聊天、角色、世界书和插件。"
        }
        return TavernVersionActionConfirmation(
            kind = kind,
            summary = when (kind) {
                TavernVersionActionKind.Update -> "会把当前酒馆切到你选中的目标版本。"
                TavernVersionActionKind.Rollback -> "会把当前酒馆切回你选中的更旧版本。"
            },
            currentVersion = current.displayVersion,
            targetVersion = targetVersion,
            sourceLabel = repoLabel,
            detail = detail,
            riskTip = when (target.kind) {
                TavernVersionKind.Custom ->
                    "你选的是自定义版本。继续前请确认版本名、分支名或 commit 没填错。"

                TavernVersionKind.Test ->
                    "你选的是测试版。继续前请确认接受它可能不如正式版稳定。"

                TavernVersionKind.Stable ->
                    "执行过程中不要切换路径、镜像源或连续重复点按钮。"
            },
        )
    }
}
