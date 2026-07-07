package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherInputGuardsTest {
    @Test
    fun `manual backup name allows normal text`() {
        assertNull(LauncherInputGuards.validateManualBackupName("更新前"))
        assertNull(LauncherInputGuards.validateManualBackupName("plugin-test-01"))
    }

    @Test
    fun `manual backup name rejects dangerous separators`() {
        val pathError = LauncherInputGuards.validateManualBackupName("foo/bar")
        assertNotNull(pathError)
        assertTrue(pathError!!.contains("/"))
        assertTrue(pathError.contains("\\"))
        assertTrue(pathError.contains("路径"))

        assertEquals(
            "名称不能包含换行、控制字符或 ::。",
            LauncherInputGuards.validateManualBackupName("foo::bar"),
        )
    }

    @Test
    fun `version target rejects unsafe patterns`() {
        assertEquals("版本不能以 - 开头。", LauncherInputGuards.validateVersionTarget("-beta"))
        assertEquals("版本不能包含连续两个点。", LauncherInputGuards.validateVersionTarget("main..bak"))
        assertEquals("版本不能包含连续斜杠。", LauncherInputGuards.validateVersionTarget("refs//heads/main"))
    }

    @Test
    fun `backup required name rejects empty or sanitized empty value`() {
        assertEquals("名称不能为空。", LauncherInputGuards.validateBackupRequiredName("   "))
        assertEquals(
            "名称过滤后会变成空，请换一个更明确的名字。",
            LauncherInputGuards.validateBackupRequiredName("..."),
        )
    }

    @Test
    fun `tavern port rejects non numeric or out of range value`() {
        assertEquals("端口不能为空。", LauncherInputGuards.validateTavernPort(" "))
        assertEquals("端口只能填写数字。", LauncherInputGuards.validateTavernPort("80a0"))
        assertEquals("端口必须在 1 到 65535 之间。", LauncherInputGuards.validateTavernPort("0"))
        assertEquals("端口必须在 1 到 65535 之间。", LauncherInputGuards.validateTavernPort("65536"))
        assertNull(LauncherInputGuards.validateTavernPort("8001"))
    }
}
