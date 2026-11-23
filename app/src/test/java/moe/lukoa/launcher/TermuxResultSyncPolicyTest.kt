package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxResultSyncPolicyTest {
    @Test
    fun `foreground console error with json envelope and recent log is not dropped`() {
        val output = """
            {
              "app": "lukoa-launcher",
              "status": "error",
              "running": false,
              "exitCode": 76,
              "message": "Address 127.0.0.1:8000 is already in use"
            }

            ==== SillyTavern recent log: /tmp/tavern.log ====
            Error: listen EADDRINUSE: address already in use 0.0.0.0:8000
            ==== end SillyTavern recent log ====
        """.trimIndent()
        val display = TermuxResultDisplay(
            key = "k1",
            command = "foreground-console",
            output = output,
            ok = false,
        )
        val extracted = TermuxDisplayContentExtractor.extract(output)

        assertTrue(extracted.commandText.isBlank())
        assertTrue(TermuxResultSyncPolicy.shouldSync(display, extracted, currentTermuxLog = ""))
    }

    @Test
    fun `json-only stopped result still syncs running state`() {
        val output = """
            {
              "app": "lukoa-launcher",
              "status": "stopped",
              "running": false,
              "exitCode": 1,
              "message": "SillyTavern foreground session exited"
            }
        """.trimIndent()
        val display = TermuxResultDisplay(
            key = "k2",
            command = "foreground-console",
            output = output,
            ok = false,
        )
        val extracted = TermuxDisplayContentExtractor.extract(output)

        assertTrue(extracted.commandText.isBlank())
        assertTrue(TermuxResultSyncPolicy.shouldSync(display, extracted, currentTermuxLog = ""))
    }

    @Test
    fun `log polls and already shown command text are skipped`() {
        val logDisplay = TermuxResultDisplay(
            key = "k3",
            command = "log",
            output = "{\"status\": \"log\", \"running\": true}",
            ok = true,
        )
        assertFalse(
            TermuxResultSyncPolicy.shouldSync(
                logDisplay,
                TermuxDisplayContentExtractor.extract(logDisplay.output),
                currentTermuxLog = "",
            ),
        )

        val versionOutput = "==== SillyTavern version ====\npackage.version=1.14.0\n==== end SillyTavern version ===="
        val versionDisplay = TermuxResultDisplay(
            key = "k4",
            command = "tavern-version",
            output = versionOutput,
            ok = true,
        )
        assertFalse(
            TermuxResultSyncPolicy.shouldSync(
                versionDisplay,
                TermuxDisplayContentExtractor.extract(versionOutput),
                currentTermuxLog = "previous\n$versionOutput",
            ),
        )
    }
}
