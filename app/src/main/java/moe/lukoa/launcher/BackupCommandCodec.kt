package moe.lukoa.launcher

import java.util.Base64

data class BackupRenameArgs(
    val archivePath: String,
    val newName: String,
)

data class BackupExportToArgs(
    val archivePath: String,
    val destinationPath: String,
)

enum class BackupRestoreMode(val wireValue: String) {
    Full("full"),
    UserDataOnly("user-data"),
    ;

    companion object {
        fun fromWireValue(value: String): BackupRestoreMode? = entries.firstOrNull { it.wireValue == value }
    }
}

data class BackupRestoreArgs(
    val archivePath: String,
    val mode: BackupRestoreMode,
)

object BackupCommandCodec {
    private const val SEPARATOR = "."

    fun encodeRename(archivePath: String, newName: String): String {
        return listOf(archivePath.trim(), newName.trim())
            .joinToString(SEPARATOR) { encode(it) }
    }

    fun decodeRename(value: String?): BackupRenameArgs? {
        val parts = value.orEmpty().split(SEPARATOR, limit = 2)
        if (parts.size != 2 || parts.any { it.isBlank() }) return null
        return try {
            BackupRenameArgs(
                archivePath = decode(parts[0]).trim(),
                newName = decode(parts[1]).trim(),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun encodeExportTo(archivePath: String, destinationPath: String): String {
        return listOf(archivePath.trim(), destinationPath.trim())
            .joinToString(SEPARATOR) { encode(it) }
    }

    fun decodeExportTo(value: String?): BackupExportToArgs? {
        val parts = value.orEmpty().split(SEPARATOR, limit = 2)
        if (parts.size != 2 || parts.any { it.isBlank() }) return null
        return try {
            BackupExportToArgs(
                archivePath = decode(parts[0]).trim(),
                destinationPath = decode(parts[1]).trim(),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun encodeRestore(archivePath: String, mode: BackupRestoreMode): String {
        return listOf(archivePath.trim(), mode.wireValue)
            .joinToString(SEPARATOR) { encode(it) }
    }

    fun decodeRestore(value: String?): BackupRestoreArgs? {
        val parts = value.orEmpty().split(SEPARATOR, limit = 2)
        if (parts.size != 2 || parts.any { it.isBlank() }) return null
        return try {
            val archivePath = decode(parts[0]).trim()
            val mode = BackupRestoreMode.fromWireValue(decode(parts[1]).trim()) ?: return null
            BackupRestoreArgs(archivePath = archivePath, mode = mode)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun encode(value: String): String {
        return Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decode(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }
}
