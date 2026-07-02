package moe.lukoa.launcher

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
import java.util.Locale

data class BackupLibrarySource(
    val fileName: String,
    val size: Long,
    val openInput: () -> InputStream,
)

data class BackupLibraryCopyResult(
    val fileName: String,
    val termuxReadablePath: String,
    val size: Long,
)

data class BackupLibraryOperationResult(
    val fileName: String,
    val termuxReadablePath: String,
    val size: Long = 0L,
)

data class BackupLibraryPruneResult(
    val deletedPaths: List<String>,
    val remainingPaths: List<String>,
)

object BackupLibraryFiles {
    const val ROOT_RELATIVE_DIR = "lukoa/backups"
    const val MANUAL_RELATIVE_DIR = "lukoa/backups/sd"
    const val AUTO_RELATIVE_DIR = "lukoa/backups/zd"
    const val RELATIVE_DIR = ROOT_RELATIVE_DIR
    private const val ANDROID_MANUAL_RELATIVE_PATH = "Download/$MANUAL_RELATIVE_DIR/"
    private const val ANDROID_AUTO_RELATIVE_PATH = "Download/$AUTO_RELATIVE_DIR/"
    private const val FALLBACK_NAME = "external-backup.tar.gz"
    private const val GZIP_MAGIC_FIRST = 0x1F
    private const val GZIP_MAGIC_SECOND = 0x8B

