package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveLogRefreshPolicyTest {
    @Test
    fun `periodic refresh keeps the regular bounded window`() {
        assertEquals(
            65_536,
            LiveLogRefreshPolicy.maxBytes(LiveLogRefreshReason.Periodic),
        )
    }

    @Test
    fun `foreground resume stays under the Termux result size limit`() {
        assertEquals(
            65_536,
            LiveLogRefreshPolicy.maxBytes(LiveLogRefreshReason.ForegroundResume),
        )
    }

    @Test
    fun `reads pending and sent bytes from the poll output`() {
        val output = """
            liveLog.cursor.before=0
            liveLog.cursor.after=65000
            liveLog.bytes=65000
            liveLog.pending=120000
        """.trimIndent()

        assertEquals(120_000L, LiveLogRefreshPolicy.pendingBytes(output))
        assertEquals(65_000L, LiveLogRefreshPolicy.sentBytes(output))
        assertEquals(0L, LiveLogRefreshPolicy.pendingBytes("no markers"))
    }

    @Test
    fun `drains a backlog quickly and slows down when the log is quiet`() {
        fun delay(pending: Long, sent: Long, idle: Int, running: Boolean = true) =
            LiveLogRefreshPolicy.nextDelayMillis(pending, sent, idle, running)

        assertEquals(LiveLogRefreshPolicy.BACKLOG_DELAY_MS, delay(pending = 1, sent = 65_000, idle = 0))
        assertEquals(LiveLogRefreshPolicy.ACTIVE_DELAY_MS, delay(pending = 0, sent = 10, idle = 0))
        assertEquals(1_500L, delay(pending = 0, sent = 0, idle = 1))
        assertEquals(3_000L, delay(pending = 0, sent = 0, idle = 10))
        assertEquals(LiveLogRefreshPolicy.STOPPED_DELAY_MS, delay(pending = 0, sent = 0, idle = 1, running = false))
        // A backlog is drained even when the tavern has just stopped.
        assertEquals(LiveLogRefreshPolicy.BACKLOG_DELAY_MS, delay(pending = 5, sent = 0, idle = 3, running = false))
    }

    @Test
    fun `live deltas join with one newline and keep repeated lines`() {
        val first = appendLiveRuntimeLog("暂无 酒馆运行日志。", "Generating...\n")
        val second = appendLiveRuntimeLog(first, "Generating...\n")

        assertEquals("Generating...", first)
        assertEquals("Generating...\nGenerating...", second)
    }
}
