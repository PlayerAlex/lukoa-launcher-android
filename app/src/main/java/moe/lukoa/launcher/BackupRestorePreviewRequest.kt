package moe.lukoa.launcher

data class BackupRestorePreviewRequest(
    val token: Long,
    val archivePath: String,
)

sealed interface BackupPreviewUiState {
    data object Hidden : BackupPreviewUiState

    data class Loading(
        val request: BackupRestorePreviewRequest,
    ) : BackupPreviewUiState

    data class Ready(
        val preview: BackupRestorePreview,
    ) : BackupPreviewUiState
}

class BackupRestorePreviewRequestCoordinator {
    private var nextToken = 0L
    private var activeRequest: BackupRestorePreviewRequest? = null

    fun begin(
        archivePath: String,
    ): BackupRestorePreviewRequest {
        nextToken += 1L
        return BackupRestorePreviewRequest(
            token = nextToken,
            archivePath = archivePath.trim(),
        ).also { activeRequest = it }
    }

    fun accepts(request: BackupRestorePreviewRequest, currentArchivePath: String): Boolean {
        return activeRequest == request && request.archivePath == currentArchivePath.trim()
    }

    fun finish(request: BackupRestorePreviewRequest) {
        if (activeRequest == request) {
            activeRequest = null
        }
    }

    fun cancel() {
        activeRequest = null
    }
}
