package moe.lukoa.launcher

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LukoaPaletteStructureTest {
    @Test
    fun structuralPaletteRoles_areNotMadeTransparentAtCallSites() {
        val sourceRoot = findSourceRoot()
        val structuralAlpha = Regex(
            """LukoaColors\.(?:Background|Surface|Elevated|Border|PrimarySoft|AccentSoft|DangerSoft)""" +
                """\s*\.copy\s*\(\s*alpha\s*=""",
        )
        val violations = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { index, line ->
                    if (structuralAlpha.containsMatchIn(line)) {
                        "${file.name}:${index + 1}: ${line.trim()}"
                    } else {
                        null
                    }
                }
            }
            .toList()

        assertTrue(
            "结构色已经是最终显示色，不能再次叠加透明度：\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private fun findSourceRoot(): File {
        var cursor = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            val candidate = File(cursor, "app/src/main/java")
            if (candidate.isDirectory) return candidate
            cursor = requireNotNull(cursor.parentFile) {
                "找不到 app/src/main/java"
            }
        }
    }
}
