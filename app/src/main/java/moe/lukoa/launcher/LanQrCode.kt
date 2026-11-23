package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets

data class QrMatrix(
    val version: Int,
    val size: Int,
    val mask: Int,
    val modules: Array<BooleanArray>,
) {
    fun isDark(row: Int, col: Int): Boolean = modules[row][col]
}

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, Error Correction Level L, Versions 1..6).
 * Supports up to 134 UTF-8 bytes — plenty for any LAN URL such as `http://192.168.1.100:8000`.
 */
object LanQrCodeEncoder {
    private data class VersionSpec(
        val version: Int,
        val totalDataBytes: Int,
        val ecPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray,
    )

    private val VERSION_SPECS = listOf(
        VersionSpec(version = 1, totalDataBytes = 19, ecPerBlock = 7, numBlocks = 1, alignmentCenters = intArrayOf()),
        VersionSpec(version = 2, totalDataBytes = 34, ecPerBlock = 10, numBlocks = 1, alignmentCenters = intArrayOf(6, 18)),
        VersionSpec(version = 3, totalDataBytes = 55, ecPerBlock = 15, numBlocks = 1, alignmentCenters = intArrayOf(6, 22)),
        VersionSpec(version = 4, totalDataBytes = 80, ecPerBlock = 20, numBlocks = 1, alignmentCenters = intArrayOf(6, 26)),
        VersionSpec(version = 5, totalDataBytes = 108, ecPerBlock = 26, numBlocks = 1, alignmentCenters = intArrayOf(6, 30)),
        VersionSpec(version = 6, totalDataBytes = 136, ecPerBlock = 18, numBlocks = 2, alignmentCenters = intArrayOf(6, 34)),
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var value = 1
        for (i in 0 until 255) {
            GF_EXP[i] = value
            GF_LOG[value] = i
            value = value shl 1
            if (value >= 256) value = value xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    fun encode(text: String): QrMatrix? {
        val data = text.toByteArray(StandardCharsets.UTF_8)
        if (data.isEmpty()) return null
        val spec = VERSION_SPECS.firstOrNull { (it.totalDataBytes * 8 - 12) / 8 >= data.size } ?: return null
        val codewords = buildCodewords(data, spec)
        val size = 17 + 4 * spec.version

        val baseModules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }
        drawFunctionPatterns(spec, size, baseModules, isFunction)
        placeDataBits(size, codewords, baseModules, isFunction)

        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestModules: Array<BooleanArray>? = null
        for (mask in 0..7) {
            val candidate = Array(size) { r -> baseModules[r].copyOf() }
            applyMask(size, candidate, isFunction, mask)
            drawFormatBits(size, candidate, mask)
            val penalty = computePenalty(size, candidate)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestModules = candidate
            }
        }
        return QrMatrix(
            version = spec.version,
            size = size,
            mask = bestMask,
            modules = bestModules ?: baseModules,
        )
    }

    internal fun formatInfoBits(mask: Int): Int {
        // EC Level L indicator is 01 (binary) followed by 3-bit mask.
        val data5 = (0b01 shl 3) or (mask and 0b111)
        var rem = data5
        repeat(10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        return ((data5 shl 10) or rem) xor 0x5412
    }

    private fun buildCodewords(data: ByteArray, spec: VersionSpec): ByteArray {
        val bits = ArrayList<Boolean>(spec.totalDataBytes * 8)
        fun appendBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits += ((value ushr i) and 1) != 0
            }
        }
        // Byte mode = 0100, count = 8 bits for versions 1..9
        appendBits(0b0100, 4)
        appendBits(data.size, 8)
        data.forEach { b -> appendBits(b.toInt() and 0xFF, 8) }

        val dataCapacityBits = spec.totalDataBytes * 8
        val terminator = minOf(4, dataCapacityBits - bits.size)
        repeat(terminator) { bits += false }
        while (bits.size % 8 != 0) bits += false

        var padIndex = 0
        while (bits.size < dataCapacityBits) {
            appendBits(if (padIndex % 2 == 0) 0xEC else 0x11, 8)
            padIndex += 1
        }

