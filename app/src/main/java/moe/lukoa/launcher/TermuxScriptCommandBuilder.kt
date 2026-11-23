package moe.lukoa.launcher

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

data class TermuxScriptCommandPlan(
    val command: String,
    val stdin: String?,
)

enum class TermuxScriptTransport {
    Stdin,
    CompressedArgument,
}

object TermuxScriptCommandBuilder {
    fun install(
        scriptText: String,
        nonce: String,
        runtimeSetup: String,
    ): TermuxScriptCommandPlan {
        require(nonce.isNotBlank()) { "selftest nonce cannot be blank" }
        return buildPlan(
            scriptText = scriptText,
            runtimeSetup = runtimeSetup,
            finalCommand = "\"\$target_script\" selftest ${shellSingleQuoted(nonce)}",
            transport = TermuxScriptTransport.Stdin,
        )
    }

    fun installAndRun(
        scriptText: String,
        scriptCommand: String,
        scriptArgs: List<String>,
        runtimeSetup: String,
        transport: TermuxScriptTransport,
        scriptFileName: String = "lukoa-tavern.sh",
    ): TermuxScriptCommandPlan {
        require(scriptCommand.isNotBlank()) { "script command cannot be blank" }
        val quotedArgs = (listOf(scriptCommand) + scriptArgs).joinToString(" ") { shellSingleQuoted(it) }
        return buildPlan(
            scriptText = scriptText,
            runtimeSetup = runtimeSetup,
            finalCommand = "exec \"\$target_script\" $quotedArgs",
            transport = transport,
            scriptFileName = scriptFileName,
        )
    }

    private fun buildPlan(
        scriptText: String,
        runtimeSetup: String,
        finalCommand: String,
        transport: TermuxScriptTransport,
        scriptFileName: String = "lukoa-tavern.sh",
    ): TermuxScriptCommandPlan {
        val normalized = scriptText.replace("\r\n", "\n").replace("\r", "\n")
        require(normalized.isNotBlank()) { "script cannot be blank" }
        require(!normalized.contains('\u0000')) { "script cannot contain nul bytes" }
        val writeCommand = when (transport) {
            TermuxScriptTransport.Stdin -> "cat > \"\$staged_script\""
            TermuxScriptTransport.CompressedArgument -> {
                val payload = gzipBase64(normalized)
                "printf '%s' ${shellSingleQuoted(payload)} | base64 -d | gzip -dc > \"\$staged_script\""
            }
        }
        val command = buildString {
            appendLine("set -eu")
            appendLine("mkdir -p \"\$HOME/.local/bin\" \"\$HOME/.local/state/lukoa-launcher\" \"\$HOME/.config/lukoa-launcher\" \"\$HOME/.termux\"")
            appendLine("target_script=\"\$HOME/.local/bin/$scriptFileName\"")
            appendLine("staged_script=\"\$HOME/.local/bin/.$scriptFileName.\$\$\"")
            appendLine("cleanup_staged_script() { rm -f \"\$staged_script\"; }")
            appendLine("trap cleanup_staged_script EXIT HUP INT TERM")
            appendLine("umask 077")
            appendLine(writeCommand)
            appendLine("test -s \"\$staged_script\"")
            appendLine("chmod 700 \"\$staged_script\"")
            appendLine("mv -f \"\$staged_script\" \"\$target_script\"")
            appendLine("trap - EXIT HUP INT TERM")
            runtimeSetup.trim().takeIf { it.isNotBlank() }?.let(::appendLine)
            appendLine(finalCommand)
        }
        if (transport == TermuxScriptTransport.CompressedArgument) {
            require(command.toByteArray(Charsets.UTF_8).size < MAX_COMPRESSED_COMMAND_BYTES) {
                "compressed foreground command exceeds the safe argument limit"
            }
        }
        return TermuxScriptCommandPlan(
            command = command,
            stdin = normalized.takeIf { transport == TermuxScriptTransport.Stdin },
        )
    }

    private fun gzipBase64(value: String): String {
        val cached = cachedCompressedScript
        if (cached != null && cached.first == value) {
            return cached.second
        }
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(Charsets.UTF_8).use { it.write(value) }
        val encoded = Base64.getEncoder().encodeToString(output.toByteArray())
        cachedCompressedScript = value to encoded
        return encoded
    }

    private fun shellSingleQuoted(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    @Volatile
    private var cachedCompressedScript: Pair<String, String>? = null

    private const val MAX_COMPRESSED_COMMAND_BYTES = 65_536
}
