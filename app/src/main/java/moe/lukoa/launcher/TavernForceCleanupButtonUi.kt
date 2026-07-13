package moe.lukoa.launcher

object TavernForceCleanupButtonUi {
    const val DefaultLabel = "强制释放端口 / 清理残留进程"
    const val DefaultHint = "只有普通停止没退干净，或你确认当前实例有残留进程、端口冲突时，才建议继续。"

    fun labelFor(suggestion: TavernForceCleanupSuggestion?): String {
        return suggestion?.buttonLabel ?: DefaultLabel
    }

    fun hintFor(suggestion: TavernForceCleanupSuggestion?): String {
        return suggestion?.buttonHint ?: DefaultHint
    }
}
