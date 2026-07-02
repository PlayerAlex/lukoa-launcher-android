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

        val changedFiles = extractChangedFiles(text)
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

    private fun extractChangedFiles(text: String): String {
        val marker = "==== Git local changes ===="
        val start = text.lastIndexOf(marker)
        if (start < 0) return ""
        val section = text.substring(start + marker.length)
            .substringBefore("====")
            .trim()
        if (section == "clean" || section == "(clean)") return ""
        return section.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "clean" && it != "(clean)" }
            .take(4)
            .joinToString("\n")
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
        if (!current.hasData || target == null) return TavernTargetRelation.Unknown
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
