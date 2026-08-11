package moe.lukoa.launcher

object LauncherTransientOperationState {
    fun finishUploadLimit(state: TavernUploadLimitStatus): TavernUploadLimitStatus {
        return if (state.checking) {
            state.copy(
                checking = false,
                message = "读取已结束，但没有收到完整结果，请重新检查。",
            )
        } else {
            state
        }
    }

    fun finishUsers(state: TavernUserManagementState): TavernUserManagementState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整用户列表，请重试。",
            )
        } else {
            state
        }
    }

    fun finishExtensions(state: TavernExtensionManagementState): TavernExtensionManagementState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整扩展列表，请重试。",
            )
        } else {
            state
        }
    }
}
