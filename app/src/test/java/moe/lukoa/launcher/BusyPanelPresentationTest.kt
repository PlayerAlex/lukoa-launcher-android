package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusyPanelPresentationTest {
    @Test
    fun `elapsed time stays readable for short and long tasks`() {
        assertEquals("0 秒", BusyPanelPresentationResolver.formatElapsed(-3))
        assertEquals("9 秒", BusyPanelPresentationResolver.formatElapsed(9))
        assertEquals("1:05", BusyPanelPresentationResolver.formatElapsed(65))
    }

    @Test
    fun `generic command reports send execute and waiting stages`() {
        assertTrue(
            BusyPanelPresentationResolver.resolve("重新检测酒馆", 1)
                .activityText.contains("已经发送"),
        )
        assertTrue(
            BusyPanelPresentationResolver.resolve("重新检测酒馆", 8)
                .activityText.contains("正在执行"),
        )
        assertTrue(
            BusyPanelPresentationResolver.resolve("重新检测酒馆", 20)
                .activityText.contains("等待 Termux 返回结果"),
        )
    }

    @Test
    fun `long tasks explain that several minutes can be normal`() {
        val presentation = BusyPanelPresentationResolver.resolve("安装酒馆", 12)

        assertTrue(presentation.helperText.contains("可能需要几分钟"))
        assertTrue(presentation.helperText.contains("不要重复点击"))
    }

    @Test
    fun `termux preparation uses more specific progress copy`() {
        assertTrue(
            BusyPanelPresentationResolver.resolve("准备 Termux 环境", 100)
                .activityText.contains("基础工具"),
        )
    }
}
