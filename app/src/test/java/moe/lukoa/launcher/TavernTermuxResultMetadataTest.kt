package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernTermuxResultMetadataTest {
    @Test
    fun `parser reads profile id and runtime state dir from json output`() {
        val metadata = TavernTermuxResultMetadataParser.parse(
            """
            {"profileId":"profile-2","runtimeStateDir":"/data/data/com.termux/files/home/.local/state/lukoa-launcher/profiles/profile-2"}
            """.trimIndent(),
        )

        assertEquals("profile-2", metadata.profileId)
        assertTrue(metadata.runtimeStateDir.endsWith("/profiles/profile-2"))
    }

    @Test
    fun `runtime state profile key matches shell sanitizing rule`() {
        assertTrue(
            TavernRuntimeStateProfileKey.matchesRuntimeStateDir(
                "profile 2",
                "/data/data/com.termux/files/home/.local/state/lukoa-launcher/profiles/profile_2",
            ),
        )
    }

    @Test
    fun `runtime state profile key keeps leading and trailing underscores like shell`() {
        assertEquals(
            "_profile_2_",
            TavernRuntimeStateProfileKey.sanitize("/profile 2/"),
        )
    }
}
