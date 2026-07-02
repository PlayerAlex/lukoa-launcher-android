package moe.lukoa.launcher

object BackupHistoryReducer {
    private const val MAX_HISTORY = 20

    fun sanitize(current: List<String>): List<String> {
        return current
            .asSequence()
            .map { it.trim() }
            .filter { isBackupLibraryArchive(it) }
            .distinctBy { backupIdentity(it) }
            .take(MAX_HISTORY)
            .toList()
    }

    fun reduce(
        current: List<String>,
        termuxOutput: String,
        ok: Boolean,
    ): List<String> {
        val deletedBackup = extractDeletedBackupArchive(termuxOutput, ok)
        val copiedBackup = extractCopiedBackupArchive(termuxOutput, ok)
        val renamedBackup = extractRenamedBackupArchive(termuxOutput, ok)
        val hasListedBackups = hasBackupListSection(termuxOutput, ok)
        val listedBackups = extractListedBackupArchives(termuxOutput, ok)
        val backupHistoryAfterDelete = deletedBackup
            ?.let { removeBackupHistoryEntry(current, it) }
            ?: current
        val backupHistoryAfterRename = renamedBackup
            ?.let { (oldPath, newPath) -> addBackupHistoryEntry(removeBackupHistoryEntry(backupHistoryAfterDelete, oldPath), newPath) }
            ?: backupHistoryAfterDelete

        return if (hasListedBackups) {
            if (listedBackups.isEmpty()) {
                sanitize(backupHistoryAfterRename)
            } else {
                replaceBackupHistoryEntries(listedBackups)
            }
        } else {
            copiedBackup
                ?.let { addBackupHistoryEntry(backupHistoryAfterRename, it) }
                ?: extractCreatedBackupArchive(termuxOutput, ok)
                    ?.let { addBackupHistoryEntry(backupHistoryAfterRename, it) }
                ?: extractImportedBackup(termuxOutput, ok)
                    ?.let { addBackupHistoryEntry(backupHistoryAfterRename, it) }
                ?: backupHistoryAfterRename
        }
    }

    private fun hasBackupListSection(termuxOutput: String, ok: Boolean): Boolean {
        return ok && termuxOutput.contains("==== SillyTavern backups ====")
    }

    private fun extractCreatedBackupArchive(termuxOutput: String, ok: Boolean): String? {
        if (!ok || !termuxOutput.contains("==== SillyTavern backup ====")) return null
        return termuxOutput.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("archive=") }
            ?.substringAfter("archive=")
            ?.trim()
            ?.takeIf { isBackupLibraryArchive(it) }
    }

    private fun extractListedBackupArchives(termuxOutput: String, ok: Boolean): List<String> {
        if (!hasBackupListSection(termuxOutput, ok)) return emptyList()
        return termuxOutput.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("backup.file=") }
            .map { it.substringAfter("backup.file=").trim() }
            .filter { isBackupLibraryArchive(it) }
            .distinct()
            .toList()
    }

    private fun extractDeletedBackupArchive(termuxOutput: String, ok: Boolean): String? {
        if (!ok || !termuxOutput.contains("==== SillyTavern backup delete ====")) return null
        return termuxOutput.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("deleted.file=") }
            ?.substringAfter("deleted.file=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractImportedBackup(termuxOutput: String, ok: Boolean): String? {
        if (!ok) return null
        return termuxOutput.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("imported.file=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { isBackupLibraryArchive(it) }
    }

    private fun extractCopiedBackupArchive(termuxOutput: String, ok: Boolean): String? {
        if (!ok || !termuxOutput.contains("==== SillyTavern backup copy ====")) return null
        return termuxOutput.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("copied.file=") }
            ?.substringAfter("copied.file=")
            ?.trim()
            ?.takeIf { isBackupLibraryArchive(it) }
    }

    private fun extractRenamedBackupArchive(termuxOutput: String, ok: Boolean): Pair<String, String>? {
        if (!ok || !termuxOutput.contains("==== SillyTavern backup rename ====")) return null
        var oldPath = ""
        var newPath = ""
        termuxOutput.lineSequence()
            .map { it.trim() }
            .forEach { line ->
                when {
                    line.startsWith("renamed.old=") -> oldPath = line.substringAfter("renamed.old=").trim()
                    line.startsWith("renamed.file=") -> newPath = line.substringAfter("renamed.file=").trim()
                }
            }
        return if (isBackupLibraryArchive(oldPath) && isBackupLibraryArchive(newPath)) {
            oldPath to newPath
        } else {
            null
        }
    }

    private fun addBackupHistoryEntry(current: List<String>, archive: String): List<String> {
        if (!isBackupLibraryArchive(archive)) return sanitize(current)
        val newIdentity = backupIdentity(archive)
        return sanitize(listOf(archive) + current.filterNot { backupIdentity(it) == newIdentity })
    }

    private fun replaceBackupHistoryEntries(archives: List<String>): List<String> {
        return sanitize(archives)
    }

    private fun removeBackupHistoryEntry(current: List<String>, archive: String): List<String> {
        val removedIdentity = backupIdentity(archive)
        return sanitize(current.filterNot { backupIdentity(it) == removedIdentity })
    }

    private fun backupIdentity(path: String): String {
        val normalized = path.trim().replace('\\', '/').lowercase()
        return normalized.ifBlank { path }
    }

    private fun isBackupLibraryArchive(path: String): Boolean {
        val normalized = path.trim().replace('\\', '/')
        if (!normalized.endsWith(".tar.gz")) return false
        if (normalized.contains("/lukoa/exports/")) return false
        if (normalized.contains("/lukoa-tavern-exports/")) return false
        if (normalized.contains("/lukoa-tavern-imports/")) return false
        return normalized.contains("/lukoa/backups/sd/") ||
            normalized.contains("/lukoa/backups/zd/") ||
            normalized.contains("/.local/state/lukoa-launcher/backups/sd/") ||
            normalized.contains("/.local/state/lukoa-launcher/backups/zd/")
    }
}
