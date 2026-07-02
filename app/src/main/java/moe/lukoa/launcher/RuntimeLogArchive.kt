package moe.lukoa.launcher

import android.content.Context
import java.io.File

object RuntimeLogArchive {
    private const val DIR_NAME = "runtime-logs"
    private const val TERMUX_FILE = "termux.log"
    private const val APP_FILE = "app.log"

    fun ensureSeeded(context: Context, state: LauncherUiState) {
        seedIfEmpty(termuxFile(context), state.termuxLog)
        seedIfEmpty(appFile(context), state.appLog)
    }

    fun appendApp(context: Context, text: String) {
        append(context, APP_FILE, "App", text)
    }

    fun appendTermux(context: Context, text: String) {
        append(context, TERMUX_FILE, "Termux", text)
    }

    fun readTermux(context: Context, fallback: String = ""): String {
        return read(termuxFile(context), fallback)
    }

    fun readApp(context: Context, fallback: String = ""): String {
        return read(appFile(context), fallback)
    }

    fun clear(context: Context, mode: ExportLogMode) {
        if (mode.includeTermux) {
            termuxFile(context).writeText("", Charsets.UTF_8)
        }
        if (mode.includeApp) {
            appFile(context).writeText("", Charsets.UTF_8)
        }
    }

    private fun append(context: Context, fileName: String, source: String, text: String) {
        if (text.isBlank()) return
        val file = logDir(context).resolve(fileName)
        val entry = logEntry(source, text)
        val prefix = if (file.exists() && file.length() > 0L) "\n\n" else ""
        file.appendText(prefix + entry, Charsets.UTF_8)
    }

    private fun seedIfEmpty(file: File, value: String) {
        if (file.exists() && file.length() > 0L) return
        if (value.isBlank() || value.startsWith("暂无 ")) return
        file.writeText(value, Charsets.UTF_8)
    }

    private fun read(file: File, fallback: String): String {
        if (!file.exists() || file.length() == 0L) return fallback
        return runCatching { file.readText(Charsets.UTF_8) }.getOrDefault(fallback)
    }

    private fun termuxFile(context: Context): File = logDir(context).resolve(TERMUX_FILE)

    private fun appFile(context: Context): File = logDir(context).resolve(APP_FILE)

    private fun logDir(context: Context): File {
        return File(context.filesDir, DIR_NAME).apply { mkdirs() }
    }
}
