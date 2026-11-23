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

    fun finishChatGuard(state: TavernChatGuardState): TavernChatGuardState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整守护状态，请重试。",
            )
        } else {
            state
        }
    }

    fun finishConfig(state: TavernConfigManagementState): TavernConfigManagementState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整 config.yaml 配置，请重试。",
            )
        } else {
            state
        }
    }

    fun finishStorage(state: TavernStorageState): TavernStorageState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "扫描已结束，但没有收到完整空间统计，请重试。",
            )
        } else {
            state
        }
    }

    fun finishServerPlugins(state: TavernServerPluginState): TavernServerPluginState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整服务器插件列表，请重试。",
            )
        } else {
            state
        }
    }

    fun finishGitStash(state: TavernGitStashState): TavernGitStashState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "读取已结束，但没有收到完整暂存列表，请重试。",
            )
        } else {
            state
        }
    }

    fun finishQuickImport(state: TavernQuickImportState): TavernQuickImportState {
        return if (state.loading) {
            state.copy(
                loading = false,
                message = "目录解析已结束，如未显示请重试。",
            )
        } else {
            state
        }
    }

    fun finishNodeMemory(state: TavernNodeMemoryStatus): TavernNodeMemoryStatus {
        return if (state.checking) {
            state.copy(
                checking = false,
                message = "读取已结束，但没有收到完整内存设置，请重试。",
            )
        } else {
            state
        }
    }

    fun finishProxyTest(state: TavernProxyTestState): TavernProxyTestState {
        return if (state.testing) {
            state.copy(
                testing = false,
                message = "代理连通性测试已结束，未收到完整返回，请重试。",
            )
        } else {
            state
        }
    }
}
