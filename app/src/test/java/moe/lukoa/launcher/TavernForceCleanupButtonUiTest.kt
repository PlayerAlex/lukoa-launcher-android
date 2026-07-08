package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TavernForceCleanupButtonUiTest {
    @Test
    fun `uses generic button copy when no suggestion is available`() {
        assertEquals(
            "强制释放端口 / 清理残留进程",
            TavernForceCleanupButtonUi.labelFor(null),
        )
        assertEquals(
            "只有普通停止没退干净，或你确认当前实例有残留进程、端口冲突时，才建议继续。",
            TavernForceCleanupButtonUi.hintFor(null),
        )
    }

    @Test
    fun `uses suggestion copy when cleanup is actually recommended`() {
        val suggestion = TavernForceCleanupSuggestion(
            kind = TavernForceCleanupKind.PortConflict,
            summary = "检测到当前端口被别的进程占用。",
            reasonDetail = "当前端口不是空闲状态。",
            buttonHint = "当前检测到端口冲突，可以手动继续。",
            riskTip = "这一步会尝试清理当前实例对应的残留进程。",
        )

        assertEquals("强制释放端口", TavernForceCleanupButtonUi.labelFor(suggestion))
        assertEquals("当前检测到端口冲突，可以手动继续。", TavernForceCleanupButtonUi.hintFor(suggestion))
    }
}
