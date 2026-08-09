package moe.lukoa.launcher

/**
 * Distinguishes the initial profile observation from a real profile switch.
 *
 * Compose effects run once when entering composition, so profile-scoped state must not be
 * invalidated until the observed profile id actually changes.
 */
internal class ActiveProfileChangeTracker(initialProfileId: String) {
    private var observedProfileId = initialProfileId

    fun update(profileId: String): Boolean {
        if (profileId == observedProfileId) return false
        observedProfileId = profileId
        return true
    }
}
