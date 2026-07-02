package moe.lukoa.launcher

import java.io.File

object ExportFileNamer {
    fun nextAvailableFile(directory: File, baseName: String, extension: String): File {
        var candidate = File(directory, "$baseName.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName-$index.$extension")
            index += 1
        }
        return candidate
    }
}
