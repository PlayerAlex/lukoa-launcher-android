package moe.lukoa.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class TavernPhaseAFeaturesTest {
    @Test
    fun `launch mode toggles cleanly and defaults to foreground`() {
        assertEquals(TavernLaunchMode.ForegroundConsole, TavernLaunchMode.fromWireValue(null))
        assertEquals(TavernLaunchMode.ForegroundConsole, TavernLaunchMode.fromWireValue("unknown"))
        assertEquals(TavernLaunchMode.BackgroundSilent, TavernLaunchMode.fromWireValue("background"))
        assertEquals(TavernLaunchMode.BackgroundSilent, TavernLaunchMode.ForegroundConsole.toggled())
        assertEquals(TavernLaunchMode.ForegroundConsole, TavernLaunchMode.BackgroundSilent.toggled())
    }

    @Test
    fun `chat guard codec validates bounds and round trips`() {
        val config = TavernChatGuardConfig(
            enabled = true,
            wakeLockEnabled = false,
            minKilobytes = 512,
            intervalSeconds = 20,
        )
        val encoded = TavernChatGuardCommandCodec.encode(config)
        assertEquals("1|0|512|20", encoded)
        assertEquals(config, TavernChatGuardCommandCodec.decode(encoded))
        assertNull(TavernChatGuardCommandCodec.decode("2|1|256|10"))
        assertNull(TavernChatGuardCommandCodec.decode("1|1|8|10"))
        assertNull(TavernChatGuardCommandCodec.decode("1|1|256|1"))
    }

    @Test
    fun `chat guard parser parses status snapshot rescues and lightweight hint`() {
        val output = """
            {"status":"chat-guard-status"}
            ==== Lukoa chat guard ====
            chatGuard.enabled=1
            chatGuard.wakeLock=0
            chatGuard.minKb=512
            chatGuard.intervalSec=15
            chatGuard.running=1
            chatGuard.pid=4321
            chatGuard.linkedCount=18
            chatGuard.rescueCount=2
            chatGuard.rescue=2026-11-23T10:00:00Z	/data/chats/Alice/chat1.jsonl	/data/chats/Alice/chat1 [露科亚救回 1123-100000].jsonl	true	340
            chatGuard.rescue=2026-11-23T11:00:00Z	/data/chats/Bob/chat2.jsonl	/data/chats/Bob/chat2 [露科亚救回 1123-110000].jsonl	false	0
            ==== end Lukoa chat guard ====
        """.trimIndent()

        val snapshot = TavernChatGuardOutputParser.parse(output)
        assertNotNull(snapshot)
        snapshot!!
        assertTrue(snapshot.config.enabled)
        assertFalse(snapshot.config.wakeLockEnabled)
        assertEquals(512, snapshot.config.minKilobytes)
        assertEquals(15, snapshot.config.intervalSeconds)
        assertTrue(snapshot.running)
        assertEquals(4321, snapshot.pid)
        assertEquals(18, snapshot.linkedChatCount)
        assertEquals(2, snapshot.rescueCount)
        assertEquals(2, snapshot.rescues.size)
        // Newest rescue is listed first
        assertEquals("chat2", snapshot.rescues[0].chatDisplayName)
        assertFalse(snapshot.rescues[0].exists)
        assertEquals("chat1", snapshot.rescues[1].chatDisplayName)
        assertTrue(snapshot.rescues[1].exists)
        assertEquals(340L, snapshot.rescues[1].sizeKilobytes)

        assertEquals(3, TavernChatGuardOutputParser.parseRescueCountHint("status ok\nchatGuard.rescueCount=3\n"))
    }

    @Test
    fun `config policy validates proxy auth backup limits and emits lan warnings`() {
        val base = TavernConfigPatch(
            listen = true,
            whitelistMode = false,
            whitelist = listOf("127.0.0.1"),
            basicAuthMode = false,
            basicAuthUsername = "admin",
            enableUserAccounts = false,
            enableExtensions = true,
            enableServerPlugins = false,
            lazyLoadCharacters = true,
            chatBackupEnabled = true,
            chatMaxTotalBackups = 50,
            requestProxyEnabled = false,
            requestProxyUrl = "",
            requestProxyBypass = listOf("localhost"),
        )
        assertNull(TavernConfigPolicy.validatePatch(base, currentlyHasPassword = false))
        assertTrue(TavernConfigPolicy.securityWarnings(base).any { it.contains("密码保护") })

        val loopbackWhitelist = base.copy(whitelistMode = true, whitelist = listOf("::1", "127.0.0.1"))
        assertTrue(TavernConfigPolicy.securityWarnings(loopbackWhitelist).any { it.contains("白名单里只有本机地址") })

        val missingPassword = base.copy(basicAuthMode = true)
        assertNotNull(TavernConfigPolicy.validatePatch(missingPassword, currentlyHasPassword = false))
        assertNull(TavernConfigPolicy.validatePatch(missingPassword, currentlyHasPassword = true))

        val badProxy = base.copy(requestProxyEnabled = true, requestProxyUrl = "ftp://127.0.0.1:21")
        assertNotNull(TavernConfigPolicy.validatePatch(badProxy, currentlyHasPassword = false))

        val goodProxy = base.copy(requestProxyEnabled = true, requestProxyUrl = "socks5://127.0.0.1:1080")
        assertNull(TavernConfigPolicy.validatePatch(goodProxy, currentlyHasPassword = false))

        val badBackups = base.copy(chatMaxTotalBackups = 2)
        assertNotNull(TavernConfigPolicy.validatePatch(badBackups, currentlyHasPassword = false))
        assertNull(TavernConfigPolicy.validatePatch(base.copy(chatMaxTotalBackups = -1), currentlyHasPassword = false))
    }

    @Test
    fun `config codec and output parser round trip safely`() {
        val patch = TavernConfigPatch(
            listen = true,
            whitelistMode = true,
            whitelist = listOf("127.0.0.1", "192.168.1.*"),
            basicAuthMode = true,
            basicAuthUsername = "lukoa",
            updateBasicAuthPassword = true,
            basicAuthPassword = "secret",
            enableUserAccounts = true,
            enableExtensions = true,
            enableServerPlugins = true,
            lazyLoadCharacters = true,
            chatBackupEnabled = true,
            chatMaxTotalBackups = 100,
            requestProxyEnabled = true,
            requestProxyUrl = "http://127.0.0.1:7890",
            requestProxyBypass = listOf("localhost", "127.0.0.1"),
        )
        val encoded = TavernConfigCommandCodec.encodePatch(patch)
        assertTrue(TavernConfigCommandCodec.isValidEncodedPayload(encoded))
        assertFalse(TavernConfigCommandCodec.isValidEncodedPayload("not-valid-json"))

        fun b64(v: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray(StandardCharsets.UTF_8))
        val output = """
            ==== SillyTavern config ====
            config.path=${b64("/data/SillyTavern/config.yaml")}
            config.exists=true
            config.backup=${b64("/data/SillyTavern/config.yaml.lukoa-backup-20261123-190000")}
            config.listen=true
            config.whitelistMode=true
            config.whitelist=${b64("127.0.0.1\n192.168.1.*")}
            config.basicAuthMode=true
            config.basicAuthUsername=${b64("lukoa")}
            config.basicAuthHasPassword=true
            config.enableUserAccounts=true
            config.enableExtensions=true
            config.enableServerPlugins=true
            config.lazyLoadCharacters=true
            config.chatBackupEnabled=true
            config.chatMaxTotalBackups=100
            config.requestProxyEnabled=true
            config.requestProxyUrl=${b64("http://127.0.0.1:7890")}
            config.requestProxyBypass=${b64("localhost\n127.0.0.1")}
            ==== end SillyTavern config ====
        """.trimIndent()

        val parsed = TermuxStructuredOutputParser.parse(output).configSnapshot
        assertNotNull(parsed)
        parsed!!
        assertEquals("/data/SillyTavern/config.yaml", parsed.configPath)
        assertTrue(parsed.exists)
        assertTrue(parsed.listen)
        assertEquals(listOf("127.0.0.1", "192.168.1.*"), parsed.whitelist)
        assertTrue(parsed.basicAuthMode)
        assertEquals("lukoa", parsed.basicAuthUsername)
        assertTrue(parsed.basicAuthHasPassword)
        assertTrue(parsed.enableUserAccounts)
        assertTrue(parsed.enableServerPlugins)
        assertTrue(parsed.lazyLoadCharacters)
        assertEquals(100, parsed.chatMaxTotalBackups)
        assertTrue(parsed.requestProxyEnabled)
        assertEquals("http://127.0.0.1:7890", parsed.requestProxyUrl)
    }

    @Test
    fun `local network resolver filters private lan ipv4 addresses cleanly`() {
        val filtered = LocalNetworkAddressResolver.filterPrivateIpv4Addresses(
            listOf("127.0.0.1", "8.8.8.8", "10.0.0.5", "192.168.31.42", "172.16.0.9", "172.32.0.1"),
        )
        assertEquals(listOf("192.168.31.42", "10.0.0.5", "172.16.0.9"), filtered)
    }

    @Test
    fun `command presentation and timeout policies cover phase a commands`() {
        listOf(
            "restart",
            "tavern-chat-guard-status",
            "tavern-chat-guard-set",
            "tavern-chat-guard-clear-history",
            "tavern-config-read",
            "tavern-config-set",
        ).forEach { command ->
            assertEquals(
                TermuxCommandPresentation.Background,
                TermuxCommandPresentationPolicy.forCommand(command),
            )
            assertTrue(TermuxCommandTimeoutPolicy.hasExplicitTimeout(command))
        }
    }
}
