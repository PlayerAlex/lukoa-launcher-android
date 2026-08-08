package moe.lukoa.launcher

import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TermuxResultParserTest {
    @Test
    fun `reads nested termux result while preserving launcher metadata`() {
        val resultBundle = Bundle().apply {
            putCharSequence("stdout", "完成")
            putString("stderr", "")
            putLong("exitCode", 7L)
        }
        val intent = Intent().apply {
            putExtra("com.termux.service.extra.PLUGIN_RESULT_BUNDLE", resultBundle)
            putExtra(TermuxCommandRunner.EXTRA_EXECUTION_ID, 42)
            putExtra(TermuxCommandRunner.EXTRA_LUKOA_COMMAND, "status")
            putExtra(TermuxCommandRunner.EXTRA_LUKOA_NONCE, "nonce")
        }

        val result = TermuxResultParser.parse(intent)

        assertTrue(result.hasResultBundle)
        assertEquals(42, result.executionId)
        assertEquals("status", result.command)
        assertEquals("nonce", result.nonce)
        assertEquals("完成", result.stdout)
        assertEquals(7, result.exitCode)
    }

    @Test
    fun `finds compatible result bundle under an unknown provider key`() {
        val providerBundle = Bundle().apply {
            putString("stdout", "fallback")
            putInt("exitCode", 0)
        }
        val result = TermuxResultParser.parse(
            Intent().apply { putExtra("provider-specific-result", providerBundle) },
        )

        assertTrue(result.hasResultBundle)
        assertEquals("fallback", result.stdout)
        assertEquals(0, result.exitCode)
    }
}
