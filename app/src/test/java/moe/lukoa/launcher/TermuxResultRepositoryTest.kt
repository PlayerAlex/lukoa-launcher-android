package moe.lukoa.launcher

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxResultRepositoryTest {
    @Test
    fun `receiver ingress notifies current command waiter`() = runBlocking {
        val persisted = mutableListOf<TermuxCommandResult>()
        val repository = TermuxResultRepository(
            loadPersistedResults = { persisted.toList() },
            persistResult = { persisted.add(0, it) },
        )
        val request = request(executionId = 42, command = "tavern-update")
        val waiting = async(start = CoroutineStart.UNDISPATCHED) {
            repository.awaitResult(request, timeoutMillis = 1_000L)
        }
        val result = result(executionId = 42, command = "tavern-update")

        repository.receive(result)

        assertSame(result, waiting.await())
        assertEquals(listOf(result), persisted)
    }

    @Test
    fun `matching execution id still rejects conflicting command text`() {
        val request = request(executionId = 9, command = "tavern-backup")

        assertFalse(
            TermuxResultMatcher.matches(
                result(executionId = 9, command = "legacy-display-command"),
                request,
            ),
        )
        assertTrue(
            TermuxResultMatcher.matches(
                result(executionId = 9, command = "tavern-backup"),
                request,
            ),
        )
        assertFalse(
            TermuxResultMatcher.matches(
                result(executionId = 10, command = "tavern-backup"),
                request,
            ),
        )
    }

    @Test
    fun `missing execution id falls back to command and time`() {
        val request = request(executionId = 9, command = "tavern-backup", startTime = 100L)

        assertTrue(
            TermuxResultMatcher.matches(
                result(executionId = 0, command = "tavern-backup", time = 100L),
                request,
            ),
        )
        assertFalse(
            TermuxResultMatcher.matches(
                result(executionId = 0, command = "tavern-backup", time = 99L),
                request,
            ),
        )
        assertFalse(
            TermuxResultMatcher.matches(
                result(executionId = 0, command = "tavern-restore", time = 100L),
                request,
            ),
        )
    }

    @Test
    fun `nonce must match dedicated field or compatible output`() {
        val request = request(executionId = 3, command = "selftest", nonce = "nonce-123")

        assertTrue(
            TermuxResultMatcher.matches(
                result(executionId = 3, command = "selftest", nonce = "nonce-123"),
                request,
            ),
        )
        assertTrue(
            TermuxResultMatcher.matches(
                result(executionId = 3, command = "selftest", stdout = "ok nonce-123"),
                request,
            ),
        )
        assertFalse(
            TermuxResultMatcher.matches(
                result(executionId = 3, command = "selftest", nonce = "old-nonce"),
                request,
            ),
        )
    }

    @Test
    fun `reused execution id cannot accept an older nonce`() {
        val request = request(
            executionId = 3,
            command = "tavern-update",
            nonce = "new-nonce",
        )

        assertFalse(
            TermuxResultMatcher.matches(
                result(
                    executionId = 3,
                    command = "tavern-update",
                    nonce = "old-nonce",
                ),
                request,
            ),
        )
    }

    @Test
    fun `await times out when no matching result arrives`() = runBlocking {
        val repository = TermuxResultRepository(
            loadPersistedResults = { emptyList() },
            persistResult = {},
        )

        val result = repository.awaitResult(
            request = request(executionId = 1, command = "status"),
            timeoutMillis = 30L,
        )

        assertNull(result)
    }

    @Test
    fun `cold start restores matching persisted result`() = runBlocking {
        val persisted = result(executionId = 77, command = "tavern-restore", time = 500L)
        val repository = TermuxResultRepository(
            loadPersistedResults = { listOf(persisted) },
            persistResult = {},
        )

        val restored = repository.awaitResult(
            request = request(executionId = 77, command = "tavern-restore", startTime = 400L),
            timeoutMillis = 1_000L,
        )

        assertSame(persisted, restored)
    }

    private fun request(
        executionId: Int,
        command: String,
        startTime: Long = 100L,
        nonce: String? = null,
    ) = TermuxResultRequest(
        executionId = executionId,
        startTimeMillis = startTime,
        expectedCommand = command,
        nonce = nonce,
    )

    private fun result(
        executionId: Int,
        command: String,
        time: Long = 100L,
        nonce: String? = null,
        stdout: String = "",
    ) = TermuxCommandResult(
        executionId = executionId,
        command = command,
        nonce = nonce,
        hasResultBundle = true,
        timeMillis = time,
        stdout = stdout,
        stderr = "",
        exitCode = 0,
        errCode = null,
        errMessage = "",
        stdoutOriginalLength = stdout.length.toString(),
        stderrOriginalLength = "0",
        raw = "",
    )
}
