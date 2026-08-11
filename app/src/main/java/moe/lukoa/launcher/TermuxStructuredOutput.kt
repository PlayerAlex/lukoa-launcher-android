package moe.lukoa.launcher

data class TermuxStructuredOutput(
    val versionInfo: TavernVersionInfo? = null,
    val officialVersions: TavernOfficialVersions? = null,
    val termuxRepoStatus: TermuxRepoStatus? = null,
    val uploadLimitStatus: TavernUploadLimitStatus? = null,
    val users: List<TavernUserRecord>? = null,
    val extensions: TavernExtensionSnapshot? = null,
)

object TermuxStructuredOutputParser {
    fun parse(
        output: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): TermuxStructuredOutput {
        val versionInfo = TavernVersionParser.parse(output)
            .takeIf { it.hasData || it.notInstalled }
        val officialVersions = TavernOfficialVersionParser.parse(output)
            .takeIf { it.hasData }
        return TermuxStructuredOutput(
            versionInfo = versionInfo,
            officialVersions = officialVersions,
            termuxRepoStatus = TermuxRepoStatusParser.parse(output, nowMillis),
            uploadLimitStatus = TavernUploadLimitStatusParser.parse(output),
            users = TavernUserOutputParser.parse(output),
            extensions = TavernExtensionOutputParser.parse(output),
        )
    }
}
