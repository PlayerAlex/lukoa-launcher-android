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
}
