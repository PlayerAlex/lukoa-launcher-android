package moe.lukoa.launcher

data class BusyPanelPresentation(
    val activityText: String,
    val helperText: String,
)

object BusyPanelPresentationResolver {
    private val longTaskKeywords = listOf(
        "安装",
        "更新",
        "回退",
        "备份",
        "恢复",
        "依赖",
        "环境",
        "迁移",
    )

    fun resolve(label: String, elapsedSeconds: Int): BusyPanelPresentation {
        val seconds = elapsedSeconds.coerceAtLeast(0)
        if (label.contains("准备 Termux 环境")) {
            return when {
                seconds < 20 -> BusyPanelPresentation(
                    activityText = "命令已经发送，正在连接 Termux 软件源",
                    helperText = "第一次准备环境需要读取软件列表，请保持 Termux 可以在后台运行。",
                )
                seconds < 90 -> BusyPanelPresentation(
                    activityText = "正在更新 Termux 的基础软件",
                    helperText = "这一步可能停留一会儿，启动器仍在等待执行结果。",
                )
                seconds < 240 -> BusyPanelPresentation(
                    activityText = "正在安装酒馆需要的基础工具",
                    helperText = "首次安装通常比之后更久，请不要重复点击或关闭 Termux。",
                )
                else -> BusyPanelPresentation(
                    activityText = "Termux 仍在处理，正在等待最终结果",
                    helperText = "耗时较长不一定代表失败；任务结束后，按钮会自动恢复。",
                )
            }
        }

        val longTask = longTaskKeywords.any(label::contains)
        val activityText = when {
            seconds < 3 -> "命令已经发送，等待 Termux 接收"
            seconds < 15 -> "Termux 正在执行这项操作"
            else -> "操作仍在进行，正在等待 Termux 返回结果"
        }
        val helperText = when {
            longTask -> "这类操作可能需要几分钟。完成前不要重复点击，也不要强行关闭 Termux。"
            seconds >= 60 -> "这次比平时更久，但启动器仍在等待；完成后会自动显示结果。"
            else -> "完成后会自动显示结果，并恢复暂时锁定的按钮。"
        }
        return BusyPanelPresentation(activityText, helperText)
    }

    fun formatElapsed(elapsedSeconds: Int): String {
        val seconds = elapsedSeconds.coerceAtLeast(0)
        val minutes = seconds / 60
        val rest = seconds % 60
        return if (minutes > 0) {
            "%d:%02d".format(minutes, rest)
        } else {
            "${rest} 秒"
        }
    }
}
