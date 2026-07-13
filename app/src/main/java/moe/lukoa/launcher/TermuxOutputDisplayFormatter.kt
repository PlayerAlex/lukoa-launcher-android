package moe.lukoa.launcher

object TermuxOutputDisplayFormatter {
    fun format(result: TermuxCommandResult): String {
        val directOutput = buildList {
            result.stdout.trim().takeIf { it.isNotBlank() }?.let(::add)
            result.stderr.trim().takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString("\n")
        if (directOutput.isNotBlank()) {
            return TermuxOutputDisplaySanitizer.sanitize(directOutput)
        }

        if (result.raw.isNotBlank()) {
            return TermuxOutputDisplaySanitizer.sanitize(result.raw.trim())
        }

        val fallback = buildList {
            if (!result.hasResultBundle) {
                add("未收到 Termux 返回包。")
            }
            if (result.hasInternalError) {
                add("Termux 内部错误：${result.errMessage.ifBlank { result.errCode.toString() }}")
            }
            if (result.errCode == 150 || result.errMessage.contains("executable regular file not found", ignoreCase = true)) {
                add("未找到 Termux 脚本，请重新打开启动器。")
            }
            if (TermuxPermissionSignals.externalAppsBlocked(result.errMessage + "\n" + result.raw)) {
                add("Termux 外部调用未开启。请在启动器权限引导里复制命令，到 Termux 粘贴执行。")
            }
            add(if (result.exitCode != null) "exitCode=${result.exitCode}" else "缺少 exitCode。")
        }.joinToString("\n")
        return TermuxOutputDisplaySanitizer.sanitize(fallback)
    }
}
