package moe.lukoa.launcher

import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernConfigSnapshot(
    val configPath: String = "",
    val exists: Boolean = false,
    val lastBackupPath: String = "",
    val listen: Boolean = false,
    val whitelistMode: Boolean = true,
    val whitelist: List<String> = listOf("::1", "127.0.0.1"),
    val basicAuthMode: Boolean = false,
    val basicAuthUsername: String = "user",
    val basicAuthHasPassword: Boolean = false,
    val enableUserAccounts: Boolean = false,
    val enableExtensions: Boolean = true,
    val enableServerPlugins: Boolean = false,
    val lazyLoadCharacters: Boolean = false,
    val chatBackupEnabled: Boolean = true,
    val chatMaxTotalBackups: Int = 50,
    val requestProxyEnabled: Boolean = false,
    val requestProxyUrl: String = "",
    val requestProxyBypass: List<String> = listOf("localhost", "127.0.0.1"),
)

data class TavernConfigPatch(
    val listen: Boolean,
    val whitelistMode: Boolean,
    val whitelist: List<String>,
    val basicAuthMode: Boolean,
    val basicAuthUsername: String,
    val updateBasicAuthPassword: Boolean = false,
    val basicAuthPassword: String = "",
    val enableUserAccounts: Boolean,
    val enableExtensions: Boolean,
    val enableServerPlugins: Boolean,
    val lazyLoadCharacters: Boolean,
    val chatBackupEnabled: Boolean,
    val chatMaxTotalBackups: Int,
    val requestProxyEnabled: Boolean,
    val requestProxyUrl: String,
    val requestProxyBypass: List<String>,
)

data class TavernConfigManagementState(
    val snapshot: TavernConfigSnapshot? = null,
    val loading: Boolean = false,
    val savedNeedsRestart: Boolean = false,
    val message: String = "尚未读取当前实例的 config.yaml 配置。",
)

object TavernConfigPolicy {
    val CHAT_BACKUP_LIMIT_PRESETS = listOf(20, 50, 100, 200, -1)
    private val PROXY_URL_REGEX = Regex("^(https?|socks4a?|socks5h?)://[^\\s]+$", RegexOption.IGNORE_CASE)

