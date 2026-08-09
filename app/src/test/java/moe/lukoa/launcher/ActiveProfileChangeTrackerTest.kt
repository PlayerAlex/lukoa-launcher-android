package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveProfileChangeTrackerTest {
    @Test
    fun `initial profile observation is not treated as a switch`() {
        val tracker = ActiveProfileChangeTracker("main")

        assertFalse(tracker.update("main"))
    }

    @Test
    fun `real profile switch is reported once`() {
        val tracker = ActiveProfileChangeTracker("main")

        assertTrue(tracker.update("profile-2"))
        assertFalse(tracker.update("profile-2"))
    }

    @Test
    fun `switching back to a previous profile is still a real switch`() {
        val tracker = ActiveProfileChangeTracker("main")

        assertTrue(tracker.update("profile-2"))
        assertTrue(tracker.update("main"))
    }
}
