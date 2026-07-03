package moe.lukoa.launcher

enum class FirstTavernStartGuideKind {
    IQooBackgroundPermission,
    KeepTermuxInSmallWindow,
}

data class FirstTavernStartGuide(
    val kind: FirstTavernStartGuideKind,
)

object FirstTavernStartGuideResolver {
    fun resolve(
        brand: String,
        manufacturer: String,
        model: String,
    ): FirstTavernStartGuide {
        val signals = listOf(brand, manufacturer, model)
            .joinToString("\n")
            .trim()
        val kind = if (signals.contains("iqoo", ignoreCase = true)) {
            FirstTavernStartGuideKind.IQooBackgroundPermission
        } else {
            FirstTavernStartGuideKind.KeepTermuxInSmallWindow
        }
        return FirstTavernStartGuide(kind = kind)
    }

    fun shouldShow(
        alreadyShown: Boolean,
        tavernInstallDetected: Boolean?,
        tavernRunning: Boolean,
        termuxLog: String,
        appLog: String,
    ): Boolean {
        if (alreadyShown) return false
        if (tavernInstallDetected != true) return false
        if (tavernRunning) return false
        return !hasSuccessfulStartHistory(termuxLog, appLog)
    }

    fun hasSuccessfulStartHistory(
        termuxLog: String,
        appLog: String,
    ): Boolean {
        val merged = "$termuxLog\n$appLog"
        return successfulStartMarkers.any { marker ->
            merged.contains(marker, ignoreCase = true)
        }
    }

    private val successfulStartMarkers = listOf(
        "SillyTavern is listening on",
        "SillyTavern HTTP endpoint is responding",
        "Go to: http://127.0.0.1:8000/",
        "Go to: http://localhost:8000/",
    )
}
