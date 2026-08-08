package moe.lukoa.launcher

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherUpdateFileTransactionTest {
    @Test
    fun `failed download removes part and preserves existing apk`() {
        withTempDirectory { directory ->
            val existing = File(directory, "launcher.apk").apply { writeText("old") }

            runCatching {
                LauncherUpdateFileTransaction.download(directory, "launcher.apk", download = { part ->
                    part.writeText("partial")
                    error("network")
                }, validate = {})
            }

            assertEquals("old", existing.readText())
            assertFalse(File(directory, ".launcher.apk.part").exists())
        }
    }

    @Test
    fun `failed validation removes part and preserves existing apk`() {
        withTempDirectory { directory ->
            val existing = File(directory, "launcher.apk").apply { writeText("old") }

            runCatching {
                LauncherUpdateFileTransaction.download(directory, "launcher.apk", download = { part ->
                    part.writeText("invalid")
                }, validate = { error("invalid apk") })
            }

            assertEquals("old", existing.readText())
            assertFalse(File(directory, ".launcher.apk.part").exists())
        }
    }

    @Test
    fun `successful validation atomically replaces target and prunes stale files`() {
        withTempDirectory { directory ->
            File(directory, "launcher.apk").writeText("old")
            File(directory, "stale.apk").writeText("stale")
            File(directory, ".stale.apk.part").writeText("partial")

            val result = LauncherUpdateFileTransaction.download(
                directory = directory,
                finalFileName = "launcher.apk",
                download = { it.writeText("new") },
                validate = { assertEquals("new", it.readText()) },
            )

            assertEquals("new", result.readText())
            assertEquals(listOf("launcher.apk"), directory.listFiles().orEmpty().map { it.name }.sorted())
        }
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("lukoa-update-test").toFile()
        try {
            assertTrue(directory.isDirectory)
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
