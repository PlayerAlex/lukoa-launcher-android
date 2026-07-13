package moe.lukoa.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TavernProfile(
    val id: String = TavernProfileDefaults.MAIN_PROFILE_ID,
    val name: String = TavernProfileDefaults.MAIN_PROFILE_NAME,
    val tavernDir: String = TavernPathDefaults.DEFAULT_TAVERN_DIR,
    val port: Int = TavernPortDefaults.DEFAULT_TAVERN_PORT,
) {
    val normalizedName: String
        get() = name.trim().ifBlank { TavernProfileDefaults.nameForId(id) }

    val normalizedTavernDir: String
        get() = TavernPathNormalizer.normalize(tavernDir)

    val displayTavernDir: String
        get() = TavernPathNormalizer.toDisplayPath(normalizedTavernDir)

    val normalizedPort: Int
        get() = TavernPortNormalizer.normalize(port)
}

data class TavernPathConfig(
    val tavernDir: String = TavernPathDefaults.DEFAULT_TAVERN_DIR,
    val port: Int = TavernPortDefaults.DEFAULT_TAVERN_PORT,
    val activeProfileId: String = TavernProfileDefaults.MAIN_PROFILE_ID,
    val profileLabel: String = TavernProfileDefaults.MAIN_PROFILE_NAME,
    val profiles: List<TavernProfile> = emptyList(),
) {
    val availableProfiles: List<TavernProfile>
        get() = TavernProfileCollection.normalize(
            profiles = profiles,
            fallbackProfile = TavernProfile(
                id = activeProfileId,
                name = profileLabel,
                tavernDir = tavernDir,
                port = port,
            ),
        )

    val activeProfile: TavernProfile
        get() = availableProfiles.firstOrNull { it.id == activeProfileId } ?: availableProfiles.first()

    val normalizedTavernDir: String
        get() = activeProfile.normalizedTavernDir

    val displayTavernDir: String
        get() = activeProfile.displayTavernDir

    val normalizedPort: Int
        get() = activeProfile.normalizedPort

    val activeProfileLabel: String
        get() = activeProfile.normalizedName

    val hasMultipleProfiles: Boolean
        get() = availableProfiles.size > 1

    val isActiveProfileMain: Boolean
        get() = activeProfile.id == TavernProfileDefaults.MAIN_PROFILE_ID

    val canRemoveActiveProfile: Boolean
        get() = hasMultipleProfiles && !isActiveProfileMain

    val isActiveProfileDefault: Boolean
        get() {
            val defaultProfile = TavernProfileDefaults.profileForId(activeProfile.id)
            return TavernComparablePath.same(normalizedTavernDir, defaultProfile.normalizedTavernDir) &&
                normalizedPort == defaultProfile.normalizedPort
        }

    fun withActiveProfile(profileId: String): TavernPathConfig {
        return withProfiles(availableProfiles, profileId)
    }

    fun withUpdatedActiveProfile(
        tavernDir: String = activeProfile.tavernDir,
        port: Int = activeProfile.port,
        name: String = activeProfile.name,
    ): TavernPathConfig {
        val updated = activeProfile.copy(
            name = name,
            tavernDir = tavernDir,
            port = port,
        )
        return withProfiles(
            availableProfiles.map { profile ->
                if (profile.id == activeProfile.id) updated else profile
            },
            updated.id,
        )
    }

    fun withUpdatedActiveProfilePathOnly(
        tavernDir: String,
    ): TavernPathConfig {
        return withUpdatedActiveProfile(
            tavernDir = tavernDir,
            port = activeProfile.port,
        )
    }

    fun withUpdatedProfile(
        profileId: String,
        tavernDir: String? = null,
        port: Int? = null,
        name: String? = null,
    ): TavernPathConfig {
        val targetProfile = availableProfiles.firstOrNull { it.id == profileId }
            ?: return withProfiles(availableProfiles, activeProfile.id)
        val updated = targetProfile.copy(
            name = name ?: targetProfile.name,
            tavernDir = tavernDir ?: targetProfile.tavernDir,
            port = port ?: targetProfile.port,
        )
        return withProfiles(
            availableProfiles.map { profile ->
                if (profile.id == profileId) updated else profile
            },
            activeProfile.id,
        )
    }

    fun restoreActiveProfileDefault(): TavernPathConfig {
        val defaults = TavernProfileDefaults.profileForId(activeProfile.id)
        return withUpdatedActiveProfile(
            tavernDir = defaults.tavernDir,
            port = defaults.port,
            name = activeProfile.name.ifBlank { defaults.name },
        )
    }

    fun addSuggestedProfile(makeActive: Boolean = true): TavernPathConfig {
        val profile = TavernProfileDefaults.suggestedClone(availableProfiles)
        return withProfiles(
            availableProfiles + profile,
            if (makeActive) profile.id else activeProfile.id,
        )
    }

    fun removeProfile(profileId: String): TavernPathConfig {
        if (profileId == TavernProfileDefaults.MAIN_PROFILE_ID) {
            return withProfiles(availableProfiles, activeProfile.id)
        }
        val remaining = availableProfiles.filterNot { it.id == profileId }
        val nextActive = when {
            remaining.isEmpty() -> TavernProfileDefaults.MAIN_PROFILE_ID
            activeProfile.id != profileId -> activeProfile.id
            else -> remaining.first().id
        }
        return withProfiles(remaining, nextActive)
    }

    fun withProfiles(
        nextProfiles: List<TavernProfile>,
        nextActiveProfileId: String = activeProfile.id,
    ): TavernPathConfig {
        val normalizedProfiles = TavernProfileCollection.normalize(
            profiles = nextProfiles,
            fallbackProfile = TavernProfile(
                id = activeProfile.id,
                name = activeProfile.name,
                tavernDir = activeProfile.tavernDir,
                port = activeProfile.port,
            ),
        )
        val nextActive = normalizedProfiles.firstOrNull { it.id == nextActiveProfileId } ?: normalizedProfiles.first()
        return copy(
            tavernDir = nextActive.tavernDir,
            port = nextActive.port,
            activeProfileId = nextActive.id,
            profileLabel = nextActive.normalizedName,
            profiles = normalizedProfiles,
        )
    }
}

