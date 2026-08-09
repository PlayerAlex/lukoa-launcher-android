package moe.lukoa.launcher

import org.junit.Assert.assertTrue
import org.junit.Test

class TavernRestoreAftercareTest {
    @Test
    fun `success message keeps old directory hint and dependency reminder`() {
        val message = TavernRestoreAftercareMessage.successMessage(
            """
                ==== SillyTavern restore ====
                restoredTo=/data/data/com.termux/files/home/SillyTavern
                previousDirectory=/data/data/com.termux/files/home/SillyTavern.lukoa-before-restore-20260703
                externalDataRootRestored=/storage/emulated/0/SillyTavern-data
                externalDataRootPrevious=/storage/emulated/0/SillyTavern-data.lukoa-before-restore-20260703
                notice=If dependencies are missing after restore, run update or npm install before starting SillyTavern.
                ==== end SillyTavern restore ====
            """.trimIndent(),
        )

        assertTrue(message.contains("旧酒馆目录已保留"))
        assertTrue(message.contains("外部数据目录也已一起恢复"))
        assertTrue(message.contains("如果启动时报依赖缺失，再执行一次更新"))
    }

    @Test
    fun `success message falls back to recheck when no extra notice exists`() {
        val message = TavernRestoreAftercareMessage.successMessage(
            """
                restoredTo=/data/data/com.termux/files/home/SillyTavern
                previousDirectory=none
            """.trimIndent(),
        )

        assertTrue(message.contains("酒馆备份已应用"))
        assertTrue(message.contains("先重新检测酒馆版本，再启动酒馆"))
    }

    @Test
    fun `user data restore message does not claim program files were replaced`() {
        val message = TavernRestoreAftercareMessage.successMessage(
            """
                restoreMode=user-data
                restoreUserDataOnly=1
                restoredTo=/data/data/com.termux/files/home/SillyTavern/data
                previousDirectory=/data/data/com.termux/files/home/SillyTavern/data.lukoa-before-restore-20260809
            """.trimIndent(),
        )

        assertTrue(message.contains("用户数据已恢复"))
        assertTrue(message.contains("旧用户数据目录已保留"))
        assertTrue(message.contains("程序版本和扩展没有改变"))
    }
}
