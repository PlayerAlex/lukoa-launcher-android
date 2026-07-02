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

    private fun encode(value: String): String {
        return Base64.getUrlEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decode(value: String): String {
        return String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }
}
