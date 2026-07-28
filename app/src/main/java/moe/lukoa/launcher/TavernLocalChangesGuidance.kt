package moe.lukoa.launcher

object TavernLocalChangesGuidance {
    private const val UPLOAD_LIMIT_FILE = "src/server-main.js"

    fun isLikelyUploadLimitChange(
        versionInfo: TavernVersionInfo,
        uploadLimitStatus: TavernUploadLimitStatus,
    ): Boolean {
        if (!versionInfo.hasLocalChanges) return false
        if (
            uploadLimitStatus.patchState == TavernUploadLimitPatchState.Active ||
            uploadLimitStatus.patchState == TavernUploadLimitPatchState.ChangedOrOverwritten
        ) {
            return true
        }
        return versionInfo.changedFilesPreview
            .replace('\\', '/')
            .lineSequence()
            .any { line -> line.contains(UPLOAD_LIMIT_FILE, ignoreCase = true) }
    }
}
