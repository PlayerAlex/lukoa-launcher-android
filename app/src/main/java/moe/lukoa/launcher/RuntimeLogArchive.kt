package moe.lukoa.launcher

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

object RuntimeLogArchive {
    private const val MAX_EXPORT_LOG_CHARS = 120_000
    private const val DIR_NAME = "runtime-logs"
    private const val TERMUX_COMMAND_FILE = "termux-command.log"
    private const val TAVERN_RUNTIME_FILE = "tavern-runtime.log"
    private const val APP_FILE = "app.log"
    private const val LEGACY_TERMUX_FILE = "termux.log"
    private const val MAX_ARCHIVE_BYTES = 4L * 1024 * 1024
    private const val KEEP_AFTER_TRIM_BYTES = 1024 * 1024

    // Appends arrive from the UI thread on every log poll. One background thread keeps them off the
    // main thread and in order; reads go through the same thread so an export sees every append.
    private val io = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lukoa-log-archive").apply { isDaemon = true }
    }

    fun ensureSeeded(context: Context, state: LauncherUiState) {
        val appContext = context.applicationContext
        io.execute {
            runCatching {
                seedIfEmpty(termuxCommandFile(appContext), state.termuxLog)
                seedIfEmpty(tavernRuntimeFile(appContext), state.tavernRuntimeLog)
                seedIfEmpty(appFile(appContext), state.appLog)
            }
        }
    }

    fun appendApp(context: Context, text: String) {
        if (text.isBlank()) return
        val appContext = context.applicationContext
        val entry = logEntry("App", text)
        io.execute { appendEntry(appContext, APP_FILE, entry) }
    }

    fun appendTermuxCommand(context: Context, text: String) {
        appendRaw(context, TERMUX_COMMAND_FILE, text)
    }

    fun appendTavernRuntime(context: Context, text: String) {
        appendRaw(context, TAVERN_RUNTIME_FILE, text)
    }

    fun readTermuxCommand(context: Context, fallback: String = ""): String {
        val appContext = context.applicationContext
        return onIo(fallback) { read(termuxCommandFile(appContext), fallback, legacyFallbackFile = legacyTermuxFile(appContext)) }
    }

    fun readTavernRuntime(context: Context, fallback: String = ""): String {
        val appContext = context.applicationContext
        return onIo(fallback) { read(tavernRuntimeFile(appContext), fallback) }
    }

    fun readApp(context: Context, fallback: String = ""): String {
        val appContext = context.applicationContext
        return onIo(fallback) { read(appFile(appContext), fallback) }
    }

    private fun appendRaw(context: Context, fileName: String, text: String) {
        val entry = text.trim()
        if (entry.isBlank()) return
        val appContext = context.applicationContext
        io.execute { appendEntry(appContext, fileName, entry) }
    }

    private fun appendEntry(context: Context, fileName: String, entry: String) {
        // The archive used to grow without bound while the tavern streamed full prompts, and a
        // full disk turned every poll into a crash.
        runCatching {
            val file = logDir(context).resolve(fileName)
            trimIfLarge(file)
            val prefix = if (file.exists() && file.length() > 0L) "\n\n" else ""
            file.appendText(prefix + entry, Charsets.UTF_8)
        }
    }

    private fun <T> onIo(fallback: T, block: () -> T): T {
        return runCatching { io.submit(block).get() }.getOrDefault(fallback)
    }

    private fun trimIfLarge(file: File) {
        if (!file.exists() || file.length() <= MAX_ARCHIVE_BYTES) return
        val keep = ByteArray(KEEP_AFTER_TRIM_BYTES)
        java.io.RandomAccessFile(file, "r").use { input ->
            input.seek(file.length() - KEEP_AFTER_TRIM_BYTES)
            input.readFully(keep)
        }
        val firstLineBreak = keep.indexOf('\n'.code.toByte()).coerceAtLeast(0)
        file.writeBytes(keep.copyOfRange(firstLineBreak + 1, keep.size))
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
