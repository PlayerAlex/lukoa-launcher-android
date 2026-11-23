package moe.lukoa.launcher

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class MirrorProbeLevel {
    Healthy,
    Warning,
    Failed,
    Unknown,
}

data class MirrorProbeItemStatus(
    val level: MirrorProbeLevel = MirrorProbeLevel.Unknown,
    val message: String = "未检测",
    val latencyMs: Long? = null,
)

data class TavernMirrorProbeStatus(
    val configSignature: String = "",
    val checking: Boolean = false,
    val checkedAtMillis: Long = 0L,
    val repoStatus: MirrorProbeItemStatus = MirrorProbeItemStatus(),
    val versionStatus: MirrorProbeItemStatus = MirrorProbeItemStatus(),
    val npmStatus: MirrorProbeItemStatus = MirrorProbeItemStatus(),
) {
    val overallLevel: MirrorProbeLevel
        get() = when {
            checking -> MirrorProbeLevel.Unknown
            repoStatus.level == MirrorProbeLevel.Failed ||
                versionStatus.level == MirrorProbeLevel.Failed ||
                npmStatus.level == MirrorProbeLevel.Failed -> MirrorProbeLevel.Failed
            repoStatus.level == MirrorProbeLevel.Warning ||
                versionStatus.level == MirrorProbeLevel.Warning ||
                npmStatus.level == MirrorProbeLevel.Warning -> MirrorProbeLevel.Warning
            repoStatus.level == MirrorProbeLevel.Healthy &&
                versionStatus.level == MirrorProbeLevel.Healthy &&
                npmStatus.level == MirrorProbeLevel.Healthy -> MirrorProbeLevel.Healthy
            else -> MirrorProbeLevel.Unknown
        }

    val lastCheckedText: String
        get() = when {
            checking -> "检测中"
            checkedAtMillis <= 0L -> "未检测"
            else -> {
                val text = CHECKED_TIME_FORMATTER.format(
                    Instant.ofEpochMilli(checkedAtMillis).atZone(ZoneId.systemDefault()),
                )
                "上次检测 $text"
            }
        }

    fun matches(config: TavernMirrorConfig): Boolean {
        return configSignature == signatureOf(config)
    }

    companion object {
        private val CHECKED_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm")

        fun signatureOf(config: TavernMirrorConfig): String {
            return "${config.normalizedRepoUrl}|${config.normalizedNpmRegistry}"
        }

        fun unknown(config: TavernMirrorConfig): TavernMirrorProbeStatus {
            return TavernMirrorProbeStatus(
                configSignature = signatureOf(config),
                repoStatus = MirrorProbeItemStatus(message = "还没检测"),
                versionStatus = MirrorProbeItemStatus(message = "还没检测"),
                npmStatus = MirrorProbeItemStatus(message = "还没检测"),
            )
        }

        fun checking(config: TavernMirrorConfig): TavernMirrorProbeStatus {
            return unknown(config).copy(checking = true)
        }
    }
}

data class MirrorCandidateScore(
    val url: String,
    val isOfficial: Boolean,
    val level: MirrorProbeLevel,
    val latencyMs: Long?,
)

data class TavernMirrorAutoSelectResult(
    val foundAny: Boolean,
    val recommendedConfig: TavernMirrorConfig,
    val probeStatus: TavernMirrorProbeStatus,
    val summaryMessage: String,
)

object TavernMirrorSelectionPolicy {
    fun pickBest(
        candidates: List<MirrorCandidateScore>,
        fallbackUrl: String,
        officialGraceMs: Long = 350L,
    ): MirrorCandidateScore? {
        val healthy = candidates.filter { it.level == MirrorProbeLevel.Healthy }
        val pool = healthy.ifEmpty { candidates.filter { it.level == MirrorProbeLevel.Warning } }
        if (pool.isEmpty()) return candidates.firstOrNull { sameRepoUrl(it.url, fallbackUrl) }
        val fastest = pool.minByOrNull { it.latencyMs ?: Long.MAX_VALUE } ?: return null
        val official = pool.firstOrNull { it.isOfficial }
        if (
            official != null &&
            official.latencyMs != null &&
            fastest.latencyMs != null &&
            official.latencyMs <= fastest.latencyMs + officialGraceMs
        ) {
            return official
        }
        return fastest
    }
}

