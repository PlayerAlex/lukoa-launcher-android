package moe.lukoa.launcher

object TavernProfileReservedPathPolicy {
    private val mainManagedPathNormalized = TavernComparablePath.normalize(
        TavernProfilePathPolicy.launcherManagedDefaultPathForProfileId(TavernProfileDefaults.MAIN_PROFILE_ID),
    )

    fun reservedMessageForProfile(profile: TavernProfile): String? {
        if (profile.id == TavernProfileDefaults.MAIN_PROFILE_ID) return null
        if (!isMainManagedPath(profile.normalizedTavernDir)) return null
        return "主实例托管目录 ~/LukoaLauncher/SillyTavern 会一直保留给主实例。分身实例请使用自己的托管目录，或填写自定义地址。"
    }

    fun candidateBlockedReason(activeProfile: TavernProfile, candidatePath: String): String? {
        if (activeProfile.id == TavernProfileDefaults.MAIN_PROFILE_ID) return null
        if (!isMainManagedPath(candidatePath)) return null
        return "这个目录会一直保留给主实例，分身实例不能直接使用。"
    }

    fun startBlockedMessage(activeProfile: TavernProfile): String? {
        return reservedMessageForProfile(activeProfile)
    }

    private fun isMainManagedPath(path: String): Boolean {
        return TavernComparablePath.normalize(path) == mainManagedPathNormalized
    }
}
