package moe.lukoa.launcher

import java.io.File
import java.util.Base64
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            transport = TermuxScriptTransport.Stdin,
        )

        assertTrue(plan.stdin.orEmpty().length > 200_000)
        assertTrue(plan.command.length < 4_096)
        assertFalse(plan.command.contains("extension-data"))
        assertEquals(script.replace("\r\n", "\n"), plan.stdin)
        assertTrue(plan.command.contains("cat > \"\$staged_script\""))
        assertTrue(plan.command.contains("mv -f \"\$staged_script\" \"\$target_script\""))
        assertTrue(plan.command.contains("exec \"\$target_script\" 'tavern-doctor'"))
    }

    @Test
    fun `foreground terminal plan uses a bounded compressed argument without stdin`() {
        val script = File("src/main/assets/lukoa-tavern.sh").readText(Charsets.UTF_8)

        val plan = TermuxScriptCommandBuilder.installAndRun(
            scriptText = script,
            scriptCommand = "extensions-list",
            scriptArgs = emptyList(),
            runtimeSetup = "printf setup",
            transport = TermuxScriptTransport.CompressedArgument,
        )

        assertNull(plan.stdin)
        assertTrue(plan.command.toByteArray(Charsets.UTF_8).size < 65_536)
        assertFalse(plan.command.contains("run_tavern_extension_action"))
        assertTrue(plan.command.contains("base64 -d | gzip -dc > \"\$staged_script\""))
        assertTrue(plan.command.contains("exec \"\$target_script\" 'extensions-list'"))

        val payload = Regex("printf '%s' '([^']+)' \\| base64 -d").find(plan.command)
            ?.groupValues
            ?.get(1)
            ?: error("compressed payload missing")
        val restored = GZIPInputStream(Base64.getDecoder().decode(payload).inputStream())
            .reader(Charsets.UTF_8)
            .use { it.readText() }
        assertEquals(script.replace("\r\n", "\n").replace("\r", "\n"), restored)
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
            transport = TermuxScriptTransport.Stdin,
        )
    }
}
