package moe.lukoa.launcher

import android.content.Context

data class TavernPathConfig(
    val tavernDir: String = TavernPathDefaults.DEFAULT_TAVERN_DIR,
) {
    val normalizedTavernDir: String
        get() = TavernPathNormalizer.normalize(tavernDir)

    val displayTavernDir: String
        get() = TavernPathNormalizer.toDisplayPath(normalizedTavernDir)
}

data class TavernPathSaveResult(
    val saved: Boolean,
    val config: TavernPathConfig,
    val message: String,
)

object TavernPathDefaults {
    const val DEFAULT_TAVERN_DIR = "~/SillyTavern"
    const val DEFAULT_TAVERN_DIR_NORMALIZED = "\$HOME/SillyTavern"
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
        return value.replace("\$HOME/", "~/").replace("\$HOME", "~")
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

class TavernPathStore(private val context: Context) {
    fun load(): TavernPathConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_TAVERN_DIR, null).orEmpty()
        return TavernPathConfig(
            tavernDir = stored.takeIf { TavernPathValidator.validate(it) == null }
                ?: TavernPathDefaults.DEFAULT_TAVERN_DIR,
        )
    }

    fun save(config: TavernPathConfig): TavernPathSaveResult {
        val normalized = config.normalizedTavernDir
        TavernPathValidator.validate(normalized)?.let { reason ->
            return TavernPathSaveResult(
                saved = false,
                config = load(),
                message = reason,
            )
        }
        val savedConfig = TavernPathConfig(tavernDir = normalized)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAVERN_DIR, savedConfig.tavernDir)
            .apply()
        return TavernPathSaveResult(
            saved = true,
            config = savedConfig,
            message = "酒馆目录已保存：${savedConfig.displayTavernDir}",
        )
    }

    fun restoreDefault(): TavernPathSaveResult {
        return save(TavernPathConfig(TavernPathDefaults.DEFAULT_TAVERN_DIR))
    }

    private companion object {
        const val PREFS = "tavern_path_config"
        const val KEY_TAVERN_DIR = "tavern_dir"
    }
}
