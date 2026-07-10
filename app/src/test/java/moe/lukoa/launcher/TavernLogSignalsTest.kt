package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernLogSignalsTest {
    @Test
    fun `prepare for app strips ansi but preserves model and extension lists`() {
        val prepared = TavernLogSignals.prepareForApp(
            """
                webpack compiled [1m[32msuccessfully[39m[22m
                Extensions available for default-user [
                  { type: 'system', name: 'assets' },
                  { type: 'global', name: 'third-party/World' }
                ]
                Available models: [
                  'gpt-5.5',
                  'gpt-image-2'
                ]
            """.trimIndent(),
        )

        assertEquals(
            """
                webpack compiled successfully
                Extensions available for default-user [
                  { type: 'system', name: 'assets' },
                  { type: 'global', name: 'third-party/World' }
                ]
                Available models: [
                  'gpt-5.5',
                  'gpt-image-2'
                ]
            """.trimIndent(),
            prepared,
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
                SillyTavern is listening on IPv4: 127.0.0.1:8000
            """.trimIndent(),
        )

        assertTrue(tail.contains("2026-07-03T08:03:07Z"))
        assertTrue(tail.contains("SillyTavern is listening"))
    }

    @Test
    fun `latest foreground session returns only the current foreground block`() {
        val session = TavernLogSignals.latestForegroundSession(
            """
                old line
                [2026-07-02T03:46:11Z] ===== Lukoa launcher foreground session =====
                old session
                [2026-07-03T08:03:07Z] ===== Lukoa launcher foreground session =====
                Installing Node Modules...
                SillyTavern is listening on IPv4: 127.0.0.1:8000
            """.trimIndent(),
        )

        assertEquals(
            """
                [2026-07-03T08:03:07Z] ===== Lukoa launcher foreground session =====
                Installing Node Modules...
                SillyTavern is listening on IPv4: 127.0.0.1:8000
            """.trimIndent(),
            session,
        )
    }
}
