package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class TermuxResultRequest(
    val executionId: Int,
    val startTimeMillis: Long,
    val expectedCommand: String,
    val nonce: String? = null,
)

object TermuxResultMatcher {
    fun matches(result: TermuxCommandResult, request: TermuxResultRequest): Boolean {
        if (result.timeMillis < request.startTimeMillis) return false
        if (result.executionId == request.executionId) {
            if (
                request.expectedCommand.isNotBlank() &&
                result.command.isNotBlank() &&
                result.command != request.expectedCommand
            ) {
                return false
            }
            return matchesNonce(result, request.nonce)
        }
        if (result.executionId != 0) return false
        if (request.expectedCommand.isBlank() || result.command != request.expectedCommand) return false
        return matchesNonce(result, request.nonce)
    }

    private fun matchesNonce(result: TermuxCommandResult, nonce: String?): Boolean {
        if (nonce == null) return true
        if (result.nonce == nonce) return true
        return sequenceOf(result.stdout, result.stderr, result.raw).any { it.contains(nonce) }
    }
}

class TermuxResultRepository(
    private val loadPersistedResults: suspend () -> List<TermuxCommandResult>,
    private val persistResult: suspend (TermuxCommandResult) -> Unit,
) {
    private val liveResults = MutableSharedFlow<TermuxCommandResult>(
        replay = LIVE_RESULT_REPLAY,
        extraBufferCapacity = LIVE_RESULT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun receive(result: TermuxCommandResult) {
        persistResult(result)
        liveResults.emit(result)
    }

    suspend fun awaitResult(
        request: TermuxResultRequest,
        timeoutMillis: Long,
    ): TermuxCommandResult? {
        return withTimeoutOrNull(timeoutMillis.coerceAtLeast(1L)) {
            coroutineScope {
                val liveResult = async(start = CoroutineStart.UNDISPATCHED) {
                    liveResults.first { TermuxResultMatcher.matches(it, request) }
                }
                val persistedResult = loadPersistedResults()
                    .firstOrNull { TermuxResultMatcher.matches(it, request) }
                if (persistedResult != null) {
                    liveResult.cancelAndJoin()
                    persistedResult
                } else {
                    liveResult.await()
                }
            }
        }
    }

    private companion object {
        const val LIVE_RESULT_REPLAY = 1
        const val LIVE_RESULT_BUFFER = 1
    }
}

object TermuxResultRepositoryProvider {
    @Volatile
    private var instance: TermuxResultRepository? = null

    fun get(context: Context): TermuxResultRepository {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: TermuxResultRepository(
                loadPersistedResults = {
                    withContext(Dispatchers.IO) { TermuxResultStore.recent(appContext) }
                },
                persistResult = { result ->
                    withContext(Dispatchers.IO) { TermuxResultStore.save(appContext, result) }
                },
            ).also { instance = it }
        }
    }
}

object TermuxResultIngress {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(
        context: Context,
        intent: Intent?,
        onComplete: () -> Unit = {},
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val result = TermuxResultParser.parse(intent)
                TermuxResultRepositoryProvider.get(appContext).receive(result)
                AutoBackupRetentionManager.enqueueAfterTermuxResult(appContext, result)
            } finally {
                onComplete()
            }
        }
    }
}
