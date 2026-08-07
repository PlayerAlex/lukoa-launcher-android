package moe.lukoa.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AutoBackupConfigStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AutoBackupConfigStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("launcher_ui_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(AutoBackupConfigStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("launcher_ui_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `configuration survives store recreation and transient state writes`() {
        LauncherStateStore(context).saveAutoBackupConfig(
            enabled = true,
            intervalMinutes = 90,
            keepCount = 12,
        )

        LauncherStateStore(context).save(defaultLauncherState(isTermuxInstalled = true))

        assertEquals(
            AutoBackupConfigSnapshot(enabled = true, intervalMinutes = 90, keepCount = 12),
            LauncherStateStore(context).readAutoBackupConfig(),
        )
    }

    @Test
    fun `configuration survives task removal state clearing`() {
        val firstStore = LauncherStateStore(context)
        firstStore.saveAutoBackupConfig(
            enabled = true,
            intervalMinutes = 150,
            keepCount = 8,
        )
        firstStore.markClearOnNextLaunch()

        val restored = LauncherStateStore(context).load(
            isTermuxInstalled = true,
            allowColdStartFallback = true,
        )

        assertEquals(true, restored.state.autoBackupEnabled)
        assertEquals(150, restored.state.autoBackupIntervalMinutes)
        assertEquals(8, restored.state.autoBackupKeepCount)
        assertEquals(
            AutoBackupConfigSnapshot(enabled = true, intervalMinutes = 150, keepCount = 8),
            LauncherStateStore(context).readAutoBackupConfig(),
        )
    }

    @Test
    fun `legacy launcher state configuration migrates into dedicated store`() {
        context.getSharedPreferences("launcher_ui_state", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("auto_backup_enabled", true)
            .putInt("auto_backup_interval_minutes", 210)
            .putInt("auto_backup_keep_count", 9)
            .commit()

        val migrated = AutoBackupConfigStore(context).read()
        context.getSharedPreferences("launcher_ui_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        assertEquals(AutoBackupConfigSnapshot(true, 210, 9), migrated)
        assertEquals(
            AutoBackupConfigSnapshot(true, 210, 9),
            AutoBackupConfigStore(context).read(),
        )
    }
}
