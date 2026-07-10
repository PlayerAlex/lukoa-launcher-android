package moe.lukoa.launcher

import android.content.Context
import java.io.File

object RuntimeLogArchive {
    private const val MAX_EXPORT_LOG_CHARS = 120_000
    private const val DIR_NAME = "runtime-logs"
    private const val TERMUX_COMMAND_FILE = "termux-command.log"
    private const val TAVERN_RUNTIME_FILE = "tavern-runtime.log"
    private const val APP_FILE = "app.log"
    private const val LEGACY_TERMUX_FILE = "termux.log"

    fun ensureSeeded(context: Context, state: LauncherUiState) {
        seedIfEmpty(termuxCommandFile(context), state.termuxLog)
        seedIfEmpty(tavernRuntimeFile(context), state.tavernRuntimeLog)
        seedIfEmpty(appFile(context), state.appLog)
    }

    fun appendApp(context: Context, text: String) {
        appendStructured(context, APP_FILE, "App", text)
    }

    fun appendTermuxCommand(context: Context, text: String) {
        appendRaw(context, TERMUX_COMMAND_FILE, text)
    }

    fun appendTavernRuntime(context: Context, text: String) {
        appendRaw(context, TAVERN_RUNTIME_FILE, text)
    }

    fun readTermuxCommand(context: Context, fallback: String = ""): String {
        return read(termuxCommandFile(context), fallback, legacyFallbackFile = legacyTermuxFile(context))
    }

    fun readTavernRuntime(context: Context, fallback: String = ""): String {
        return read(tavernRuntimeFile(context), fallback)
    }

    fun readApp(context: Context, fallback: String = ""): String {
        return read(appFile(context), fallback)
    }

    fun clear(context: Context, mode: ExportLogMode) {
        if (mode.includeTermux) {
            termuxCommandFile(context).writeText("", Charsets.UTF_8)
            tavernRuntimeFile(context).writeText("", Charsets.UTF_8)
            legacyTermuxFile(context).takeIf { it.exists() }?.writeText("", Charsets.UTF_8)
        }
        if (mode.includeApp) {
            appFile(context).writeText("", Charsets.UTF_8)
        }
    }

    private fun appendStructured(context: Context, fileName: String, source: String, text: String) {
        if (text.isBlank()) return
        val file = logDir(context).resolve(fileName)
        val entry = logEntry(source, text)
        val prefix = if (file.exists() && file.length() > 0L) "\n\n" else ""
        file.appendText(prefix + entry, Charsets.UTF_8)
    }

    private fun appendRaw(context: Context, fileName: String, text: String) {
        val entry = text.trim()
        if (entry.isBlank()) return
        val file = logDir(context).resolve(fileName)
        val prefix = if (file.exists() && file.length() > 0L) "\n\n" else ""
        file.appendText(prefix + entry, Charsets.UTF_8)
    }

    private fun seedIfEmpty(file: File, value: String) {
        if (file.exists() && file.length() > 0L) return
        if (value.isBlank() || value.startsWith("暂无 ")) return
        file.writeText(value, Charsets.UTF_8)
    }

    private fun read(
        file: File,
        fallback: String,
        legacyFallbackFile: File? = null,
    ): String {
        val source = when {
            file.exists() && file.length() > 0L -> file
            legacyFallbackFile != null && legacyFallbackFile.exists() && legacyFallbackFile.length() > 0L -> legacyFallbackFile
            else -> null
        } ?: return fallback
        return runCatching {
            BoundedLogFile.readTail(source, MAX_EXPORT_LOG_CHARS).ifBlank { fallback }
        }.getOrDefault(fallback)
    }

    private fun termuxCommandFile(context: Context): File = logDir(context).resolve(TERMUX_COMMAND_FILE)

    private fun tavernRuntimeFile(context: Context): File = logDir(context).resolve(TAVERN_RUNTIME_FILE)

    private fun appFile(context: Context): File = logDir(context).resolve(APP_FILE)

    private fun legacyTermuxFile(context: Context): File = logDir(context).resolve(LEGACY_TERMUX_FILE)

    private fun logDir(context: Context): File {
        return File(context.filesDir, DIR_NAME).apply { mkdirs() }
    }
}
