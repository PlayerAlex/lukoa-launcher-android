package moe.lukoa.launcher

data class TermuxStructuredOutput(
    val versionInfo: TavernVersionInfo? = null,
    val officialVersions: TavernOfficialVersions? = null,
    val termuxRepoStatus: TermuxRepoStatus? = null,
    val uploadLimitStatus: TavernUploadLimitStatus? = null,
    val users: List<TavernUserRecord>? = null,
    val extensions: TavernExtensionSnapshot? = null,
    val chatGuardSnapshot: TavernChatGuardSnapshot? = null,
    val chatGuardRescueCountHint: Int? = null,
    val configSnapshot: TavernConfigSnapshot? = null,
    val storageSnapshot: TavernStorageSnapshot? = null,
    val serverPluginsSnapshot: TavernServerPluginSnapshot? = null,
    val gitStashSnapshot: Pair<Boolean, List<TavernGitStashEntry>>? = null,
    val quickImportPaths: TavernQuickImportPaths? = null,
    val quickImportedFile: String? = null,
    val quickImportedFiles: List<String> = emptyList(),
    val nodeMemoryStatus: TavernNodeMemoryStatus? = null,
    val proxyTestResult: TavernProxyTestResult? = null,
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
        val chatGuardSnapshot = TavernChatGuardOutputParser.parse(output)
        val importedFiles = TavernQuickImportOutputParser.parseImportedTargetFiles(output)
        return TermuxStructuredOutput(
            versionInfo = versionInfo,
            officialVersions = officialVersions,
            termuxRepoStatus = TermuxRepoStatusParser.parse(output, nowMillis),
            uploadLimitStatus = TavernUploadLimitStatusParser.parse(output),
            users = TavernUserOutputParser.parse(output),
            extensions = TavernExtensionOutputParser.parse(output),
            chatGuardSnapshot = chatGuardSnapshot,
            chatGuardRescueCountHint = chatGuardSnapshot?.rescueCount
                ?: TavernChatGuardOutputParser.parseRescueCountHint(output),
            configSnapshot = TavernConfigOutputParser.parse(output),
            storageSnapshot = TavernStorageOutputParser.parse(output),
            serverPluginsSnapshot = TavernServerPluginOutputParser.parse(output),
            gitStashSnapshot = TavernGitStashOutputParser.parse(output),
            quickImportPaths = TavernQuickImportOutputParser.parsePaths(output),
            quickImportedFile = TavernQuickImportOutputParser.parseImportedTargetFile(output)
                ?: importedFiles.lastOrNull(),
            quickImportedFiles = importedFiles,
            nodeMemoryStatus = TavernNodeMemoryOutputParser.parse(output),
            proxyTestResult = TavernProxyTestOutputParser.parse(output),
        )
    }
}
