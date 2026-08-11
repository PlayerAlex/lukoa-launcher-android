package moe.lukoa.launcher

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

object BackupArchivePngCharacterCardInspector {
    private val pngSignature = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
    )
    private const val MAX_CHARACTER_TEXT_CHUNK_BYTES = 16 * 1024 * 1024

    fun inspect(input: InputStream, declaredSize: Long): BackupArchiveJsonInspection {
        if (declaredSize < pngSignature.size + 12L) return BackupArchiveJsonInspection()
        return runCatching {
            val signature = ByteArray(pngSignature.size)
            if (!input.readExactly(signature) || !signature.contentEquals(pngSignature)) {
                return@runCatching BackupArchiveJsonInspection()
            }

            var consumed = signature.size.toLong()
            var charaInspection: BackupArchiveJsonInspection? = null
            while (consumed + 12L <= declaredSize) {
                val lengthBytes = ByteArray(4)
                val typeBytes = ByteArray(4)
                if (!input.readExactly(lengthBytes) || !input.readExactly(typeBytes)) break
                consumed += 8L

                val dataLength = lengthBytes.toUnsignedInt32()
                if (dataLength > declaredSize - consumed - 4L) break
                val type = String(typeBytes, StandardCharsets.US_ASCII)
                val textData = if (
                    type == "tEXt" &&
                    dataLength in 1..MAX_CHARACTER_TEXT_CHUNK_BYTES.toLong()
                ) {
                    ByteArray(dataLength.toInt()).also { bytes ->
                        if (!input.readExactly(bytes)) return@runCatching charaInspection
                            ?: BackupArchiveJsonInspection()
                    }
                } else {
                    if (!input.skipExactly(dataLength)) break
                    null
                }
                consumed += dataLength
                if (!input.skipExactly(4L)) break
                consumed += 4L

                textData?.let { data -> runCatching { data.decodeCharacterInspection() }.getOrNull() }
                    ?.let { (keyword, inspection) ->
                        if (keyword == "ccv3") return@runCatching inspection
                        if (keyword == "chara" && charaInspection == null) {
                            charaInspection = inspection
                        }
                    }
                if (type == "IEND") break
            }
            charaInspection ?: BackupArchiveJsonInspection()
        }.getOrDefault(BackupArchiveJsonInspection())
    }

    private fun ByteArray.decodeCharacterInspection(): Pair<String, BackupArchiveJsonInspection>? {
        val separatorIndex = indexOf(0)
        if (separatorIndex <= 0 || separatorIndex >= lastIndex) return null
        val keyword = String(this, 0, separatorIndex, StandardCharsets.ISO_8859_1)
            .trim()
            .lowercase(Locale.ROOT)
        if (keyword != "chara" && keyword != "ccv3") return null
        val encodedJson = String(
            this,
            separatorIndex + 1,
            size - separatorIndex - 1,
            StandardCharsets.ISO_8859_1,
        ).trim()
        if (encodedJson.isBlank()) return null
        val jsonBytes = Base64.getDecoder().decode(encodedJson)
        return keyword to BackupArchiveJsonInspector.inspect(jsonBytes)
    }

    private fun ByteArray.toUnsignedInt32(): Long {
        return fold(0L) { value, byte -> (value shl 8) or (byte.toLong() and 0xFFL) }
    }

    private fun InputStream.readExactly(destination: ByteArray): Boolean {
        var offset = 0
        while (offset < destination.size) {
            val read = read(destination, offset, destination.size - offset)
            if (read <= 0) return false
            offset += read
        }
        return true
    }

    private fun InputStream.skipExactly(byteCount: Long): Boolean {
        var remaining = byteCount
        val discard = ByteArray(8 * 1024)
        while (remaining > 0L) {
            val skipped = skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
                continue
            }
            val read = read(discard, 0, minOf(discard.size.toLong(), remaining).toInt())
            if (read <= 0) return false
            remaining -= read
        }
        return true
    }
}
