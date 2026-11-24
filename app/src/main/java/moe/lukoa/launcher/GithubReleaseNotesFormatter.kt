package moe.lukoa.launcher

internal data class GithubReleaseNotesDocument(
    val versionTitle: String,
    val sections: List<GithubReleaseNotesSection>,
)

internal data class GithubReleaseNotesSection(
    val title: String,
    val items: List<String>,
)

private enum class GithubReleaseNoteSection(
    val title: String,
) {
    New("新增功能"),
    Optimize("体验优化"),
    Fix("修复更新"),
    Notice("使用说明"),
    Other("其他调整"),
}

object GithubReleaseNotesFormatter {
    internal fun parse(versionName: String, body: String): GithubReleaseNotesDocument {
        val normalizedVersion = versionName.trim().ifBlank { "当前" }
        val items = parseItems(body).ifEmpty {
            listOf(
                GithubReleaseNoteSection.Other to cleanInlineMarkdown(body)
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .joinToString("\n")
                    .ifBlank { "这个版本没有填写更新说明。" },
            )
        }
        val sections = GithubReleaseNoteSection.entries.mapNotNull { section ->
            items.asSequence()
                .filter { it.first == section }
                .map { it.second }
                .distinct()
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let { GithubReleaseNotesSection(title = section.title, items = it) }
        }
        return GithubReleaseNotesDocument(
            versionTitle = "$normalizedVersion 版本更新日志：",
            sections = sections,
        )
    }

    fun format(versionName: String, body: String): String {
        val document = parse(versionName, body)
        return buildString {
            append(document.versionTitle)
            document.sections.forEach { section ->
                append("\n\n")
                append(section.title)
                append('：')
                section.items.forEachIndexed { index, item ->
                    append('\n')
                    append(index + 1)
                    append(". ")
                    append(item)
                }
            }
        }
    }

    private fun parseItems(body: String): List<Pair<GithubReleaseNoteSection, String>> {
        val orderedItems = mutableListOf<Pair<GithubReleaseNoteSection, String>>()
        var currentSection = GithubReleaseNoteSection.Other
        var inCodeBlock = false
        body.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEach
            }
            if (inCodeBlock || line.isBlank()) return@forEach
            if (line.startsWith("#")) {
                currentSection = sectionForHeading(line.trimStart('#').trim())
                return@forEach
            }

            val bulletText = when {
                line.matches(Regex("""^[-*+]\s+.+$""")) -> line.substringAfter(' ').trim()
                line.matches(Regex("""^\d+\.\s+.+$""")) ->
                    line.replaceFirst(Regex("""^\d+\.\s+"""), "").trim()
                else -> null
            }
            val content = cleanInlineMarkdown(bulletText ?: line).trim()
            if (content.isBlank() || content == "本次更新") return@forEach

            val section = when {
                bulletText == null -> currentSection
                currentSection == GithubReleaseNoteSection.Notice -> GithubReleaseNoteSection.Notice
                else -> sectionForText(content, currentSection)
            }
            if (shouldKeepAsItem(content, bulletText != null, currentSection)) {
                orderedItems += section to content.trimEnd('。', '：', ':')
            }
        }
        return orderedItems.distinctBy { it.second }
    }

    private fun shouldKeepAsItem(
        text: String,
        fromBullet: Boolean,
        currentSection: GithubReleaseNoteSection,
    ): Boolean {
        if (fromBullet) return true
        if (text == "本次更新") return false
        if (currentSection == GithubReleaseNoteSection.Other) return false
        return text.contains('。') || text.contains('，') || text.contains(':') || text.contains('：')
    }

    private fun sectionForHeading(text: String): GithubReleaseNoteSection {
        val normalized = text.lowercase()
        return when {
            normalized.contains("验证") ||
                normalized.contains("说明") ||
                normalized.contains("提示") ||
                normalized.contains("注意") ||
                normalized.contains("已知") -> GithubReleaseNoteSection.Notice
            normalized.contains("修复") || normalized.contains("修正") || normalized.contains("解决") ->
                GithubReleaseNoteSection.Fix
            normalized.contains("新增") || normalized.contains("新功能") || normalized.contains("全新") ->
                GithubReleaseNoteSection.New
            normalized.contains("优化") ||
                normalized.contains("改进") ||
                normalized.contains("调整") ||
                normalized.contains("重绘") ||
                normalized.contains("重构") ||
                normalized.contains("精简") ||
                normalized.contains("去除") ||
                normalized.contains("改为") ||
                normalized.contains("升级") ||
                normalized.contains("体验") ||
                normalized.contains("界面") ||
                normalized.contains("弹窗") ||
                normalized.contains("引导") -> GithubReleaseNoteSection.Optimize
            else -> GithubReleaseNoteSection.Other
        }
    }

    private fun sectionForText(
        text: String,
        fallback: GithubReleaseNoteSection,
    ): GithubReleaseNoteSection {
        val normalized = text.lowercase()
        return when {
            normalized.startsWith("新增") || normalized.startsWith("支持") -> GithubReleaseNoteSection.New
            normalized.startsWith("优化") ||
                normalized.startsWith("改进") ||
                normalized.startsWith("精简") ||
                normalized.startsWith("移除") ||
                normalized.startsWith("去掉") ||
                normalized.startsWith("升级") ||
                normalized.startsWith("重构") -> GithubReleaseNoteSection.Optimize
            normalized.startsWith("修复") || normalized.startsWith("修正") -> GithubReleaseNoteSection.Fix
            normalized.startsWith("说明") || normalized.startsWith("注意") || normalized.startsWith("提示") ->
                GithubReleaseNoteSection.Notice
            else -> fallback
        }
    }

    private fun cleanInlineMarkdown(text: String): String {
        return text
            .replace(Regex("""\[(.+?)]\((.+?)\)"""), "$1")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("""^\s*[-*+]\s+"""), "")
            .trim()
    }
}
