package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TermuxDisplayContentExtractorTest {
    @Test
    fun `extract keeps foreground console output in command text`() {
        val content = TermuxDisplayContentExtractor.extract(
            """
                [2026-07-08T17:52:30Z] ===== Lukoa launcher foreground session =====
                Installing Node Modules...
                SillyTavern is listening on IPv4: 127.0.0.1:8000
            """.trimIndent(),
        )

        assertEquals(
            """
                [2026-07-08T17:52:30Z] ===== Lukoa launcher foreground session =====
                Installing Node Modules...
                SillyTavern is listening on IPv4: 127.0.0.1:8000
            """.trimIndent(),
            content.commandText,
        )
        assertEquals("", content.tavernRuntimeLogText)
    }

    @Test
    fun `extract removes machine metadata from command text and keeps live log body separately`() {
        val content = TermuxDisplayContentExtractor.extract(
            """
                {
                  "status": "log",
                  "profileId": "main",
                  "runtimeStateDir": "/tmp/runtime"
                }

                ==== SillyTavern directory candidates ====
                candidate.1=/data/data/com.termux/files/home/LukoaLauncher/SillyTavern
                ==== end SillyTavern directory candidates ====
                liveLog.cursor.before=10
                liveLog.cursor.after=20
                liveLog.bytes=10

                ==== SillyTavern live log: /tmp/tavern.log ====
                line 1
                line 2
                ==== end SillyTavern live log ====
            """.trimIndent(),
        )

        assertEquals("", content.commandText)
        assertEquals(
            """
                line 1
                line 2
            """.trimIndent(),
            content.tavernRuntimeLogText,
        )
    }

    @Test
    fun `extract preserves ansi colors in tavern runtime log`() {
        val esc = '\u001B'
        val content = TermuxDisplayContentExtractor.extract(
            """
                ==== SillyTavern live log: /tmp/tavern.log ====
                ${esc}[32mAvailable models: [${esc}[0m
                  'gpt-5.5'
                ]
                ==== end SillyTavern live log ====
            """.trimIndent(),
        )

        assertEquals(
            """
                ${esc}[32mAvailable models: [${esc}[0m
                  'gpt-5.5'
                ]
            """.trimIndent(),
            content.tavernRuntimeLogText,
        )
    }

    @Test
    fun `extract keeps human readable version section after stripping json envelope`() {
        val content = TermuxDisplayContentExtractor.extract(
            """
                {
                  "status": "version",
                  "message": "ok"
                }

                ==== SillyTavern version ====
                package.version=1.18.0
                git.branch=release
                ==== end SillyTavern version ====
            """.trimIndent(),
        )

        assertEquals(
            """
                ==== SillyTavern version ====
                package.version=1.18.0
                git.branch=release
                ==== end SillyTavern version ====
            """.trimIndent(),
            content.commandText,
        )
        assertEquals("", content.tavernRuntimeLogText)
    }
}
