package moe.lukoa.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

class TavernMirrorProbeManager(private val context: Context) {
    fun check(
        scope: CoroutineScope,
        config: TavernMirrorConfig,
        callback: (TavernMirrorProbeStatus) -> Unit,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                probeBlocking(config)
            }
            callback(result)
        }
    }

    private fun probeBlocking(config: TavernMirrorConfig): TavernMirrorProbeStatus {
        val client = LukoaHttpClient(context)
        val source = TavernGithubSourceParser.parse(config.normalizedRepoUrl)

        val repoStatus: MirrorProbeItemStatus
        val versionStatus: MirrorProbeItemStatus
        if (source == null) {
            repoStatus = MirrorProbeItemStatus(
                level = MirrorProbeLevel.Warning,
                message = "这是自定义 Git 源，App 不能直接探测它。",
            )
            versionStatus = MirrorProbeItemStatus(
                level = MirrorProbeLevel.Warning,
                message = "App 暂时没法帮你读取这个源的官方版本列表。",
            )
        } else {
            val tagsResult = probeJsonArray(client, source.tagsApiUrl)
            val branchesResult = probeJsonArray(client, source.branchesApiUrl)

            repoStatus = when {
                tagsResult.ok || branchesResult.ok -> {
                    if (source.proxyPrefix.isBlank()) {
                        MirrorProbeItemStatus(
                            level = MirrorProbeLevel.Healthy,
                            message = "Git 源可用，能连到仓库接口。",
                        )
                    } else {
                        MirrorProbeItemStatus(
                            level = MirrorProbeLevel.Warning,
                            message = "代理源可用，但稳定性取决于代理服务。",
                        )
                    }
                }

                else -> MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Failed,
                    message = tagsResult.errorMessage.ifBlank { branchesResult.errorMessage.ifBlank { "当前 Git 源连不上。" } },
                )
            }

            versionStatus = when {
                tagsResult.ok && branchesResult.ok -> MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Healthy,
                    message = "官方版本列表可读取。",
                )

                tagsResult.ok || branchesResult.ok -> MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Warning,
                    message = "只读到一部分版本列表，读取测试版时可能不完整。",
                )

                else -> MirrorProbeItemStatus(
                    level = MirrorProbeLevel.Failed,
                    message = "官方版本列表读不到，请先换源或检查网络。",
                )
            }
        }

        val npmStatus = probeNpm(client, config.normalizedNpmRegistry)
        return TavernMirrorProbeStatus(
            configSignature = TavernMirrorProbeStatus.signatureOf(config),
            checking = false,
            checkedAtMillis = System.currentTimeMillis(),
            repoStatus = repoStatus,
            versionStatus = versionStatus,
            npmStatus = npmStatus,
        )
    }

    private fun probeJsonArray(client: LukoaHttpClient, url: String): ProbeResult {
        return runCatching {
            val body = client.getText(url, accept = "application/vnd.github+json")
            JSONArray(body)
        }.fold(
            onSuccess = { array ->
                ProbeResult(
                    ok = true,
                )
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
        return runCatching {
            client.getText("$base/-/ping", accept = "application/json")
        }.fold(
            onSuccess = {
                MirrorProbeItemStatus(
                    level = if (base.equals(TavernMirrorDefaults.NPMMIRROR_REGISTRY.trimEnd('/'), ignoreCase = true)) {
                        MirrorProbeLevel.Warning
                    } else {
                        MirrorProbeLevel.Healthy
                    },
                    message = if (base.equals(TavernMirrorDefaults.NPMMIRROR_REGISTRY.trimEnd('/'), ignoreCase = true)) {
                        "npm 镜像可用，但同步速度取决于镜像站。"
                    } else {
                        "npm 源可用。"
                    },
                )
            },
            onFailure = {
                runCatching {
                    client.getText(base, accept = "*/*")
                }.fold(
                    onSuccess = {
                        MirrorProbeItemStatus(
                            level = MirrorProbeLevel.Warning,
                            message = "npm 源能连通，但没有通过标准 ping 检测。",
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
    MirrorProbeLevel.Healthy -> LukoaColors.Accent
    MirrorProbeLevel.Warning -> LukoaColors.Amber
    MirrorProbeLevel.Failed -> LukoaColors.Danger
    MirrorProbeLevel.Unknown -> LukoaColors.Muted
}

fun MirrorProbeLevel.backgroundColor() = when (this) {
    MirrorProbeLevel.Healthy -> LukoaColors.AccentSoft
    MirrorProbeLevel.Warning -> LukoaColors.AmberSoft
    MirrorProbeLevel.Failed -> LukoaColors.DangerSoft
    MirrorProbeLevel.Unknown -> LukoaColors.SurfaceAlt
}
