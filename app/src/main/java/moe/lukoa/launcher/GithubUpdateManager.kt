package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
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
import java.security.MessageDigest
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
    val prerelease: Boolean,
    val isNewer: Boolean,
) {
    val releaseTypeLabel: String
        get() = if (prerelease) "测试版" else "稳定版"
}

data class GithubUpdateCheckResult(
    val ok: Boolean,
    val message: String,
    val info: GithubUpdateInfo? = null,
    val currentInfo: GithubUpdateInfo? = null,
)

data class GithubUpdateInstallResult(
    val ok: Boolean,
    val message: String,
)

data class GithubUpdateUiState(
    val repository: String = "",
    val channel: GithubReleaseChannel = GithubUpdateDefaults.CHANNEL,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val latest: GithubUpdateInfo? = null,
    val currentRelease: GithubUpdateInfo? = null,
    val message: String = "未配置 GitHub 仓库。",
    val lastCheckedText: String = "",
) {
    val hasUpdate: Boolean
        get() = latest?.isNewer == true

    val canInstallUpdate: Boolean
        get() = hasUpdate && latest?.apkDownloadUrl?.isNotBlank() == true && !checking && !downloading
}

class GithubUpdateManager(private val context: Context) {
    private val httpClient = LukoaHttpClient(context)

