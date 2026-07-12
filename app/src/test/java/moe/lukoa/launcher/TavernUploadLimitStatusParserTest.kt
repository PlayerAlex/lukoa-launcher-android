package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TavernUploadLimitStatusParserTest {
    @Test
    fun `parser reads current value and active patch details`() {
        val parsed = TavernUploadLimitStatusParser.parse(
            """
            ==== SillyTavern upload limit ====
            uploadLimit.currentMb=1024
            uploadLimit.recordedPreviousMb=500
            uploadLimit.recordedAppliedMb=1024
            uploadLimit.recordedCommit=abc123
            uploadLimit.patchState=active
            ==== end SillyTavern upload limit ====
            """.trimIndent(),
        )
        assertEquals(1024, parsed?.currentMegabytes)
        assertEquals(500, parsed?.recordedPreviousMegabytes)
        assertEquals(TavernUploadLimitPatchState.Active, parsed?.patchState)
        assertEquals("abc123", parsed?.recordedCommit)
    }

    @Test
    fun `parser rejects unrelated or incomplete output`() {
        assertNull(TavernUploadLimitStatusParser.parse("uploadLimit.currentMb=1024"))
        assertNull(TavernUploadLimitStatusParser.parse("==== SillyTavern upload limit ====\nuploadLimit.patchState=active"))
    }
}
