package moe.lukoa.launcher

data class LauncherApkIdentity(
    val packageName: String,
    val versionCode: Long,
    val signerDigests: Set<String>,
)

object LauncherApkValidationPolicy {
    fun validate(current: LauncherApkIdentity, downloaded: LauncherApkIdentity): String? = when {
        downloaded.packageName != current.packageName -> "下载的 APK 不是露科亚启动器，已拦截。"
        downloaded.versionCode <= current.versionCode -> "下载的 APK 不是新版本。"
        current.signerDigests.isEmpty() || downloaded.signerDigests.isEmpty() -> "无法读取 APK 签名，已停止安装。"
        current.signerDigests.intersect(downloaded.signerDigests).isEmpty() -> "APK 签名与当前启动器不一致，已拦截。"
        else -> null
    }
}
