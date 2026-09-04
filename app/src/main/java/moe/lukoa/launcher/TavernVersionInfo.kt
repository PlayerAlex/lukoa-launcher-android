package moe.lukoa.launcher

data class TavernVersionInfo(
    val hasData: Boolean = false,
    val notInstalled: Boolean = false,
    val directory: String = "",
    val packageVersion: String = "",
    val branch: String = "",
    val commit: String = "",
    val describe: String = "",
    val remote: String = "",
    val upstream: String = "",
    val rollbackTarget: String = "",
    val localChanges: String = "",
    val changedFilesPreview: String = "",
    /** Paths from `git status --porcelain`, without status columns. Capped by the parser. */
    val changedFiles: List<String> = emptyList(),
) {
    val displayVersion: String
        get() = if (notInstalled) {
            "未安装酒馆"
        } else {
            packageVersion.ifBlank {
                describe.ifBlank {
                    commit.ifBlank { "未读取" }
                }
            }
        }

    val rollbackDisplay: String
        get() = rollbackTarget.ifBlank { "暂无可回退快照" }

    val hasLocalChanges: Boolean
        get() = localChanges == "1" || changedFilesPreview.isNotBlank()

    /** Changed paths, falling back to the preview block for records that predate [changedFiles]. */
    val changedFilePaths: List<String>
        get() = changedFiles.ifEmpty {
            changedFilesPreview.lineSequence()
                .filter { it.isNotBlank() }
                .map(TavernLocalChangesGuidance::pathFromStatusLine)
                .filter { it.isNotBlank() }
                .toList()
        }
}

object TavernVersionParser {
    private val missingDirectoryPatterns = listOf(
        "SillyTavern directory not found",
        "酒馆目录不存在",
        "SillyTavern 目录不存在",
    )
    private val missingDirectoryRegex = Regex("""SillyTavern directory not found:\s*([^"\r\n]+)""")

    private val keys = setOf(
        "directory",
        "package.version",
        "git.branch",
        "git.commit",
        "git.describe",
        "git.remote",
        "git.upstream",
        "git.localChanges",
        "rollback.target",
        "after",
    )

    fun parse(text: String): TavernVersionInfo {
        val values = mutableMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val splitAt = trimmed.indexOf('=')
            if (splitAt <= 0) return@forEach
            val key = trimmed.substring(0, splitAt).trim()
            if (key in keys) {
                values[key] = trimmed.substring(splitAt + 1).trim()
            }
        }

        val changedStatusLines = extractChangedStatusLines(text)
        val changedFiles = changedStatusLines.take(PREVIEW_LINES).joinToString("\n")
        val hasData = values.isNotEmpty() || changedFiles.isNotBlank()
        if (!hasData) {
            return if (isNotInstalledSignal(text)) {
                TavernVersionInfo(
                    notInstalled = true,
                    directory = extractMissingDirectory(text),
                )
            } else {
                TavernVersionInfo()
            }
        }

        return TavernVersionInfo(
            hasData = true,
            notInstalled = false,
            directory = values["directory"].orEmpty(),
            packageVersion = values["package.version"].orEmpty(),
            branch = values["git.branch"].orEmpty(),
            commit = values["git.commit"].orEmpty().ifBlank { values["after"].orEmpty() },
            describe = values["git.describe"].orEmpty(),
            remote = values["git.remote"].orEmpty(),
            upstream = values["git.upstream"].orEmpty(),
            rollbackTarget = values["rollback.target"].orEmpty(),
            localChanges = values["git.localChanges"].orEmpty(),
            changedFilesPreview = changedFiles,
            changedFiles = changedStatusLines.map(TavernLocalChangesGuidance::pathFromStatusLine),
        )
    }

    private fun isNotInstalledSignal(text: String): Boolean {
        return missingDirectoryPatterns.any { text.contains(it, ignoreCase = true) }
    }

    private fun extractMissingDirectory(text: String): String {
        return missingDirectoryRegex.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
    }

    private fun extractChangedStatusLines(text: String): List<String> {
        val marker = "==== Git local changes ===="
        val start = text.lastIndexOf(marker)
        if (start < 0) return emptyList()
        val section = text.substring(start + marker.length)
            .substringBefore("====")
            .trim()
        if (section == "clean" || section == "(clean)") return emptyList()
        return section.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && it.trim() != "clean" && it.trim() != "(clean)" }
            .take(MAX_CHANGED_FILES)
            .toList()
    }

    private const val PREVIEW_LINES = 4
    private const val MAX_CHANGED_FILES = 60
}

