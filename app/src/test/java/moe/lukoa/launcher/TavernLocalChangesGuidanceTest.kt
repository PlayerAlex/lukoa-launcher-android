package moe.lukoa.launcher

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
}
