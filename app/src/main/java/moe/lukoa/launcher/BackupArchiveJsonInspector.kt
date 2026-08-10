package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupArchiveJsonInspection(
    val extensionDisplayName: String? = null,
    val globalTavernHelperScriptNames: List<String> = emptyList(),
    val presetTavernHelperScriptNames: List<String> = emptyList(),
    val localTavernHelperScriptNames: List<String> = emptyList(),
)

object BackupArchiveJsonInspector {
    private const val MAX_SCRIPT_TREE_NODES = 4_000
    private const val MAX_SCRIPT_TREE_DEPTH = 32

    fun inspect(bytes: ByteArray): BackupArchiveJsonInspection {
        if (bytes.isEmpty()) return BackupArchiveJsonInspection()
        return runCatching {
            val root = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val globalScriptNames = linkedSetOf<String>()
            val presetScriptNames = linkedSetOf<String>()
            val localScriptNames = linkedSetOf<String>()

            listOfNotNull(
                root.optJSONObject("extension_settings"),
                root.optJSONObject("extensions"),
            ).forEach { extensionSettings ->
                extensionSettings
                    .optJSONObject("tavern_helper")
                    ?.optJSONObject("script")
                    ?.opt("scripts")
                    ?.let { globalScriptNames += collectScriptNames(it) }
                extensionSettings
                    .optJSONObject("TavernHelper")
                    ?.optJSONObject("script")
                    ?.opt("scriptsRepository")
                    ?.let { globalScriptNames += collectScriptNames(it) }
            }
            root.optJSONObject("extensions")
                ?.optJSONObject("tavern_helper")
                ?.opt("scripts")
                ?.let { presetScriptNames += collectScriptNames(it) }
            root.optJSONObject("data")
                ?.optJSONObject("extensions")
                ?.optJSONObject("tavern_helper")
                ?.opt("scripts")
                ?.let { localScriptNames += collectScriptNames(it) }

            BackupArchiveJsonInspection(
                extensionDisplayName = root.optString("display_name")
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.take(120),
                globalTavernHelperScriptNames = globalScriptNames.toList(),
                presetTavernHelperScriptNames = presetScriptNames.toList(),
                localTavernHelperScriptNames = localScriptNames.toList(),
            )
        }.getOrDefault(BackupArchiveJsonInspection())
    }

    private fun collectScriptNames(root: Any?): List<String> {
        val scriptNames = linkedSetOf<String>()
        var visitedNodes = 0

        fun visit(value: Any?, depth: Int) {
            if (value == null || value === JSONObject.NULL) return
            if (depth > MAX_SCRIPT_TREE_DEPTH || visitedNodes >= MAX_SCRIPT_TREE_NODES) return
            visitedNodes += 1
            when (value) {
                is JSONArray -> {
                    for (index in 0 until value.length()) visit(value.opt(index), depth + 1)
                }
                is JSONObject -> {
                    val type = value.optString("type").trim().lowercase(Locale.ROOT)
                    if (type == "script") {
                        val name = value.optString("name").trim().ifBlank {
                            value.optJSONObject("value")?.optString("name")?.trim().orEmpty()
                        }
                        if (name.isNotBlank()) scriptNames += name.take(120)
                    } else {
                        value.opt("scripts")?.let { visit(it, depth + 1) }
                        value.opt("value")?.let { visit(it, depth + 1) }
                        if (type.isBlank()) {
                            val keys = value.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                if (key != "scripts" && key != "value") {
                                    visit(value.opt(key), depth + 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        visit(root, 0)
        return scriptNames.toList()
    }
}
