package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLogContentBuilderTest {
    private val state = LauncherUiState(
        status = "酒馆正在运行",
        summary = "运行中",
        termuxLog = "命令回传不应导出",
        tavernRuntimeLog = "",
        appLog = "",
        verified = true,
    )

    @Test
    fun `termux side export contains only full tavern runtime log`() {
        val content = SessionLogContentBuilder.build(
            state = state,
            mode = ExportLogMode.TermuxOnly,
            tavernRuntimeLog = """
                Extensions available for default-user [
                  { name: 'World' }
                ]
                Available models: [
                  'gpt-5.5'
                ]
            """.trimIndent(),
            appLog = "App feedback",
            exportTime = "12:00:00",
        )

        assertTrue(content.contains("==== 酒馆运行日志 ===="))
        assertTrue(content.contains("页面清空不影响归档"))
        assertTrue(content.contains("{ name: 'World' }"))
        assertTrue(content.contains("'gpt-5.5'"))
        assertFalse(content.contains("Termux 前台回传"))
        assertFalse(content.contains("命令回传不应导出"))
        assertFalse(content.contains("App feedback"))
    }

    @Test
    fun `tavern runtime export preserves ansi colors`() {
        val esc = '\u001B'
        val content = SessionLogContentBuilder.build(
            state = state,
            mode = ExportLogMode.TermuxOnly,
            tavernRuntimeLog = "${esc}[32mgreen${esc}[0m",
            appLog = "",
            exportTime = "12:00:00",
        )

        assertTrue(content.contains("${esc}[32mgreen${esc}[0m"))
    }

    @Test
    fun `combined export includes tavern runtime and app feedback`() {
        val content = SessionLogContentBuilder.build(
            state = state,
            mode = ExportLogMode.Both,
            tavernRuntimeLog = "runtime line",
            appLog = "app line",
            exportTime = "12:00:00",
        )

        assertTrue(content.contains("runtime line"))
        assertTrue(content.contains("==== App 操作反馈 ===="))
        assertTrue(content.contains("app line"))
    }
}
