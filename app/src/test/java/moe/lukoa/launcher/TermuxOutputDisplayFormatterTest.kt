package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxOutputDisplayFormatterTest {
    @Test
    fun `format prefers stdout and stderr without synthetic exit code`() {
        val formatted = TermuxOutputDisplayFormatter.format(
            result(
                stdout = "status=ok",
                stderr = "warning=line 2",
                exitCode = 1,
            ),
        )

        assertEquals("status=ok\nwarning=line 2", formatted)
        assertFalse(formatted.contains("exitCode="))
    }

    @Test
    fun `format falls back to raw output before synthetic summary`() {
        val formatted = TermuxOutputDisplayFormatter.format(
            result(
                raw = "raw termux output",
                exitCode = 1,
            ),
        )

        assertEquals("raw termux output", formatted)
    }

    @Test
    fun `format still provides fallback message when termux returned nothing`() {
        val formatted = TermuxOutputDisplayFormatter.format(
            result(
                hasResultBundle = false,
                errCode = 150,
                errMessage = "executable regular file not found",
                exitCode = null,
            ),
        )

        assertTrue(formatted.contains("未收到 Termux 返回包。"))
        assertTrue(formatted.contains("未找到 Termux 脚本，请重新打开启动器。"))
        assertTrue(formatted.contains("缺少 exitCode。"))
    }

    private fun result(
        hasResultBundle: Boolean = true,
        stdout: String = "",
        stderr: String = "",
        exitCode: Int? = 0,
        errCode: Int? = null,
        errMessage: String = "",
        raw: String = "",
    ): TermuxCommandResult {
        return TermuxCommandResult(
            executionId = 1,
            command = "log",
            nonce = null,
            hasResultBundle = hasResultBundle,
            timeMillis = 1L,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            errCode = errCode,
            errMessage = errMessage,
            stdoutOriginalLength = stdout.length.toString(),
            stderrOriginalLength = stderr.length.toString(),
            raw = raw,
        )
    }
}
