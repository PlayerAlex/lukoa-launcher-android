package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseNotesFormatterTest {
    @Test
    fun `format converts markdown sections into numbered app style notes`() {
        val formatted = GithubReleaseNotesFormatter.format(
            versionName = "0.9.1-beta2",
            body = """
                ## 本次更新

                这次版本主要把首次安装和高风险操作做得更清楚。

                ### 新增与优化

                - 更新和回退改成统一确认框。
                - 首次安装向导补充默认稳定版和默认路径提示。

                ### 修复

                - 修复恢复完成后提示过短的问题。

                ### 说明

                - 这是测试版。
            """.trimIndent(),
        )

        assertEquals(
            """
                0.9.1-beta2 版本更新日志：
                1. 更新和回退改成统一确认框
                2. 首次安装向导补充默认稳定版和默认路径提示
                3. 修复恢复完成后提示过短的问题
                4. 这是测试版
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `format falls back when body has no bullet list`() {
        val formatted = GithubReleaseNotesFormatter.format(
            versionName = "0.9.0",
            body = "这个版本没有写列表，但补充了更清楚的更新说明。",
        )

        assertTrue(formatted.contains("0.9.0 版本更新日志："))
        assertTrue(formatted.contains("1. 这个版本没有写列表，但补充了更清楚的更新说明。"))
    }
}
