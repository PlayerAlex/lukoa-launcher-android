package moe.lukoa.launcher

import android.content.Context
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class BackupContentCatalogState(
    val summary: BackupArchiveContentSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String = "",
)

class BackupContentCatalogStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun read(details: BackupLibraryArchiveDetails): BackupArchiveContentSummary? {
        val entry = readCatalog().optJSONObject(normalizePath(details.termuxReadablePath)) ?: return null
        if (entry.optLong("size", Long.MIN_VALUE) != details.size) return null
        if (entry.optLong("modifiedAtMillis", Long.MIN_VALUE) != details.modifiedAtMillis) return null
        return entry.optJSONObject("summary")?.toSummary()
    }

    @Synchronized
    fun write(details: BackupLibraryArchiveDetails, summary: BackupArchiveContentSummary) {
        val catalog = readCatalog()
        catalog.put(
            normalizePath(details.termuxReadablePath),
            JSONObject()
                .put("size", details.size)
                .put("modifiedAtMillis", details.modifiedAtMillis)
                .put("summary", summary.toJson()),
        )
        preferences.edit().putString(KEY_CATALOG, catalog.toString()).apply()
    }

    @Synchronized
    fun prune(activeArchives: Collection<BackupLibraryArchiveDetails>) {
        val activeKeys = activeArchives.mapTo(linkedSetOf()) {
            normalizePath(it.termuxReadablePath)
        }
        val catalog = readCatalog()
        val keys = catalog.keys().asSequence().toList()
        var changed = false
        keys.filterNot(activeKeys::contains).forEach { key ->
            catalog.remove(key)
            changed = true
        }
        if (changed) {
            preferences.edit().putString(KEY_CATALOG, catalog.toString()).apply()
        }
    }

    private fun readCatalog(): JSONObject {
        val raw = preferences.getString(KEY_CATALOG, null).orEmpty()
        return runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrElse { JSONObject() }
    }

    private fun BackupArchiveContentSummary.toJson(): JSONObject = JSONObject()
        .put("entryCount", entryCount)
        .put("hasUserData", hasUserData)
        .put("hasExtensions", hasExtensions)
        .put("hasConfiguration", hasConfiguration)
        .put("hasLukoaManifest", hasLukoaManifest)
        .put("truncated", truncated)
        .put(
            "groups",
            JSONArray().apply {
                displayGroups.forEach { group ->
                    put(
                        JSONObject()
                            .put("kind", group.kind.name)
                            .put("entryCount", group.entryCount)
                            .put("names", JSONArray(group.names))
                            .put("namesTruncated", group.namesTruncated)
                            .put("children", group.children.toJson()),
                    )
                }
            },
        )

    private fun JSONObject.toSummary(): BackupArchiveContentSummary? = runCatching {
        val groupsJson = optJSONArray("groups") ?: JSONArray()
        val groups = buildList {
            for (index in 0 until groupsJson.length()) {
                val groupJson = groupsJson.optJSONObject(index) ?: continue
                val kind = runCatching {
                    BackupArchiveContentKind.valueOf(groupJson.getString("kind"))
                }.getOrNull() ?: continue
                val namesJson = groupJson.optJSONArray("names") ?: JSONArray()
                val names = buildList {
                    for (nameIndex in 0 until namesJson.length()) {
                        namesJson.optString(nameIndex).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                add(
                    BackupArchiveContentGroup(
                        kind = kind,
                        entryCount = groupJson.optInt("entryCount", names.size),
                        names = names,
                        namesTruncated = groupJson.optBoolean("namesTruncated", false),
                        children = groupJson.optJSONArray("children")?.toNodes() ?: emptyList(),
                    ),
                )
            }
        }
        BackupArchiveContentSummary(
            entryCount = getInt("entryCount"),
            hasUserData = optBoolean("hasUserData", false),
            hasExtensions = optBoolean("hasExtensions", false),
            hasConfiguration = optBoolean("hasConfiguration", false),
            hasLukoaManifest = optBoolean("hasLukoaManifest", false),
            truncated = optBoolean("truncated", false),
            groups = groups,
        )
    }.getOrNull()

    private fun List<BackupArchiveContentNode>.toJson(): JSONArray = JSONArray().apply {
        forEach { node ->
            put(
                JSONObject()
                    .put("title", node.title)
                    .put("entryCount", node.entryCount)
                    .put("names", JSONArray(node.names))
                    .put("children", node.children.toJson()),
            )
        }
    }

    private fun JSONArray.toNodes(depth: Int = 0): List<BackupArchiveContentNode> {
        if (depth > MAX_CATALOG_TREE_DEPTH) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val nodeJson = optJSONObject(index) ?: continue
                val title = nodeJson.optString("title").trim().takeIf(String::isNotBlank)
                    ?: continue
                val namesJson = nodeJson.optJSONArray("names") ?: JSONArray()
                val names = buildList {
                    for (nameIndex in 0 until namesJson.length()) {
                        namesJson.optString(nameIndex).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                add(
                    BackupArchiveContentNode(
                        title = title,
                        entryCount = nodeJson.optInt("entryCount", names.size),
                        names = names,
                        children = nodeJson.optJSONArray("children")
                            ?.toNodes(depth + 1)
                            ?: emptyList(),
                    ),
                )
            }
        }
    }

    private fun normalizePath(path: String): String {
        return path.trim().replace('\\', '/').lowercase(Locale.ROOT)
    }

    companion object {
        const val PREFS_NAME = "lukoa_backup_content_catalog"
        private const val KEY_CATALOG = "catalog_v5"
        private const val MAX_CATALOG_TREE_DEPTH = 8
    }
}
