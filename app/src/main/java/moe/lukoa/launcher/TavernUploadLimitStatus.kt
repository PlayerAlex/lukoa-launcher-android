package moe.lukoa.launcher

enum class TavernUploadLimitPatchState {
    Active,
    NotManaged,
    ChangedOrOverwritten,
    Unknown,
}

data class TavernUploadLimitStatus(
    val currentMegabytes: Int? = null,
    val patchState: TavernUploadLimitPatchState = TavernUploadLimitPatchState.Unknown,
    val recordedPreviousMegabytes: Int? = null,
    val recordedAppliedMegabytes: Int? = null,
    val recordedCommit: String = "",
    val checking: Boolean = false,
    val message: String = "尚未检查当前上传限制。",
)

object TavernUploadLimitStatusParser {
    fun parse(output: String): TavernUploadLimitStatus? {
        if (!output.contains("==== SillyTavern upload limit ====")) return null
        val current = output.lineValue("uploadLimit.currentMb")?.toIntOrNull() ?: return null
        val patchState = when (output.lineValue("uploadLimit.patchState")) {
            "active" -> TavernUploadLimitPatchState.Active
            "not-managed" -> TavernUploadLimitPatchState.NotManaged
            "changed-or-overwritten" -> TavernUploadLimitPatchState.ChangedOrOverwritten
            else -> TavernUploadLimitPatchState.Unknown
        }
        return TavernUploadLimitStatus(
            currentMegabytes = current,
            patchState = patchState,
            recordedPreviousMegabytes = output.lineValue("uploadLimit.recordedPreviousMb")?.toIntOrNull(),
            recordedAppliedMegabytes = output.lineValue("uploadLimit.recordedAppliedMb")?.toIntOrNull(),
            recordedCommit = output.lineValue("uploadLimit.recordedCommit").orEmpty(),
            message = when (patchState) {
                TavernUploadLimitPatchState.Active -> "当前限制与启动器记录一致。"
                TavernUploadLimitPatchState.NotManaged -> "已识别当前限制，但还没有由启动器管理。"
                TavernUploadLimitPatchState.ChangedOrOverwritten -> "当前值与记录不一致，可能已被 ST 更新或手动修改。"
                TavernUploadLimitPatchState.Unknown -> "已读取当前限制，但无法确认补丁状态。"
            },
        )
    }

    private fun String.lineValue(key: String): String? = lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')
        ?.trim()
}