class TavernMirrorProbeManager(private val context: Context) {
    fun check(
        scope: CoroutineScope,
        config: TavernMirrorConfig,
        callback: (TavernMirrorProbeStatus) -> Unit,
    ) {
        scope.launch {
            val result = probeSuspending(config)
            callback(result)
        }
    }

    fun autoSelectBest(
        scope: CoroutineScope,
        currentConfig: TavernMirrorConfig,
        callback: (TavernMirrorAutoSelectResult) -> Unit,
    ) {
        scope.launch {
            val result = autoSelectBestSuspending(currentConfig)
            callback(result)
        }
    }

    private suspend fun probeSuspending(config: TavernMirrorConfig): TavernMirrorProbeStatus = coroutineScope {
        val client = LukoaHttpClient(context)
        val gitDeferred = async(Dispatchers.IO) { probeGitAndVersions(client, config.normalizedRepoUrl) }
        val npmDeferred = async(Dispatchers.IO) { probeNpm(client, config.normalizedNpmRegistry) }
        val (repoStatus, versionStatus) = gitDeferred.await()
        val npmStatus = npmDeferred.await()
        TavernMirrorProbeStatus(
            configSignature = TavernMirrorProbeStatus.signatureOf(config),
            checking = false,
            checkedAtMillis = System.currentTimeMillis(),
            repoStatus = repoStatus,
            versionStatus = versionStatus,
            npmStatus = npmStatus,
        )
    }

    private suspend fun autoSelectBestSuspending(
        currentConfig: TavernMirrorConfig,
    ): TavernMirrorAutoSelectResult = coroutineScope {
        val client = LukoaHttpClient(context)
        val gitUrls = listOf(
            TavernMirrorDefaults.OFFICIAL_REPO,
            TavernMirrorDefaults.GITHUB_PROXY_REPO,
            TavernMirrorDefaults.GITHUB_PROXY_ALT_REPO,
        )
        val npmUrls = listOf(
            TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
            TavernMirrorDefaults.NPMMIRROR_REGISTRY,
        )

        val gitEvaluations = gitUrls.map { url ->
            async(Dispatchers.IO) {
                val (repoStatus, versionStatus) = probeGitAndVersions(client, url)
                val combinedLevel = when {
                    repoStatus.level == MirrorProbeLevel.Healthy && versionStatus.level == MirrorProbeLevel.Healthy ->
                        MirrorProbeLevel.Healthy
                    repoStatus.level == MirrorProbeLevel.Failed || versionStatus.level == MirrorProbeLevel.Failed ->
                        MirrorProbeLevel.Failed
                    else -> MirrorProbeLevel.Warning
                }
                Triple(
                    MirrorCandidateScore(
                        url = url,
                        isOfficial = url == TavernMirrorDefaults.OFFICIAL_REPO,
                        level = combinedLevel,
                        latencyMs = repoStatus.latencyMs,
                    ),
                    repoStatus,
                    versionStatus,
                )
            }
        }
        val npmEvaluations = npmUrls.map { url ->
            async(Dispatchers.IO) {
                val status = probeNpm(client, url)
                MirrorCandidateScore(
                    url = url,
                    isOfficial = url == TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
                    level = status.level,
                    latencyMs = status.latencyMs,
                ) to status
            }
        }

        val gitResults = gitEvaluations.awaitAll()
        val npmResults = npmEvaluations.awaitAll()

        val bestGitScore = TavernMirrorSelectionPolicy.pickBest(
            candidates = gitResults.map { it.first },
            fallbackUrl = currentConfig.normalizedRepoUrl,
            officialGraceMs = 450L,
        )
        val bestNpmScore = TavernMirrorSelectionPolicy.pickBest(
            candidates = npmResults.map { it.first },
            fallbackUrl = currentConfig.normalizedNpmRegistry,
            officialGraceMs = 250L,
        )

        val bestGitTuple = bestGitScore?.let { chosen -> gitResults.firstOrNull { it.first.url == chosen.url } }
        val bestNpmPair = bestNpmScore?.let { chosen -> npmResults.firstOrNull { it.first.url == chosen.url } }

        val gitReachable = bestGitScore != null && bestGitScore.level != MirrorProbeLevel.Failed
        val npmReachable = bestNpmScore != null && bestNpmScore.level != MirrorProbeLevel.Failed

        val nextConfig = TavernMirrorConfig(
            repoUrl = if (gitReachable) bestGitScore!!.url else currentConfig.normalizedRepoUrl,
            npmRegistry = if (npmReachable) bestNpmScore!!.url else currentConfig.normalizedNpmRegistry,
        )

        val finalProbeStatus = TavernMirrorProbeStatus(
            configSignature = TavernMirrorProbeStatus.signatureOf(nextConfig),
            checking = false,
            checkedAtMillis = System.currentTimeMillis(),
            repoStatus = bestGitTuple?.second ?: MirrorProbeItemStatus(MirrorProbeLevel.Failed, "所有内置 Git 源均连不上。"),
            versionStatus = bestGitTuple?.third ?: MirrorProbeItemStatus(MirrorProbeLevel.Failed, "无法读取版本列表。"),
            npmStatus = bestNpmPair?.second ?: MirrorProbeItemStatus(MirrorProbeLevel.Failed, "所有内置 npm 源均连不上。"),
        )

        val summaryMessage = when {
            gitReachable && npmReachable -> {
                val gitMs = bestGitScore?.latencyMs?.let { "（${it}ms）" }.orEmpty()
                val npmMs = bestNpmScore?.latencyMs?.let { "（${it}ms）" }.orEmpty()
                "已优选：${nextConfig.repoLabel}$gitMs + ${nextConfig.npmLabel}$npmMs"
            }
            gitReachable -> "已优选酒馆源：${nextConfig.repoLabel}；npm 源均测速失败，请检查网络。"
            npmReachable -> "已优选 npm 源：${nextConfig.npmLabel}；内置 Git 源均连不上，可尝试填写自定义镜像。"
            else -> "所有内置下载源均连不上，请检查网络连接或开启代理后重试。"
        }

        TavernMirrorAutoSelectResult(
            foundAny = gitReachable || npmReachable,
            recommendedConfig = nextConfig,
            probeStatus = finalProbeStatus,
            summaryMessage = summaryMessage,
        )
    }

