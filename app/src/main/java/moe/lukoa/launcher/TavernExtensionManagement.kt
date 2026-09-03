package moe.lukoa.launcher

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

enum class TavernExtensionUpdateStatus(val wireValue: String) {
    NotGitRepository("not_git"),
    UnsupportedSource("unsupported_source"),
    NotChecked("not_checked"),
    UpToDate("up_to_date"),
    UpdateAvailable("update_available"),
    LocalChanges("local_changes"),
    CheckFailed("check_failed"),
    ;

    companion object {
        fun fromWireValue(value: String?): TavernExtensionUpdateStatus = when {
            value.isNullOrBlank() -> NotChecked
            else -> entries.firstOrNull { it.wireValue == value } ?: CheckFailed
        }
    }
}

data class TavernExtensionRecord(
    val directoryName: String,
    val displayName: String,
    val version: String,
    val hasManifest: Boolean,
    val author: String = "",
    val directoryKilobytes: Long? = null,
    val enabled: Boolean = true,
    val repositoryUrl: String = "",
    val currentRevision: String = "",
    val latestRevision: String = "",
    val updateStatus: TavernExtensionUpdateStatus = TavernExtensionUpdateStatus.NotChecked,
    val rollbackRevision: String = "",
)

data class TavernExtensionSnapshot(
    val rootDirectory: String,
    val disabledRootDirectory: String,
    val extensions: List<TavernExtensionRecord>,
)

data class TavernExtensionManagementState(
    val rootDirectory: String = "",
    val disabledRootDirectory: String = "",
    val extensions: List<TavernExtensionRecord> = emptyList(),
    val loading: Boolean = false,
    val message: String = "尚未读取当前酒馆的扩展。",
)

object TavernExtensionOutputParser {
    private const val HEADER = "==== SillyTavern extensions ===="
    private const val FOOTER = "==== end SillyTavern extensions ===="

    fun parse(output: String): TavernExtensionSnapshot? {
        val block = extractLastCompleteBlock(output) ?: return null
        var rootDirectory = ""
        var disabledRootDirectory = ""
        val extensions = mutableListOf<TavernExtensionRecord>()
        block.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("extension.root=") -> {
                    rootDirectory = decode(line.substringAfter('=')) ?: rootDirectory
                }

                line.startsWith("extension.disabledRoot=") -> {
                    disabledRootDirectory = decode(line.substringAfter('=')) ?: disabledRootDirectory
                }

                line.startsWith("extension.record=") -> {
                    val fields = line.substringAfter('=').split('|')
                    if (fields.size !in 4..12) return@forEach
                    val directoryName = decode(fields[0]) ?: return@forEach
                    if (TavernExtensionCommandCodec.validateDirectoryName(directoryName) != null) return@forEach
                    extensions += TavernExtensionRecord(
                        directoryName = directoryName,
                        displayName = decode(fields[1]).orEmpty().ifBlank { directoryName },
                        version = decode(fields[2]).orEmpty(),
                        hasManifest = fields[3] == "true",
                        author = fields.getOrNull(4)?.let(::decode).orEmpty(),
                        directoryKilobytes = fields.getOrNull(5)
                            ?.toLongOrNull()
                            ?.takeIf { it >= 0L },
                        enabled = fields.getOrNull(6) != "false",
                        repositoryUrl = fields.getOrNull(7)?.let(::decode).orEmpty(),
                        currentRevision = fields.getOrNull(8).orEmpty().takeIf(::isGitRevision).orEmpty(),
                        latestRevision = fields.getOrNull(9).orEmpty().takeIf(::isGitRevision).orEmpty(),
                        updateStatus = TavernExtensionUpdateStatus.fromWireValue(fields.getOrNull(10)),
                        rollbackRevision = fields.getOrNull(11).orEmpty().takeIf(::isGitRevision).orEmpty(),
                    )
                }
            }
        }
        return TavernExtensionSnapshot(
            rootDirectory = rootDirectory,
            disabledRootDirectory = disabledRootDirectory,
            extensions = extensions.sortedWith(
                compareByDescending<TavernExtensionRecord> { it.enabled }
                    .thenBy { it.displayName.lowercase() },
            ),
        )
    }

    private fun extractLastCompleteBlock(output: String): String? {
        val headerIndex = output.lastIndexOf(HEADER)
        if (headerIndex < 0) return null
        val footerIndex = output.indexOf(FOOTER, startIndex = headerIndex + HEADER.length)
        if (footerIndex < 0) return null
        return output.substring(headerIndex, footerIndex + FOOTER.length)
    }

    private fun decode(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun isGitRevision(value: String): Boolean =
        value.length in 7..40 && value.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }
}

