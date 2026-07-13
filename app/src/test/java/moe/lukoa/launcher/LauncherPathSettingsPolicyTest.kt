package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherPathSettingsPolicyTest {
    @Test
    fun `空迁移路径会提示填写目录`() {
        assertEquals(
            "请先填写要迁移到的目录。",
            LauncherPathSettingsPolicy.customMigrationError("   "),
        )
    }

    @Test
    fun `传统默认路径通过校验`() {
        assertNull(LauncherPathSettingsPolicy.customMigrationError("~/SillyTavern"))
    }

    @Test
    fun `非数字端口保留当前端口`() {
        assertEquals(8000, LauncherPathSettingsPolicy.resolvePort("abc", 8000))
    }

    @Test
    fun `合法端口正常解析`() {
        assertEquals(9000, LauncherPathSettingsPolicy.resolvePort(" 9000 ", 8000))
    }
}
