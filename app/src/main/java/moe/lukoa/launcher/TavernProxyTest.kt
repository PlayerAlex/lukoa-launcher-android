package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class TavernProxyTestResult(
    val ok: Boolean,
    val tcpOk: Boolean = false,
    val httpCode: String = "",
    val latencyMs: Int? = null,
    val detail: String = "",
)

data class TavernProxyTestState(
    val testing: Boolean = false,
    val lastTestedUrl: String = "",
    val result: TavernProxyTestResult? = null,
    val message: String = "",
)

object TavernProxyTestCommandCodec {
    private val PROXY_URL_REGEX = Regex("^(https?|socks4a?|socks5h?)://[^\\s]+$", RegexOption.IGNORE_CASE)

    fun validateProxyUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return "请先填写代理地址再测试（例如 http://127.0.0.1:7890）。"
        }
        if (trimmed.length > 256 || !PROXY_URL_REGEX.matches(trimmed)) {
            return "代理地址格式不正确，支持 http://、https://、socks4://、socks5://。"
        }
        return null
    }

    fun encode(url: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(url.trim().toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(encoded: String?): String? {
        val trimmed = encoded.orEmpty().trim()
        if (trimmed.isBlank() || trimmed.length > 512) return null
        return try {
            val decoded = String(Base64.getUrlDecoder().decode(trimmed), StandardCharsets.UTF_8).trim()
            decoded.takeIf { validateProxyUrl(it) == null }
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

object TavernProxyTestOutputParser {
    private const val HEADER = "==== Lukoa proxy test ===="
    private const val FOOTER = "==== end Lukoa proxy test ===="

    fun parse(output: String): TavernProxyTestResult? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        val block = output.substring(headerIndex, footerIndex + FOOTER.length)

        var ok = false
        var tcpOk = false
        var httpCode = ""
        var latencyMs: Int? = null
        var detail = ""

        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val value = line.substringAfter('=', "").trim()
            when {
                line.startsWith("proxy.ok=") -> ok = value == "true"
                line.startsWith("proxy.tcpOk=") -> tcpOk = value == "true"
                line.startsWith("proxy.httpCode=") -> httpCode = value
                line.startsWith("proxy.latencyMs=") -> latencyMs = value.toIntOrNull()?.takeIf { it >= 0 }
                line.startsWith("proxy.detail=") -> detail = decodeBase64Url(value).orEmpty()
            }
        }
        return TavernProxyTestResult(
            ok = ok,
            tcpOk = tcpOk,
            httpCode = httpCode,
            latencyMs = latencyMs,
            detail = detail,
        )
    }

    private fun decodeBase64Url(value: String): String? {
        if (value.isBlank()) return ""
        return try {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
