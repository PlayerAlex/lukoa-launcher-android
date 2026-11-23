package moe.lukoa.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

enum class AutoStartDecision {
    Wait,
    SkipAlreadyRunning,
    SkipDisabledOrBlocked,
    StartTavern,
}

object TavernAutoStartPolicy {
    fun decide(
        enabledAtLaunch: Boolean,
        enabledNow: Boolean,
        alreadyHandled: Boolean,
        startupRefreshScheduled: Boolean,
        startupRefreshCompleted: Boolean,
        startupRefreshInFlight: Boolean,
        termuxReady: Boolean,
        tavernInstallDetected: Boolean?,
        tavernRunning: Boolean,
        tavernStarting: Boolean,
        localHttpResponding: Boolean,
        actionInProgress: Boolean,
    ): AutoStartDecision {
        if (!enabledAtLaunch || !enabledNow || alreadyHandled) {
            return AutoStartDecision.SkipDisabledOrBlocked
        }
        if (tavernRunning || tavernStarting || localHttpResponding) {
            return AutoStartDecision.SkipAlreadyRunning
        }
        if (!termuxReady) {
            return AutoStartDecision.SkipDisabledOrBlocked
        }
        if (startupRefreshInFlight || (startupRefreshScheduled && !startupRefreshCompleted) || actionInProgress) {
            return AutoStartDecision.Wait
        }
        if (tavernInstallDetected != true) {
            return AutoStartDecision.SkipDisabledOrBlocked
        }
        return AutoStartDecision.StartTavern
    }
}

object TavernLocalHttpProbe {
    suspend fun isTavernHttpResponding(
        port: Int,
        timeoutMs: Int = 450,
    ): Boolean = withContext(Dispatchers.IO) {
        if (port !in 1..65535) return@withContext false
        runCatching {
            Socket().use { socket ->
                socket.soTimeout = timeoutMs
                socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
                val out = socket.getOutputStream()
                out.write("GET / HTTP/1.0\r\nHost: 127.0.0.1:$port\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                out.flush()
                val firstLine = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII)).readLine()
                firstLine != null && firstLine.startsWith("HTTP/", ignoreCase = true)
            }
        }.getOrDefault(false)
    }
}
