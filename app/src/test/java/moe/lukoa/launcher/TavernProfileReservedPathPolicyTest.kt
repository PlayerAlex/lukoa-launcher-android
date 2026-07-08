package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernProfileReservedPathPolicyTest {
    @Test
    fun `main profile keeps launcher managed root available`() {
        val message = TavernProfileReservedPathPolicy.reservedMessageForProfile(
            TavernPathConfig().activeProfile,
        )

        assertNull(message)
    }

    @Test
    fun `clone cannot use main launcher managed path`() {
        val profile = TavernPathConfig()
            .addSuggestedProfile()
            .activeProfile
            .copy(tavernDir = "~/LukoaLauncher/SillyTavern")

        val message = TavernProfileReservedPathPolicy.reservedMessageForProfile(profile)

        assertTrue(message.orEmpty().contains("会一直保留给主实例"))
    }

    @Test
    fun `clone candidate blocks main launcher managed path`() {
        val reason = TavernProfileReservedPathPolicy.candidateBlockedReason(
            activeProfile = TavernPathConfig().addSuggestedProfile().activeProfile,
            candidatePath = "/data/data/com.termux/files/home/LukoaLauncher/SillyTavern",
        )

        assertEquals("这个目录会一直保留给主实例，分身实例不能直接使用。", reason)
    }

    @Test
    fun `clone candidate also blocks disguised main launcher managed path`() {
        val reason = TavernProfileReservedPathPolicy.candidateBlockedReason(
            activeProfile = TavernPathConfig().addSuggestedProfile().activeProfile,
            candidatePath = "~/LukoaLauncher/../LukoaLauncher//SillyTavern/",
        )

        assertEquals("这个目录会一直保留给主实例，分身实例不能直接使用。", reason)
    }
}
