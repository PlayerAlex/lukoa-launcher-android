package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TavernUserManagementTest {
    @Test
    fun `parser reads encoded user records`() {
        fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val parsed = TavernUserOutputParser.parse(
            """
            ==== SillyTavern users ====
            user.record=${encoded("default-user")}|${encoded("默认用户")}|true|true|true|2048
            ==== end SillyTavern users ====
            """.trimIndent(),
        )
        assertNotNull(parsed)
        assertEquals("默认用户", parsed?.single()?.name)
        assertEquals(2048L, parsed?.single()?.directoryKilobytes)
    }

    @Test
    fun `parser ignores unrelated output`() {
        assertNull(TavernUserOutputParser.parse("user.record=broken"))
    }

    @Test
    fun `input guards reject unsafe handles and names`() {
        assertNull(TavernUserCommandCodec.validateHandle("user-2"))
        assertNotNull(TavernUserCommandCodec.validateHandle("../user"))
        assertNotNull(TavernUserCommandCodec.validateHandle("User"))
        assertNull(TavernUserCommandCodec.validateName("新用户"))
        assertNotNull(TavernUserCommandCodec.validateName("bad\nname"))
    }
}