    fun copyExternalUriToLibrary(context: Context, sourceUri: Uri): BackupLibraryCopyResult {
        val sourceName = displayName(context, sourceUri)
        val sourceIsGzip = sourceLooksLikeGzip(context, sourceUri)
        if (!sourceIsGzip) {
            error("这个文件不是有效的 gzip 备份。请重新选择酒馆备份文件")
        }
        val fileName = uniqueLibraryFileName(context, normalizedBackupSourceName(sourceName))
        val copiedPath = runCatching {
            copyWithPublicFile(context, sourceUri, fileName)
        }.getOrElse { directCopyError ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !canUseDirectSharedStorage()) {
                copyWithMediaStore(context, sourceUri, fileName)
            } else {
                throw directCopyError
            }
        }
        val copiedSource = openLibrarySource(context, copiedPath)
        copiedSource.canOpenOrThrow()
        return BackupLibraryCopyResult(
            fileName = fileName,
            termuxReadablePath = copiedPath,
            size = copiedSource.size,
        )
    }

    fun listLibraryArchives(context: Context): List<String> {
        return listLibraryArchiveEntries(context).map { it.termuxReadablePath }
    }

    fun pruneAutoLibraryArchives(context: Context, keepCount: Int): BackupLibraryPruneResult {
        val safeKeepCount = keepCount.coerceIn(0, 50)
        val autoEntries = listLibraryArchiveEntries(context)
            .filter { isAutoBackupArchivePath(it.termuxReadablePath) }
            .sortedWith(
                compareByDescending<BackupLibraryArchiveEntry> { it.modifiedAtMillis }
                    .thenBy { it.sourcePriority }
                    .thenBy { it.fileName.lowercase(Locale.US) },
            )
        val deleteTargets = if (safeKeepCount == 0) {
            autoEntries
        } else {
            autoEntries.drop(safeKeepCount)
        }
        val deletedPaths = mutableListOf<String>()
        deleteTargets.forEach { entry ->
            val deleted = runCatching {
                deleteLibraryArchive(context, entry.termuxReadablePath)
            }.getOrNull()
            if (deleted != null) {
                deletedPaths += deleted.termuxReadablePath
            }
        }
        return BackupLibraryPruneResult(
            deletedPaths = deletedPaths,
            remainingPaths = listLibraryArchives(context),
        )
    }

    fun copyLibraryArchive(context: Context, archivePath: String): BackupLibraryOperationResult {
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            error("备份源无效：$reason")
        }
        rejectUnsupportedRootBackupPath(archivePath)
        val source = openLibrarySource(context, archivePath)
        source.canOpenOrThrow()
        val fileName = uniqueLibraryFileName(context, source.fileName)
        val copiedPath = runCatching {
            copySourceWithPublicFile(source, fileName)
        }.getOrElse { directCopyError ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !canUseDirectSharedStorage()) {
                copySourceWithMediaStore(context, source, fileName)
            } else {
                throw directCopyError
            }
        }
        val copiedSource = openLibrarySource(context, copiedPath)
        copiedSource.canOpenOrThrow()
        return BackupLibraryOperationResult(
            fileName = fileName,
            termuxReadablePath = copiedPath,
            size = copiedSource.size,
        )
    }

    fun renameLibraryArchive(
        context: Context,
        archivePath: String,
        newName: String,
    ): BackupLibraryOperationResult {
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            error("备份源无效：$reason")
        }
        rejectUnsupportedRootBackupPath(archivePath)
        LauncherInputGuards.validateBackupRequiredName(newName)?.let { reason ->
            error(reason)
        }
        val sourceFileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        val targetFileName = LauncherInputGuards.backupFileNameForLabel(newName)
            ?: error("名称过滤后为空")
        if (sourceFileName.equals(targetFileName, ignoreCase = true)) {
            val source = openLibrarySource(context, archivePath)
            source.canOpenOrThrow()
            return BackupLibraryOperationResult(
                fileName = source.fileName,
                termuxReadablePath = archivePath.trim(),
                size = source.size,
            )
        }
        existingLibraryNames(context)
            .firstOrNull { it.equals(targetFileName, ignoreCase = true) }
            ?.let { error("已有同名备份：$it") }

        findDirectLibraryFile(archivePath)?.let { sourceFile ->
            val targetFile = File(sourceFile.parentFile ?: libraryDirectories().first(), targetFileName)
            if (targetFile.exists()) error("已有同名备份：$targetFileName")
            if (!sourceFile.renameTo(targetFile)) {
                error("重命名失败，请确认文件没有被其他应用占用")
            }
            val renamedPath = targetFile.absolutePath
            openLibrarySource(context, renamedPath).canOpenOrThrow()
            return BackupLibraryOperationResult(
                fileName = targetFileName,
                termuxReadablePath = renamedPath,
                size = targetFile.length(),
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val renamedPath = renameMediaStoreBackup(context, archivePath, targetFileName)
            if (!renamedPath.isNullOrBlank()) {
                val renamedSource = openLibrarySource(context, renamedPath)
                renamedSource.canOpenOrThrow()
                return BackupLibraryOperationResult(
                    fileName = targetFileName,
                    termuxReadablePath = renamedPath,
                    size = renamedSource.size,
                )
            }
        }

        error("没有在 Download/lukoa/backups/sd 或 zd 找到可重命名的文件")
    }

    fun deleteLibraryArchive(context: Context, archivePath: String): BackupLibraryOperationResult {
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            error("备份源无效：$reason")
        }
        rejectUnsupportedRootBackupPath(archivePath)
        val fileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        if (fileName.isBlank()) error("备份文件名为空")
        var deleted = false
        findDirectLibraryFile(archivePath, requireReadableBackup = false)?.let { file ->
            deleted = !file.exists() || file.delete()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleted = deleteMediaStoreBackups(context, archivePath) || deleted
        }
        if (!deleted) {
            error("删除失败：没有在 Download/lukoa/backups/sd 或 zd 找到这个备份，或文件被占用")
        }
        return BackupLibraryOperationResult(
            fileName = fileName,
            termuxReadablePath = archivePath.trim(),
        )
    }

    fun openLibrarySource(context: Context, archivePath: String): BackupLibrarySource {
        LauncherInputGuards.validateBackupArchivePath(archivePath)?.let { reason ->
            error("备份源无效：$reason")
        }
        rejectUnsupportedRootBackupPath(archivePath)
        val fileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findMediaStoreBackup(context, fileName, acceptedAndroidRelativePaths(archivePath))
                ?.takeIf { it.canOpen() }
                ?.let { return it }
        }
        libraryFileCandidates(archivePath).forEach { file ->
            if (file.isFile && file.length() > 0L && file.canRead()) {
                return BackupLibrarySource(
                    fileName = file.name,
                    size = file.length(),
                    openInput = { file.inputStream() },
                )
            }
        }
        error("没有在 Download/lukoa/backups/sd 或 zd 找到：$fileName")
    }

    fun canReadLibrarySource(context: Context, archivePath: String): Boolean {
        return runCatching {
            openLibrarySource(context, archivePath).canOpen()
        }.getOrDefault(false)
    }

    private fun BackupLibrarySource.canOpen(): Boolean {
        return runCatching {
            canOpenOrThrow()
        }.isSuccess
    }

    private fun BackupLibrarySource.canOpenOrThrow() {
        if (size == 0L) error("备份文件是空文件")
        openInput().use { input ->
            val firstByte = input.read()
            val secondByte = input.read()
            if (firstByte < 0 || secondByte < 0) error("备份文件读不到内容")
            if (firstByte != GZIP_MAGIC_FIRST || secondByte != GZIP_MAGIC_SECOND) {
                error("备份文件不是有效的 gzip 格式")
            }
        }
    }

    fun displayName(context: Context, uri: Uri): String {
        val queried = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        }
        return queried?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: FALLBACK_NAME
    }

    fun termuxReadablePath(fileName: String): String {
        return termuxReadablePath(fileName, MANUAL_RELATIVE_DIR)
    }

    private fun termuxReadablePath(fileName: String, relativeDir: String): String {
        return "/storage/emulated/0/Download/$relativeDir/$fileName"
    }

    private fun uniqueLibraryFileName(context: Context, sourceName: String): String {
        val safeBase = safeBackupBaseName(sourceName)
        val existing = existingLibraryNames(context)
            .map { it.lowercase(Locale.US) }
            .toSet()
        fun candidate(index: Int): String {
            return if (index == 0) {
                "$safeBase.tar.gz"
            } else {
                "$safeBase($index).tar.gz"
            }
        }
        var index = 0
        while (true) {
            val name = candidate(index)
            if (!existing.contains(name.lowercase(Locale.US))) {
                return name
            }
            index += 1
        }
    }

    private fun safeBackupBaseName(sourceName: String): String {
        val base = sourceName
            .trim()
            .replace(Regex("(?i)(\\.tar\\(\\d+\\)\\.gz|\\.tar\\.gz|\\.t\\.\\.gz|\\.tgz|\\.gz)$"), "")
            .replace('\r', '-')
            .replace('\n', '-')
            .replace('\t', '-')
            .replace(Regex("[\\\\/:*?\"<>|]+"), "-")
            .trim('-')
            .trimStart('.')
            .replace(Regex("-+"), "-")
            .ifBlank { "external-backup" }
        return base.take(80)
    }

    private fun normalizedBackupSourceName(sourceName: String): String {
        val trimmed = sourceName.trim().ifBlank { FALLBACK_NAME }
        normalizedTarGzName(trimmed)?.let { return it }
        return "${safeBackupBaseName(trimmed)}.tar.gz"
    }

    private fun normalizedTarGzName(sourceName: String): String? {
        val tarGzMatch = Regex("(?i)\\.tar\\.gz").find(sourceName)
        if (tarGzMatch != null) {
            return sourceName.substring(0, tarGzMatch.range.last + 1)
        }
        val indexedTarGzMatch = Regex("(?i)\\.tar\\((\\d+)\\)\\.gz").find(sourceName)
        if (indexedTarGzMatch != null) {
            val suffix = indexedTarGzMatch.groupValues[1]
            return sourceName.substring(0, indexedTarGzMatch.range.first) + "($suffix).tar.gz"
        }
        if (sourceName.endsWith(".t..gz", ignoreCase = true)) {
            return sourceName.dropLast(".t..gz".length) + ".tar.gz"
        }
        if (sourceName.endsWith(".tgz", ignoreCase = true)) {
            return sourceName.dropLast(".tgz".length) + ".tar.gz"
        }
        return null
    }

    private fun sourceLooksLikeGzip(context: Context, sourceUri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.read() == GZIP_MAGIC_FIRST && input.read() == GZIP_MAGIC_SECOND
            } ?: false
        }.getOrDefault(false)
    }

    private data class BackupLibraryArchiveEntry(
        val fileName: String,
        val termuxReadablePath: String,
        val size: Long,
        val modifiedAtMillis: Long,
        val sourcePriority: Int,
    )

    private data class DirectBackupDirectory(
        val relativeDir: String,
        val directory: File,
        val priority: Int,
    )

    private fun listLibraryArchiveEntries(context: Context): List<BackupLibraryArchiveEntry> {
        val entries = mutableListOf<BackupLibraryArchiveEntry>()
        directLibraryDirectories().forEach { backupDir ->
            backupDir.directory.listFiles { file ->
                file.isFile && file.name.endsWith(".tar.gz", ignoreCase = true)
            }?.forEach { file ->
                if (file.length() > 0L && file.canRead() && file.looksLikeGzip()) {
                    entries += BackupLibraryArchiveEntry(
                        fileName = file.name,
                        termuxReadablePath = file.absolutePath,
                        size = file.length(),
                        modifiedAtMillis = file.lastModified(),
                        sourcePriority = backupDir.priority,
                    )
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            entries += listMediaStoreBackups(context)
        }
        return entries
            .sortedWith(
                compareByDescending<BackupLibraryArchiveEntry> { it.modifiedAtMillis }
                    .thenBy { it.sourcePriority }
                    .thenBy { it.fileName.lowercase(Locale.US) },
            )
            .distinctBy { it.termuxReadablePath.lowercase(Locale.US) }
    }

    @SuppressLint("NewApi")
    private fun copyWithMediaStore(context: Context, sourceUri: Uri, fileName: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/gzip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, ANDROID_MANUAL_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建备份库文件")
        try {
            val copiedBytes = resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(outputUri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("无法写入备份库文件")
            } ?: error("无法读取选择的备份文件")
            if (copiedBytes <= 0L) {
                error("选择的备份文件是空文件")
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(outputUri, values, null, null)
            val mediaStoreSource = findMediaStoreBackup(context, fileName)
                ?: error("导入后的备份文件无法读取")
            if (mediaStoreSource.size > 0L && mediaStoreSource.size != copiedBytes) {
                error("备份文件写入不完整")
            }
            mediaStoreSource.canOpenOrThrow()
        } catch (error: Exception) {
            resolver.delete(outputUri, null, null)
            throw error
        }
        return termuxReadablePath(fileName)
    }

    @Suppress("DEPRECATION")
    private fun copyWithPublicFile(context: Context, sourceUri: Uri, fileName: String): String {
        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$MANUAL_RELATIVE_DIR/$fileName",
        ).apply {
            parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("无法创建备份库目录")
            }
        }
        val copiedBytes = try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("无法读取选择的备份文件")
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }
        if (copiedBytes <= 0L || outputFile.length() <= 0L) {
            outputFile.delete()
            error("选择的备份文件是空文件")
        }
        if (outputFile.length() != copiedBytes) {
            outputFile.delete()
            error("备份文件写入不完整")
        }
        return outputFile.absolutePath
    }

    @Suppress("DEPRECATION")
    private fun copySourceWithPublicFile(source: BackupLibrarySource, fileName: String): String {
        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$MANUAL_RELATIVE_DIR/$fileName",
        ).apply {
            parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("无法创建备份库目录")
            }
        }
        val copiedBytes = try {
            source.openInput().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        }
        if (copiedBytes <= 0L || outputFile.length() <= 0L) {
            outputFile.delete()
            error("复制出来的备份是空文件")
        }
        if (source.size > 0L && outputFile.length() != source.size) {
            outputFile.delete()
            error("备份复制不完整")
        }
        return outputFile.absolutePath
    }

    @SuppressLint("NewApi")
    private fun copySourceWithMediaStore(
        context: Context,
        source: BackupLibrarySource,
        fileName: String,
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/gzip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, ANDROID_MANUAL_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建备份库文件")
        try {
            val copiedBytes = source.openInput().use { input ->
                resolver.openOutputStream(outputUri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("无法写入备份库文件")
            }
            if (copiedBytes <= 0L || (source.size > 0L && copiedBytes != source.size)) {
                error("备份复制不完整")
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(outputUri, values, null, null)
            findMediaStoreBackup(context, fileName)?.canOpenOrThrow()
                ?: error("复制后的备份无法读取")
        } catch (error: Exception) {
            resolver.delete(outputUri, null, null)
            throw error
        }
        return termuxReadablePath(fileName)
    }

    private fun canUseDirectSharedStorage(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    private fun existingLibraryNames(context: Context): Set<String> {
        val names = linkedSetOf<String>()
        directLibraryDirectories().forEach { backupDir ->
            backupDir.directory.listFiles { file -> file.isFile && file.name.endsWith(".tar.gz", ignoreCase = true) }
                ?.forEach { names += it.name }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            names += mediaStoreBackupNames(context)
        }
        return names
    }

    @Suppress("DEPRECATION")
    private fun libraryDirectories(): List<File> {
        return directLibraryDirectories().map { it.directory }
    }

    @Suppress("DEPRECATION")
    private fun directLibraryDirectories(): List<DirectBackupDirectory> {
        val relativeDirs = listOf(
            MANUAL_RELATIVE_DIR to 0,
            AUTO_RELATIVE_DIR to 1,
        )
        return externalStorageRootCandidates()
            .flatMap { root ->
                relativeDirs.map { (relative, priority) ->
                    DirectBackupDirectory(
                        relativeDir = relative,
                        directory = File("$root/Download/$relative"),
                        priority = priority,
                    )
                }
            }
            .distinctBy { it.directory.absolutePath }
    }

    @Suppress("DEPRECATION")
    private fun libraryFileCandidates(archivePath: String): List<File> {
        val fileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        return directLibraryDirectories()
            .filter { dir -> relativeDirMatchesArchivePath(archivePath, dir.relativeDir) }
            .map { dir -> File(dir.directory, fileName) }
            .distinctBy { it.absolutePath }
    }

    private fun findDirectLibraryFile(
        archivePath: String,
        requireReadableBackup: Boolean = true,
    ): File? {
        return libraryFileCandidates(archivePath)
            .firstOrNull { file ->
                file.isFile && (!requireReadableBackup || (file.length() > 0L && file.canRead()))
            }
    }

    @Suppress("DEPRECATION")
    private fun externalStorageRootCandidates(): List<String> {
        val publicDownloadsParent = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .parentFile
            ?.absolutePath
            ?.trimEnd('/')
        return listOfNotNull(
            Environment.getExternalStorageDirectory().absolutePath.trimEnd('/'),
            publicDownloadsParent,
        )
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun relativeDirMatchesArchivePath(archivePath: String, relativeDir: String): Boolean {
        val normalized = archivePath.trim().replace('\\', '/')
        return when {
            normalized.contains("/$MANUAL_RELATIVE_DIR/", ignoreCase = true) -> relativeDir == MANUAL_RELATIVE_DIR
            normalized.contains("/$AUTO_RELATIVE_DIR/", ignoreCase = true) -> relativeDir == AUTO_RELATIVE_DIR
            isUnsupportedRootBackupPath(normalized) -> false
            else -> true
        }
    }

    private fun rejectUnsupportedRootBackupPath(archivePath: String) {
        if (isUnsupportedRootBackupPath(archivePath)) {
            error("不再支持 Download/lukoa/backups 根目录里的备份。请放到 sd 或 zd 文件夹。")
        }
        if (archivePath.trim().replace('\\', '/').contains("/lukoa-tavern-backups/", ignoreCase = true)) {
            error("不再支持旧版 lukoa-tavern-backups 目录。请先导入到备份库。")
        }
    }

    private fun isUnsupportedRootBackupPath(archivePath: String): Boolean {
        val normalized = archivePath.trim().replace('\\', '/')
        return normalized.contains("/$ROOT_RELATIVE_DIR/", ignoreCase = true) &&
            !normalized.contains("/$MANUAL_RELATIVE_DIR/", ignoreCase = true) &&
            !normalized.contains("/$AUTO_RELATIVE_DIR/", ignoreCase = true)
    }

    private fun isAutoBackupArchivePath(path: String): Boolean {
        return path.trim().replace('\\', '/').contains("/$AUTO_RELATIVE_DIR/", ignoreCase = true)
    }

    private fun acceptedAndroidRelativePaths(archivePath: String): Set<String>? {
        val normalized = archivePath.trim().replace('\\', '/')
        return when {
            normalized.contains("/$MANUAL_RELATIVE_DIR/", ignoreCase = true) -> setOf(ANDROID_MANUAL_RELATIVE_PATH)
            normalized.contains("/$AUTO_RELATIVE_DIR/", ignoreCase = true) -> setOf(ANDROID_AUTO_RELATIVE_PATH)
            else -> null
        }
    }

    @SuppressLint("InlinedApi")
    private fun mediaStoreBackupNames(context: Context): Set<String> {
        val names = linkedSetOf<String>()
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex).orEmpty() else ""
                if (!isBackupRelativePath(relative)) continue
                val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                if (name.endsWith(".tar.gz", ignoreCase = true)) {
                    names += name
                }
            }
        }
        return names
    }

    @SuppressLint("InlinedApi")
    private fun listMediaStoreBackups(context: Context): List<BackupLibraryArchiveEntry> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val entries = mutableListOf<BackupLibraryArchiveEntry>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex).orEmpty() else ""
                if (!isBackupRelativePath(relative)) continue
                val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                if (!name.endsWith(".tar.gz", ignoreCase = true)) continue
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                if (size <= 0L) continue
                val uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idIndex),
                )
                val source = BackupLibrarySource(
                    fileName = name,
                    size = size,
                    openInput = {
                        context.contentResolver.openInputStream(uri)
                            ?: error("无法读取备份库文件")
                    },
                )
                if (!source.canOpen()) continue
                val modified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) * 1000L else 0L
                entries += BackupLibraryArchiveEntry(
                    fileName = name,
                    termuxReadablePath = termuxReadablePathFor(relative, name),
                    size = size,
                    modifiedAtMillis = modified,
                    sourcePriority = 1,
                )
            }
        }
        return entries
    }

    @SuppressLint("InlinedApi")
    private fun renameMediaStoreBackup(context: Context, archivePath: String, targetFileName: String): String? {
        val resolver = context.contentResolver
        val sourceFileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        mediaStoreBackupUrisWithRelativePath(
            context = context,
            fileName = sourceFileName,
            acceptedRelatives = acceptedAndroidRelativePaths(archivePath),
        ).forEach { (uri, relative) ->
            val renamed = runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                }
                resolver.update(uri, values, null, null) > 0
            }.getOrDefault(false)
            if (renamed) {
                return termuxReadablePathFor(relative, targetFileName)
            }
        }
        return null
    }

    @SuppressLint("InlinedApi")
    private fun deleteMediaStoreBackups(context: Context, archivePath: String): Boolean {
        val resolver = context.contentResolver
        val fileName = archivePath.trim().replace('\\', '/').substringAfterLast('/')
        var deleted = false
        mediaStoreBackupUris(context, fileName, acceptedAndroidRelativePaths(archivePath)).forEach { uri ->
            deleted = runCatching { resolver.delete(uri, null, null) > 0 }.getOrDefault(false) || deleted
        }
        return deleted
    }

    @SuppressLint("InlinedApi")
    private fun mediaStoreBackupUris(
        context: Context,
        fileName: String,
        acceptedRelatives: Set<String>?,
    ): List<Uri> {
        return mediaStoreBackupUrisWithRelativePath(context, fileName, acceptedRelatives).map { it.first }
    }

    @SuppressLint("InlinedApi")
    private fun mediaStoreBackupUrisWithRelativePath(
        context: Context,
        fileName: String,
        acceptedRelatives: Set<String>? = null,
    ): List<Pair<Uri, String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val uris = mutableListOf<Pair<Uri, String>>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(fileName),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex).orEmpty() else ""
                if (!isBackupRelativePath(relative)) continue
                if (acceptedRelatives != null && acceptedRelatives.none { it.equals(relative, ignoreCase = true) }) {
                    continue
                }
                val uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idIndex),
                )
                uris += uri to relative
            }
        }
        return uris
    }

    private fun isBackupRelativePath(relative: String): Boolean {
        return relative.equals(ANDROID_MANUAL_RELATIVE_PATH, ignoreCase = true) ||
            relative.equals(ANDROID_AUTO_RELATIVE_PATH, ignoreCase = true)
    }

    private fun termuxReadablePathFor(relative: String, fileName: String): String {
        return when {
            relative.equals(ANDROID_MANUAL_RELATIVE_PATH, ignoreCase = true) -> {
                termuxReadablePath(fileName, MANUAL_RELATIVE_DIR)
            }
            relative.equals(ANDROID_AUTO_RELATIVE_PATH, ignoreCase = true) -> {
                termuxReadablePath(fileName, AUTO_RELATIVE_DIR)
            }
            else -> termuxReadablePath(fileName)
        }
    }

    private fun File.looksLikeGzip(): Boolean {
        return runCatching {
            inputStream().use { input ->
                input.read() == GZIP_MAGIC_FIRST && input.read() == GZIP_MAGIC_SECOND
            }
        }.getOrDefault(false)
    }

    @SuppressLint("InlinedApi")
    private fun findMediaStoreBackup(
        context: Context,
        fileName: String,
        acceptedRelatives: Set<String>? = null,
    ): BackupLibrarySource? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        return context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(fileName),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            while (cursor.moveToNext()) {
                val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex).orEmpty() else ""
                if (!isBackupRelativePath(relative)) {
                    continue
                }
                if (acceptedRelatives != null && acceptedRelatives.none { it.equals(relative, ignoreCase = true) }) {
                    continue
                }
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else -1L
                if (size == 0L) continue
                val uri = ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idIndex),
                )
                return BackupLibrarySource(
                    fileName = fileName,
                    size = size,
                    openInput = {
                        context.contentResolver.openInputStream(uri)
                            ?: error("无法读取备份库文件")
                    },
                )
            }
            null
        }
    }
}
