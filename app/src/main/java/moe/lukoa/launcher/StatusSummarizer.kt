package moe.lukoa.launcher

object StatusSummarizer {
    fun summarize(status: String, termuxOutput: String, ok: Boolean): String {
        val merged = "$status\n$termuxOutput"
        val lower = merged.lowercase()
        return when {
            TermuxPermissionSignals.externalAppsBlocked(merged) -> "Termux 外部调用未开启"

            lower.contains("termux-storage-permission") ||
                (
                    lower.contains("permission denied") &&
                        lower.contains("restore archive cannot be listed")
                ) -> "应用失败：Termux 没有存储权限"

            lower.contains("aptlockheld=1") ||
                lower.contains("waitingforaptlockpids=") ||
                lower.contains("could not get lock") ||
                lower.contains("unable to acquire the dpkg frontend lock") -> "Termux 正在安装东西，等它结束后再试"

            lower.contains("dpkg was interrupted") ||
                lower.contains("dpkg configure recovery") ||
                lower.contains("dpkgconfigureexitcode") ||
                lower.contains("apt fix-broken install") -> "Termux 上次安装被打断，正在修复"

            merged.contains("HTTP endpoint is still responding") ||
                merged.contains("\"exitCode\": 76") -> "停止酒馆失败：酒馆网页仍在响应"

            merged.contains("\"status\": \"stopped\"") ||
                merged.contains("SillyTavern stopped") ||
                merged.contains("SillyTavern was not running") ||
                merged.contains("SillyTavern foreground session exited") ||
                merged.contains("process is not running", ignoreCase = true) -> {
                if (
                    merged.contains("命令已发送到 Termux：stop") ||
                    merged.contains("SillyTavern stopped")
                ) {
                    "停止酒馆成功"
                } else {
                    "酒馆当前未运行"
                }
            }

            merged.contains("命令已发送到 Termux：start") ||
                merged.contains("命令已发送到 Termux：foreground-console") ||
                merged.contains("正在请求 Termux 打开前台酒馆日志窗口") ||
                merged.contains("已发送酒馆启动命令") ||
                merged.contains("启动命令已发送") ||
                merged.contains("正在执行 start") ||
                merged.contains("\"status\": \"starting\"") ||
                merged.contains("懒人启动：") -> "正在启动酒馆"

            merged.contains("命令已发送到 Termux：stop") ||
                merged.contains("停止命令已发送") -> "正在停止酒馆"

            merged.contains("正在查询酒馆状态") -> "正在查询状态"

            merged.contains("正在读取 Termux 日志") -> "正在读取日志"

            merged.contains("日志已读取") -> "日志已读取"

            merged.contains("日志读取失败") -> "日志读取失败"

            merged.contains("正在读取酒馆版本") -> "正在读取酒馆版本"

            merged.contains("酒馆版本已读取") -> "酒馆版本已读取"

            merged.contains("正在读取官方版本列表") -> "正在读取官方版本列表"

            merged.contains("官方版本列表已读取") ||
                merged.contains("Official SillyTavern versions collected") -> "官方版本列表已读取"

            merged.contains("正在切换 Termux 包源") -> "正在切换 Termux 包源"

            merged.contains("Termux 包源已切换") ||
                merged.contains("Termux package mirror switched") -> "Termux 包源已切换"

            merged.contains("正在安装酒馆") -> "正在安装酒馆"

            merged.contains("\"status\": \"installed\"") ||
                merged.contains("SillyTavern installed successfully") -> "酒馆已安装"

            merged.contains("正在更新酒馆源码") -> "正在更新酒馆源码"

            merged.contains("\"status\": \"updated\"") ||
                merged.contains("SillyTavern source updated successfully") -> "酒馆源码已更新"

            merged.contains("正在回退酒馆版本") -> "正在回退酒馆版本"

            merged.contains("\"status\": \"rolled-back\"") ||
                merged.contains("SillyTavern source rolled back successfully") -> "酒馆版本已回退"

            merged.contains("Please stop SillyTavern before") -> "请先停止酒馆，再执行版本操作"

            merged.contains("local tracked changes") -> "酒馆源码有本地改动，已阻止更新/回退"

            merged.contains("No rollback snapshot") -> "暂无可回退快照"

            merged.contains("npm command not found") -> "版本操作失败：Termux 缺少 npm"

            merged.contains("git fetch failed") ||
                merged.contains("git update failed") ||
                merged.contains("git rollback failed") -> "版本操作失败：Git 执行失败"

            merged.contains("备份已生成到备份库") ||
                merged.contains("手动备份已创建") -> "备份已生成"

            merged.contains("自动备份已创建") -> "自动备份已完成"

            merged.contains("酒馆备份已删除") ||
                merged.contains("SillyTavern backup deleted") -> "备份已删除"

            merged.contains("酒馆备份已导出") ||
                merged.contains("SillyTavern backup exported") -> "备份已导出"

            merged.contains("酒馆备份已复制") ||
                merged.contains("SillyTavern backup copied") -> "备份已复制"

            merged.contains("酒馆备份已重命名") ||
                merged.contains("SillyTavern backup renamed") -> "备份已重命名"

            merged.contains("酒馆备份已导入") ||
                merged.contains("SillyTavern backup imported") -> "备份已导入"

            merged.contains("状态已刷新") &&
                merged.contains("\"status\": \"running\"") -> "酒馆正在运行"

            merged.contains("状态已刷新") &&
                (merged.contains("\"status\": \"running-unknown\"") ||
                    merged.contains("\"status\": \"unreachable\"")) -> "酒馆进程在运行，网页暂时打不开"

            merged.contains("\"status\": \"stale-process\"") -> "发现疑似残留进程，酒馆网页没开"

            merged.contains("状态已刷新") &&
                merged.contains("\"status\": \"stopped\"") -> "酒馆当前未运行"

            merged.contains("\"status\": \"running\"") &&
                merged.contains("\"exitCode\": 0") -> "酒馆正在运行"

            merged.contains("SillyTavern is already running") -> "酒馆已经在运行"

            merged.contains("\"status\": \"unreachable\"") -> "酒馆进程存在，但网页暂时打不开"

            merged.contains("SillyTavern directory not found") -> "没有找到酒馆目录"

            merged.contains("node command not found") -> "启动失败：Termux 里没有找到 node"

            merged.contains("no start.sh or server.js found") -> "启动失败：酒馆目录里没有启动文件"

            merged.contains("自动读取日志超时") ||
                merged.contains("持续日志追踪超时") -> "持续日志追踪等待中"

            merged.contains("脚本已安装") ||
                merged.contains("脚本已确认可用") -> "脚本已准备好"

            ok -> "操作完成"

            merged.contains("失败") ||
                merged.contains("\"status\": \"error\"") -> "操作失败，请查看 Termux 调用返回"

            else -> status.lineSequence().firstOrNull().orEmpty().ifBlank { "等待操作" }
        }
    }
}
