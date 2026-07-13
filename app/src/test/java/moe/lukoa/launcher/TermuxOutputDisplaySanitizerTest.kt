package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TermuxOutputDisplaySanitizerTest {
    @Test
    fun `sanitize removes live log wrapper lines but keeps content`() {
        val sanitized = TermuxOutputDisplaySanitizer.sanitize(
            """
                status=log
                ==== SillyTavern live log: /tmp/tavern.log ====
                line 1
                line 2
                ==== end SillyTavern live log ====
            """.trimIndent(),
        )

        assertEquals(
            """
                status=log
                line 1
                line 2
            """.trimIndent(),
            sanitized,
        )
    }

    @Test
    fun `sanitize removes recent log wrapper lines but keeps surrounding output`() {
        val sanitized = TermuxOutputDisplaySanitizer.sanitize(
            """
                exitCode=0

                ==== SillyTavern recent log: /tmp/tavern.log ====
                line 1
                ==== end SillyTavern recent log ====
                tail=done
            """.trimIndent(),
        )

        assertEquals(
            """
                exitCode=0

                line 1
                tail=done
            """.trimIndent(),
            sanitized,
        )
    }
}