    private suspend fun probeGitAndVersions(
        client: LukoaHttpClient,
        repoUrl: String,
    ): Pair<MirrorProbeItemStatus, MirrorProbeItemStatus> = coroutineScope {
        val source = TavernGithubSourceParser.parse(repoUrl)
        if (source == null) {
            return@coroutineScope Pair(
                MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Warning,
                    message = "自定义 Git 源，暂不支持直接测速。",
                ),
                MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Warning,
                    message = "无法由 App 直接读取此源的官方版本列表。",
                ),
            )
        }

        val tagsDeferred = async(Dispatchers.IO) { probeJsonArray(client, source.tagsApiUrl) }
        val branchesDeferred = async(Dispatchers.IO) { probeJsonArray(client, source.branchesApiUrl) }
        val tagsResult = tagsDeferred.await()
        val branchesResult = branchesDeferred.await()
        val bestLatency = listOfNotNull(
            tagsResult.latencyMs.takeIf { tagsResult.ok },
            branchesResult.latencyMs.takeIf { branchesResult.ok },
        ).minOrNull()
        val latencySuffix = bestLatency?.let { "（${it}ms）" }.orEmpty()

        val repoStatus = when {
            tagsResult.ok || branchesResult.ok -> MirrorProbeItemStatus(
                level = MirrorProbeLevel.Healthy,
                message = if (source.proxyPrefix.isBlank()) {
                    "直连可用$latencySuffix"
                } else {
                    "加速源可用$latencySuffix"
                },
                latencyMs = bestLatency,
            )

            else -> MirrorProbeItemStatus(
                level = MirrorProbeLevel.Failed,
                message = tagsResult.errorMessage.ifBlank { branchesResult.errorMessage.ifBlank { "当前 Git 源连不上。" } },
            )
        }

        val versionStatus = when {
            tagsResult.ok && branchesResult.ok -> MirrorProbeItemStatus(
                level = MirrorProbeLevel.Healthy,
                message = "官方版本列表可读取",
                latencyMs = bestLatency,
            )

            tagsResult.ok || branchesResult.ok -> MirrorProbeItemStatus(
                level = MirrorProbeLevel.Warning,
                message = "只读到一部分版本列表，测试版可能不完整。",
                latencyMs = bestLatency,
            )

            else -> MirrorProbeItemStatus(
                level = MirrorProbeLevel.Failed,
                message = "版本列表读不到，建议点「一键测速优选」换源。",
            )
        }
        repoStatus to versionStatus
    }

    private fun probeJsonArray(client: LukoaHttpClient, url: String): ProbeResult {
        val started = SystemClock.elapsedRealtime()
        return runCatching {
            val body = client.getText(url, accept = "application/vnd.github+json")
            JSONArray(body)
        }.fold(
            onSuccess = {
                val elapsed = (SystemClock.elapsedRealtime() - started).coerceAtLeast(1L)
                ProbeResult(ok = true, latencyMs = elapsed)
            },
            onFailure = { error ->
                ProbeResult(
                    ok = false,
                    errorMessage = "读取失败：${error.message ?: error.javaClass.simpleName}",
                )
            },
        )
    }

    private fun probeNpm(client: LukoaHttpClient, registry: String): MirrorProbeItemStatus {
        val base = registry.trim().trimEnd('/')
        val started = SystemClock.elapsedRealtime()
        return runCatching {
            client.getText("$base/-/ping", accept = "application/json")
        }.fold(
            onSuccess = {
                val elapsed = (SystemClock.elapsedRealtime() - started).coerceAtLeast(1L)
                val isMirror = base.equals(TavernMirrorDefaults.NPMMIRROR_REGISTRY.trimEnd('/'), ignoreCase = true)
                MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Healthy,
                    message = if (isMirror) "国内镜像可用（${elapsed}ms）" else "官方源可用（${elapsed}ms）",
                    latencyMs = elapsed,
                )
            },
            onFailure = {
                val fallbackStarted = SystemClock.elapsedRealtime()
                runCatching {
                    client.getText(base, accept = "*/*")
                }.fold(
                    onSuccess = {
                        val elapsed = (SystemClock.elapsedRealtime() - fallbackStarted).coerceAtLeast(1L)
                        MirrorProbeItemStatus(
                            level = MirrorProbeLevel.Warning,
                            message = "源可连通（${elapsed}ms），但未响应标准 ping。",
                            latencyMs = elapsed,
                        )
                    },
                    onFailure = { error ->
                        MirrorProbeItemStatus(
                            level = MirrorProbeLevel.Failed,
                            message = "npm 源连不上：${error.message ?: error.javaClass.simpleName}",
                        )
                    },
                )
            },
        )
    }

    private data class ProbeResult(
        val ok: Boolean,
        val latencyMs: Long? = null,
        val errorMessage: String = "",
    )
}

fun MirrorProbeLevel.label(): String = when (this) {
    MirrorProbeLevel.Healthy -> "正常"
    MirrorProbeLevel.Warning -> "提醒"
    MirrorProbeLevel.Failed -> "失败"
    MirrorProbeLevel.Unknown -> "未检测"
}

fun MirrorProbeLevel.toneColor() = when (this) {
    MirrorProbeLevel.Healthy -> LukoaColors.Primary
    MirrorProbeLevel.Warning -> LukoaColors.Accent
    MirrorProbeLevel.Failed -> LukoaColors.Danger
    MirrorProbeLevel.Unknown -> LukoaColors.TextSecondary
}

fun MirrorProbeLevel.backgroundColor() = when (this) {
    MirrorProbeLevel.Healthy -> LukoaColors.PrimarySoft
    MirrorProbeLevel.Warning -> LukoaColors.AccentSoft
    MirrorProbeLevel.Failed -> LukoaColors.DangerSoft
    MirrorProbeLevel.Unknown -> LukoaColors.Elevated
}