data class TavernVersionOperationSummary(
    val kind: TavernVersionActionKind,
    val target: String,
    val beforeRevision: String,
    val afterRevision: String,
    val exitCode: Int,
    val npmExitCode: Int?,
    val safetyBackupPath: String,
    /** Name of the git stash holding the user's local edits, when the script had to set them aside. */
    val localChangesStash: String = "",
) {
    val succeeded: Boolean
        get() = exitCode == 0 && (npmExitCode == null || npmExitCode == 0)

    val revisionChanged: Boolean
        get() = beforeRevision.isNotBlank() &&
            afterRevision.isNotBlank() &&
            beforeRevision != "unknown" &&
            afterRevision != "unknown" &&
            !beforeRevision.equals(afterRevision, ignoreCase = true)
}

object TavernVersionOperationSummaryParser {
    fun parse(
        output: String,
        kind: TavernVersionActionKind,
        safetyBackupPath: String,
    ): TavernVersionOperationSummary? {
        val action = when (kind) {
            TavernVersionActionKind.Update -> "update"
            TavernVersionActionKind.Rollback -> "rollback"
        }
        val header = "==== SillyTavern $action ===="
        val footer = "==== end SillyTavern $action ===="
        val start = output.lastIndexOf(header)
        if (start < 0) return null
        val end = output.indexOf(footer, start + header.length)
        if (end < 0) return null
        val values = mutableMapOf<String, String>()
        output.substring(start + header.length, end).lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val splitAt = line.indexOf('=')
            if (splitAt <= 0) return@forEach
            val key = line.substring(0, splitAt)
            if (key in setOf("target", "before", "after", "exitCode", "npmExitCode", "localChanges.stash")) {
                values[key] = line.substring(splitAt + 1).trim().take(240)
            }
        }
        val exitCode = values["exitCode"]?.toIntOrNull() ?: return null
        val npmExitCode = values["npmExitCode"]?.toIntOrNull()
        if (values.containsKey("npmExitCode") && npmExitCode == null) return null
        return TavernVersionOperationSummary(
            kind = kind,
            target = values["target"].orEmpty(),
            beforeRevision = values["before"].orEmpty(),
            afterRevision = values["after"].orEmpty(),
            exitCode = exitCode,
            npmExitCode = npmExitCode,
            safetyBackupPath = safetyBackupPath.trim().take(1024),
            localChangesStash = values["localChanges.stash"].orEmpty(),
        )
    }
}

enum class TavernTargetRelation {
    Older,
    Same,
    Newer,
    Unknown,
}

object TavernVersionComparator {
    private val semverPattern = Regex("""(?i)(?:^|[^0-9a-z])v?(\d+)\.(\d+)\.(\d+)(?:[-+][0-9a-z.-]+)?""")

    fun matchesCurrent(current: TavernVersionInfo, target: TavernVersionChoice?): Boolean {
        return relation(current, target) == TavernTargetRelation.Same
    }

    fun relation(current: TavernVersionInfo, target: TavernVersionChoice?): TavernTargetRelation {
        if (!current.hasData || current.notInstalled || target == null) return TavernTargetRelation.Unknown
        branchRelation(current, target)?.let { return it }
        val currentVersion = parseVersion(current.packageVersion)
            ?: parseVersion(current.describe)
            ?: parseVersion(current.displayVersion)
            ?: return TavernTargetRelation.Unknown
        val targetVersion = parseVersion(target.target)
            ?: parseVersion(target.name)
            ?: return TavernTargetRelation.Unknown
        val compare = currentVersion.compareTo(targetVersion)
        return when {
            compare > 0 -> TavernTargetRelation.Older
            compare < 0 -> TavernTargetRelation.Newer
            else -> TavernTargetRelation.Same
        }
    }

    private fun branchRelation(
        current: TavernVersionInfo,
        target: TavernVersionChoice,
    ): TavernTargetRelation? {
        val currentBranch = current.branch.trim()
        val targetBranch = target.target.trim()
        if (currentBranch.isBlank() || targetBranch.isBlank()) return null
        if (!currentBranch.equals(targetBranch, ignoreCase = true)) return null

        val currentCommit = current.commit.trim()
        val targetCommit = target.commit.trim()
        if (currentCommit.isBlank() || targetCommit.isBlank()) return null
        if (
            currentCommit.startsWith(targetCommit, ignoreCase = true) ||
            targetCommit.startsWith(currentCommit, ignoreCase = true)
        ) {
            return TavernTargetRelation.Same
        }
        return if (target.kind == TavernVersionKind.Test) {
            TavernTargetRelation.Newer
        } else {
            null
        }
    }

    private fun parseVersion(value: String): SimpleSemVer? {
        val match = semverPattern.find(" $value") ?: return null
        return SimpleSemVer(
            major = match.groupValues[1].toIntOrNull() ?: return null,
            minor = match.groupValues[2].toIntOrNull() ?: return null,
            patch = match.groupValues[3].toIntOrNull() ?: return null,
        )
    }

    private data class SimpleSemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<SimpleSemVer> {
        override fun compareTo(other: SimpleSemVer): Int {
            return compareValuesBy(this, other, SimpleSemVer::major, SimpleSemVer::minor, SimpleSemVer::patch)
        }
    }
}
