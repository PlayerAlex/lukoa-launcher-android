package moe.lukoa.launcher

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object LauncherUpdateFileTransaction {
    fun download(
        directory: File,
        finalFileName: String,
        download: (File) -> Unit,
        validate: (File) -> Unit,
    ): File {
        require(finalFileName.endsWith(".apk", ignoreCase = true)) { "update file must be an apk" }
        if (!directory.exists() && !directory.mkdirs()) {
            error("无法创建更新缓存目录。")
        }
        val finalFile = File(directory, finalFileName)
        val temporaryFile = File(directory, ".$finalFileName.part")
        temporaryFile.delete()
        try {
            download(temporaryFile)
            validate(temporaryFile)
            moveReplacing(temporaryFile, finalFile)
            prune(directory, keep = finalFile)
            return finalFile
        } catch (error: Throwable) {
            temporaryFile.delete()
            throw error
        }
    }

    private fun moveReplacing(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun prune(directory: File, keep: File) {
        directory.listFiles().orEmpty().forEach { candidate ->
            if (candidate == keep) return@forEach
            if (candidate.name.endsWith(".apk", ignoreCase = true) || candidate.name.endsWith(".part")) {
                candidate.delete()
            }
        }
    }
}
