package moe.lukoa.launcher

enum class TavernDoctorLevel {
    Healthy,
    Warning,
    Failed,
    Unknown,
}

data class TavernDoctorReport(
    val tavernDir: String = "",
    val port: Int = 8000,
    val termuxRepoLabel: String = "未读取",
    val termuxRepoUri: String = "",
    val gitAvailable: Boolean? = null,
    val nodeAvailable: Boolean? = null,
    val npmAvailable: Boolean? = null,
    val curlAvailable: Boolean? = null,
    val tavernDirExists: Boolean? = null,
    val packageJsonExists: Boolean? = null,
    val startEntryExists: Boolean? = null,
    val gitRepo: Boolean? = null,
    val pidFilePresent: Boolean? = null,
    val processDetected: Boolean? = null,
    val httpOk: Boolean? = null,
    val portListening: Boolean? = null,
    val portConflict: Boolean? = null,
    val summaryLevel: TavernDoctorLevel = TavernDoctorLevel.Unknown,
    val summaryMessage: String = "",
    val candidateDirectories: List<String> = emptyList(),
) {
    val hasData: Boolean
        get() = summaryMessage.isNotBlank() || tavernDir.isNotBlank()
}

object TavernDoctorParser {
    fun parse(output: String): TavernDoctorReport? {
        if (!output.contains("==== Lukoa doctor ====")) return null
        return TavernDoctorReport(
            tavernDir = output.lineValue("doctor.tavernDir").orEmpty(),
            port = output.lineValue("doctor.port")?.toIntOrNull() ?: 8000,
            termuxRepoLabel = output.lineValue("doctor.termuxRepo.label")
                ?: output.lineValue("current.label")
                ?: "未读取",
            termuxRepoUri = output.lineValue("doctor.termuxRepo.uri")
                ?: output.lineValue("current.uri")
                ?: "",
            gitAvailable = output.lineBoolean("doctor.tool.git"),
            nodeAvailable = output.lineBoolean("doctor.tool.node"),
            npmAvailable = output.lineBoolean("doctor.tool.npm"),
            curlAvailable = output.lineBoolean("doctor.tool.curl"),
            tavernDirExists = output.lineBoolean("doctor.tavernDir.exists"),
            packageJsonExists = output.lineBoolean("doctor.tavernDir.packageJson"),
            startEntryExists = output.lineBoolean("doctor.tavernDir.startEntry"),
            gitRepo = output.lineBoolean("doctor.tavernDir.gitRepo"),
            pidFilePresent = output.lineBoolean("doctor.process.pidFile"),
            processDetected = output.lineBoolean("doctor.process.detected"),
            httpOk = output.lineBoolean("doctor.process.httpOk"),
            portListening = output.lineBoolean("doctor.process.portListening"),
            portConflict = output.lineBoolean("doctor.process.portConflict"),
            summaryLevel = parseLevel(output.lineValue("doctor.summary.level")),
            summaryMessage = output.lineValue("doctor.summary.message").orEmpty(),
            candidateDirectories = output.lineValues("candidate."),
        )
    }

    private fun parseLevel(value: String?): TavernDoctorLevel {
        return when (value?.trim()?.lowercase()) {
            "healthy" -> TavernDoctorLevel.Healthy
            "warning" -> TavernDoctorLevel.Warning
            "failed" -> TavernDoctorLevel.Failed
            else -> TavernDoctorLevel.Unknown
        }
    }

    private fun String.lineValue(key: String): String? {
        return lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun String.lineBoolean(key: String): Boolean? {
        return when (lineValue(key)) {
            "1", "true", "ok" -> true
            "0", "false", "missing" -> false
            else -> null
        }
    }

    private fun String.lineValues(prefix: String): List<String> {
        return lineSequence()
            .map { it.trim() }
            .filter { it.startsWith(prefix) }
            .mapNotNull { line -> line.substringAfter("=", "").trim().takeIf { it.isNotBlank() } }
            .distinct()
            .toList()
    }
}
