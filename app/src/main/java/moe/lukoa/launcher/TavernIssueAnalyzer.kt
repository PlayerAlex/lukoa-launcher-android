package moe.lukoa.launcher

data class TavernIssue(
    val title: String,
    val detail: String,
    val action: String,
    val severity: IssueSeverity,
)

enum class IssueSeverity {
    Info,
    Warning,
    Danger,
}

object TavernIssueAnalyzer {
    fun analyze(termuxLog: String, status: String): List<TavernIssue> {
        val latestTermux = latestLogEntry(termuxLog)
        val recentTermux = recentLogWindow(termuxLog)
        val permissionText = stripAnsi("$status\n$recentTermux\n$latestTermux")
        val text = stripAnsi(latestTermux)
        val lower = text.lowercase()
        val broadText = stripAnsi("$status\n$recentTermux")
        val broadLower = broadText.lowercase()
        val issues = mutableListOf<TavernIssue>()

        if (
            activeError(
                permissionText.lowercase(),
                listOf("allow-external-apps", "runcommandservice requires", "termux.properties"),
                listOf("termux-repo-status", "current.uri=", "termux-bootstrap-ok", "termux packages are ready", "\"status\":"),
            )
        ) {
            issues += TavernIssue(
                title = "Termux 外部调用未开启",
                detail = "Termux 已安装，但还不允许启动器调用它。",
                action = "在权限引导里复制命令，打开 Termux 粘贴执行，再回启动器重新检测。",
                severity = IssueSeverity.Danger,
            )
        }

        if (text.isBlank() && broadText.isBlank()) {
            return issues
        }

        if (
            lower.contains("429") ||
            lower.contains("too many requests") ||
            lower.contains("rate limit") ||
            lower.contains("ratelimit")
        ) {
            issues += TavernIssue(
                title = "请求太快或额度不够",
                detail = "接口返回 429，通常是请求太多或额度受限。",
                action = "先等几分钟，检查额度，必要时换模型或 API。",
                severity = IssueSeverity.Danger,
            )
        }

        if (
            lower.contains("401") ||
            lower.contains("unauthorized") ||
            lower.contains("invalid api key") ||
            lower.contains("incorrect api key")
        ) {
            issues += TavernIssue(
                title = "API Key 可能不对",
                detail = "Key 可能填错、失效，或接口选错。",
                action = "重新复制 Key，并确认接口类型。",
                severity = IssueSeverity.Danger,
            )
        }

        val termuxStoragePermissionIssue = activeError(
            broadLower,
            listOf(
                "termux-storage-permission",
                "restore archive cannot be listed",
                "termux cannot read the backup archive",
            ),
            listOf(
                "storage.permission.ok=true",
                "termux storage permission is available",
                "backup restored successfully",
                "restore.completed=1",
                "酒馆备份已应用",
            ),
        )

        if (
            termuxStoragePermissionIssue
        ) {
            issues += TavernIssue(
                title = "Termux 没有存储权限",
                detail = "启动器能看到备份，但 Termux 读不到 Download 里的备份包。",
                action = "打开 Termux，执行 termux-setup-storage，允许文件权限后再点应用。",
                severity = IssueSeverity.Danger,
            )
        }

        if (
            !termuxStoragePermissionIssue &&
            (
                lower.contains("403") ||
                    lower.contains("forbidden") ||
                    lower.contains("permission denied")
            )
        ) {
            issues += TavernIssue(
                title = "没有权限访问模型",
                detail = "账号可能没开通这个模型。",
                action = "换已开通模型，或检查套餐权限。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            lower.contains("context length") ||
            lower.contains("maximum context") ||
            lower.contains("token limit") ||
            lower.contains("maximum tokens")
        ) {
            issues += TavernIssue(
                title = "内容太长",
                detail = "聊天、世界书、角色卡或插件内容太多。",
                action = "减少上下文，或换更大上下文模型。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            activeError(
                broadLower,
                listOf("no mirror or mirror group selected", "termux package mirror is unavailable", "apt update failed"),
                listOf("termux-bootstrap-ok", "termux packages are ready", "aptupdateexitcode=0"),
            )
        ) {
            issues += TavernIssue(
                title = "Termux 包源没准备好",
                detail = "Termux 没有可用包源，基础依赖装不上。",
                action = "先到设置里切换 Termux 清华源，再点“准备 Termux 环境”。",
                severity = IssueSeverity.Danger,
            )
        }

        val aptLockActive = activeError(
            broadLower,
            listOf(
                "could not get lock",
                "unable to acquire the dpkg frontend lock",
                "aptlockheld=1",
                "waitingforaptlockpids=",
                "termux package manager is busy",
            ),
            listOf("termux-bootstrap-ok", "termux packages are ready"),
        )
        if (aptLockActive) {
            issues += TavernIssue(
                title = "Termux 正在安装东西",
                detail = "另一个 apt 任务还没结束，所以这次准备环境被锁住了。",
                action = "等 Termux 里安装结束，再点一次“准备 Termux 环境”。",
                severity = IssueSeverity.Warning,
            )
        }

        val dpkgInterruptedActive = activeError(
            broadLower,
            listOf(
                "dpkg was interrupted",
                "dpkg configure recovery",
                "dpkgconfigureexitcode",
                "apt fix-broken install",
            ),
            listOf("termux-bootstrap-ok", "termux packages are ready"),
        )
        if (dpkgInterruptedActive) {
            issues += TavernIssue(
                title = "Termux 上次安装被打断",
                detail = "Termux 的包数据库还没收尾，所以升级会失败。",
                action = "新版会自动修复；如果还失败，打开 Termux 等它跑完，再点“准备 Termux 环境”。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            activeError(
                broadLower,
                listOf("configured multiple times", "sources.list.d/termux.sources"),
                listOf("termux package mirror switched", "termux-bootstrap-ok", "termux packages are ready"),
            )
        ) {
            issues += TavernIssue(
                title = "Termux 包源重复",
                detail = "同一个包源被写了两份，apt 会一直警告。",
                action = "新版会自动只保留一份包源。安装新版后再切换一次 Termux 清华源。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            !aptLockActive && !dpkgInterruptedActive && activeError(
                broadLower,
                listOf("ssl_set_quic_tls_transport_params", "cannot link executable", "apt full-upgrade failed"),
                listOf("\"status\": \"installed\"", "sillytavern installed successfully", "termux-bootstrap-ok", "termux packages are ready"),
            )
        ) {
            issues += TavernIssue(
                title = "Termux 基础包损坏",
                detail = "curl 或 git 的动态库版本不匹配，下载酒馆会失败。",
                action = "点“准备 Termux 环境”自动执行升级；失败就先切换 Termux 清华源再试。",
                severity = IssueSeverity.Danger,
            )
        }

        if (
            activeError(
                broadLower,
                listOf("not allowed to start service", "app is in background"),
                listOf("已同步 termux", "status updated", "状态已刷新"),
            )
        ) {
            issues += TavernIssue(
                title = "启动器在后台发命令",
                detail = "系统不允许后台 App 直接叫醒 Termux。",
                action = "回到启动器前台再点按钮，不要在切换动画没结束时连点。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            activeError(
                broadLower,
                listOf("git.localchanges=1"),
                listOf("git.localchanges=0", "cleanup generated package-lock.json change"),
            ) &&
            broadLower.contains("package-lock.json")
        ) {
            issues += TavernIssue(
                title = "版本状态被依赖文件弄脏",
                detail = "npm 改了 package-lock.json，所以酒馆显示 dirty。",
                action = "新版会自动清理这类依赖文件改动，再重新检测版本即可。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            lower.contains("git clone failed") ||
            lower.contains("git fetch failed") ||
            lower.contains("failed to read official sillytavern versions") ||
            lower.contains("ls-remote") && lower.contains("failed")
        ) {
            issues += TavernIssue(
                title = "酒馆源码下载失败",
                detail = "当前酒馆 Git 源可能连不上，国内网络常见。",
                action = "到设置里的网络与镜像源，切换 GitHub 加速或填写可用镜像源。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            lower.contains("npm install failed") ||
            lower.contains("npm") && lower.contains("network") ||
            lower.contains("npm") && lower.contains("timeout")
        ) {
            issues += TavernIssue(
                title = "依赖下载失败",
                detail = "npm 依赖源可能连不上或太慢。",
                action = "到设置里的网络与镜像源，切换 npm 镜像后重试。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            lower.contains("econnrefused") ||
            lower.contains("enotfound") ||
            lower.contains("etimedout") ||
            lower.contains("network") && lower.contains("error")
        ) {
            issues += TavernIssue(
                title = "网络或代理不通",
                detail = "接口地址、代理或网络可能有问题。",
                action = "检查代理和 API 地址。",
                severity = IssueSeverity.Warning,
            )
        }

        if (
            lower.contains("model not found") ||
            lower.contains("does not exist") && lower.contains("model") ||
            lower.contains("unknown model")
        ) {
            issues += TavernIssue(
                title = "模型名不可用",
                detail = "模型名可能写错，或账号不能用。",
                action = "复制官方模型名再试。",
                severity = IssueSeverity.Warning,
            )
        }

        return issues.distinctBy { it.title }.take(4)
    }

    private fun latestLogEntry(log: String): String {
        val normalized = log.trim()
        if (normalized.isBlank() || normalized.startsWith("暂无 ")) return ""
        val markerIndex = normalized.lastIndexOf("\n\n[")
        return if (markerIndex >= 0) {
            normalized.substring(markerIndex + 2)
        } else {
            normalized
        }
    }

    private fun recentLogWindow(log: String, maxChars: Int = 14_000): String {
        val normalized = log.trim()
        if (normalized.isBlank() || normalized.startsWith("暂无 ")) return ""
        return if (normalized.length <= maxChars) normalized else normalized.takeLast(maxChars)
    }

    private fun activeError(text: String, errors: List<String>, recoveries: List<String>): Boolean {
        val errorIndex = lastIndexOfAny(text, errors)
        if (errorIndex < 0) return false
        val recoveryIndex = lastIndexOfAny(text, recoveries)
        return errorIndex > recoveryIndex
    }

    private fun lastIndexOfAny(text: String, needles: List<String>): Int {
        return needles.maxOfOrNull { text.lastIndexOf(it) } ?: -1
    }

    private fun stripAnsi(value: String): String {
        return value
            .replace(Regex("\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B\\[[0-?]*[ -/]*[@-~]"), "")
    }
}
