package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupOperationLockPolicyTest {
    @Test
    fun `retention waits while owned automatic backup is running`() {
        val snapshot = OperationLockSnapshot(
            label = AutoBackupOperationLockPolicy.LABEL,
            startedAtMillis = 10_000L,
            expiresAtMillis = 20_000L,
            ownerToken = "auto-owner",
        )

        assertTrue(AutoBackupOperationLockPolicy.shouldDeferRetention(snapshot))
    }

    @Test
    fun `retention continues for other or unowned locks`() {
        val unownedAuto = OperationLockSnapshot(
            label = AutoBackupOperationLockPolicy.LABEL,
            startedAtMillis = 10_000L,
            expiresAtMillis = 20_000L,
        )
        val otherOwned = unownedAuto.copy(label = "更新酒馆", ownerToken = "other-owner")

        assertFalse(AutoBackupOperationLockPolicy.shouldDeferRetention(unownedAuto))
        assertFalse(AutoBackupOperationLockPolicy.shouldDeferRetention(otherOwned))
        assertFalse(AutoBackupOperationLockPolicy.shouldDeferRetention(null))
    }
}
