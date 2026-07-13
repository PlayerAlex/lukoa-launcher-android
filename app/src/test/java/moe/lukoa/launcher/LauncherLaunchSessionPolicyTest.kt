package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLaunchSessionPolicyTest {
    @Test
    fun `new task launch enables task removal fallback even when process survived`() {
        assertTrue(LauncherLaunchSessionPolicy.isFreshTaskLaunch(hasSavedInstanceState = false))
    }

    @Test
    fun `activity recreation with saved state keeps current display logs`() {
        assertFalse(LauncherLaunchSessionPolicy.isFreshTaskLaunch(hasSavedInstanceState = true))
    }
}