    fun checkLatest(
        scope: CoroutineScope,
        repository: String,
        currentVersionName: String,
        channel: GithubReleaseChannel,
        callback: (GithubUpdateCheckResult) -> Unit,
    ) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                checkLatestBlocking(repository, currentVersionName, channel)
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
        return openWebPage(
            url = updateInfo.releaseUrl,
            successMessage = "已打开 GitHub 发布页。",
        )
    }

    fun openRepositoryReleasesPage(repositoryInput: String): GithubUpdateInstallResult {
        val repository = GithubRepositoryParser.normalize(repositoryInput)
        if (repository == null || repository.isBlank()) {
            return GithubUpdateInstallResult(false, "请先填写有效的 GitHub 仓库。")
        }
        return openWebPage(
            url = "https://github.com/$repository/releases",
            successMessage = "已打开 GitHub 发布列表。",
        )
    }

    private fun openWebPage(
        url: String,
        successMessage: String,
    ): GithubUpdateInstallResult {
        if (url.isBlank()) {
            return GithubUpdateInstallResult(false, "发布页地址为空，请先检查更新。")
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            GithubUpdateInstallResult(true, successMessage)
        } catch (error: Exception) {
            GithubUpdateInstallResult(false, "打开发布页失败：${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun checkLatestBlocking(
        repositoryInput: String,
        currentVersionName: String,
        channel: GithubReleaseChannel,
    ): GithubUpdateCheckResult {
        val repository = GithubRepositoryParser.normalize(repositoryInput)
        if (repository == null || repository.isBlank()) {
            return GithubUpdateCheckResult(
                ok = false,
                message = "请先填写 GitHub 仓库。",
            )
        }

        return try {
            val releases = loadReleases(repository)
            val currentRelease = releaseForVersion(releases, currentVersionName)
            val release = bestVersionedRelease(releases, channel)
                ?: return GithubUpdateCheckResult(
                    ok = false,
                    message = when (channel) {
                        GithubReleaseChannel.Stable -> "仓库里还没有可用稳定版 Release。"
                        GithubReleaseChannel.Test -> "仓库里还没有可用 Release。"
                    },
                    currentInfo = currentRelease?.toUpdateInfo(repository, currentVersionName),
                )
            val info = release.toUpdateInfo(repository, currentVersionName)
            val currentInfo = currentRelease?.toUpdateInfo(repository, currentVersionName)

            val compareToCurrent = VersionComparator.compare(info.versionName, currentVersionName)
            val message = when {
                compareToCurrent == 0 -> "当前已是最新${channel.label}：v$currentVersionName。"
                compareToCurrent < 0 -> "本机版本比 GitHub 的${channel.label}更高：v$currentVersionName。"
                info.apkDownloadUrl.isBlank() -> "发现${info.releaseTypeLabel} v${info.versionName}，但没有 APK。"
                else -> "发现${info.releaseTypeLabel} v${info.versionName}。"
            }
            GithubUpdateCheckResult(
                ok = true,
                message = message,
                info = info,
                currentInfo = currentInfo,
            )
        } catch (error: Exception) {
            GithubUpdateCheckResult(
                ok = false,
                message = "检查 GitHub 更新失败：${error.message ?: error.javaClass.simpleName}",
            )
        }
    }

    private fun loadReleases(repository: String): List<JSONObject> {
        val releasesText = httpClient.getText(
            url = "https://api.github.com/repos/$repository/releases?per_page=30",
            accept = GITHUB_ACCEPT,
        )
        val releases = JSONArray(releasesText)
        return buildList {
            for (index in 0 until releases.length()) {
                releases.optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun bestVersionedRelease(
        releases: List<JSONObject>,
        channel: GithubReleaseChannel,
    ): JSONObject? {
        var bestRelease: JSONObject? = null
        var bestVersion = "0"
        var bestPrerelease = false
        for (release in releases) {
            if (release.optBoolean("draft")) continue
            val prerelease = release.optBoolean("prerelease")
            if (channel == GithubReleaseChannel.Stable && prerelease) continue
            val tagName = release.optString("tag_name").ifBlank { release.optString("name") }
            val version = VersionComparator.extractVersionName(tagName)
            if (
                bestRelease == null ||
                VersionComparator.compareRelease(
                    left = version,
                    leftPrerelease = prerelease,
                    right = bestVersion,
                    rightPrerelease = bestPrerelease,
                ) > 0
            ) {
                bestRelease = release
                bestVersion = version
                bestPrerelease = prerelease
            }
        }
        return bestRelease
    }

    private fun releaseForVersion(
        releases: List<JSONObject>,
        currentVersionName: String,
    ): JSONObject? {
        val normalizedVersion = currentVersionName.trim()
        if (normalizedVersion.isBlank()) return null
        val expectedTag = "v$normalizedVersion"
        return releases.firstOrNull { release ->
            if (release.optBoolean("draft")) return@firstOrNull false
            val tagName = release.optString("tag_name").ifBlank { release.optString("name") }
            tagName.equals(expectedTag, ignoreCase = true) ||
                tagName.equals(normalizedVersion, ignoreCase = true) ||
                VersionComparator.extractVersionName(tagName).equals(normalizedVersion, ignoreCase = true)
        }
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
            prerelease = optBoolean("prerelease"),
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
            val updatesDir = File(context.cacheDir, "updates")
            val safeFileName = sanitizeFileName(
                updateInfo.apkName.ifBlank { "lukoa-launcher-${updateInfo.versionName}.apk" },
            ).let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
            val currentIdentity = currentLauncherIdentity()
            val file = LauncherUpdateFileTransaction.download(
                directory = updatesDir,
                finalFileName = safeFileName,
                download = { temporaryFile ->
                    httpClient.downloadToFile(
                        url = updateInfo.apkDownloadUrl,
                        file = temporaryFile,
                        accept = GITHUB_ACCEPT,
                    )
                },
                validate = { temporaryFile ->
                    if (temporaryFile.length() < MIN_APK_SIZE_BYTES) {
                        error("下载的 APK 不完整。")
                    }
                    val downloadedIdentity = archiveIdentity(temporaryFile)
                        ?: error("下载完成，但不是 APK。")
                    LauncherApkValidationPolicy.validate(currentIdentity, downloadedIdentity)?.let(::error)
                },
            )

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

    @Suppress("DEPRECATION")
    private fun currentLauncherIdentity(): LauncherApkIdentity {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        return packageIdentity(info)
    }

    @Suppress("DEPRECATION")
    private fun archiveIdentity(file: File): LauncherApkIdentity? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: return null
        return packageIdentity(info)
    }

    @Suppress("DEPRECATION")
    private fun packageIdentity(info: PackageInfo): LauncherApkIdentity {
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { signingInfo ->
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            }
        } else {
            info.signatures
        }.orEmpty()
        val digests = signatures.mapTo(linkedSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }
        }
        return LauncherApkIdentity(
            packageName = info.packageName.orEmpty(),
            versionCode = versionCode,
            signerDigests = digests,
        )
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
        const val GITHUB_ACCEPT = "application/vnd.github+json, application/octet-stream"
        const val MIN_APK_SIZE_BYTES = 100 * 1024
    }
}

object VersionComparator {
    fun extractVersionName(text: String): String {
        return parse(text)?.normalized
            ?: text.removePrefix("v").removePrefix("V").trim().ifBlank { "0" }
    }

    fun compare(left: String, right: String): Int {
        val leftParsed = parse(left)
        val rightParsed = parse(right)
        if (leftParsed != null && rightParsed != null) {
            return compareParsed(leftParsed, rightParsed)
        }
        val leftText = extractVersionName(left)
        val rightText = extractVersionName(right)
        return leftText.compareTo(rightText)
    }

    fun compareRelease(
        left: String,
        leftPrerelease: Boolean,
        right: String,
        rightPrerelease: Boolean,
    ): Int {
        val base = compare(left, right)
        if (base != 0) return base
        return when {
            leftPrerelease == rightPrerelease -> 0
            leftPrerelease -> -1
            else -> 1
        }
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val prereleaseLabel: String?,
        val prereleaseNumber: Int?,
        val normalized: String,
    )

    private fun parse(text: String): ParsedVersion? {
        val match = VERSION_PATTERN.find(text.removePrefix("v").removePrefix("V").trim()) ?: return null
        val numberText = match.groupValues[1]
        val prereleaseLabel = match.groupValues.getOrNull(2)?.lowercase(Locale.US)?.ifBlank { null }
        val prereleaseNumber = match.groupValues.getOrNull(3)?.toIntOrNull()
        val normalized = buildString {
            append(numberText)
            if (prereleaseLabel != null) {
                append("-")
                append(prereleaseLabel)
                if (prereleaseNumber != null) {
                    append(prereleaseNumber)
                }
            }
        }
        return ParsedVersion(
            numbers = numberText.split(".").map { it.toIntOrNull() ?: 0 },
            prereleaseLabel = prereleaseLabel,
            prereleaseNumber = prereleaseNumber,
            normalized = normalized,
        )
    }

    private fun compareParsed(left: ParsedVersion, right: ParsedVersion): Int {
        val maxSize = maxOf(left.numbers.size, right.numbers.size, 4)
        for (index in 0 until maxSize) {
            val l = left.numbers.getOrElse(index) { 0 }
            val r = right.numbers.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }

        if (left.prereleaseLabel == null && right.prereleaseLabel == null) return 0
        if (left.prereleaseLabel == null) return 1
        if (right.prereleaseLabel == null) return -1

        val leftRank = prereleaseRank(left.prereleaseLabel)
        val rightRank = prereleaseRank(right.prereleaseLabel)
        if (leftRank != rightRank) return leftRank.compareTo(rightRank)

        val leftNumber = left.prereleaseNumber ?: 0
        val rightNumber = right.prereleaseNumber ?: 0
        if (leftNumber != rightNumber) return leftNumber.compareTo(rightNumber)

        return left.prereleaseLabel.compareTo(right.prereleaseLabel)
    }

    private fun prereleaseRank(label: String): Int {
        return when {
            label.startsWith("dev") -> 0
            label.startsWith("canary") -> 1
            label.startsWith("alpha") || label == "a" -> 2
            label.startsWith("preview") || label.startsWith("test") -> 3
            label.startsWith("beta") || label == "b" -> 4
            label.startsWith("rc") -> 5
            else -> 3
        }
    }

    private val VERSION_PATTERN = Regex(
        pattern = """(\d+(?:\.\d+){0,3})(?:[-._]?([A-Za-z]+)(?:[-._]?(\d+))?)?""",
        option = RegexOption.IGNORE_CASE,
    )
}
