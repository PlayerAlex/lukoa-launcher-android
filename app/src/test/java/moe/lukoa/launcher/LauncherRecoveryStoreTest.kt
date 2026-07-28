package moe.lukoa.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LauncherRecoveryStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        cleanUp()
    }

    @After
    fun cleanUp() {
        PendingLauncherTaskStore.clear(context)
        OperationLockStore.release(context)
    }

    @Test
    fun `pending task is readable immediately after durable save`() {
        val task = PendingLauncherTask(
            kind = PendingLauncherTaskKind.RestoreBackup,
            commandName = "tavern-restore",
            detail = "正在应用酒馆备份",
            startedAtMillis = 123L,
            profileId = "main",
        )

        assertTrue(PendingLauncherTaskStore.save(context, task))
        assertEquals(task, PendingLauncherTaskStore.load(context))
        assertTrue(PendingLauncherTaskStore.clear(context))
        assertNull(PendingLauncherTaskStore.load(context))
    }

    @Test
    fun `operation lock is readable before command dispatch continues`() {
        assertTrue(OperationLockStore.acquire(context, "应用酒馆备份", 10_000L))

        val snapshot = OperationLockStore.active(context)
        assertNotNull(snapshot)
        assertEquals("应用酒馆备份", snapshot?.label)
    }
}
