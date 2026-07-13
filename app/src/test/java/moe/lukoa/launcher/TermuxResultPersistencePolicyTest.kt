package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxResultPersistencePolicyTest {
    @Test
    fun `history result has explicit combined character limit`() {
        val trimmed = TermuxResultHistoryPolicy.forHistory(
            result(
                stdout = "o".repeat(20_000),
                stderr = "e".repeat(10_000),
                raw = "r".repeat(5_000),
            ),
        )

        assertEquals(
            TermuxResultHistoryPolicy.MAX_HISTORY_TEXT_CHARACTERS,
            TermuxResultHistoryPolicy.textCharacterCount(trimmed),
        )
        assertTrue(trimmed.stdout.all { it == 'o' })
        assertTrue(trimmed.stderr.all { it == 'e' })
        assertTrue(trimmed.raw.all { it == 'r' })
    }

    @Test
    fun `raw metadata does not duplicate stdout or stderr`() {
        val raw = TermuxRawMetadata.build(
            mapOf(
                "lukoa_command" to "status",
                "result" to mapOf(
                    "stdout" to "large stdout",
                    "stderr" to "large stderr",
                    "exitCode" to 0,
                    "custom_metadata" to "kept",
                ),
                "delivery_source" to "receiver",
            ),
        )

        assertFalse(raw.contains("large stdout"))
        assertFalse(raw.contains("large stderr"))
        assertTrue(raw.contains("custom_metadata=kept"))
        assertTrue(raw.contains("delivery_source=receiver"))
    }

    private fun result(
        stdout: String,
        stderr: String,
        raw: String,
    ) = TermuxCommandResult(
        executionId = 1,
        command = "status",
        nonce = null,
        hasResultBundle = true,
        timeMillis = 1L,
        stdout = stdout,
        stderr = stderr,
        exitCode = 0,
        errCode = null,
        errMessage = "",
        stdoutOriginalLength = stdout.length.toString(),
        stderrOriginalLength = stderr.length.toString(),
        raw = raw,
    )
}