        val dataBytes = ByteArray(spec.totalDataBytes) { idx ->
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or if (bits[idx * 8 + b]) 1 else 0
            }
            byteVal.toByte()
        }

        val blockDataSize = spec.totalDataBytes / spec.numBlocks
        val dataBlocks = Array(spec.numBlocks) { b ->
            dataBytes.copyOfRange(b * blockDataSize, (b + 1) * blockDataSize)
        }
        val gen = rsGeneratorPoly(spec.ecPerBlock)
        val ecBlocks = Array(spec.numBlocks) { b ->
            rsRemainder(dataBlocks[b], gen)
        }

        val out = ByteArray(spec.totalDataBytes + spec.numBlocks * spec.ecPerBlock)
        var pos = 0
        for (i in 0 until blockDataSize) {
            for (b in 0 until spec.numBlocks) {
                out[pos++] = dataBlocks[b][i]
            }
        }
        for (i in 0 until spec.ecPerBlock) {
            for (b in 0 until spec.numBlocks) {
                out[pos++] = ecBlocks[b][i]
            }
        }
        return out
    }

    private fun gfMul(a: Int, b: Int): Int {
        return if (a == 0 || b == 0) 0 else GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun rsRemainder(data: ByteArray, gen: IntArray): ByteArray {
        val ecLen = gen.size - 1
        val rem = IntArray(ecLen)
        for (byte in data) {
            val factor = (byte.toInt() and 0xFF) xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecLen - 1)
            rem[ecLen - 1] = 0
            if (factor != 0) {
                for (j in 0 until ecLen) {
                    rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return ByteArray(ecLen) { rem[it].toByte() }
    }

    private fun drawFunctionPatterns(
        spec: VersionSpec,
        size: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
    ) {
        fun setFunc(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        // Timing patterns
        for (i in 0 until size) {
            setFunc(6, i, i % 2 == 0)
            setFunc(i, 6, i % 2 == 0)
        }

        // Finder patterns + 1-module white separator
        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val dist = maxOf(kotlin.math.abs(dr - 3), kotlin.math.abs(dc - 3))
                    val dark = dist != 2 && dist != 4
                    setFunc(r, c, dark)
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Alignment patterns (skip corners overlapping finder patterns)
        val centers = spec.alignmentCenters
        for (i in centers.indices) {
            for (j in centers.indices) {
                val isTopLeft = i == 0 && j == 0
                val isTopRight = i == 0 && j == centers.lastIndex
                val isBottomLeft = i == centers.lastIndex && j == 0
                if (isTopLeft || isTopRight || isBottomLeft) continue
                val cr = centers[i]
                val cc = centers[j]
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                        setFunc(cr + dr, cc + dc, dist != 1)
                    }
                }
            }
        }

        // Reserve format info areas & dark module
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in size - 8 until size) {
            setFunc(8, i, false)
            setFunc(i, 8, false)
        }
        setFunc(size - 8, 8, true) // Dark module
    }

    private fun placeDataBits(
        size: Int,
        codewords: ByteArray,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
    ) {
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var upward = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val row = if (upward) size - 1 - vert else vert
                for (dx in 0..1) {
                    val col = right - dx
                    if (!isFunction[row][col]) {
                        val dark = if (bitIndex < totalBits) {
                            val byteVal = codewords[bitIndex ushr 3].toInt() and 0xFF
                            ((byteVal ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[row][col] = dark
                        bitIndex += 1
                    }
                }
            }
            upward = !upward
            right -= 2
        }
    }

    private fun applyMask(
        size: Int,
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        mask: Int,
    ) {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (isFunction[r][c]) continue
                val invert = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> ((r / 2) + (c / 3)) % 2 == 0
                    5 -> ((r * c) % 2) + ((r * c) % 3) == 0
                    6 -> (((r * c) % 2) + ((r * c) % 3)) % 2 == 0
                    7 -> (((r + c) % 2) + ((r * c) % 3)) % 2 == 0
                    else -> false
                }
                if (invert) modules[r][c] = !modules[r][c]
            }
        }
    }

    private fun drawFormatBits(size: Int, modules: Array<BooleanArray>, mask: Int) {
        val bits = formatInfoBits(mask)
        fun bit(index: Int): Boolean = ((bits ushr index) and 1) != 0

        // Copy 1 (top-left)
        for (i in 0..5) modules[i][8] = bit(i)
        modules[7][8] = bit(6)
        modules[8][8] = bit(7)
        modules[8][7] = bit(8)
        for (i in 9..14) modules[8][14 - i] = bit(i)

        // Copy 2 (top-right + bottom-left)
        for (i in 0..7) modules[8][size - 1 - i] = bit(i)
        for (i in 8..14) modules[size - 15 + i][8] = bit(i)
        modules[size - 8][8] = true
    }

    private fun computePenalty(size: Int, modules: Array<BooleanArray>): Int {
        var penalty = 0
        // Rule 1: adjacent modules in row/col of same color
        for (r in 0 until size) {
            var runColor = modules[r][0]
            var runLen = 1
            for (c in 1 until size) {
                if (modules[r][c] == runColor) {
                    runLen += 1
                    if (runLen == 5) penalty += 3 else if (runLen > 5) penalty += 1
                } else {
                    runColor = modules[r][c]
                    runLen = 1
                }
            }
        }
        for (c in 0 until size) {
            var runColor = modules[0][c]
            var runLen = 1
            for (r in 1 until size) {
                if (modules[r][c] == runColor) {
                    runLen += 1
                    if (runLen == 5) penalty += 3 else if (runLen > 5) penalty += 1
                } else {
                    runColor = modules[r][c]
                    runLen = 1
                }
            }
        }
        // Rule 2: 2x2 blocks of same color
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = modules[r][c]
                if (modules[r][c + 1] == color && modules[r + 1][c] == color && modules[r + 1][c + 1] == color) {
                    penalty += 3
                }
            }
        }
        // Rule 3: finder-like patterns in rows/cols
        for (r in 0 until size) {
            for (c in 0..size - 11) {
                if (matchesFinderLike(
                        modules[r][c], modules[r][c + 1], modules[r][c + 2], modules[r][c + 3],
                        modules[r][c + 4], modules[r][c + 5], modules[r][c + 6], modules[r][c + 7],
                        modules[r][c + 8], modules[r][c + 9], modules[r][c + 10],
                    )
                ) {
                    penalty += 40
                }
            }
        }
        for (c in 0 until size) {
            for (r in 0..size - 11) {
                if (matchesFinderLike(
                        modules[r][c], modules[r + 1][c], modules[r + 2][c], modules[r + 3][c],
                        modules[r + 4][c], modules[r + 5][c], modules[r + 6][c], modules[r + 7][c],
                        modules[r + 8][c], modules[r + 9][c], modules[r + 10][c],
                    )
                ) {
                    penalty += 40
                }
            }
        }
        // Rule 4: dark module balance
        var darkCount = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (modules[r][c]) darkCount += 1
            }
        }
        val total = size * size
        val k = kotlin.math.abs(darkCount * 20 - total * 10) / total
        penalty += k * 10
        return penalty
    }

    private fun matchesFinderLike(
        b0: Boolean, b1: Boolean, b2: Boolean, b3: Boolean, b4: Boolean, b5: Boolean,
        b6: Boolean, b7: Boolean, b8: Boolean, b9: Boolean, b10: Boolean,
    ): Boolean {
        val left = b0 && !b1 && b2 && b3 && b4 && !b5 && b6 && !b7 && !b8 && !b9 && !b10
        val right = !b0 && !b1 && !b2 && !b3 && b4 && !b5 && b6 && b7 && b8 && !b9 && b10
        return left || right
    }
}

@Composable
fun LanQrCodeCard(
    url: String,
    modifier: Modifier = Modifier,
    qrSize: Dp = 168.dp,
) {
    val matrix = remember(url) { LanQrCodeEncoder.encode(url) } ?: return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lan-qr-code-card"),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LukoaColors.Border),
            ) {
                Canvas(
                    modifier = Modifier
                        .padding(10.dp)
                        .size(qrSize),
                ) {
                    val quietZone = 2
                    val totalModules = matrix.size + quietZone * 2
                    val cell = size.width / totalModules
                    drawRect(color = Color.White, size = size)
                    for (r in 0 until matrix.size) {
                        for (c in 0 until matrix.size) {
                            if (matrix.isDark(r, c)) {
                                drawRect(
                                    color = Color(0xFF09090B),
                                    topLeft = Offset((c + quietZone) * cell, (r + quietZone) * cell),
                                    size = Size(cell + 0.5f, cell + 0.5f),
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "用电脑或其他设备扫一扫就能打开酒馆",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "$url（连同一个 Wi-Fi，改完配置重启酒馆后生效）",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
