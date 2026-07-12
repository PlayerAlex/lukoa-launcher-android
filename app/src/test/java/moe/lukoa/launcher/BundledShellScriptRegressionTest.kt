package moe.lukoa.launcher

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledShellScriptRegressionTest {
    private val script by lazy {
        File("src/main/assets/lukoa-tavern.sh").readText(Charsets.UTF_8)
    }

    @Test
    fun `node memory configuration does not source executable state`() {
        assertFalse(script.contains(". \"\$NODE_MEMORY_FILE\""))
        assertTrue(script.contains("2048|4096|6144"))
        assertTrue(script.contains("\${NODE_OPTIONS:+\$NODE_OPTIONS }--max-old-space-size="))
    }

    @Test
    fun `case blocks do not retain an if terminator`() {
        assertFalse(Regex("""esac\s*\nfi\b""").containsMatchIn(script))
    }

    @Test
    fun `upload patch protects update and rollback`() {
        assertTrue(script.contains("Managed upload limit could not be safely removed before update"))
        assertTrue(script.contains("Managed upload limit could not be safely removed before rollback"))
        assertTrue(script.contains("upload_limit_reapply_after_update"))
    }

    @Test
    fun `user management uses SillyTavern storage module and protects data`() {
        assertTrue(script.contains("await users.initUserStorage(dataRoot)"))
        assertTrue(script.contains("handle === 'default-user'"))
        assertTrue(script.contains("await storage.removeItem(users.toKey(handle))"))
        assertFalse(script.contains("fs.rmSync(users.getUserDirectories(handle).root"))
    }
}
