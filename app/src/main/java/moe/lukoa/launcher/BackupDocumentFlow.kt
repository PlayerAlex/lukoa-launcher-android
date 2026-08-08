package moe.lukoa.launcher

data class BackupDocumentFlowState(
    val importPending: Boolean = false,
    val importUri: String = "",
    val exportSourcePath: String = "",
    val exportFileName: String = "",
    val exportUri: String = "",
) {
    val exportPending: Boolean
        get() = exportSourcePath.isNotBlank()
}

sealed interface BackupDocumentResult {
    data class Import(val result: ExternalBackupImportResult) : BackupDocumentResult

    data class Export(val result: BackupExportDestinationResult) : BackupDocumentResult
}

object BackupDocumentFlowStateCodec {
    const val KEY_IMPORT_PENDING = "backup_document.import_pending"
    const val KEY_IMPORT_URI = "backup_document.import_uri"
    const val KEY_EXPORT_SOURCE_PATH = "backup_document.export_source_path"
    const val KEY_EXPORT_FILE_NAME = "backup_document.export_file_name"
    const val KEY_EXPORT_URI = "backup_document.export_uri"

    fun encode(state: BackupDocumentFlowState): Map<String, String> = buildMap {
        put(KEY_IMPORT_PENDING, state.importPending.toString())
        put(KEY_IMPORT_URI, state.importUri)
        put(KEY_EXPORT_SOURCE_PATH, state.exportSourcePath)
        put(KEY_EXPORT_FILE_NAME, state.exportFileName)
        put(KEY_EXPORT_URI, state.exportUri)
    }

    fun decode(value: (String) -> String?): BackupDocumentFlowState {
        return BackupDocumentFlowState(
            importPending = value(KEY_IMPORT_PENDING).toBoolean(),
            importUri = value(KEY_IMPORT_URI).orEmpty(),
            exportSourcePath = value(KEY_EXPORT_SOURCE_PATH).orEmpty(),
            exportFileName = value(KEY_EXPORT_FILE_NAME).orEmpty(),
            exportUri = value(KEY_EXPORT_URI).orEmpty(),
        )
    }
}