data class TavernPathSaveResult(
    val saved: Boolean,
    val config: TavernPathConfig,
    val message: String,
)

object TavernPathDefaults {
    const val LAUNCHER_MANAGED_ROOT_DIR = "~/LukoaLauncher"
    const val LAUNCHER_MANAGED_ROOT_DIR_NORMALIZED = "\$HOME/LukoaLauncher"
    const val LEGACY_DEFAULT_TAVERN_DIR = "~/SillyTavern"
    const val LEGACY_DEFAULT_TAVERN_DIR_NORMALIZED = "\$HOME/SillyTavern"
    const val DEFAULT_TAVERN_DIR = "$LAUNCHER_MANAGED_ROOT_DIR/SillyTavern"
    const val DEFAULT_TAVERN_DIR_NORMALIZED = "$LAUNCHER_MANAGED_ROOT_DIR_NORMALIZED/SillyTavern"
    const val TERMUX_HOME_DIR = "/data/data/com.termux/files/home"
}

object TavernPortDefaults {
    const val DEFAULT_TAVERN_PORT = 8000
}

object TavernProfileDefaults {
    const val MAIN_PROFILE_ID = "main"
    const val MAIN_PROFILE_NAME = "主实例"
    private const val CLONE_PROFILE_PREFIX = "profile-"
    private const val CLONE_PROFILE_NAME = "分身实例"

    fun profileForId(id: String): TavernProfile {
        val slot = slotNumberForId(id)
        return TavernProfile(
            id = id.ifBlank { MAIN_PROFILE_ID },
            name = nameForSlot(slot),
            tavernDir = defaultPathForSlot(slot),
            port = defaultPortForSlot(slot),
        )
    }

    fun nameForId(id: String): String = nameForSlot(slotNumberForId(id))

    fun suggestedClone(existingProfiles: List<TavernProfile>): TavernProfile {
        val usedIds = existingProfiles.map { it.id }.toSet()
        var slot = 2
        while (usedIds.contains(profileIdForSlot(slot))) {
            slot += 1
        }
        return TavernProfile(
            id = profileIdForSlot(slot),
            name = nameForSlot(slot),
            tavernDir = defaultPathForSlot(slot),
            port = defaultPortForSlot(slot),
        )
    }

    private fun profileIdForSlot(slot: Int): String {
        return if (slot <= 1) MAIN_PROFILE_ID else "$CLONE_PROFILE_PREFIX$slot"
    }

    private fun defaultPathForSlot(slot: Int): String {
        return TavernProfilePathPolicy.launcherManagedDefaultPathForProfileId(profileIdForSlot(slot))
    }

    private fun defaultPortForSlot(slot: Int): Int {
        return (TavernPortDefaults.DEFAULT_TAVERN_PORT + slot - 1).coerceIn(1, 65535)
    }

    private fun nameForSlot(slot: Int): String {
        return when (slot) {
            1 -> MAIN_PROFILE_NAME
            2 -> CLONE_PROFILE_NAME
            else -> "$CLONE_PROFILE_NAME ${slot - 1}"
        }
    }