object TavernExtensionActionPolicy {
    fun updateDisabledReason(
        extension: TavernExtensionRecord,
        actionsLocked: Boolean,
        tavernRunning: Boolean,
    ): String? = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再更新扩展。"
        tavernRunning -> "更新扩展前必须先停止酒馆，避免运行中的文件被替换。"
        extension.updateStatus == TavernExtensionUpdateStatus.LocalChanges ->
            "扩展包含本地改动，为避免覆盖你的修改，启动器不会自动更新。"
        extension.updateStatus != TavernExtensionUpdateStatus.UpdateAvailable ->
            "请先检查更新；只有确认发现新版本且没有本地改动时才能更新。"
        TavernExtensionCommandCodec.normalizeRepositoryUrl(extension.repositoryUrl) == null ->
            "扩展来源不是 http(s) 形式的 Git 仓库地址，启动器无法自动更新。"
        !isRevision(extension.currentRevision) || !isRevision(extension.latestRevision) ->
            "扩展版本信息不完整，请重新检查更新。"
        extension.currentRevision.equals(extension.latestRevision, ignoreCase = true) ->
            "当前扩展已经是检查到的最新版本。"
        else -> null
    }

    fun rollbackDisabledReason(
        extension: TavernExtensionRecord,
        actionsLocked: Boolean,
        tavernRunning: Boolean,
    ): String? = when {
        actionsLocked -> "当前有其他任务正在处理，请等任务完成后再回退扩展。"
        tavernRunning -> "回退扩展前必须先停止酒馆，避免运行中的文件被替换。"
        !isRevision(extension.rollbackRevision) -> "这个扩展还没有可用的更新前快照。"
        else -> null
    }

    private fun isRevision(value: String): Boolean =
        value.length in 7..40 && value.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }
}

/**
 * 扩展仓库地址只要求是 http(s) 的 Git 地址，不限定托管站点：GitHub、Gitee、GitLab、自建 Gitea
 * 以及 `https://gh-proxy.com/https://github.com/...` 这类加速前缀都可以。
 * 仍然拒绝带账号密码、查询串、锚点和控制字符的地址，避免凭据进入日志或参数被拆解。
 */
object TavernExtensionCommandCodec {
    private val repositorySegmentPattern = Regex("^[A-Za-z0-9._-]+$")

    private data class ParsedRepository(
        val normalizedUrl: String,
        val directoryName: String,
    )

    fun encodeDirectoryName(directoryName: String): String {
        require(validateDirectoryName(directoryName) == null) { "unsafe extension directory name" }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(directoryName.toByteArray(StandardCharsets.UTF_8))
    }

    fun decodeDirectoryName(encoded: String): String? {
        val decoded = try {
            String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return decoded.takeIf { validateDirectoryName(it) == null }
    }

    fun encodeRepositoryUrl(repositoryUrl: String): String {
        val normalized = requireNotNull(normalizeRepositoryUrl(repositoryUrl)) {
            "unsafe extension repository URL"
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(normalized.toByteArray(StandardCharsets.UTF_8))
    }

    fun decodeRepositoryUrl(encoded: String): String? {
        val decoded = try {
            String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return normalizeRepositoryUrl(decoded)
    }

    fun normalizeRepositoryUrl(value: String): String? =
        parseRepositoryUrl(value)?.normalizedUrl

    fun repositoryDirectoryName(value: String): String? =
        parseRepositoryUrl(value)?.directoryName

    fun validateRepositoryUrl(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "扩展仓库地址不能为空。"
            normalized.length > 240 -> "扩展仓库地址太长。"
            normalized.any { it.code < 32 || it.code == 127 } -> "扩展仓库地址不能包含控制字符。"
            normalized.contains('@') -> "扩展仓库地址不能包含账号或密码。"
            parseRepositoryUrl(normalized) == null ->
                "请输入完整的 Git 仓库网址，例如 https://github.com/作者/扩展名，也可以用镜像站或 Gitee 地址。"
            else -> null
        }
    }

    fun validateDirectoryName(value: String): String? = when {
        value.isBlank() -> "扩展目录名不能为空。"
        value != value.trim() -> "扩展目录名首尾不能包含空格。"
        value == "." || value == ".." -> "扩展目录名不能指向上级目录。"
        value.length > 128 -> "扩展目录名不能超过 128 个字符。"
        value.any { it == '/' || it == '\\' } -> "扩展目录名不能包含路径分隔符。"
        value.any { it.code < 32 || it.code == 127 } -> "扩展目录名不能包含控制字符。"
        else -> null
    }

    private fun parseRepositoryUrl(value: String): ParsedRepository? {
        val normalized = value.trim().trimEnd('/')
        if (normalized.isBlank() || normalized.length > 240 || '%' in normalized) return null
        if (normalized.any { it.code < 32 || it.code == 127 || it.isWhitespace() }) return null
        val uri = try {
            URI(normalized)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return null
        val segments = uri.rawPath.orEmpty().trim('/').split('/')
        if (segments.size < 2) return null
        // 中间段允许出现 "https:" 和空段，这样 gh-proxy 一类把完整地址拼在路径里的镜像也能用。
        if (segments.dropLast(1).any { it == "." || it == ".." }) return null
        // GitHub 仓库地址一定是 作者/仓库 两段；多出来的通常是 /tree/main 这类页面链接，直接拒绝更清楚。
        val githubSegmentIndex = segments.indexOfFirst { it.equals("github.com", ignoreCase = true) }
        val segmentsAfterGithub = when {
            uri.host.equals("github.com", ignoreCase = true) -> segments.size
            githubSegmentIndex >= 0 -> segments.size - githubSegmentIndex - 1
            else -> -1
        }
        if (segmentsAfterGithub >= 0 && segmentsAfterGithub != 2) return null
        val rawRepository = segments.last()
        val repository = if (rawRepository.endsWith(".git", ignoreCase = true)) {
            rawRepository.dropLast(4)
        } else {
            rawRepository
        }
        if (repository.isBlank() || repository == "." || repository == "..") return null
        if (!repositorySegmentPattern.matches(repository)) return null
        val normalizedUrl = if (rawRepository.endsWith(".git", ignoreCase = true)) normalized else "$normalized.git"
        return ParsedRepository(normalizedUrl = normalizedUrl, directoryName = repository)
    }
}
