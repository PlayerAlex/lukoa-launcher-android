package moe.lukoa.launcher

object LauncherInputGuards {
    private const val COMMAND_SEPARATOR = "::"
    private const val MAX_BACKUP_NAME_LENGTH = 48
    private const val MAX_BACKUP_PATH_LENGTH = 1024
    private const val MAX_VERSION_TARGET_LENGTH = 128
    private val versionTargetPattern = Regex("^[A-Za-z0-9][A-Za-z0-9._/@+-]{0,127}$")
    private val backupUnsafeNamePattern = Regex("[/\\\\:*?\"<>|]+")
    private val repeatedDashPattern = Regex("-+")

    fun validateManualBackupName(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.length > MAX_BACKUP_NAME_LENGTH -> "名称最多 $MAX_BACKUP_NAME_LENGTH 个字符。"
            hasUnsafeProtocolChars(normalized) -> "名称不能包含换行、控制字符或 ::。"
            hasPathSeparator(normalized) -> "名称不能包含 / 或 \\，避免被当成路径。"
            hasInvalidFileNameChars(normalized) -> "名称不能包含 : * ? \" < > |。"
            normalized == "." || normalized == ".." -> "名称不能只写成 . 或 ..。"
            else -> null
        }
    }

    fun validateBackupRequiredName(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "名称不能为空。"
            backupFileNameForLabel(normalized) == null -> "名称过滤后会变成空，请换一个更明确的名字。"
            else -> validateManualBackupName(normalized)
        }
    }

    fun backupFileNameForLabel(value: String): String? {
        val base = value.trim()
            .removeSuffix(".tar.gz")
            .replace('\r', '-')
            .replace('\n', '-')
            .replace('\t', '-')
            .replace(backupUnsafeNamePattern, "-")
            .trim('-')
            .replace(repeatedDashPattern, "-")
            .trimStart('.')
        return base.takeIf { it.isNotBlank() }?.let { "$it.tar.gz" }
    }

    fun validateBackupArchivePath(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "路径不能为空。"
            normalized.length > MAX_BACKUP_PATH_LENGTH -> "路径太长，请换一个更短的位置。"
            hasUnsafeProtocolChars(normalized) -> "路径不能包含换行、控制字符或 ::。"
            !normalized.endsWith(".tar.gz") -> "路径必须指向 .tar.gz 备份文件。"
            else -> null
        }
    }

    fun validateVersionTarget(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "版本不能为空。"
            normalized.length > MAX_VERSION_TARGET_LENGTH -> "版本名太长。"
            hasUnsafeProtocolChars(normalized) -> "版本不能包含换行、控制字符或 ::。"
            normalized.startsWith("-") -> "版本不能以 - 开头。"
            normalized.contains("..") -> "版本不能包含连续两个点。"
            normalized.contains("//") -> "版本不能包含连续斜杠。"
            normalized.endsWith("/") -> "版本不能以斜杠结尾。"
            !versionTargetPattern.matches(normalized) -> "只能填 tag、分支名或 commit。"
            else -> null
        }
    }

    private fun hasUnsafeProtocolChars(value: String): Boolean {
        return value.contains(COMMAND_SEPARATOR) || value.any { it == '\n' || it == '\r' || it.code < 32 }
    }

    private fun hasPathSeparator(value: String): Boolean {
        return value.any { it == '/' || it == '\\' }
    }

    private fun hasInvalidFileNameChars(value: String): Boolean {
        return value.any { it == ':' || it == '*' || it == '?' || it == '"' || it == '<' || it == '>' || it == '|' }
    }
}
