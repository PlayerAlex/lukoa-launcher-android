package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupArchiveJsonInspection(
    val extensionDisplayName: String? = null,
    val tavernHelperScriptNames: List<String> = emptyList(),
)

object BackupArchiveJsonInspector {
    private const val MAX_SCRIPT_TREE_NODES = 4_000
    private const val MAX_SCRIPT_TREE_DEPTH = 32

    fun inspect(bytes: ByteArray): BackupArchiveJsonInspection {
        if (bytes.isEmpty()) return BackupArchiveJsonInspection()
        return runCatching {
            val root = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val scriptNames = linkedSetOf<String>()
            var visitedNodes = 0

            fun collectScripts(value: Any?, depth: Int) {
                if (value == null || value === JSONObject.NULL) return
                if (depth > MAX_SCRIPT_TREE_DEPTH || visitedNodes >= MAX_SCRIPT_TREE_NODES) return
                visitedNodes += 1
                when (value) {
                    is JSONArray -> {
                        for (index in 0 until value.length()) {
                            collectScripts(value.opt(index), depth + 1)
                        }
                    }
                    is JSONObject -> {
                        val type = value.optString("type").trim().lowercase(Locale.ROOT)
                        if (type == "script") {
                            val name = value.optString("name").trim().ifBlank {
                                value.optJSONObject("value")?.optString("name")?.trim().orEmpty()
                            }
                            if (name.isNotBlank()) scriptNames += name.take(120)
                        } else {
                            value.opt("scripts")?.let { collectScripts(it, depth + 1) }
                            value.opt("value")?.let { collectScripts(it, depth + 1) }
                            if (type.isBlank()) {
                                val keys = value.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    if (key != "scripts" && key != "value") {
                                        collectScripts(value.opt(key), depth + 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val extensionSettings = root.optJSONObject("extension_settings")
            extensionSettings
                ?.optJSONObject("tavern_helper")
                ?.optJSONObject("script")
                ?.opt("scripts")
                ?.let { collectScripts(it, 0) }
            extensionSettings
                ?.optJSONObject("TavernHelper")
                ?.optJSONObject("script")
                ?.opt("scriptsRepository")
                ?.let { collectScripts(it, 0) }
            root.optJSONObject("extensions")
                ?.optJSONObject("tavern_helper")
                ?.opt("scripts")
                ?.let { collectScripts(it, 0) }
            root.optJSONObject("data")
                ?.optJSONObject("extensions")
                ?.optJSONObject("tavern_helper")
                ?.opt("scripts")
                ?.let { collectScripts(it, 0) }

            BackupArchiveJsonInspection(
                extensionDisplayName = root.optString("display_name")
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.take(120),
                tavernHelperScriptNames = scriptNames.toList(),
            )
        }.getOrDefault(BackupArchiveJsonInspection())
    }
}
