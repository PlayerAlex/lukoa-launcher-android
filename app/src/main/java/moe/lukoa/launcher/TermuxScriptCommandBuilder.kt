package moe.lukoa.launcher

data class TermuxScriptCommandPlan(
    val command: String,
    val stdin: String,
)

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
        )
    }

    fun installAndRun(
        scriptText: String,
        scriptCommand: String,
        scriptArgs: List<String>,
        runtimeSetup: String,
    ): TermuxScriptCommandPlan {
        require(scriptCommand.isNotBlank()) { "script command cannot be blank" }
        val quotedArgs = (listOf(scriptCommand) + scriptArgs).joinToString(" ") { shellSingleQuoted(it) }
        return buildPlan(
            scriptText = scriptText,
            runtimeSetup = runtimeSetup,
            finalCommand = "exec \"\$target_script\" $quotedArgs",
        )
    }

    private fun buildPlan(
        scriptText: String,
        runtimeSetup: String,
        finalCommand: String,
    ): TermuxScriptCommandPlan {
        val normalized = scriptText.replace("\r\n", "\n").replace("\r", "\n")
        require(normalized.isNotBlank()) { "script cannot be blank" }
        require(!normalized.contains('\u0000')) { "script cannot contain nul bytes" }
        val command = buildString {
            appendLine("set -eu")
            appendLine("mkdir -p \"\$HOME/.local/bin\" \"\$HOME/.local/state/lukoa-launcher\" \"\$HOME/.config/lukoa-launcher\" \"\$HOME/.termux\"")
            appendLine("target_script=\"\$HOME/.local/bin/lukoa-tavern.sh\"")
            appendLine("staged_script=\"\$HOME/.local/bin/.lukoa-tavern.sh.\$\$\"")
            appendLine("cleanup_staged_script() { rm -f \"\$staged_script\"; }")
            appendLine("trap cleanup_staged_script EXIT HUP INT TERM")
            appendLine("umask 077")
            appendLine("cat > \"\$staged_script\"")
            appendLine("test -s \"\$staged_script\"")
            appendLine("chmod 700 \"\$staged_script\"")
            appendLine("mv -f \"\$staged_script\" \"\$target_script\"")
            appendLine("trap - EXIT HUP INT TERM")
            runtimeSetup.trim().takeIf { it.isNotBlank() }?.let(::appendLine)
            appendLine(finalCommand)
        }
        return TermuxScriptCommandPlan(command = command, stdin = normalized)
    }

    private fun shellSingleQuoted(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }
}
