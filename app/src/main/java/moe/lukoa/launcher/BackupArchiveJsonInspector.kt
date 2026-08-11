package moe.lukoa.launcher

import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupArchiveJsonInspection(
    val extensionDisplayName: String? = null,
    val globalRegexScriptNames: List<String> = emptyList(),
    val presetRegexScriptNames: List<String> = emptyList(),
    val localRegexScriptNames: List<String> = emptyList(),
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
            val globalRegexScriptNames = linkedSetOf<String>()
            val presetRegexScriptNames = linkedSetOf<String>()
            val localRegexScriptNames = linkedSetOf<String>()

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
            root.optJSONObject("extension_settings")
                ?.let { globalRegexScriptNames += collectRegexNamesFromContainer(it) }
            root.optJSONObject("extensions")
                ?.let { presetRegexScriptNames += collectRegexNamesFromContainer(it) }
            root.optJSONObject("data")
                ?.optJSONObject("extensions")
                ?.let { localRegexScriptNames += collectRegexNamesFromContainer(it) }
            globalRegexScriptNames += collectRegexNamesFromContainer(root)

            BackupArchiveJsonInspection(
                extensionDisplayName = root.optString("display_name")
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.take(120),
                globalRegexScriptNames = globalRegexScriptNames.toList(),
                presetRegexScriptNames = presetRegexScriptNames.toList(),
                localRegexScriptNames = localRegexScriptNames.toList(),
                globalTavernHelperScriptNames = globalScriptNames.toList(),
                presetTavernHelperScriptNames = presetScriptNames.toList(),
                localTavernHelperScriptNames = localScriptNames.toList(),
            )
        }.getOrDefault(BackupArchiveJsonInspection())
    }

    private fun collectRegexNamesFromContainer(container: JSONObject): List<String> {
        val names = linkedSetOf<String>()
        listOf("regex", "regex_scripts", "regexScripts").forEach { key ->
            container.opt(key)?.let { names += collectRegexNames(it) }
        }
        return names.toList()
    }

    private fun collectRegexNames(root: Any?): List<String> {
        val names = linkedSetOf<String>()
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
                    val name = sequenceOf("scriptName", "script_name")
                        .map { key -> value.optString(key).trim() }
                        .firstOrNull(String::isNotBlank)
                        ?: value.optString("name").trim().takeIf {
                            it.isNotBlank() && (
                                value.has("findRegex") ||
                                    value.has("find_regex") ||
                                    value.has("replaceString") ||
                                    value.has("replace_string")
                                )
                        }
                    name?.let { names += it.take(120) }
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        visit(value.opt(keys.next()), depth + 1)
                    }
                }
            }
        }

        visit(root, 0)
        return names.toList()
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
