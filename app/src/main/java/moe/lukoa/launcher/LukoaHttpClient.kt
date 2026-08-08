package moe.lukoa.launcher

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LukoaHttpClient internal constructor(
    private val userAgent: String,
    private val connectTimeoutMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) {
    constructor(context: Context) : this(
        userAgent = "LukoaLauncher/${VersionBackupManager.versionInfo(context).versionName}",
        connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS,
        readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS,
    )

    fun getText(
        url: String,
        accept: String = "*/*",
    ): String {
        val connection = openConnection(url, accept = accept)
        return connection.useConnection {
            val code = it.responseCode
            val stream = if (code in 200..299) it.inputStream else it.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${body.take(120)}")
            }
            body
        }
    }

    fun downloadToFile(
        url: String,
        file: File,
        accept: String = "application/octet-stream",
    ) {
        val connection = openConnection(url, accept = accept)
        connection.useConnection {
            val code = it.responseCode
            if (code !in 200..299) {
                val body = it.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() }.orEmpty()
                throw IllegalStateException("HTTP $code: ${body.take(120)}")
            }
            it.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun openConnection(
        url: String,
        accept: String,
        redirectCount: Int = 0,
    ): HttpURLConnection {
        if (redirectCount > MAX_REDIRECTS) {
            throw IllegalStateException("重定向次数过多。")
        }
        val connection = connectionFactory(URL(url)).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            setRequestProperty("Accept", accept)
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", userAgent)
        }
        val code = connection.responseCode
        if (code in REDIRECT_CODES) {
            val location = connection.getHeaderField("Location")
                ?: throw IllegalStateException("重定向缺少地址。")
            connection.disconnect()
            return openConnection(
                url = URL(URL(url), location).toString(),
                accept = accept,
                redirectCount = redirectCount + 1,
            )
        }
        return connection
    }

    private companion object {
        val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM,
            HttpURLConnection.HTTP_MOVED_TEMP,
            HttpURLConnection.HTTP_SEE_OTHER,
            307,
            308,
        )
        const val MAX_REDIRECTS = 5
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 12_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 30_000
    }
}

private inline fun <T : HttpURLConnection, R> T.useConnection(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}
