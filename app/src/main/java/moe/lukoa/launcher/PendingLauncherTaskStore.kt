package moe.lukoa.launcher

import android.content.Context
import org.json.JSONObject

enum class PendingLauncherTaskKind(
    val title: String,
) {
    InstallTavern("安装酒馆"),
    UpdateTavern("更新酒馆"),
    RollbackTavern("回退酒馆"),
    ManualBackup("创建酒馆备份"),
    RestoreBackup("应用酒馆备份"),
}

data class PendingLauncherTask(
    val kind: PendingLauncherTaskKind,
    val commandName: String,
    val detail: String,
    val startedAtMillis: Long,
    val targetLabel: String = "",
    val archivePath: String = "",
    val safetyBackupPath: String = "",
) {
    val title: String
        get() = kind.title
}

object PendingLauncherTaskStore {
    private const val PREFS = "lukoa_pending_launcher_task"
    private const val KEY_TASK = "task"

    fun save(context: Context, task: PendingLauncherTask) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TASK, encode(task))
            .apply()
    }

    fun load(context: Context): PendingLauncherTask? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TASK, null)
            .orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            decode(JSONObject(raw))
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TASK)
            .apply()
    }

    private fun encode(task: PendingLauncherTask): String {
        return JSONObject().apply {
            put("kind", task.kind.name)
            put("commandName", task.commandName)
            put("detail", task.detail)
            put("startedAtMillis", task.startedAtMillis)
            put("targetLabel", task.targetLabel)
            put("archivePath", task.archivePath)
            put("safetyBackupPath", task.safetyBackupPath)
        }.toString()
    }

    private fun decode(objectJson: JSONObject): PendingLauncherTask {
        return PendingLauncherTask(
            kind = PendingLauncherTaskKind.valueOf(objectJson.optString("kind")),
            commandName = objectJson.optString("commandName", ""),
            detail = objectJson.optString("detail", ""),
            startedAtMillis = objectJson.optLong("startedAtMillis", 0L),
            targetLabel = objectJson.optString("targetLabel", ""),
            archivePath = objectJson.optString("archivePath", ""),
            safetyBackupPath = objectJson.optString("safetyBackupPath", ""),
        )
    }
}
