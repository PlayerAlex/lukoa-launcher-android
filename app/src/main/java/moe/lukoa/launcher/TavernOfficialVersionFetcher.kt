package moe.lukoa.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Comparator
import java.util.Locale

data class TavernOfficialVersionFetchResult(
    val ok: Boolean,
    val message: String,
    val versions: TavernOfficialVersions = TavernOfficialVersions(),
)

class TavernOfficialVersionFetcher(private val context: Context) {
    fun fetchOfficialVersions(
        scope: CoroutineScope,
        mirrorConfig: TavernMirrorConfig,
        callback: (TavernOfficialVersionFetchResult) -> Unit,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchBlocking(mirrorConfig)
            }
            callback(result)
        }
    }

    private fun fetchBlocking(mirrorConfig: TavernMirrorConfig): TavernOfficialVersionFetchResult {
        val repoUrl = mirrorConfig.normalizedRepoUrl
        val githubSource = TavernGithubSourceParser.parse(repoUrl)
            ?: return TavernOfficialVersionFetchResult(
                ok = false,
                message = "当前酒馆 Git 源不是 GitHub 地址，App 端暂时没法直接读取版本列表。",
            )

        return try {
            val client = LukoaHttpClient(context)
            val tags = JSONArray(client.getText(githubSource.tagsApiUrl, accept = "application/vnd.github+json"))
            val branches = JSONArray(client.getText(githubSource.branchesApiUrl, accept = "application/vnd.github+json"))
            val versions = TavernOfficialVersions(
                stable = parseStableVersions(tags, repoUrl),
                test = parseTestVersions(branches, repoUrl),
            )

            if (!versions.hasData) {
                TavernOfficialVersionFetchResult(
                    ok = false,
                    message = "没有从官方仓库读到可用版本，请换个镜像源再试。",
                )
            } else {
                TavernOfficialVersionFetchResult(
                    ok = true,
                    message = "官方版本列表已更新。",
                    versions = versions,
                )
            }
        } catch (error: Exception) {
            TavernOfficialVersionFetchResult(
                ok = false,
                message = "读取官方版本失败：${error.message ?: error.javaClass.simpleName}",
            )
        }
    }

    private fun parseStableVersions(tags: JSONArray, repoUrl: String): List<TavernVersionChoice> {
        val stablePattern = Regex("""^v?\d+(?:\.\d+){1,3}$""")
        val choices = mutableListOf<TavernVersionChoice>()
        for (index in 0 until tags.length()) {
            val tag = tags.optJSONObject(index) ?: continue
            val name = tag.optString("name").trim()
            if (!stablePattern.matches(name)) continue
            val commit = tag.optJSONObject("commit")
                ?.optString("sha")
                .orEmpty()
                .take(12)
            choices += TavernVersionChoice(
                kind = TavernVersionKind.Stable,
                name = name,
                target = name,
                commit = commit,
                repoUrl = repoUrl,
            )
        }
        return choices
            .distinctBy { it.name.lowercase(Locale.US) }
            .sortedWith(Comparator { left, right -> VersionComparator.compare(right.name, left.name) })
            .take(5)
    }

    private fun parseTestVersions(branches: JSONArray, repoUrl: String): List<TavernVersionChoice> {
        val branchCommitByName = linkedMapOf<String, String>()
        for (index in 0 until branches.length()) {
            val branch = branches.optJSONObject(index) ?: continue
            val name = branch.optString("name").trim()
            if (name.isBlank()) continue
            val commit = branch.optJSONObject("commit")
                ?.optString("sha")
                .orEmpty()
                .take(12)
            branchCommitByName[name] = commit
        }

        return TEST_BRANCH_PRIORITY.mapNotNull { name ->
            val commit = branchCommitByName[name] ?: return@mapNotNull null
            TavernVersionChoice(
                kind = TavernVersionKind.Test,
                name = name,
                target = name,
                commit = commit,
                repoUrl = repoUrl,
            )
        }.take(3)
    }

    private companion object {
        val TEST_BRANCH_PRIORITY = listOf(
            "staging",
            "stats-2.0",
            "release",
            "main",
            "dev",
            "develop",
            "testing",
            "test",
            "preview",
            "next",
            "canary",
            "beta",
            "alpha",
        )
    }
}
