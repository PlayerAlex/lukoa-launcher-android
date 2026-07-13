package moe.lukoa.launcher

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

internal object BoundedLogFile {
    private const val UTF8_MAX_BYTES_PER_CHAR = 4L
    private const val TRUNCATED_MARKER = "... 前面历史日志过大，导出时已截断 ...\n"

    fun readTail(file: File, maxChars: Int): String {
        require(maxChars > 0)
        if (!file.exists() || file.length() <= 0L) return ""

        val fileLength = file.length()
        val maxBytes = maxChars.toLong() * UTF8_MAX_BYTES_PER_CHAR + UTF8_MAX_BYTES_PER_CHAR
        val bytesToRead = min(fileLength, maxBytes).toInt()
        val startOffset = fileLength - bytesToRead
        val bytes = ByteArray(bytesToRead)

        RandomAccessFile(file, "r").use { input ->
            input.seek(startOffset)
            input.readFully(bytes)
        }

        val utf8Offset = if (startOffset > 0L) firstUtf8CharacterOffset(bytes) else 0
        val decoded = String(bytes, utf8Offset, bytes.size - utf8Offset, Charsets.UTF_8)
        val truncated = startOffset > 0L || decoded.length > maxChars
        if (!truncated) return decoded

        if (maxChars <= TRUNCATED_MARKER.length) {
            return decoded.takeLast(maxChars)
        }
        return TRUNCATED_MARKER + decoded.takeLast(maxChars - TRUNCATED_MARKER.length)
    }

    private fun firstUtf8CharacterOffset(bytes: ByteArray): Int {
        var offset = 0
        while (offset < bytes.size && bytes[offset].toInt() and 0xC0 == 0x80) {
            offset += 1
        }
        return offset
    }
}
