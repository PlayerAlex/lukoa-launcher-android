package moe.lukoa.launcher

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object SessionLogExporter {
    fun export(
        context: Context,
        state: LauncherUiState,
        mode: ExportLogMode,
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val file = ExportFileNamer.nextAvailableFile(exportsDir, "lukoa-session-log-$timestamp", "txt")
        val fullTavernRuntimeLog = RuntimeLogArchive.readTavernRuntime(context, state.tavernRuntimeLog)
        val fullAppLog = RuntimeLogArchive.readApp(context, state.appLog)
        file.writeText(
            SessionLogContentBuilder.build(
                state = state,
                mode = mode,
                tavernRuntimeLog = fullTavernRuntimeLog,
                appLog = fullAppLog,
                exportTime = LocalTime.now().withNano(0).toString(),
            ),
            Charsets.UTF_8,
        )
        return file
    }
}
