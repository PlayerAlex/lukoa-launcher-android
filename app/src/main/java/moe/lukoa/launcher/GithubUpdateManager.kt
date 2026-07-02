package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class GithubUpdateInfo(
    val repository: String,
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseUrl: String,
    val apkName: String,
    val apkDownloadUrl: String,
    val publishedAt: String,
    val body: String,
    val isNewer: Boolean,
)

data class GithubUpdateCheckResult(
    val ok: Boolean,
    val message: String,
    val info: GithubUpdateInfo? = null,
)

data class GithubUpdateInstallResult(
    val ok: Boolean,
    val message: String,
)

data class GithubUpdateUiState(
    val repository: String = "",
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val latest: GithubUpdateInfo? = null,
    val message: String = "未配置 GitHub 仓库。",
    val lastCheckedText: String = "",
) {
    val hasUpdate: Boolean
        get() = latest?.isNewer == true

    val canInstallUpdate: Boolean
        get() = hasUpdate && latest?.apkDownloadUrl?.isNotBlank() == true && !checking && !downloading
}

class GithubUpdateManager(private val context: Context) {
    fun checkLatest(
        scope: CoroutineScope,
        repository: String,
        currentVersionName: String,
        callback: (GithubUpdateCheckResult) -> Unit,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                checkLatestBlocking(repository, currentVersionName)
            }
            callback(result)
        }
    }

    fun downloadAndInstall(
        scope: CoroutineScope,
        updateInfo: GithubUpdateInfo,
        callback: (GithubUpdateInstallResult) -> Unit,
    ) {
        scope.launch {
            val downloaded = withContext(Dispatchers.IO) {
                downloadApk(updateInfo)
            }
            if (!downloaded.ok || downloaded.file == null) {
                callback(GithubUpdateInstallResult(false, downloaded.message))
                return@launch
            }
            callback(openInstaller(downloaded.file))
        }
    }

    fun openReleasePage(updateInfo: GithubUpdateInfo): GithubUpdateInstallResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.releaseUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            GithubUpdateInstallResult(true, "已打开 GitHub 发布页。")
        } catch (error: Exception) {
            GithubUpdateInstallResult(false, "打开发布页失败：${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun checkLatestBlocking(
        repositoryInput: String,
        currentVersionName: String,
    ): GithubUpdateCheckResult {
        val repository = GithubRepositoryParser.normalize(repositoryInput)
        if (repository == null || repository.isBlank()) {
            return GithubUpdateCheckResult(
                ok = false,
                message = "请先填写 GitHub 仓库。",
            )
        }

        return try {
            val release = bestVersionedRelease(repository)
                ?: return GithubUpdateCheckResult(
                    ok = false,
                    message = "仓库里还没有可用 Release。",
                )
            val info = release.toUpdateInfo(repository, currentVersionName)

            val compareToCurrent = VersionComparator.compare(info.versionName, currentVersionName)
            val message = when {
                compareToCurrent == 0 -> "当前已是最新版本：v$currentVersionName。"
                compareToCurrent < 0 -> "本机版本比 GitHub 高：v$currentVersionName。"
                info.apkDownloadUrl.isBlank() -> "发现新版 v${info.versionName}，但没有 APK。"
                else -> "发现新版 v${info.versionName}。"
            }
            GithubUpdateCheckResult(ok = true, message = message, info = info)
        } catch (error: Exception) {
            GithubUpdateCheckResult(
                ok = false,
                message = "检查 GitHub 更新失败：${error.message ?: error.javaClass.simpleName}",
            )
        }
    }

    private fun bestVersionedRelease(repository: String): JSONObject? {
        val releasesText = httpGetText("https://api.github.com/repos/$repository/releases?per_page=30")
        val releases = JSONArray(releasesText)
        var bestRelease: JSONObject? = null
        var bestVersion = "0"
        for (index in 0 until releases.length()) {
            val release = releases.optJSONObject(index) ?: continue
            if (release.optBoolean("draft")) continue
            val tagName = release.optString("tag_name").ifBlank { release.optString("name") }
            val version = VersionComparator.extractVersionName(tagName)
            if (bestRelease == null || VersionComparator.compare(version, bestVersion) > 0) {
                bestRelease = release
                bestVersion = version
            }
        }
        return bestRelease
    }

    private fun JSONObject.toUpdateInfo(repository: String, currentVersionName: String): GithubUpdateInfo {
        val tagName = optString("tag_name").ifBlank { optString("name") }
        val latestVersion = VersionComparator.extractVersionName(tagName)
        val asset = bestApkAsset(this)
        return GithubUpdateInfo(
            repository = repository,
            tagName = tagName.ifBlank { latestVersion },
            versionName = latestVersion,
            releaseName = optString("name").ifBlank { tagName },
            releaseUrl = optString("html_url"),
            apkName = asset?.first.orEmpty(),
            apkDownloadUrl = asset?.second.orEmpty(),
            publishedAt = optString("published_at"),
            body = optString("body"),
            isNewer = VersionComparator.compare(latestVersion, currentVersionName) > 0,
        )
    }

    private fun bestApkAsset(release: JSONObject): Pair<String, String>? {
        val assets = release.optJSONArray("assets") ?: return null
        val candidates = mutableListOf<Pair<String, String>>()
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                candidates += name to url
            }
        }
        return candidates.maxByOrNull { (name, _) ->
            when {
                name.contains("lukoa", ignoreCase = true) -> 4
                name.contains("露科亚") -> 3
                name.contains("launcher", ignoreCase = true) -> 2
                else -> 1
            }
        }
    }

    private fun downloadApk(updateInfo: GithubUpdateInfo): DownloadedApk {
        if (updateInfo.apkDownloadUrl.isBlank()) {
            return DownloadedApk(false, "这个 Release 没有 APK。", null)
        }

        return try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val safeBaseName = sanitizeFileName(
                updateInfo.apkName.ifBlank { "lukoa-launcher-${updateInfo.versionName}.apk" },
            ).removeSuffix(".apk")
            val file = ExportFileNamer.nextAvailableFile(updatesDir, safeBaseName, "apk")
            downloadToFile(updateInfo.apkDownloadUrl, file)

            if (file.length() < MIN_APK_SIZE_BYTES) {
                file.delete()
                return DownloadedApk(false, "下载的 APK 不完整。", null)
            }

            val archiveInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            if (archiveInfo == null) {
                file.delete()
                return DownloadedApk(false, "下载完成，但不是 APK。", null)
            }
            if (archiveInfo.packageName != context.packageName) {
                file.delete()
                return DownloadedApk(
                    false,
                    "下载的 APK 不是露科亚启动器，已拦截。",
                    null,
                )
            }

            val currentInfo = VersionBackupManager.versionInfo(context)
            val archiveCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode.toLong()
            }
            if (archiveCode <= currentInfo.versionCode) {
                file.delete()
                return DownloadedApk(
                    false,
                    "下载的 APK 不是新版本。",
                    null,
                )
            }

            DownloadedApk(true, "新版 APK 已下载：${file.name}", file)
        } catch (error: Exception) {
            DownloadedApk(false, "下载新版失败：${error.message ?: error.javaClass.simpleName}", null)
        }
    }

    private fun openInstaller(file: File): GithubUpdateInstallResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            return try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                GithubUpdateInstallResult(
                    ok = false,
                    message = "请先允许安装未知来源应用，然后再点更新。",
                )
            } catch (error: Exception) {
                GithubUpdateInstallResult(
                    ok = false,
                    message = "需要开启安装未知来源应用权限：${error.message ?: error.javaClass.simpleName}",
                )
            }
        }

        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            GithubUpdateInstallResult(true, "已打开安装器，请确认安装。")
        } catch (error: Exception) {
            GithubUpdateInstallResult(false, "打开安装器失败：${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun httpGetText(url: String): String {
        val connection = openConnection(url)
        return connection.use {
            val code = it.responseCode
            val stream = if (code in 200..299) it.inputStream else it.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("GitHub 返回 HTTP $code：${body.take(120)}")
            }
            body
        }
    }

    private fun downloadToFile(url: String, file: File) {
        val connection = openConnection(url)
        connection.use {
            val code = it.responseCode
            if (code !in 200..299) {
                val body = it.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() }.orEmpty()
                throw IllegalStateException("下载返回 HTTP $code：${body.take(120)}")
            }
            it.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun openConnection(url: String, redirectCount: Int = 0): HttpURLConnection {
        if (redirectCount > MAX_REDIRECTS) {
            throw IllegalStateException("重定向次数过多。")
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "LukoaLauncher/${VersionBackupManager.versionInfo(context).versionName}")
        }
        val code = connection.responseCode
        if (code in listOf(
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER,
                307,
                308,
            )
        ) {
            val location = connection.getHeaderField("Location")
                ?: throw IllegalStateException("GitHub 重定向缺少 Location。")
            connection.disconnect()
            return openConnection(URL(URL(url), location).toString(), redirectCount + 1)
        }
        return connection
    }

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "-")
            .lowercase(Locale.US)
            .ifBlank { "lukoa-launcher.apk" }
    }

    private data class DownloadedApk(
        val ok: Boolean,
        val message: String,
        val file: File?,
    )

    private companion object {
        const val MIN_APK_SIZE_BYTES = 100 * 1024
        const val MAX_REDIRECTS = 5
    }
}

object VersionComparator {
    fun extractVersionName(text: String): String {
        return Regex("\\d+(?:\\.\\d+){0,3}")
            .find(text)
            ?.value
            ?: text.removePrefix("v").removePrefix("V").ifBlank { "0" }
    }

    fun compare(left: String, right: String): Int {
        val leftParts = extractVersionName(left).split(".").map { it.toIntOrNull() ?: 0 }
        val rightParts = extractVersionName(right).split(".").map { it.toIntOrNull() ?: 0 }
        val maxSize = maxOf(leftParts.size, rightParts.size, 4)
        for (index in 0 until maxSize) {
            val l = leftParts.getOrElse(index) { 0 }
            val r = rightParts.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }
}

private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        disconnect()
    }
}
