package moe.lukoa.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernTermuxResultProfileScopeTest {
    @Test
    fun `result from another profile cannot update active profile`() {
        val result = TermuxResultDisplay(
            key = "other",
            command = "tavern-version",
            output = "profile_id=profile-2",
            ok = true,
            profileId = "profile-2",
        )

        assertFalse(
            TavernTermuxResultProfileScope.matches(
                profileId = "main",
                result = result,
                requireMetadata = false,
            ),
        )
    }

    @Test
    fun `legacy result without metadata is allowed for passive sync only`() {
        val result = TermuxResultDisplay(
            key = "legacy",
            command = "status",
            output = "running=false",
            ok = true,
        )

        assertTrue(
            TavernTermuxResultProfileScope.matches(
                profileId = "main",
                result = result,
                requireMetadata = false,
            ),
        )
        assertFalse(
            TavernTermuxResultProfileScope.matches(
                profileId = "main",
                result = result,
                requireMetadata = true,
            ),
        )
    }
}
