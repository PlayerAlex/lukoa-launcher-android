package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernLogSignalsTest {
    private val esc = '\u001B'

    @Test
    fun `prepare for app preserves ansi colors model lists and extension lists`() {
        val original = """
            webpack compiled ${esc}[1m${esc}[32msuccessfully${esc}[39m${esc}[22m
            Extensions available for default-user [
              { type: 'system', name: 'assets' },
              { type: 'global', name: 'third-party/World' }
            ]
            Available models: [
              'gpt-5.5',
              'gpt-image-2'
            ]
        """.trimIndent()

        assertEquals(original, TavernLogSignals.prepareForApp(original))
    }

    @Test
    fun `live signal analysis ignores ansi colors`() {
        assertTrue(
            TavernLogSignals.hasRecentLiveSignal(
                "${esc}[32mSillyTavern is listening on IPv4: 127.0.0.1:8000${esc}[0m",
            ),
        )
    }

    @Test
    fun `important tail only reads latest foreground session`() {
        val tail = TavernLogSignals.importantTail(
            """
                [2026-07-02T03:46:11Z] ===== Lukoa launcher foreground session =====
                Available models: [
                  'gpt-5.5'
                ]
                [2026-07-03T08:03:07Z] ===== Lukoa launcher foreground session =====
                SillyTavern 1.18.0
                Compiling frontend libraries...
                ${esc}[32mSillyTavern is listening on IPv4: 127.0.0.1:8000${esc}[0m
            """.trimIndent(),
        )

        assertTrue(tail.contains("2026-07-03T08:03:07Z"))
        assertTrue(tail.contains("SillyTavern is listening"))
        assertFalse(tail.contains("${esc}[32m"))
    }

    @Test
    fun `latest foreground session preserves ansi colors`() {
        val session = TavernLogSignals.latestForegroundSession(
            """
                old line
                [2026-07-02T03:46:11Z] ===== Lukoa launcher foreground session =====
                old session
                [2026-07-03T08:03:07Z] ===== Lukoa launcher foreground session =====
                ${esc}[33mInstalling Node Modules...${esc}[0m
                ${esc}[32mSillyTavern is listening on IPv4: 127.0.0.1:8000${esc}[0m
            """.trimIndent(),
        )

        assertTrue(session.startsWith("[2026-07-03T08:03:07Z]"))
        assertTrue(session.contains("${esc}[33mInstalling Node Modules...${esc}[0m"))
        assertTrue(session.contains("${esc}[32mSillyTavern is listening"))
    }
}
