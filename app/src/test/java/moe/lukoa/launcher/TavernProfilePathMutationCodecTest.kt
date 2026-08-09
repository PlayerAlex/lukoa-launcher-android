package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TavernProfilePathMutationCodecTest {
    @Test
    fun `clone output reads exact nonblank target path`() {
        val output = """
            status=cloned-profile-dir
            clonedFrom=/data/data/com.termux/files/home/SillyTavern
            clonedTo=/data/data/com.termux/files/home/LukoaLauncher/SillyTavern2
        """.trimIndent()

        assertEquals(
            "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern2",
            TavernProfilePathMutationOutputParser.clonedTargetPath(output),
        )
    }

    @Test
    fun `clone output rejects missing or blank target`() {
        assertNull(TavernProfilePathMutationOutputParser.clonedTargetPath("status=error"))
        assertNull(TavernProfilePathMutationOutputParser.clonedTargetPath("clonedTo=   "))
    }
}
