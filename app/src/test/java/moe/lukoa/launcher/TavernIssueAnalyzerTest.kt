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

    @Test
    fun `diagnoses startup port conflict oom missing module and outdated node`() {
        val portIssues = TavernIssueAnalyzer.analyze(
            termuxLog = "Error: listen EADDRINUSE: address already in use 0.0.0.0:8000",
            status = "",
        )
        assertTrue(portIssues.any { it.title == "酒馆端口被占用" })

        val oomIssues = TavernIssueAnalyzer.analyze(
            termuxLog = "FATAL ERROR: Reached heap limit Allocation failed - JavaScript heap out of memory",
            status = "",
        )
        assertTrue(oomIssues.any { it.title == "Node.js 内存不足" })

        val moduleIssues = TavernIssueAnalyzer.analyze(
            termuxLog = "Error: Cannot find module 'express'",
            status = "",
        )
        assertTrue(moduleIssues.any { it.title == "酒馆依赖缺失或损坏" })

        val nodeIssues = TavernIssueAnalyzer.analyze(
            termuxLog = "Unsupported Node.js version v16.20.0. SillyTavern requires Node.js v18 or higher.",
            status = "",
        )
        assertTrue(nodeIssues.any { it.title == "Node.js 版本过旧" })
    }
}