    private fun slotNumberForId(id: String): Int {
        if (id == MAIN_PROFILE_ID) return 1
        return id.removePrefix("$CLONE_PROFILE_PREFIX")
            .toIntOrNull()
            ?.takeIf { it >= 2 }
            ?: 2
    }
}

object TavernPathNormalizer {
    fun normalize(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return TavernPathDefaults.DEFAULT_TAVERN_DIR_NORMALIZED
        return when {
            trimmed.startsWith("~/") -> "\$HOME/${trimmed.removePrefix("~/")}"
            trimmed == "~" -> "\$HOME"
            trimmed.startsWith("\$HOME/") || trimmed == "\$HOME" -> trimmed
            trimmed.startsWith("/data/") || trimmed.startsWith("/storage/") || trimmed.startsWith("/") -> trimmed
            else -> "\$HOME/$trimmed"
        }
    }

    fun toDisplayPath(value: String): String {
        return value
            .replace("\$HOME/", "~/")
            .replace("\$HOME", "~")
            .replace("${TavernPathDefaults.TERMUX_HOME_DIR}/", "~/")
            .replace(TavernPathDefaults.TERMUX_HOME_DIR, "~")
    }
}

object TavernPortNormalizer {
    fun normalize(value: Int): Int {
        return if (value in 1..65535) value else TavernPortDefaults.DEFAULT_TAVERN_PORT
    }
}

object TavernPathValidator {
    fun validate(value: String): String? {
        val normalized = TavernPathNormalizer.normalize(value)
        return when {
            normalized.isBlank() -> "酒馆目录不能为空。"
            normalized.length > 320 -> "酒馆目录太长了。"
            normalized.contains("::") || normalized.any { it == '\n' || it == '\r' || it.code < 32 } ->
                "酒馆目录里不能有换行或 ::。"

            else -> null
        }
    }
}

private object TavernProfileCollection {
    fun normalize(
        profiles: List<TavernProfile>,
        fallbackProfile: TavernProfile,
    ): List<TavernProfile> {
        val base = normalizeMainProfileSlot(
            if (profiles.isEmpty()) listOf(fallbackProfile) else profiles,
            fallbackProfile = fallbackProfile,
        )
        val usedIds = linkedSetOf<String>()
        return base.mapIndexed { index, profile ->
            val defaultProfile = when {
                profile.id == TavernProfileDefaults.MAIN_PROFILE_ID -> TavernProfileDefaults.profileForId(profile.id)
                profile.id.startsWith("profile-") -> TavernProfileDefaults.profileForId(profile.id)
                index == 0 -> TavernProfileDefaults.profileForId(TavernProfileDefaults.MAIN_PROFILE_ID)
                else -> TavernProfileDefaults.suggestedClone(
                    usedIds.map { TavernProfile(id = it) },
                )
            }
            var nextId = profile.id.trim().ifBlank { defaultProfile.id }
            if (usedIds.contains(nextId)) {
                var suffix = 2
                while (usedIds.contains("$nextId-$suffix")) {
                    suffix += 1
                }
                nextId = "$nextId-$suffix"
            }
            usedIds += nextId
            TavernProfile(
                id = nextId,
                name = profile.name.trim().ifBlank { TavernProfileDefaults.nameForId(nextId) },
                tavernDir = profile.tavernDir.trim().ifBlank { defaultProfile.tavernDir },
                port = TavernPortNormalizer.normalize(profile.port),
            )
        }
    }

    private fun normalizeMainProfileSlot(
        profiles: List<TavernProfile>,
        fallbackProfile: TavernProfile,
    ): List<TavernProfile> {
        if (profiles.isEmpty()) {
            return listOf(fallbackProfile.asMainProfile(preserveName = false))
        }

        val existingMainIndex = profiles.indexOfFirst { it.id.trim() == TavernProfileDefaults.MAIN_PROFILE_ID }
        if (existingMainIndex >= 0) {
            return buildList {
                add(profiles[existingMainIndex].asMainProfile(preserveName = true))
                profiles.forEachIndexed { index, profile ->
                    if (index != existingMainIndex) {
                        add(profile)
                    }
                }
            }
        }

        return buildList {
            add(profiles.first().asMainProfile(preserveName = false))
            addAll(profiles.drop(1))
        }
    }

    private fun TavernProfile.asMainProfile(preserveName: Boolean): TavernProfile {
        return copy(
            id = TavernProfileDefaults.MAIN_PROFILE_ID,
            name = if (preserveName) {
                name.trim().ifBlank { TavernProfileDefaults.MAIN_PROFILE_NAME }
            } else {
                TavernProfileDefaults.MAIN_PROFILE_NAME
            },
        )
    }
}

