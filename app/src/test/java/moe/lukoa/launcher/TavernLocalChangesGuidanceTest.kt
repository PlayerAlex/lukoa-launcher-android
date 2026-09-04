package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernLocalChangesGuidanceTest {
    @Test
    fun `managed upload limit is recognized as actionable local change`() {
        assertTrue(
            TavernLocalChangesGuidance.isLikelyUploadLimitChange(
                versionInfo = TavernVersionInfo(hasData = true, localChanges = "1"),
                uploadLimitStatus = TavernUploadLimitStatus(
                    currentMegabytes = 1024,
                    patchState = TavernUploadLimitPatchState.Active,
                ),
            ),
        )
    }

    @Test
    fun `server main preview is recognized before upload status is loaded`() {
        assertTrue(
            TavernLocalChangesGuidance.isLikelyUploadLimitChange(
                versionInfo = TavernVersionInfo(
                    hasData = true,
                    localChanges = "1",
                    changedFilesPreview = " M src/server-main.js",
                ),
                uploadLimitStatus = TavernUploadLimitStatus(),
            ),
        )
    }

    @Test
    fun `unrelated local changes keep generic recovery guidance`() {
        assertFalse(
            TavernLocalChangesGuidance.isLikelyUploadLimitChange(
                versionInfo = TavernVersionInfo(
                    hasData = true,
                    localChanges = "1",
                    changedFilesPreview = " M public/index.html",
                ),
                uploadLimitStatus = TavernUploadLimitStatus(),
            ),
        )
    }

    @Test
    fun `launcher managed files never need discard consent`() {
        val info = TavernVersionInfo(
            hasData = true,
            localChanges = "1",
            changedFiles = listOf("src/server-main.js", "package-lock.json"),
        )

        assertEquals(emptyList<String>(), TavernLocalChangesGuidance.userOwnedChanges(info))
        assertFalse(TavernLocalChangesGuidance.requiresDiscardConsent(info))
    }

    @Test
    fun `user edits require consent and are listed without status columns`() {
        val info = TavernVersionInfo(
            hasData = true,
            localChanges = "1",
            changedFiles = listOf("src/server-main.js", "start.sh", "default/config.yaml"),
        )

        assertEquals(listOf("start.sh", "default/config.yaml"), TavernLocalChangesGuidance.userOwnedChanges(info))
        assertTrue(TavernLocalChangesGuidance.requiresDiscardConsent(info))
    }

    @Test
    fun `unattributed changes are treated as needing consent`() {
        val info = TavernVersionInfo(hasData = true, localChanges = "1")

        assertTrue(TavernLocalChangesGuidance.requiresDiscardConsent(info))
    }

    @Test
    fun `porcelain lines are reduced to paths`() {
        assertEquals("src/server-main.js", TavernLocalChangesGuidance.pathFromStatusLine(" M src/server-main.js"))
        assertEquals("start.sh", TavernLocalChangesGuidance.pathFromStatusLine("MM start.sh"))
        assertEquals("new.sh", TavernLocalChangesGuidance.pathFromStatusLine("R  old.sh -> new.sh"))
        assertEquals("a b.txt", TavernLocalChangesGuidance.pathFromStatusLine(" M a b.txt"))
    }

    @Test
    fun `version parser keeps full changed file list and a short preview`() {
        val output = buildString {
            appendLine("directory=~/SillyTavern")
            appendLine("git.localChanges=1")
            appendLine("==== Git local changes ====")
            (1..6).forEach { appendLine(" M file$it.js") }
            appendLine("==== end Git local changes ====")
        }

        val info = TavernVersionParser.parse(output)

        assertEquals(6, info.changedFiles.size)
        assertEquals("file6.js", info.changedFiles.last())
        assertEquals(4, info.changedFilesPreview.lines().size)
        assertTrue(info.hasLocalChanges)
    }
}