    fun parseLineList(raw: String): List<String> {
        return raw.lineSequence()
            .flatMap { it.split(',').asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    fun validatePatch(patch: TavernConfigPatch, currentlyHasPassword: Boolean): String? {
        if (patch.whitelist.any { it.length > 128 || it.contains('\u0000') || it.contains('\n') || it.contains('\r') }) {
            return "白名单条目包含无效字符或过长。"
        }
        if (patch.basicAuthMode) {
            val username = patch.basicAuthUsername.trim()
            if (username.isBlank() || username.length > 64 || username.any { it == '\n' || it == '\r' || it == '\u0000' }) {
                return "开启密码保护时，访问用户名不能为空且不能超过 64 个字符。"
            }
            if (patch.updateBasicAuthPassword) {
                if (patch.basicAuthPassword.isBlank() || patch.basicAuthPassword.length > 128) {
                    return "新访问密码不能为空且不能超过 128 个字符。"
                }
            } else if (!currentlyHasPassword) {
                return "开启密码保护前，请先设置一个访问密码。"
            }
        }
        if (patch.chatMaxTotalBackups != -1 && patch.chatMaxTotalBackups !in 5..500) {
            return "聊天滚动备份上限需设为 5–500，或设为不限（-1）。"
        }
        val proxyUrl = patch.requestProxyUrl.trim()
        if (patch.requestProxyEnabled && proxyUrl.isBlank()) {
            return "开启请求代理时，请填写代理地址（例如 http://127.0.0.1:7890 或 socks5://127.0.0.1:1080）。"
        }
        if (proxyUrl.isNotBlank() && !PROXY_URL_REGEX.matches(proxyUrl)) {
            return "代理地址格式不正确，支持 http://、https://、socks4://、socks5://。"
        }
        if (patch.requestProxyBypass.any { it.length > 128 || it.contains('\u0000') || it.contains('\n') || it.contains('\r') }) {
            return "代理白名单条目包含无效字符或过长。"
        }
        return null
    }

    fun securityWarnings(patch: TavernConfigPatch): List<String> = buildList {
        if (patch.listen && !patch.whitelistMode && !patch.basicAuthMode) {
            add("已开启局域网访问且关闭了白名单，建议同时开启“访问账号密码保护”，避免同 Wi-Fi 下其他设备直接进入酒馆。")
        }
        if (patch.listen && patch.whitelistMode && patch.whitelist.all { isLoopbackOnlyEntry(it) }) {
            add("当前白名单里只有本机地址（127.0.0.1 / ::1）。想让电脑或平板访问，请把对方 IP 或局域网段（如 192.168.1.*）加入白名单，或改用“密码保护”。")
        }
    }

    private fun isLoopbackOnlyEntry(entry: String): Boolean {
        val normalized = entry.trim().lowercase()
        return normalized == "127.0.0.1" || normalized == "::1" || normalized == "localhost"
    }
}

object TavernConfigCommandCodec {
    fun encodePatch(patch: TavernConfigPatch): String {
        val entries = buildList {
            add("\"listen\":${patch.listen}")
            add("\"whitelistMode\":${patch.whitelistMode}")
            add("\"whitelist\":${jsonStringArray(patch.whitelist)}")
            add("\"basicAuthMode\":${patch.basicAuthMode}")
            add("\"basicAuthUsername\":${jsonString(patch.basicAuthUsername.trim().ifBlank { "user" })}")
            if (patch.updateBasicAuthPassword && patch.basicAuthPassword.isNotBlank()) {
                add("\"updateBasicAuthPassword\":true")
                add("\"basicAuthPassword\":${jsonString(patch.basicAuthPassword)}")
            }
            add("\"enableUserAccounts\":${patch.enableUserAccounts}")
            add("\"enableExtensions\":${patch.enableExtensions}")
            add("\"enableServerPlugins\":${patch.enableServerPlugins}")
            add("\"lazyLoadCharacters\":${patch.lazyLoadCharacters}")
            add("\"chatBackupEnabled\":${patch.chatBackupEnabled}")
            add("\"chatMaxTotalBackups\":${patch.chatMaxTotalBackups}")
            add("\"requestProxyEnabled\":${patch.requestProxyEnabled}")
            add("\"requestProxyUrl\":${jsonString(patch.requestProxyUrl.trim())}")
            add("\"requestProxyBypass\":${jsonStringArray(patch.requestProxyBypass)}")
        }
        val json = "{${entries.joinToString(",")}}"
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    }

    fun isValidEncodedPayload(value: String?): Boolean {
        val trimmed = value.orEmpty().trim()
        if (trimmed.isBlank() || trimmed.length > 8_192) return false
        return try {
            val decoded = String(Base64.getUrlDecoder().decode(trimmed), StandardCharsets.UTF_8).trim()
            decoded.startsWith("{") && decoded.endsWith("}") && decoded.contains("\"listen\"")
        } catch (_: Exception) {
            false
        }
    }

    private fun jsonStringArray(items: List<String>): String {
        return items.joinToString(prefix = "[", postfix = "]", separator = ",") { jsonString(it) }
    }

    private fun jsonString(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (ch.code < 0x20) {
                    out.append("\\u%04x".format(ch.code))
                } else {
                    out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }
}

object TavernConfigOutputParser {
    private const val HEADER = "==== SillyTavern config ===="
    private const val FOOTER = "==== end SillyTavern config ===="

    fun parse(output: String): TavernConfigSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var configPath = ""
        var exists = false
        var backupPath = ""
        var listen = false
        var whitelistMode = true
        var whitelist = listOf("::1", "127.0.0.1")
        var basicAuthMode = false
        var basicAuthUsername = "user"
        var basicAuthHasPassword = false
        var enableUserAccounts = false
        var enableExtensions = true
        var enableServerPlugins = false
        var lazyLoadCharacters = false
        var chatBackupEnabled = true
        var chatMaxTotalBackups = 50
        var requestProxyEnabled = false
        var requestProxyUrl = ""
        var requestProxyBypass = listOf("localhost", "127.0.0.1")

        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val value = line.substringAfter('=', "")
            when {
                line.startsWith("config.path=") -> configPath = decode(value).orEmpty()
                line.startsWith("config.exists=") -> exists = value == "true"
                line.startsWith("config.backup=") -> backupPath = decode(value).orEmpty()
                line.startsWith("config.listen=") -> listen = value == "true"
                line.startsWith("config.whitelistMode=") -> whitelistMode = value != "false"
                line.startsWith("config.whitelist=") -> {
                    whitelist = decode(value).orEmpty().lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                }
                line.startsWith("config.basicAuthMode=") -> basicAuthMode = value == "true"
                line.startsWith("config.basicAuthUsername=") -> basicAuthUsername = decode(value).orEmpty().ifBlank { "user" }
                line.startsWith("config.basicAuthHasPassword=") -> basicAuthHasPassword = value == "true"
                line.startsWith("config.enableUserAccounts=") -> enableUserAccounts = value == "true"
                line.startsWith("config.enableExtensions=") -> enableExtensions = value != "false"
                line.startsWith("config.enableServerPlugins=") -> enableServerPlugins = value == "true"
                line.startsWith("config.lazyLoadCharacters=") -> lazyLoadCharacters = value == "true"
                line.startsWith("config.chatBackupEnabled=") -> chatBackupEnabled = value != "false"
                line.startsWith("config.chatMaxTotalBackups=") -> {
                    chatMaxTotalBackups = value.toIntOrNull() ?: 50
                }
                line.startsWith("config.requestProxyEnabled=") -> requestProxyEnabled = value == "true"
                line.startsWith("config.requestProxyUrl=") -> requestProxyUrl = decode(value).orEmpty()
                line.startsWith("config.requestProxyBypass=") -> {
                    requestProxyBypass = decode(value).orEmpty().lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                }
            }
        }

        return TavernConfigSnapshot(
            configPath = configPath,
            exists = exists,
            lastBackupPath = backupPath,
            listen = listen,
            whitelistMode = whitelistMode,
            whitelist = whitelist,
            basicAuthMode = basicAuthMode,
            basicAuthUsername = basicAuthUsername,
            basicAuthHasPassword = basicAuthHasPassword,
            enableUserAccounts = enableUserAccounts,
            enableExtensions = enableExtensions,
            enableServerPlugins = enableServerPlugins,
            lazyLoadCharacters = lazyLoadCharacters,
            chatBackupEnabled = chatBackupEnabled,
            chatMaxTotalBackups = chatMaxTotalBackups,
            requestProxyEnabled = requestProxyEnabled,
            requestProxyUrl = requestProxyUrl,
            requestProxyBypass = requestProxyBypass,
        )
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }

    private fun decode(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }
}

object LocalNetworkAddressResolver {
    fun localLanIpv4Addresses(): List<String> {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            val candidates = mutableListOf<String>()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let(candidates::add)
                    }
                }
            }
            filterPrivateIpv4Addresses(candidates)
        }.getOrDefault(emptyList())
    }

    internal fun filterPrivateIpv4Addresses(addresses: List<String>): List<String> {
        return addresses
            .map { it.trim() }
            .filter { isPrivateLanIpv4(it) }
            .distinct()
            .sortedWith(compareByDescending<String> { it.startsWith("192.168.") }.thenBy { it })
    }

    internal fun isPrivateLanIpv4(address: String): Boolean {
        val parts = address.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return when {
            octets[0] == 192 && octets[1] == 168 -> true
            octets[0] == 10 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            else -> false
        }
    }
}