private object TavernPathConfigCodec {
    fun encode(config: TavernPathConfig): String {
        val normalized = config.withProfiles(config.availableProfiles, config.activeProfile.id)
        val json = JSONObject()
        json.put("activeProfileId", normalized.activeProfile.id)
        val profiles = JSONArray()
        normalized.availableProfiles.forEach { profile ->
            val item = JSONObject()
            item.put("id", profile.id)
            item.put("name", profile.normalizedName)
            item.put("tavernDir", profile.tavernDir)
            item.put("port", profile.normalizedPort)
            profiles.put(item)
        }
        json.put("profiles", profiles)
        return json.toString()
    }

    fun decode(value: String?): TavernPathConfig? {
        val text = value?.trim().orEmpty()
        if (text.isBlank()) return null
        return runCatching {
            val json = JSONObject(text)
            val activeProfileId = json.optString("activeProfileId").ifBlank {
                TavernProfileDefaults.MAIN_PROFILE_ID
            }
            val profilesJson = json.optJSONArray("profiles")
            val profiles = buildList {
                if (profilesJson != null) {
                    for (index in 0 until profilesJson.length()) {
                        val item = profilesJson.optJSONObject(index) ?: continue
                        add(
                            TavernProfile(
                                id = item.optString("id"),
                                name = item.optString("name"),
                                tavernDir = item.optString("tavernDir"),
                                port = item.optInt("port", TavernPortDefaults.DEFAULT_TAVERN_PORT),
                            ),
                        )
                    }
                }
            }
            TavernPathConfig(
                activeProfileId = activeProfileId,
                profiles = profiles,
            ).withProfiles(profiles, activeProfileId)
        }.getOrNull()
    }
}

class TavernPathStore(private val context: Context) {
    fun load(): TavernPathConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        TavernPathConfigCodec.decode(prefs.getString(KEY_PROFILE_CONFIG_JSON, null))?.let { decoded ->
            return decoded
        }

        val stored = prefs.getString(KEY_TAVERN_DIR, null).orEmpty()
        return TavernPathConfig(
            tavernDir = stored.takeIf { TavernPathValidator.validate(it) == null }
                ?: TavernPathDefaults.DEFAULT_TAVERN_DIR,
        )
    }

    fun save(config: TavernPathConfig): TavernPathSaveResult {
        val normalized = config.withProfiles(config.availableProfiles, config.activeProfile.id)
        validateProfileConfig(normalized)?.let { reason ->
            return TavernPathSaveResult(
                saved = false,
                config = load(),
                message = reason,
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_CONFIG_JSON, TavernPathConfigCodec.encode(normalized))
            .putString(KEY_TAVERN_DIR, normalized.activeProfile.tavernDir)
            .apply()
        return TavernPathSaveResult(
            saved = true,
            config = normalized,
            message = "${normalized.activeProfileLabel}已保存：${normalized.displayTavernDir}（端口 ${normalized.normalizedPort}）",
        )
    }

    fun restoreDefault(): TavernPathSaveResult {
        return save(load().restoreActiveProfileDefault())
    }

    private fun validateProfileConfig(config: TavernPathConfig): String? {
        if (config.availableProfiles.isEmpty()) {
            return "至少要保留一个实例。"
        }
        val duplicatePath = config.availableProfiles
            .groupBy { TavernComparablePath.normalize(it.normalizedTavernDir) }
            .entries
            .firstOrNull { it.key.isNotBlank() && it.value.size > 1 }
        if (duplicatePath != null) {
            return "实例目录不能重复：${TavernPathNormalizer.toDisplayPath(duplicatePath.key)}"
        }
        val duplicatePort = config.availableProfiles
            .groupBy { it.normalizedPort }
            .entries
            .firstOrNull { it.value.size > 1 }
        if (duplicatePort != null) {
            return "实例端口不能重复：${duplicatePort.key}"
        }
        config.availableProfiles.forEach { profile ->
            TavernPathValidator.validate(profile.tavernDir)?.let { reason ->
                return "${profile.normalizedName}：$reason"
            }
            TavernProfileReservedPathPolicy.reservedMessageForProfile(profile)?.let { reason ->
                return "${profile.normalizedName}：$reason"
            }
            if (profile.normalizedPort !in 1..65535) {
                return "${profile.normalizedName}的端口无效。"
            }
        }
        return null
    }

    private companion object {
        const val PREFS = "tavern_path_config"
        const val KEY_TAVERN_DIR = "tavern_dir"
        const val KEY_PROFILE_CONFIG_JSON = "profile_config_json"
    }
}
