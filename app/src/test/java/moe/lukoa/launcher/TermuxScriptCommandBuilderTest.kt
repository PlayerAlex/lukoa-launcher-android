package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxScriptCommandBuilderTest {
    @Test
    fun `large bundled script travels through stdin instead of command arguments`() {
        val script = "#!/bin/sh\r\n" + "echo extension-data\r\n".repeat(10_000)

        val plan = TermuxScriptCommandBuilder.installAndRun(
            scriptText = script,
            scriptCommand = "tavern-doctor",
            scriptArgs = emptyList(),
            runtimeSetup = "printf setup",
        )

        assertTrue(plan.stdin.length > 200_000)
        assertTrue(plan.command.length < 4_096)
        assertFalse(plan.command.contains("extension-data"))
        assertEquals(script.replace("\r\n", "\n"), plan.stdin)
        assertTrue(plan.command.contains("cat > \"\$staged_script\""))
        assertTrue(plan.command.contains("mv -f \"\$staged_script\" \"\$target_script\""))
        assertTrue(plan.command.contains("exec \"\$target_script\" 'tavern-doctor'"))
    }

    @Test
    fun `install plan selftests the stdin script with the requested nonce`() {
        val plan = TermuxScriptCommandBuilder.install(
            scriptText = "#!/bin/sh\necho ok\n",
            nonce = "abc123",
            runtimeSetup = "printf setup",
        )

        assertEquals("#!/bin/sh\necho ok\n", plan.stdin)
        assertTrue(plan.command.contains("\"\$target_script\" selftest 'abc123'"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `script transport rejects nul bytes`() {
        TermuxScriptCommandBuilder.installAndRun(
            scriptText = "#!/bin/sh\u0000echo unsafe",
            scriptCommand = "status",
            scriptArgs = emptyList(),
            runtimeSetup = "",
        )
    }
}
