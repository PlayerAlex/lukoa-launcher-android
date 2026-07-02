package moe.lukoa.launcher

import android.content.Context

data class GithubRepositorySaveResult(
    val saved: Boolean,
    val repository: String,
    val message: String,
)

class GithubUpdateStore(private val context: Context) {
    fun loadRepository(): String {
        val saved = context.getSharedPreferences(PREFS_GITHUB_UPDATE, Context.MODE_PRIVATE)
            .getString(KEY_REPOSITORY, GithubUpdateDefaults.REPOSITORY)
            .orEmpty()
        return saved.ifBlank { GithubUpdateDefaults.REPOSITORY }
    }

    fun loadIgnoredUpdateTag(): String {
        return context.getSharedPreferences(PREFS_GITHUB_UPDATE, Context.MODE_PRIVATE)
            .getString(KEY_IGNORED_UPDATE_TAG, "")
            .orEmpty()
    }

    fun saveRepository(input: String): GithubRepositorySaveResult {
        val normalized = GithubRepositoryParser.normalize(input.ifBlank { GithubUpdateDefaults.REPOSITORY })
        if (normalized == null) {
            return GithubRepositorySaveResult(
                saved = false,
                repository = loadRepository(),
                message = "仓库格式不对，请填 用户名/仓库名。",
            )
        }

        context.getSharedPreferences(PREFS_GITHUB_UPDATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REPOSITORY, normalized)
            .remove(KEY_IGNORED_UPDATE_TAG)
            .apply()

        return GithubRepositorySaveResult(
            saved = true,
            repository = normalized,
            message = if (normalized.isBlank()) {
                "已清空 GitHub 仓库。启动器更新提醒已关闭。"
            } else {
                "已保存 GitHub 仓库：$normalized。"
            },
        )
    }

    fun ignoreUpdateTag(tagName: String) {
        context.getSharedPreferences(PREFS_GITHUB_UPDATE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IGNORED_UPDATE_TAG, tagName)
            .apply()
    }

    private companion object {
        const val PREFS_GITHUB_UPDATE = "github_update"
        const val KEY_REPOSITORY = "repository"
        const val KEY_IGNORED_UPDATE_TAG = "ignored_update_tag"
    }
}

object GithubUpdateDefaults {
    const val REPOSITORY = "PlayerAlex/lukoa-launcher-android"
}

object GithubRepositoryParser {
    fun normalize(input: String): String? {
        var text = input.trim()
        if (text.isBlank()) return ""

        text = text
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("git@")
            .removeSuffix(".git")
            .substringBefore("?")
            .substringBefore("#")

        if (text.startsWith("github.com:", ignoreCase = true)) {
            text = text.removePrefix("github.com:")
        } else if (text.startsWith("github.com/", ignoreCase = true)) {
            text = text.substringAfter("/")
        }

        val parts = text
            .split("/")
            .filter { it.isNotBlank() }

        if (parts.size < 2) return null
        val owner = parts[0]
        val repo = parts[1].removeSuffix(".git")
        val safeName = Regex("[A-Za-z0-9_.-]+")
        if (!safeName.matches(owner) || !safeName.matches(repo)) return null
        return "$owner/$repo"
    }
}
