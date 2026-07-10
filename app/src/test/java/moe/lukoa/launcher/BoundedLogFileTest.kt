package moe.lukoa.launcher

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedLogFileTest {
    @Test
    fun `small file is returned unchanged`() = withTempFile("line 1\nline 2") { file ->
        assertEquals("line 1\nline 2", BoundedLogFile.readTail(file, maxChars = 100))
    }

    @Test
    fun `large file only returns bounded tail`() = withTempFile("a".repeat(1_000) + "TAIL") { file ->
        val result = BoundedLogFile.readTail(file, maxChars = 120)

        assertEquals(120, result.length)
        assertTrue(result.startsWith("... 前面历史日志过大，导出时已截断 ..."))
        assertTrue(result.endsWith("TAIL"))
    }

    @Test
    fun `utf8 tail starts at valid character boundary`() = withTempFile("酒馆日志".repeat(500) + "结束") { file ->
        val result = BoundedLogFile.readTail(file, maxChars = 121)

        assertEquals(121, result.length)
        assertTrue(result.endsWith("结束"))
        assertFalse(result.contains('\uFFFD'))
    }

    private fun withTempFile(content: String, block: (File) -> Unit) {
        val file = File.createTempFile("lukoa-log-", ".txt")
        try {
            file.writeText(content, Charsets.UTF_8)
            block(file)
        } finally {
            file.delete()
        }
    }
}
