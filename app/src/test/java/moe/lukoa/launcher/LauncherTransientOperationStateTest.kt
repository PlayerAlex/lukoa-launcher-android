package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class LauncherTransientOperationStateTest {
    @Test
    fun `unfinished loading states are released when operation ends`() {
        val upload = LauncherTransientOperationState.finishUploadLimit(
            TavernUploadLimitStatus(checking = true, message = "正在读取"),
        )
        val users = LauncherTransientOperationState.finishUsers(
            TavernUserManagementState(loading = true, message = "正在读取"),
        )
        val extensions = LauncherTransientOperationState.finishExtensions(
            TavernExtensionManagementState(loading = true, message = "正在读取"),
        )

        assertFalse(upload.checking)
        assertFalse(users.loading)
        assertFalse(extensions.loading)
        assertEquals("读取已结束，但没有收到完整结果，请重新检查。", upload.message)
        assertEquals("读取已结束，但没有收到完整用户列表，请重试。", users.message)
        assertEquals("读取已结束，但没有收到完整扩展列表，请重试。", extensions.message)
    }

    @Test
    fun `completed states stay unchanged`() {
        val upload = TavernUploadLimitStatus(message = "完成")
        val users = TavernUserManagementState(message = "完成")
        val extensions = TavernExtensionManagementState(message = "完成")

        assertSame(upload, LauncherTransientOperationState.finishUploadLimit(upload))
        assertSame(users, LauncherTransientOperationState.finishUsers(users))
        assertSame(extensions, LauncherTransientOperationState.finishExtensions(extensions))
    }
}
