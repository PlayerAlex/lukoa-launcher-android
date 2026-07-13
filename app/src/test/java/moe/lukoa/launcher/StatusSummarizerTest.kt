package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusSummarizerTest {
    @Test
    fun `manual termux open warning after start dispatch still counts as starting`() {
        val summary = StatusSummarizer.summarize(
            status = "启动命令已发送，但系统没有自动打开 Termux。请手动打开 Termux 查看启动日志，先不要重复点启动。",
            termuxOutput = "",
            ok = true,
        )

        assertEquals("正在启动酒馆", summary)
    }

    @Test
    fun `unreachable foreground result is not summarized as stopped`() {
        val summary = StatusSummarizer.summarize(
            status = "已同步 Termux：foreground-console",
            termuxOutput = """
                {"status": "unreachable", "running": true, "exitCode": 75}
                SillyTavern process already exists, but HTTP endpoint is not responding.
            """.trimIndent(),
            ok = false,
        )

        assertEquals("酒馆进程存在，但网页暂时打不开", summary)
    }

    @Test
    fun `port conflict is summarized separately from running state`() {
        val summary = StatusSummarizer.summarize(
            status = "启动酒馆失败。",
            termuxOutput = "Error: Address 127.0.0.1:8001 is already in use",
            ok = false,
        )

        assertEquals("酒馆端口已被别的进程占用", summary)
    }

    @Test
    fun `force cleanup dispatch is summarized separately from normal stop`() {
        val summary = StatusSummarizer.summarize(
            status = "命令已发送到 Termux：tavern-force-cleanup",
            termuxOutput = "",
            ok = true,
        )

        assertEquals("正在强制清理端口", summary)
    }

    @Test
    fun `force cleanup completion is summarized as completed cleanup`() {
        val summary = StatusSummarizer.summarize(
            status = "强制清理已返回。",
            termuxOutput = """
                {"status": "stopped", "running": false, "exitCode": 0}
                SillyTavern force cleanup completed
            """.trimIndent(),
            ok = true,
        )

        assertEquals("强制清理已完成", summary)
    }
}
