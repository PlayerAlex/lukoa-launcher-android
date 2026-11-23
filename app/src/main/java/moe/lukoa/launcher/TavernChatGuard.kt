package moe.lukoa.launcher

data class TavernChatGuardConfig(
    val enabled: Boolean = true,
    val wakeLockEnabled: Boolean = true,
    val minKilobytes: Int = TavernChatGuardPolicy.DEFAULT_MIN_KB,
    val intervalSeconds: Int = TavernChatGuardPolicy.DEFAULT_INTERVAL_SECONDS,
)

data class TavernChatGuardRescueRecord(
    val timestamp: String,
    val originalChatPath: String,
    val rescuedChatPath: String,
    val exists: Boolean,
    val sizeKilobytes: Long,
) {
    val chatDisplayName: String
        get() = originalChatPath.substringAfterLast('/').removeSuffix(".jsonl").ifBlank { originalChatPath }

    val rescuedFileName: String
        get() = rescuedChatPath.substringAfterLast('/').ifBlank { rescuedChatPath }
}

data class TavernChatGuardSnapshot(
    val config: TavernChatGuardConfig = TavernChatGuardConfig(),
    val running: Boolean = false,
    val pid: Int? = null,
    val linkedChatCount: Int = 0,
    val rescueCount: Int = 0,
    val rescues: List<TavernChatGuardRescueRecord> = emptyList(),
)

data class TavernChatGuardState(
    val snapshot: TavernChatGuardSnapshot? = null,
    val knownRescueCount: Int = 0,
    val loading: Boolean = false,
    val message: String = "尚未读取当前实例的聊天守护状态。",
) {
    val effectiveRescueCount: Int
        get() = snapshot?.rescueCount ?: knownRescueCount
}

object TavernChatGuardPolicy {
    const val MIN_KB = 32
    const val MAX_KB = 8_192
    const val DEFAULT_MIN_KB = 256
    const val MIN_INTERVAL_SECONDS = 5
    const val MAX_INTERVAL_SECONDS = 120
    const val DEFAULT_INTERVAL_SECONDS = 10

    val MIN_KB_PRESETS = listOf(64, 128, 256, 512, 1024)
    val INTERVAL_PRESETS = listOf(5, 10, 20, 30)

    fun isValidMinKb(value: Int?): Boolean = value != null && value in MIN_KB..MAX_KB

    fun isValidIntervalSeconds(value: Int?): Boolean =
        value != null && value in MIN_INTERVAL_SECONDS..MAX_INTERVAL_SECONDS

    fun validate(config: TavernChatGuardConfig): String? = when {
        !isValidMinKb(config.minKilobytes) -> "最小救回阈值需在 $MIN_KB–$MAX_KB KB 之间。"
        !isValidIntervalSeconds(config.intervalSeconds) ->
            "扫描间隔需在 $MIN_INTERVAL_SECONDS–$MAX_INTERVAL_SECONDS 秒之间。"
        else -> null
    }
}

object TavernChatGuardCommandCodec {
    fun encode(config: TavernChatGuardConfig): String {
        val enabled = if (config.enabled) "1" else "0"
        val wakeLock = if (config.wakeLockEnabled) "1" else "0"
        val minKb = config.minKilobytes.coerceIn(TavernChatGuardPolicy.MIN_KB, TavernChatGuardPolicy.MAX_KB)
        val interval = config.intervalSeconds.coerceIn(
            TavernChatGuardPolicy.MIN_INTERVAL_SECONDS,
            TavernChatGuardPolicy.MAX_INTERVAL_SECONDS,
        )
        return "$enabled|$wakeLock|$minKb|$interval"
    }

    fun decode(value: String?): TavernChatGuardConfig? {
        val parts = value.orEmpty().trim().split('|')
        if (parts.size != 4) return null
        val enabled = when (parts[0]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        val wakeLock = when (parts[1]) {
            "1" -> true
            "0" -> false
            else -> return null
        }
        val minKb = parts[2].toIntOrNull()?.takeIf(TavernChatGuardPolicy::isValidMinKb) ?: return null
        val interval = parts[3].toIntOrNull()?.takeIf(TavernChatGuardPolicy::isValidIntervalSeconds) ?: return null
        return TavernChatGuardConfig(
            enabled = enabled,
            wakeLockEnabled = wakeLock,
            minKilobytes = minKb,
            intervalSeconds = interval,
        )
    }
}

object TavernChatGuardOutputParser {
    private const val HEADER = "==== Lukoa chat guard ===="
    private const val FOOTER = "==== end Lukoa chat guard ===="

    fun parse(output: String): TavernChatGuardSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var enabled = true
        var wakeLock = true
        var minKb = TavernChatGuardPolicy.DEFAULT_MIN_KB
        var intervalSec = TavernChatGuardPolicy.DEFAULT_INTERVAL_SECONDS
        var running = false
        var pid: Int? = null
        var linkedCount = 0
        var rescueCount = 0
        val rescues = mutableListOf<TavernChatGuardRescueRecord>()

        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("chatGuard.enabled=") -> enabled = line.substringAfter('=') != "0"
                line.startsWith("chatGuard.wakeLock=") -> wakeLock = line.substringAfter('=') != "0"
                line.startsWith("chatGuard.minKb=") -> {
                    line.substringAfter('=').toIntOrNull()
                        ?.takeIf(TavernChatGuardPolicy::isValidMinKb)
                        ?.let { minKb = it }
                }
                line.startsWith("chatGuard.intervalSec=") -> {
                    line.substringAfter('=').toIntOrNull()
                        ?.takeIf(TavernChatGuardPolicy::isValidIntervalSeconds)
                        ?.let { intervalSec = it }
                }
                line.startsWith("chatGuard.running=") -> running = line.substringAfter('=') == "1"
                line.startsWith("chatGuard.pid=") -> pid = line.substringAfter('=').toIntOrNull()?.takeIf { it > 0 }
                line.startsWith("chatGuard.linkedCount=") -> {
                    linkedCount = line.substringAfter('=').toIntOrNull()?.coerceAtLeast(0) ?: 0
                }
                line.startsWith("chatGuard.rescueCount=") -> {
                    rescueCount = line.substringAfter('=').toIntOrNull()?.coerceAtLeast(0) ?: 0
                }
                line.startsWith("chatGuard.rescue=") -> {
                    val fields = line.substringAfter('=').split('\t')
                    if (fields.size >= 3) {
                        rescues += TavernChatGuardRescueRecord(
                            timestamp = fields[0].trim(),
                            originalChatPath = fields[1].trim(),
                            rescuedChatPath = fields[2].trim(),
                            exists = fields.getOrNull(3)?.trim() != "false",
                            sizeKilobytes = fields.getOrNull(4)?.trim()?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                        )
                    }
                }
            }
        }

        return TavernChatGuardSnapshot(
            config = TavernChatGuardConfig(
                enabled = enabled,
                wakeLockEnabled = wakeLock,
                minKilobytes = minKb,
                intervalSeconds = intervalSec,
            ),
            running = running,
            pid = pid,
            linkedChatCount = linkedCount,
            rescueCount = maxOf(rescueCount, rescues.size),
            rescues = rescues.asReversed(),
        )
    }

    fun parseRescueCountHint(output: String): Int? {
        return output.lineSequence()
            .map { it.trim() }
            .lastOrNull { it.startsWith("chatGuard.rescueCount=") }
            ?.substringAfter('=')
            ?.toIntOrNull()
            ?.coerceAtLeast(0)
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }
}
