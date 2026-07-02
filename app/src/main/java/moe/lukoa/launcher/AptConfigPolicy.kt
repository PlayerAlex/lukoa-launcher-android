package moe.lukoa.launcher

enum class AptConfigPolicy(
    val wireValue: String,
    val label: String,
) {
    KeepCurrent("keep", "保留当前配置"),
    UsePackageVersion("replace", "使用新版配置"),
}

enum class AptConfigTask(
    val actionLabel: String,
) {
    Bootstrap("准备 Termux 环境"),
    InstallTavern("安装酒馆"),
}

data class PendingAptConfigTask(
    val task: AptConfigTask,
    val installTarget: String = "",
    val installRepoUrl: String = "",
)

object AptConfigPolicyCodec {
    private const val SEPARATOR = "||"

    fun normalize(value: String?): AptConfigPolicy {
        return when (value.orEmpty().trim().lowercase()) {
            "replace", "new", "confnew", AptConfigPolicy.UsePackageVersion.wireValue -> AptConfigPolicy.UsePackageVersion
            else -> AptConfigPolicy.KeepCurrent
        }
    }

    fun encodeInstallTarget(target: String, policy: AptConfigPolicy): String {
        return "${target.trim()}$SEPARATOR${policy.wireValue}"
    }

    fun decodeInstallTarget(value: String?): Pair<String, AptConfigPolicy> {
        val parts = value.orEmpty().split(SEPARATOR, limit = 2)
        return parts.firstOrNull().orEmpty() to normalize(parts.getOrNull(1))
    }
}
