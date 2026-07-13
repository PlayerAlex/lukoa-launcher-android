package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherCommandCodecTest {
    @Test
    fun `decode separates command name from one structured argument`() {
        val command = LauncherCommandCodec.decode("tavern-update::encoded::payload")

        assertEquals("tavern-update", command.name)
        assertEquals("encoded::payload", command.argument)
    }

    @Test
    fun `decode command without argument keeps argument null`() {
        val command = LauncherCommandCodec.decode("status")

        assertEquals("status", command.name)
        assertNull(command.argument)
    }

    @Test
    fun `encode omits separator for blank argument`() {
        assertEquals("status", LauncherCommandCodec.encode("status"))
        assertEquals("status", LauncherCommandCodec.encode("status", "  "))
        assertEquals("tavern-restore::archive", LauncherCommandCodec.encode("tavern-restore", "archive"))
    }
}
