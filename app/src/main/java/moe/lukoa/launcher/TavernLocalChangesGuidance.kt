package moe.lukoa.launcher

/**
 * Decides how tracked local changes in the SillyTavern checkout affect update / rollback.
 *
 * Two kinds of files are "launcher-managed": the upload-limit patch target and npm's
 * regenerated lock file. `lukoa-tavern.sh` lifts / restores those on its own, so they never
 * need the user's attention. Anything else is the user's own edit and must be explicitly
 * discarded (stashed) before git can move to another revision.
 */
object TavernLocalChangesGuidance {
    private const val UPLOAD_LIMIT_FILE = "src/server-main.js"
    private const val PACKAGE_LOCK_FILE = "package-lock.json"
    private val managedFiles = setOf(UPLOAD_LIMIT_FILE, PACKAGE_LOCK_FILE)

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
        return versionInfo.changedFilePaths.any { it.equals(UPLOAD_LIMIT_FILE, ignoreCase = true) }
    }

    /** Files the user changed themselves, i.e. everything the script will not handle automatically. */
    fun userOwnedChanges(versionInfo: TavernVersionInfo): List<String> {
        if (!versionInfo.hasLocalChanges) return emptyList()
        return versionInfo.changedFilePaths.filterNot { file -> file.lowercase() in managedFiles }
    }

    /**
     * True when update / rollback must stash something the user edited. Also true when git
     * reported changes but the file list did not come through, because then we cannot prove
     * the changes are launcher-managed.
     */
    fun requiresDiscardConsent(versionInfo: TavernVersionInfo): Boolean {
        if (!versionInfo.hasLocalChanges) return false
        if (versionInfo.changedFilePaths.isEmpty()) return true
        return userOwnedChanges(versionInfo).isNotEmpty()
    }

    /** Extracts the path from a `git status --porcelain` line such as ` M src/foo.js` or `R  a -> b`. */
    fun pathFromStatusLine(line: String): String {
        val trimmed = line.trimEnd()
        if (trimmed.length <= 3) return trimmed.trim()
        val path = trimmed.substring(3).trim()
        return path.substringAfter(" -> ", path).replace('\\', '/')
    }
}
