package moe.lukoa.launcher

enum class TavernProfilePathKind(
    val label: String,
) {
    LauncherManaged("启动器托管目录"),
    TraditionalDefault("传统默认目录"),
    Custom("自定义目录"),
}

data class TavernProfilePathInfo(
    val kind: TavernProfilePathKind,
    val currentPath: String,
    val currentDisplayPath: String,
    val launcherManagedDefaultPath: String,
    val launcherManagedDefaultDisplayPath: String,
    val traditionalDefaultPath: String,
    val traditionalDefaultDisplayPath: String,
    val canDeleteDirectoryWithProfile: Boolean,
    val canMigrateToTraditionalDefault: Boolean,
)

object TavernProfilePathPolicy {
    fun describe(profile: TavernProfile): TavernProfilePathInfo {
        val managedDefaultPath = launcherManagedDefaultPathForProfileId(profile.id)
        val normalizedCurrentPath = profile.normalizedTavernDir
        val kind = when {
            normalizedCurrentPath == TavernPathNormalizer.normalize(managedDefaultPath) -> {
                TavernProfilePathKind.LauncherManaged
            }

            normalizedCurrentPath == TavernPathDefaults.LEGACY_DEFAULT_TAVERN_DIR_NORMALIZED -> {
                TavernProfilePathKind.TraditionalDefault
            }

            else -> TavernProfilePathKind.Custom
        }
        return TavernProfilePathInfo(
            kind = kind,
            currentPath = normalizedCurrentPath,
            currentDisplayPath = profile.displayTavernDir,
            launcherManagedDefaultPath = managedDefaultPath,
            launcherManagedDefaultDisplayPath = TavernPathNormalizer.toDisplayPath(
                TavernPathNormalizer.normalize(managedDefaultPath),
            ),
            traditionalDefaultPath = TavernPathDefaults.LEGACY_DEFAULT_TAVERN_DIR,
            traditionalDefaultDisplayPath = TavernPathNormalizer.toDisplayPath(
                TavernPathDefaults.LEGACY_DEFAULT_TAVERN_DIR_NORMALIZED,
            ),
            canDeleteDirectoryWithProfile = !isMainProfile(profile.id) &&
                kind == TavernProfilePathKind.LauncherManaged,
            canMigrateToTraditionalDefault = isMainProfile(profile.id),
        )
    }

    fun launcherManagedDefaultPathForProfileId(profileId: String): String {
        val slot = slotNumberForId(profileId)
        return if (slot <= 1) {
            "${TavernPathDefaults.LAUNCHER_MANAGED_ROOT_DIR}/SillyTavern"
        } else {
            "${TavernPathDefaults.LAUNCHER_MANAGED_ROOT_DIR}/SillyTavern$slot"
        }
    }

    fun isLauncherManagedPath(path: String): Boolean {
        val normalized = TavernPathNormalizer.normalize(path)
        val root = TavernPathDefaults.LAUNCHER_MANAGED_ROOT_DIR_NORMALIZED
        return normalized == root || normalized.startsWith("$root/")
    }

    private fun isMainProfile(profileId: String): Boolean {
        return profileId.trim() == TavernProfileDefaults.MAIN_PROFILE_ID
    }

    private fun slotNumberForId(profileId: String): Int {
        if (isMainProfile(profileId)) return 1
        return profileId.removePrefix("profile-")
            .toIntOrNull()
            ?.takeIf { it >= 2 }
            ?: 2
    }
}
