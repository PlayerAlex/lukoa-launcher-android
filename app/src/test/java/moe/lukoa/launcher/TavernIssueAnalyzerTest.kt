package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernIssueAnalyzerTest {
    @Test
    fun `recognizes api throttling and invalid key`() {
        val issues = TavernIssueAnalyzer.analyze(
            termuxLog = "HTTP 429 Too Many Requests; HTTP 401 Unauthorized",
            status = "",
        )

        assertTrue(issues.any { it.title == "请求太快或额度不够" })
        assertTrue(issues.any { it.title == "API Key 可能不对" })
    }

    @Test
    fun `storage permission error wins over generic permission denied`() {
        val issues = TavernIssueAnalyzer.analyze(
            termuxLog = "TERMUX-STORAGE-PERMISSION: permission denied while reading restore archive",
            status = "",
        )

        assertTrue(issues.any { it.title == "Termux 没有存储权限" })
        assertFalse(issues.any { it.title == "没有权限访问模型" })
    }

    @Test
    fun `reports active apt lock`() {
        val issues = TavernIssueAnalyzer.analyze(
            termuxLog = "Unable to acquire the dpkg frontend lock",
            status = "aptLockHeld=1",
        )

        assertTrue(issues.any { it.title == "Termux 正在安装东西" })
    }

    @Test
    fun `later recovery signal clears stale apt lock`() {
        val issues = TavernIssueAnalyzer.analyze(
            termuxLog = "Could not get lock\nTermux packages are ready",
            status = "",
        )

        assertFalse(issues.any { it.title == "Termux 正在安装东西" })
    }

    @Test
    fun `deduplicates findings and limits the panel to four issues`() {
        val issues = TavernIssueAnalyzer.analyze(
            termuxLog = """
                HTTP 429 rate limit, then another 429.
                HTTP 401 invalid api key.
                maximum context length reached.
                git clone failed.
                npm install failed because of network timeout.
                model not found.
            """.trimIndent(),
            status = "",
        )

        assertEquals(4, issues.size)
        assertEquals(issues.size, issues.map { it.title }.distinct().size)
    }
}
