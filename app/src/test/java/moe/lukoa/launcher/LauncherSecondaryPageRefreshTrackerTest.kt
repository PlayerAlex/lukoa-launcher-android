package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherSecondaryPageRefreshTrackerTest {
    @Test
    fun `page entry refreshes exactly once`() {
        val tracker = LauncherSecondaryPageRefreshTracker()

        assertEquals(
            LauncherSecondaryPageRefreshTarget.VersionManagement,
            tracker.next(LauncherSecondaryPage.VersionManagement, actionInProgress = false),
        )
        assertNull(tracker.next(LauncherSecondaryPage.VersionManagement, actionInProgress = true))
        assertNull(tracker.next(LauncherSecondaryPage.VersionManagement, actionInProgress = false))
    }

    @Test
    fun `busy page entry waits until operation finishes`() {
        val tracker = LauncherSecondaryPageRefreshTracker()

        assertNull(tracker.next(LauncherSecondaryPage.Backup, actionInProgress = true))
        assertEquals(
            LauncherSecondaryPageRefreshTarget.Backup,
            tracker.next(LauncherSecondaryPage.Backup, actionInProgress = false),
        )
        assertNull(tracker.next(LauncherSecondaryPage.Backup, actionInProgress = false))
    }

    @Test
    fun `leaving page cancels pending refresh`() {
        val tracker = LauncherSecondaryPageRefreshTracker()

        assertNull(tracker.next(LauncherSecondaryPage.VersionManagement, actionInProgress = true))
        assertNull(tracker.next(null, actionInProgress = true))
        assertNull(tracker.next(null, actionInProgress = false))
    }
}
