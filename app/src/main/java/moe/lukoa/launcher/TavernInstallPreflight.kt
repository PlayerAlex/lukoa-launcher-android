package moe.lukoa.launcher

data class PendingTavernInstallRequest(
    val choice: TavernVersionChoice,
    val target: String,
    val repoUrl: String,
)

data class TavernInstallConfirmation(
    val title: String,
    val summary: String,
    val details: List<String>,
)

data class TavernInstallPreflightResult(
    val ok: Boolean,
    val blockingMessage: String? = null,
    val confirmation: TavernInstallConfirmation? = null,
)

object TavernInstallPreflight {
    fun needsOfficialVersionRefresh(
        choice: TavernVersionChoice,
        officialVersions: TavernOfficialVersions,
        repoUrl: String,
    ): Boolean {
        if (choice.kind == TavernVersionKind.Custom) return false
        if (!officialVersions.hasData) return true
        if (choice.repoUrl.isNotBlank() && !sameRepo(choice.repoUrl, repoUrl)) return true
        return officialVersions.all.none { candidate ->
            candidate.kind == choice.kind &&
                candidate.target.equals(choice.target, ignoreCase = true)
        }
    }

    fun evaluate(
        request: PendingTavernInstallRequest,
        officialVersions: TavernOfficialVersions,
        mirrorProbeStatus: TavernMirrorProbeStatus,
    ): TavernInstallPreflightResult {
        if (mirrorProbeStatus.repoStatus.level == MirrorProbeLevel.Failed) {
            return TavernInstallPreflightResult(
                ok = false,
                blockingMessage = "当前 Git 源不可用。${mirrorProbeStatus.repoStatus.message}",
            )
        }
        if (mirrorProbeStatus.npmStatus.level == MirrorProbeLevel.Failed) {
            return TavernInstallPreflightResult(
                ok = false,
                blockingMessage = "当前 npm 源不可用。${mirrorProbeStatus.npmStatus.message}",
            )
        }

        if (request.choice.kind != TavernVersionKind.Custom) {
            if (!officialVersions.hasData) {
                return TavernInstallPreflightResult(
                    ok = false,
                    blockingMessage = "还没从当前源读到官方版本列表，请先检测镜像源或刷新版本列表。",
                )
            }
            val targetExists = officialVersions.all.any { candidate ->
                candidate.kind == request.choice.kind &&
                    candidate.target.equals(request.target, ignoreCase = true)
            }
            if (!targetExists) {
                return TavernInstallPreflightResult(
                    ok = false,
                    blockingMessage = "当前源里没有找到 ${request.choice.label}，请先刷新官方版本列表。",
                )
            }
        }

        val warnings = mutableListOf<String>()
        if (isRiskyTestChoice(request.choice)) {
            warnings += "你选的是测试版，可能会有 bug 或兼容问题。"
        }
        if (request.choice.kind == TavernVersionKind.Custom) {
            warnings += "你填的是自定义目标，启动器没法保证它一定存在，也没法保证一定能装上。"
        }
        if (mirrorProbeStatus.repoStatus.level == MirrorProbeLevel.Warning) {
            warnings += mirrorProbeStatus.repoStatus.message
        }
        if (mirrorProbeStatus.versionStatus.level == MirrorProbeLevel.Warning) {
            warnings += mirrorProbeStatus.versionStatus.message
        }
        if (mirrorProbeStatus.npmStatus.level == MirrorProbeLevel.Warning) {
            warnings += mirrorProbeStatus.npmStatus.message
        }

        if (warnings.isEmpty()) {
            return TavernInstallPreflightResult(ok = true)
        }

        return TavernInstallPreflightResult(
            ok = true,
            confirmation = TavernInstallConfirmation(
                title = "安装前确认",
                summary = when {
                    request.choice.kind == TavernVersionKind.Custom ->
                        "这是一次自定义安装，先确认版本名、分支名或 commit 没填错。"

                    isRiskyTestChoice(request.choice) ->
                        "这是一次测试版安装，继续前先确认你接受它可能不稳定。"

                    else ->
                        "当前镜像源有提醒信息，确认后再继续安装会更稳妥。"
                },
                details = buildList {
                    add("目标：${request.choice.label}")
                    add("源：${repoLabelFor(request.repoUrl)}")
                    addAll(warnings.distinct())
                },
            ),
        )
    }

    private fun isRiskyTestChoice(choice: TavernVersionChoice): Boolean {
        return choice.kind == TavernVersionKind.Test &&
            !choice.target.equals("release", ignoreCase = true) &&
            !choice.name.equals("release", ignoreCase = true)
    }

    private fun sameRepo(left: String, right: String): Boolean {
        return left.trim().trimEnd('/').equals(right.trim().trimEnd('/'), ignoreCase = true)
    }
}
