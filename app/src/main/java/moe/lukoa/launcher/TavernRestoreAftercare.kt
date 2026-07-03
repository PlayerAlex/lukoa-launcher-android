package moe.lukoa.launcher

data class TavernRestoreAftercare(
    val restoredTo: String = "",
    val previousDirectory: String = "",
    val externalDataRootRestored: String = "",
    val externalDataRootPrevious: String = "",
    val dependencyNotice: String = "",
)

object TavernRestoreAftercareMessage {
    fun successMessage(output: String): String {
        val aftercare = parse(output)
        val lines = mutableListOf("酒馆备份已应用。")
        if (aftercare.previousDirectory.isNotBlank() && aftercare.previousDirectory != "none") {
            lines += "恢复前的旧酒馆目录已保留：${aftercare.previousDirectory}"
        }
        if (aftercare.externalDataRootRestored.isNotBlank() && aftercare.externalDataRootRestored != "none") {
            lines += "备份里的外部数据目录也已一起恢复。"
        }
        if (aftercare.externalDataRootPrevious.isNotBlank() && aftercare.externalDataRootPrevious != "none") {
            lines += "原外部数据目录已保留：${aftercare.externalDataRootPrevious}"
        }
        lines += if (aftercare.dependencyNotice.isNotBlank()) {
            "恢复后先重新检测酒馆版本；如果启动时报依赖缺失，再执行一次更新。"
        } else {
            "恢复后先重新检测酒馆版本，再启动酒馆。"
        }
        return lines.joinToString("\n")
    }

    fun parse(output: String): TavernRestoreAftercare {
        return TavernRestoreAftercare(
            restoredTo = output.lineValue("restoredTo").orEmpty(),
            previousDirectory = output.lineValue("previousDirectory").orEmpty(),
            externalDataRootRestored = output.lineValue("externalDataRootRestored").orEmpty(),
            externalDataRootPrevious = output.lineValue("externalDataRootPrevious").orEmpty(),
            dependencyNotice = output.lineValue("notice").orEmpty(),
        )
    }

    private fun String.lineValue(key: String): String? {
        return lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
