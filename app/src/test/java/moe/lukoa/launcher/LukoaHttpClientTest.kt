package moe.lukoa.launcher

import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LukoaHttpClientTest {
    @Test
    fun `text request follows relative redirect and sends configured headers`() {
        val requests = mutableListOf<FakeConnection>()
        val client = LukoaHttpClient(
            userAgent = "LukoaTest/1",
            connectionFactory = { url ->
                FakeConnection(
                    url = url,
                    code = if (url.path == "/redirect") 302 else 200,
                    body = if (url.path == "/text") "ok" else "",
                    location = if (url.path == "/redirect") "/text" else null,
                ).also(requests::add)
            },
        )

        assertEquals("ok", client.getText("https://example.test/redirect", "application/json"))
        assertEquals(listOf("/redirect", "/text"), requests.map { it.url.path })
        assertEquals("LukoaTest/1", requests.last().getRequestProperty("User-Agent"))
        assertEquals("application/json", requests.last().getRequestProperty("Accept"))
        assertTrue(requests.all { it.disconnected })
    }

    @Test
    fun `download writes exact response and http errors include status`() {
        val client = LukoaHttpClient(
            userAgent = "LukoaTest/1",
            connectionFactory = { url ->
                if (url.path == "/apk") {
                    FakeConnection(url, 200, byteArrayOf(1, 2, 3, 4))
                } else {
                    FakeConnection(url, 503, "unavailable")
                }
            },
        )
        val directory = Files.createTempDirectory("lukoa-http-test").toFile()
        try {
            val target = File(directory, "download.apk")
            client.downloadToFile("https://example.test/apk", target)
            assertTrue(target.readBytes().contentEquals(byteArrayOf(1, 2, 3, 4)))

            val failure = runCatching { client.getText("https://example.test/error") }.exceptionOrNull()
            assertTrue(failure?.message.orEmpty().contains("HTTP 503"))
        } finally {
            directory.deleteRecursively()
        }
    }
}

private class FakeConnection(
    url: URL,
    private val code: Int,
    private val response: ByteArray,
    private val location: String? = null,
) : HttpURLConnection(url) {
    constructor(url: URL, code: Int, body: String, location: String? = null) : this(
        url,
        code,
        body.toByteArray(),
        location,
    )

    var disconnected: Boolean = false
        private set

    override fun getResponseCode(): Int = code

    override fun getInputStream() = ByteArrayInputStream(response)

    override fun getErrorStream() = ByteArrayInputStream(response)

    override fun getHeaderField(name: String?): String? {
        return if (name.equals("Location", ignoreCase = true)) location else null
    }

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() = Unit
}
