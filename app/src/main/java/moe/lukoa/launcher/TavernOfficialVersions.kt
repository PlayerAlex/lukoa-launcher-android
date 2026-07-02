package moe.lukoa.launcher

enum class TavernVersionKind(val title: String) {
    Stable("稳定版"),
    Test("测试版"),
    Custom("自定义"),
}

data class TavernVersionChoice(
    val kind: TavernVersionKind,
    val name: String,
    val target: String,
    val commit: String = "",
    val repoUrl: String = "",
) {
    val label: String
        get() = when (kind) {
            TavernVersionKind.Stable -> name
            TavernVersionKind.Test -> "$name 分支"
            TavernVersionKind.Custom -> "$name 自定义"
        }
}

data class TavernOfficialVersions(
    val stable: List<TavernVersionChoice> = emptyList(),
    val test: List<TavernVersionChoice> = emptyList(),
) {
    val hasData: Boolean
        get() = stable.isNotEmpty() || test.isNotEmpty()

    val all: List<TavernVersionChoice>
        get() = stable + test
}

object TavernInstallDefaults {
    val Release = TavernVersionChoice(
        kind = TavernVersionKind.Test,
        name = "release",
        target = "release",
        repoUrl = TavernMirrorDefaults.OFFICIAL_REPO,
    )
}

object TavernOfficialVersionParser {
    fun parse(text: String): TavernOfficialVersions {
        val values = mutableMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            val splitAt = trimmed.indexOf('=')
            if (splitAt <= 0) return@forEach
            val key = trimmed.substring(0, splitAt).trim()
            if (
                key.startsWith("stable.") ||
                key.startsWith("test.") ||
                key == "official.repo"
            ) {
                values[key] = trimmed.substring(splitAt + 1).trim()
            }
        }

        val repoUrl = values["official.repo"].orEmpty()
        return TavernOfficialVersions(
            stable = parseChoices(values, "stable", TavernVersionKind.Stable, repoUrl),
            test = parseChoices(values, "test", TavernVersionKind.Test, repoUrl),
        )
    }

    fun encode(versions: TavernOfficialVersions): String {
        if (!versions.hasData) return ""
        val repoUrl = versions.all.firstOrNull { it.repoUrl.isNotBlank() }?.repoUrl.orEmpty()
        return buildString {
            if (repoUrl.isNotBlank()) {
                appendLine("official.repo=$repoUrl")
            }
            appendChoices(this, versions.stable, "stable")
            appendChoices(this, versions.test, "test")
        }.trim()
    }

    private fun parseChoices(
        values: Map<String, String>,
        prefix: String,
        kind: TavernVersionKind,
        repoUrl: String,
    ): List<TavernVersionChoice> {
        return (1..20).mapNotNull { index ->
            val name = values["$prefix.$index.name"].orEmpty()
            val target = values["$prefix.$index.target"].orEmpty()
            if (name.isBlank() || target.isBlank()) {
                null
            } else {
                TavernVersionChoice(
                    kind = kind,
                    name = name,
                    target = target,
                    commit = values["$prefix.$index.commit"].orEmpty(),
                    repoUrl = repoUrl,
                )
            }
        }
    }

    private fun appendChoices(
        builder: StringBuilder,
        values: List<TavernVersionChoice>,
        prefix: String,
    ) {
        values.forEachIndexed { index, choice ->
            val itemIndex = index + 1
            builder.appendLine("$prefix.$itemIndex.name=${choice.name}")
            builder.appendLine("$prefix.$itemIndex.target=${choice.target}")
            if (choice.commit.isNotBlank()) {
                builder.appendLine("$prefix.$itemIndex.commit=${choice.commit}")
            }
        }
    }
}
