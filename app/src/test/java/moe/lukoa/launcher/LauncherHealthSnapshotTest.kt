package moe.lukoa.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LauncherHealthSnapshotTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearLauncherState()
    }

    @After
    fun tearDown() {
        clearLauncherState()
    }

    @Test
    fun `snapshot codec preserves health summary and Chinese item text`() {
        val snapshot = LauncherHealthSnapshot(
            checkedAtMillis = 1_786_428_240_000L,
            summaryTitle = "基本正常",
            summaryDetail = "权限与路径均可用。\n仍建议定期备份。",
            items = listOf(
                LauncherHealthItem(
                    title = "酒馆目录",
                    detail = "已找到 ~/SillyTavern",
                    level = LauncherHealthLevel.Good,
                ),
                LauncherHealthItem(
                    title = "镜像源",
                    detail = "暂时无法确认",
                    level = LauncherHealthLevel.Unknown,
                ),
            ),
            errorCount = 0,
            warningCount = 0,
            unknownCount = 1,
        )

        assertEquals(snapshot, LauncherHealthSnapshotCodec.decode(LauncherHealthSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun `snapshot codec rejects malformed content`() {
        assertNull(LauncherHealthSnapshotCodec.decode("health-v1\nnot-a-time\n0|0|0\na\nb"))
        assertNull(LauncherHealthSnapshotCodec.decode("health-v1\n123\n0|0|0\na\nb\ninvalid-item"))
    }

    @Test
    fun `display report never restores actions or doctor data`() {
        val source = LauncherHealthReport(
            checkedAtMillis = 123L,
            summaryTitle = "需要处理",
            summaryDetail = "有一项需要处理",
            items = listOf(LauncherHealthItem("路径", "目录不存在", LauncherHealthLevel.Error)),
            errorCount = 1,
            primaryAction = LauncherHealthAction(
                type = LauncherHealthActionType.OpenPathSettings,
                label = "调整酒馆路径",
            ),
        )

        val restored = LauncherHealthSnapshot.fromReport(source)?.toDisplayReport()

        assertEquals("需要处理", restored?.summaryTitle)
        assertNull(restored?.primaryAction)
        assertNull(restored?.doctorReport)
    }

    @Test
    fun `saved health snapshot survives store recreation and display log clearing`() {
        val snapshot = sampleSnapshot()
        val firstStore = LauncherStateStore(context)
        firstStore.save(defaultLauncherState(isTermuxInstalled = true).copy(lastHealthCheck = snapshot))

        assertEquals(
            snapshot,
            LauncherStateStore(context).load(
                isTermuxInstalled = true,
                allowColdStartFallback = false,
            ).state.lastHealthCheck,
        )

        firstStore.markClearOnNextLaunch()
        val afterLogClear = LauncherStateStore(context).load(
            isTermuxInstalled = true,
            allowColdStartFallback = true,
        )

        assertTrue(afterLogClear.displayLogsCleared)
        assertEquals(snapshot, afterLogClear.state.lastHealthCheck)
    }

    @Test
    fun `missing Termux clears stored health snapshot`() {
        val store = LauncherStateStore(context)
        store.save(defaultLauncherState(isTermuxInstalled = true).copy(lastHealthCheck = sampleSnapshot()))

        val withoutTermux = store.load(
            isTermuxInstalled = false,
            allowColdStartFallback = false,
        )
        val afterTermuxReturns = store.load(
            isTermuxInstalled = true,
            allowColdStartFallback = false,
        )

        assertNull(withoutTermux.state.lastHealthCheck)
        assertNull(afterTermuxReturns.state.lastHealthCheck)
        assertFalse(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .contains("last_health_check"),
        )
    }

    private fun sampleSnapshot(): LauncherHealthSnapshot {
        return LauncherHealthSnapshot(
            checkedAtMillis = 1_786_428_240_000L,
            summaryTitle = "基本正常",
            summaryDetail = "当前环境可以使用",
            items = listOf(LauncherHealthItem("Termux", "已就绪", LauncherHealthLevel.Good)),
            errorCount = 0,
            warningCount = 0,
            unknownCount = 0,
        )
    }

    private fun clearLauncherState() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        const val PREFS_NAME = "launcher_ui_state"
    }
}
