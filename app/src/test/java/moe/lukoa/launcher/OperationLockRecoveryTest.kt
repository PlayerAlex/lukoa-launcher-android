package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationLockRecoveryTest {
    @Test
    fun `active lock restores label elapsed time and remaining wait`() {
        val restored = OperationLockRecovery.restore(
            snapshot = OperationLockSnapshot(
                label = "更新酒馆源码",
                startedAtMillis = 10_000L,
                expiresAtMillis = 40_000L,
            ),
            nowMillis = 25_000L,
            elapsedRealtimeMillis = 100_000L,
        )

        assertEquals("更新酒馆源码", restored?.label)
        assertEquals(85_000L, restored?.busyStartedAtElapsedMillis)
        assertEquals(15_000L, restored?.remainingMillis)
    }

    @Test
    fun `expired lock is not restored`() {
        val restored = OperationLockRecovery.restore(
            snapshot = OperationLockSnapshot(
                label = "创建酒馆备份",
                startedAtMillis = 10_000L,
                expiresAtMillis = 20_000L,
            ),
            nowMillis = 20_000L,
            elapsedRealtimeMillis = 50_000L,
        )

        assertNull(restored)
    }

    @Test
    fun `wall clock elapsed time cannot produce negative elapsed realtime start`() {
        val restored = OperationLockRecovery.restore(
            snapshot = OperationLockSnapshot(
                label = "应用酒馆备份",
                startedAtMillis = 1_000L,
                expiresAtMillis = 20_000L,
            ),
            nowMillis = 15_000L,
            elapsedRealtimeMillis = 5_000L,
        )

        assertEquals(0L, restored?.busyStartedAtElapsedMillis)
        assertEquals(5_000L, restored?.remainingMillis)
    }

    @Test
    fun `owned lock can only be released by matching non blank token`() {
        val snapshot = OperationLockSnapshot(
            label = "自动备份",
            startedAtMillis = 10_000L,
            expiresAtMillis = 20_000L,
            ownerToken = "owner-a",
        )

        assertTrue(OperationLockOwnership.canRelease(snapshot, "owner-a"))
        assertFalse(OperationLockOwnership.canRelease(snapshot, "owner-b"))
        assertFalse(OperationLockOwnership.canRelease(snapshot, ""))
        assertFalse(OperationLockOwnership.canRelease(null, "owner-a"))
    }

    @Test
    fun `unowned release never clears an owned lock`() {
        val unowned = OperationLockSnapshot("前台操作", 10_000L, 20_000L)
        val owned = unowned.copy(ownerToken = "background-owner")

        assertTrue(OperationLockOwnership.canReleaseUnowned(unowned))
        assertFalse(OperationLockOwnership.canReleaseUnowned(owned))
        assertFalse(OperationLockOwnership.canReleaseUnowned(null))
    }
}
