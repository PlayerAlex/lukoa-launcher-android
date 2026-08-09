package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoBackupSchedulePolicyTest {
    @Test
    fun `currently executing work is not cancelled when it schedules its successor`() {
        assertNull(
            AutoBackupSchedulePolicy.staleWorkToCancel(
                previousWorkName = "work-current",
                executingWorkName = "work-current",
                nextWorkName = "work-next",
            ),
        )
    }

    @Test
    fun `a different queued work is cancelled to collapse duplicate chains`() {
        assertEquals(
            "work-already-queued",
            AutoBackupSchedulePolicy.staleWorkToCancel(
                previousWorkName = "work-already-queued",
                executingWorkName = "work-current",
                nextWorkName = "work-next",
            ),
        )
    }

    @Test
    fun `external reschedule replaces the previously queued work`() {
        assertEquals(
            "work-old",
            AutoBackupSchedulePolicy.staleWorkToCancel(
                previousWorkName = "work-old",
                executingWorkName = null,
                nextWorkName = "work-next",
            ),
        )
    }

    @Test
    fun `same next work name does not cancel itself`() {
        assertNull(
            AutoBackupSchedulePolicy.staleWorkToCancel(
                previousWorkName = "work-next",
                executingWorkName = null,
                nextWorkName = "work-next",
            ),
        )
    }
}
