package moe.lukoa.launcher

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

class LukoaHttpClient(private val context: Context) {
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

    private fun openConnection(
        url: String,
        accept: String,
        redirectCount: Int = 0,
    ): HttpURLConnection {
        if (redirectCount > MAX_REDIRECTS) {
            throw IllegalStateException("重定向次数过多。")
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", accept)
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty(
                "User-Agent",
                "LukoaLauncher/${VersionBackupManager.versionInfo(context).versionName}",
            )
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
    }
}

private inline fun <T : HttpURLConnection, R> T.useConnection(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}
