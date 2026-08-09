package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseNotesFormatterTest {
    @Test
    fun `parse groups markdown sections in a stable app order`() {
        val document = GithubReleaseNotesFormatter.parse(
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
            GithubReleaseNotesDocument(
                versionTitle = "0.9.1-beta2 版本更新日志：",
                sections = listOf(
                    GithubReleaseNotesSection(
                        title = "新增功能",
                        items = listOf(
                            "更新和回退改成统一确认框",
                            "首次安装向导补充默认稳定版和默认路径提示",
                        ),
                    ),
                    GithubReleaseNotesSection(
                        title = "修复更新",
                        items = listOf("修复恢复完成后提示过短的问题"),
                    ),
                    GithubReleaseNotesSection(
                        title = "使用说明",
                        items = listOf("这是测试版"),
                    ),
                ),
            ),
            document,
        )
    }

    @Test
    fun `parse recognizes unheaded bullets and removes duplicates`() {
        val document = GithubReleaseNotesFormatter.parse(
            versionName = "0.9.2",
            body = """
                - 修复启动预检误报
                - 新增扩展管理
                - 优化设置页间距
                - 修复启动预检误报
            """.trimIndent(),
        )

        assertEquals(listOf("新增功能", "体验优化", "修复更新"), document.sections.map { it.title })
        assertEquals(listOf("新增扩展管理"), document.sections[0].items)
        assertEquals(listOf("优化设置页间距"), document.sections[1].items)
        assertEquals(listOf("修复启动预检误报"), document.sections[2].items)
    }

    @Test
    fun `format keeps grouped plain text shape for non compose consumers`() {
        val formatted = GithubReleaseNotesFormatter.format(
            versionName = "0.9.3",
            body = "## 新增\n- 新增扩展管理\n## 修复\n- 修复启动预检误报",
        )

        assertEquals(
            """
                0.9.3 版本更新日志：

                新增功能：
                1. 新增扩展管理

                修复更新：
                1. 修复启动预检误报
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `parse falls back when body has no bullet list`() {
        val document = GithubReleaseNotesFormatter.parse(
            versionName = "0.9.0",
            body = "这个版本没有写列表，但补充了更清楚的更新说明。",
        )

        assertEquals("0.9.0 版本更新日志：", document.versionTitle)
        assertEquals("其他调整", document.sections.single().title)
        assertTrue(document.sections.single().items.single().contains("这个版本没有写列表"))
    }
}
