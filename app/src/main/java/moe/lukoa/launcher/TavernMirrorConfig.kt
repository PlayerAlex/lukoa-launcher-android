package moe.lukoa.launcher

import android.content.Context

data class TavernMirrorConfig(
    val repoUrl: String = TavernMirrorDefaults.OFFICIAL_REPO,
    val npmRegistry: String = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
) {
    val repoLabel: String
        get() = repoLabelFor(repoUrl)

    val npmLabel: String
        get() = when (npmRegistry.trim().trimEnd('/')) {
            TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY.trimEnd('/') -> "官方 npm"
            TavernMirrorDefaults.NPMMIRROR_REGISTRY.trimEnd('/') -> "npmmirror"
            else -> "自定义"
        }

    val normalizedRepoUrl: String
        get() = repoUrl.trim().ifBlank { TavernMirrorDefaults.OFFICIAL_REPO }

    val normalizedNpmRegistry: String
        get() = npmRegistry.trim().ifBlank { TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY }
}

fun repoLabelFor(repoUrl: String): String {
    return when (repoUrl.trim()) {
        TavernMirrorDefaults.OFFICIAL_REPO -> "官方 GitHub"
        TavernMirrorDefaults.GITHUB_PROXY_REPO -> "GitHub 加速"
        else -> "自定义"
    }
}

data class TavernMirrorSaveResult(
    val saved: Boolean,
    val config: TavernMirrorConfig,
    val message: String,
)

object TavernMirrorDefaults {
    const val OFFICIAL_REPO = "https://github.com/SillyTavern/SillyTavern.git"
    const val GITHUB_PROXY_REPO = "https://gh-proxy.com/https://github.com/SillyTavern/SillyTavern.git"
    const val OFFICIAL_NPM_REGISTRY = "https://registry.npmjs.org/"
    const val NPMMIRROR_REGISTRY = "https://registry.npmmirror.com/"
}

object TavernMirrorValidator {
    private val gitUrlPattern = Regex("""^https://[A-Za-z0-9._~:/?#\[\]@!&'()*+,;=%-]+(?:\.git)?$""")
    private val npmUrlPattern = Regex("""^https://[A-Za-z0-9._~:/?#\[\]@!&'()*+,;=%-]+/?$""")
    private val termuxAptUrlPattern = Regex("""^https?://[A-Za-z0-9._~:/?#\[\]@!&'()*+,;=%-]+/?$""")

    fun validateRepoUrl(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "酒馆 Git 源不能为空。"
            normalized.length > 240 -> "酒馆 Git 源太长。"
            !gitUrlPattern.matches(normalized) -> "只支持 https 地址。"
            else -> null
        }
    }

    fun validateNpmRegistry(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "npm 源不能为空。"
            normalized.length > 200 -> "npm 源太长。"
            !npmUrlPattern.matches(normalized) -> "只支持 https 地址。"
            else -> null
        }
    }

    fun validateTermuxAptUrl(value: String): String? {
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> "Termux 包源不能为空。"
            normalized.length > 240 -> "Termux 包源太长。"
            normalized.contains("::") || normalized.any { it == '\n' || it == '\r' || it.code < 32 } -> "Termux 包源不能包含换行或 ::。"
            normalized.contains(' ') || normalized.contains('\t') -> "Termux 包源不能有空格。"
            !termuxAptUrlPattern.matches(normalized) -> "只支持 http 或 https 地址。"
            else -> null
        }
    }
}

class TavernMirrorStore(private val context: Context) {
    fun load(): TavernMirrorConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return TavernMirrorConfig(
            repoUrl = prefs.getString(KEY_REPO_URL, null)
                ?.takeIf { TavernMirrorValidator.validateRepoUrl(it) == null }
                ?: TavernMirrorDefaults.OFFICIAL_REPO,
            npmRegistry = prefs.getString(KEY_NPM_REGISTRY, null)
                ?.takeIf { TavernMirrorValidator.validateNpmRegistry(it) == null }
                ?: TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
        )
    }

    fun save(config: TavernMirrorConfig): TavernMirrorSaveResult {
        val repo = config.normalizedRepoUrl
        TavernMirrorValidator.validateRepoUrl(repo)?.let { reason ->
            return TavernMirrorSaveResult(false, load(), reason)
        }
        val npm = config.normalizedNpmRegistry
        TavernMirrorValidator.validateNpmRegistry(npm)?.let { reason ->
            return TavernMirrorSaveResult(false, load(), reason)
        }
        val savedConfig = TavernMirrorConfig(repoUrl = repo, npmRegistry = npm)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REPO_URL, savedConfig.repoUrl)
            .putString(KEY_NPM_REGISTRY, savedConfig.npmRegistry)
            .apply()
        return TavernMirrorSaveResult(
            saved = true,
            config = savedConfig,
            message = "镜像源已保存：${savedConfig.repoLabel} / ${savedConfig.npmLabel}。",
        )
    }

    fun saveOfficial(): TavernMirrorSaveResult {
        return save(
            TavernMirrorConfig(
                repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
                npmRegistry = TavernMirrorDefaults.OFFICIAL_NPM_REGISTRY,
            ),
        )
    }

    private companion object {
        const val PREFS = "tavern_mirror_config"
        const val KEY_REPO_URL = "repo_url"
        const val KEY_NPM_REGISTRY = "npm_registry"
    }
}
