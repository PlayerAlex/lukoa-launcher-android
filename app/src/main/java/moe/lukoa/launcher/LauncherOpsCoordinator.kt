package moe.lukoa.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LauncherOpsUiState {
    var chatGuardState by mutableStateOf(TavernChatGuardState())
    var configState by mutableStateOf(TavernConfigManagementState())
    var proxyTestState by mutableStateOf(TavernProxyTestState())
    var storageState by mutableStateOf(TavernStorageState())
    var serverPluginState by mutableStateOf(TavernServerPluginState())
    var gitStashState by mutableStateOf(TavernGitStashState())
    var quickImportState by mutableStateOf(TavernQuickImportState())
    var nodeMemoryStatus by mutableStateOf(TavernNodeMemoryStatus())

    fun resetForProfileSwitch(prefix: String = "已切换酒馆实例") {
        chatGuardState = TavernChatGuardState(message = "$prefix，请重新读取聊天守护状态。")
        configState = TavernConfigManagementState(message = "$prefix，请重新读取 config.yaml 配置。")
        proxyTestState = TavernProxyTestState()
        storageState = TavernStorageState(message = "$prefix，请重新扫描磁盘占用。")
        serverPluginState = TavernServerPluginState(message = "$prefix，请重新读取服务器插件。")
        gitStashState = TavernGitStashState(message = "$prefix，请重新读取暂存记录。")
        quickImportState = TavernQuickImportState()
        nodeMemoryStatus = TavernNodeMemoryStatus(message = "$prefix，请重新读取当前运行内存上限。")
    }

    fun clearConfigSavedNeedsRestart() {
        if (configState.savedNeedsRestart) {
            configState = configState.copy(savedNeedsRestart = false)
        }
    }

    fun updateQuickImportUserHandle(handle: String) {
        quickImportState = quickImportState.copy(userHandle = handle.take(64))
    }

    fun updateQuickImportChatCharacter(characterDir: String) {
        quickImportState = quickImportState.copy(chatCharacterDir = characterDir.take(120))
    }

    fun finishTransientOperations() {
        chatGuardState = LauncherTransientOperationState.finishChatGuard(chatGuardState)
        configState = LauncherTransientOperationState.finishConfig(configState)
        proxyTestState = LauncherTransientOperationState.finishProxyTest(proxyTestState)
        storageState = LauncherTransientOperationState.finishStorage(storageState)
        serverPluginState = LauncherTransientOperationState.finishServerPlugins(serverPluginState)
        gitStashState = LauncherTransientOperationState.finishGitStash(gitStashState)
        quickImportState = LauncherTransientOperationState.finishQuickImport(quickImportState)
        nodeMemoryStatus = LauncherTransientOperationState.finishNodeMemory(nodeMemoryStatus)
    }

    fun applyStructuredOutput(parsed: TermuxStructuredOutput, rawOutput: String) {
        parsed.chatGuardSnapshot?.let { snapshot ->
            chatGuardState = TavernChatGuardState(
                snapshot = snapshot,
                knownRescueCount = snapshot.rescueCount,
                loading = false,
                message = if (snapshot.running) {
                    "聊天守护运行中，已保护 ${snapshot.linkedChatCount} 个聊天文件。"
                } else {
                    "聊天守护状态已读取。"
                },
            )
        } ?: parsed.chatGuardRescueCountHint?.let { count ->
            chatGuardState = chatGuardState.copy(knownRescueCount = count)
        }

        parsed.configSnapshot?.let { snapshot ->
            val savedJustNow = rawOutput.contains("\"status\": \"config-set\"")
            configState = TavernConfigManagementState(
                snapshot = snapshot,
                loading = false,
                savedNeedsRestart = if (savedJustNow) true else configState.savedNeedsRestart,
                message = when {
                    savedJustNow -> "配置已保存并自动备份原文件，重启酒馆后生效。"
                    snapshot.exists -> "已读取当前实例的 config.yaml 配置。"
                    else -> "当前实例还没有 config.yaml，显示为默认模板；保存后会自动生成。"
                },
            )
        }

        parsed.proxyTestResult?.let { result ->
            proxyTestState = proxyTestState.copy(
                testing = false,
                result = result,
                message = result.detail.ifBlank {
                    if (result.ok) "代理连通性测试通过。" else "代理连通性测试未通过。"
                },
            )
        }

        parsed.storageSnapshot?.let { snapshot ->
            storageState = TavernStorageState(
                snapshot = snapshot,
                loading = false,
                message = if (snapshot.lastFreedKb != null) {
                    "安全清理已完成，本次释放约 ${formatStorageKilobytes(snapshot.lastFreedKb)}。"
                } else {
                    "已扫描：酒馆目录共 ${formatStorageKilobytes(snapshot.tavernTotalKb)}，可安全清理约 ${formatStorageKilobytes(snapshot.totalCleanableKb)}。"
                },
            )
        }

        parsed.serverPluginsSnapshot?.let { snapshot ->
            serverPluginState = TavernServerPluginState(
                snapshot = snapshot,
                loading = false,
                message = "已读取 ${snapshot.plugins.size} 个服务器插件。",
            )
        }

        parsed.gitStashSnapshot?.let { (available, entries) ->
            gitStashState = TavernGitStashState(
                gitAvailable = available,
                entries = entries,
                loading = false,
                message = if (!available) {
                    "当前酒馆目录不是 Git 仓库。"
                } else if (entries.isEmpty()) {
                    "当前没有暂存记录。"
                } else {
                    "共读取到 ${entries.size} 条暂存记录。"
                },
            )
        }

        parsed.quickImportPaths?.let { paths ->
            val nextChar = quickImportState.chatCharacterDir.ifBlank {
                paths.chatCharacterDirs.firstOrNull().orEmpty()
            }
            quickImportState = quickImportState.copy(
                paths = paths,
                chatCharacterDir = nextChar,
                loading = false,
                message = "已解析当前实例的数据目录。",
            )
        }

        val importedFiles = parsed.quickImportedFiles
        if (importedFiles.isNotEmpty()) {
            val latest = importedFiles.last()
            quickImportState = quickImportState.copy(
                loading = false,
                lastImportedFile = latest,
                lastImportedFiles = importedFiles,
                message = if (importedFiles.size == 1) {
                    "导入成功：${latest.substringAfterLast('/')}"
                } else {
                    "批量导入成功：共 ${importedFiles.size} 个文件。"
                },
            )
        } else {
            parsed.quickImportedFile?.let { importedFile ->
                quickImportState = quickImportState.copy(
                    loading = false,
                    lastImportedFile = importedFile,
                    lastImportedFiles = listOf(importedFile),
                    message = "导入成功：${importedFile.substringAfterLast('/')}",
                )
            }
        }

        parsed.nodeMemoryStatus?.let { status ->
            nodeMemoryStatus = status
        }
    }
}

class LauncherOpsCoordinator(
    val state: LauncherOpsUiState,
    private val runGuarded: (String, Long, Boolean, (LauncherUpdate) -> Unit) -> Unit,
    private val onCommand: (String, LauncherUpdate) -> Unit,
    private val onStatusUpdate: (String, String, Boolean) -> Unit,
    private val isTransientStatus: (String) -> Boolean,
) {
    fun refreshChatGuardStatus() {
        runGuarded(
            "读取聊天守护状态",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-chat-guard-status"),
            false,
        ) { guardedUpdate ->
            state.chatGuardState = state.chatGuardState.copy(loading = true, message = "正在读取聊天守护状态…")
            onCommand("tavern-chat-guard-status") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernChatGuardOutputParser.parse(output) == null) {
                    state.chatGuardState = state.chatGuardState.copy(
                        loading = false,
                        message = if (ok) "命令已完成，但没有读到守护状态。" else "读取聊天守护状态失败。",
                    )
                }
            }
        }
    }

    fun saveChatGuardConfig(config: TavernChatGuardConfig) {
        TavernChatGuardPolicy.validate(config)?.let { reason ->
            onStatusUpdate(reason, "", false)
            return
        }
        val payload = TavernChatGuardCommandCodec.encode(config)
        runGuarded(
            "保存聊天守护设置",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-chat-guard-set"),
            false,
        ) { guardedUpdate ->
            state.chatGuardState = state.chatGuardState.copy(loading = true, message = "正在保存聊天守护设置…")
            onCommand(LauncherCommandCodec.encode("tavern-chat-guard-set", payload), guardedUpdate)
        }
    }

    fun clearChatGuardHistory() {
        runGuarded(
            "清空救回记录列表",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-chat-guard-clear-history"),
            false,
        ) { guardedUpdate ->
            state.chatGuardState = state.chatGuardState.copy(loading = true, message = "正在清空救回记录列表…")
            onCommand("tavern-chat-guard-clear-history", guardedUpdate)
        }
    }

    fun refreshTavernConfig() {
        runGuarded(
            "读取酒馆核心设置",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-config-read"),
            false,
        ) { guardedUpdate ->
            state.configState = state.configState.copy(loading = true, message = "正在读取 config.yaml 配置…")
            onCommand("tavern-config-read") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernConfigOutputParser.parse(output) == null) {
                    state.configState = state.configState.copy(
                        loading = false,
                        message = if (ok) {
                            "命令已完成，但没有读到 config.yaml 数据。"
                        } else {
                            "读取 config.yaml 失败，请确认酒馆目录有效且已安装依赖。"
                        },
                    )
                }
            }
        }
    }

    fun saveTavernConfig(patch: TavernConfigPatch) {
        TavernConfigPolicy.validatePatch(
            patch = patch,
            currentlyHasPassword = state.configState.snapshot?.basicAuthHasPassword == true,
        )?.let { reason ->
            onStatusUpdate(reason, "", false)
            return
        }
        val payload = TavernConfigCommandCodec.encodePatch(patch)
        runGuarded(
            "保存酒馆核心设置",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-config-set"),
            false,
        ) { guardedUpdate ->
            state.configState = state.configState.copy(loading = true, message = "正在备份并保存 config.yaml…")
            onCommand(LauncherCommandCodec.encode("tavern-config-set", payload), guardedUpdate)
        }
    }

    fun testProxyConnectivity(proxyUrl: String) {
        TavernProxyTestCommandCodec.validateProxyUrl(proxyUrl)?.let { reason ->
            state.proxyTestState = state.proxyTestState.copy(testing = false, message = reason)
            onStatusUpdate(reason, "", false)
            return
        }
        val trimmed = proxyUrl.trim()
        val encoded = TavernProxyTestCommandCodec.encode(trimmed)
        runGuarded(
            "测试代理连通性",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-proxy-test"),
            false,
        ) { guardedUpdate ->
            state.proxyTestState = TavernProxyTestState(
                testing = true,
                lastTestedUrl = trimmed,
                message = "正在通过 Termux 测试代理 $trimmed …",
            )
            onCommand(LauncherCommandCodec.encode("tavern-proxy-test", encoded)) { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernProxyTestOutputParser.parse(output) == null) {
                    state.proxyTestState = state.proxyTestState.copy(
                        testing = false,
                        message = "代理连通性测试未返回有效结果，请检查 Termux 日志。",
                    )
                }
            }
        }
    }

    fun scanTavernStorage() {
        runGuarded(
            "扫描酒馆空间占用",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-storage-scan"),
            false,
        ) { guardedUpdate ->
            state.storageState = state.storageState.copy(loading = true, message = "正在扫描磁盘占用…")
            onCommand("tavern-storage-scan") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernStorageOutputParser.parse(output) == null) {
                    state.storageState = state.storageState.copy(loading = false, message = "扫描空间占用失败。")
                }
            }
        }
    }

    fun cleanTavernStorage(categories: Set<TavernStorageCleanupCategory>) {
        if (categories.isEmpty()) return
        val encoded = TavernStorageCommandCodec.encodeCategories(categories)
        runGuarded(
            "安全清理酒馆空间",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-storage-clean"),
            false,
        ) { guardedUpdate ->
            state.storageState = state.storageState.copy(loading = true, message = "正在清理已选安全项…")
            onCommand(LauncherCommandCodec.encode("tavern-storage-clean", encoded), guardedUpdate)
        }
    }

    fun refreshServerPlugins() {
        runGuarded(
            "读取服务器插件",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-server-plugins-list"),
            false,
        ) { guardedUpdate ->
            state.serverPluginState = state.serverPluginState.copy(loading = true, message = "正在读取服务器插件…")
            onCommand("tavern-server-plugins-list") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernServerPluginOutputParser.parse(output) == null) {
                    state.serverPluginState = state.serverPluginState.copy(loading = false, message = "读取服务器插件失败。")
                }
            }
        }
    }

    fun installServerPlugin(repoUrl: String) {
        TavernServerPluginCommandCodec.validateRepositoryUrl(repoUrl)?.let { reason ->
            onStatusUpdate(reason, "", false)
            return
        }
        val payload = TavernServerPluginCommandCodec.encode(repoUrl)
        runGuarded(
            "安装服务器插件",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-server-plugins-install"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-server-plugins-install", payload), guardedUpdate)
        }
    }

    fun updateServerPlugin(directoryName: String) {
        val payload = TavernServerPluginCommandCodec.encode(directoryName)
        runGuarded(
            "更新服务器插件",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-server-plugins-update"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-server-plugins-update", payload), guardedUpdate)
        }
    }

    fun deleteServerPlugin(directoryName: String) {
        val payload = TavernServerPluginCommandCodec.encode(directoryName)
        runGuarded(
            "删除服务器插件",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-server-plugins-delete"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-server-plugins-delete", payload), guardedUpdate)
        }
    }

    fun refreshGitStash() {
        runGuarded(
            "读取暂存记录",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-git-stash-list"),
            false,
        ) { guardedUpdate ->
            state.gitStashState = state.gitStashState.copy(loading = true, message = "正在读取 Git 暂存列表…")
            onCommand("tavern-git-stash-list") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernGitStashOutputParser.parse(output) == null) {
                    state.gitStashState = state.gitStashState.copy(loading = false, message = "读取暂存记录失败。")
                }
            }
        }
    }

    fun applyGitStash(index: Int) {
        runGuarded(
            "恢复暂存改动",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-git-stash-apply"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-git-stash-apply", index.toString()), guardedUpdate)
        }
    }

    fun dropGitStash(index: Int) {
        runGuarded(
            "删除暂存记录",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-git-stash-drop"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-git-stash-drop", index.toString()), guardedUpdate)
        }
    }

    fun refreshQuickImportPaths() {
        val handle = TavernQuickImportCommandCodec.normalizeUserHandle(state.quickImportState.userHandle)
        runGuarded(
            "解析数据目录",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-quick-import-paths"),
            false,
        ) { guardedUpdate ->
            state.quickImportState = state.quickImportState.copy(loading = true, message = "正在解析常用数据目录…")
            onCommand(LauncherCommandCodec.encode("tavern-quick-import-paths", handle), guardedUpdate)
        }
    }

    fun handleQuickImportBatch(batch: TavernQuickImportStagedBatch) {
        if (!batch.ok || batch.stagedFiles.isEmpty()) {
            state.quickImportState = state.quickImportState.copy(message = batch.message)
            onStatusUpdate(batch.message, "", false)
            return
        }
        val chatChar = state.quickImportState.chatCharacterDir.trim()
        if (batch.kind == TavernQuickImportKind.Chat) {
            TavernQuickImportCommandCodec.validateChatCharacterDir(chatChar)?.let { reason ->
                state.quickImportState = state.quickImportState.copy(message = reason)
                onStatusUpdate(reason, "", false)
                return
            }
        }
        val encoded = TavernQuickImportCommandCodec.encodeBatch(
            kind = batch.kind,
            stagedPaths = batch.stagedFiles.map { it.termuxReadablePath },
            userHandle = state.quickImportState.userHandle,
            chatCharacterDir = chatChar,
        )
        runGuarded(
            batch.kind.label,
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-quick-import-batch"),
            false,
        ) { guardedUpdate ->
            state.quickImportState = state.quickImportState.copy(loading = true, message = batch.message)
            onCommand(LauncherCommandCodec.encode("tavern-quick-import-batch", encoded), guardedUpdate)
        }
    }

    fun checkNodeMemoryStatus() {
        runGuarded(
            "读取运行内存上限",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-node-memory-status"),
            false,
        ) { guardedUpdate ->
            state.nodeMemoryStatus = state.nodeMemoryStatus.copy(checking = true, message = "正在读取当前实例的内存上限…")
            onCommand("tavern-node-memory-status") { newStatus, output, ok ->
                guardedUpdate(newStatus, output, ok)
                if (!isTransientStatus(newStatus) && TavernNodeMemoryOutputParser.parse(output) == null) {
                    state.nodeMemoryStatus = state.nodeMemoryStatus.copy(
                        checking = false,
                        message = "读取内存上限失败，请稍后重试。",
                    )
                }
            }
        }
    }

    fun setNodeMemory(memory: Int) {
        runGuarded(
            "设置 Node.js 内存",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-node-memory"),
            false,
        ) { guardedUpdate ->
            onCommand(LauncherCommandCodec.encode("tavern-node-memory", memory.toString()), guardedUpdate)
        }
    }

    fun resetNodeMemory() {
        runGuarded(
            "恢复默认内存设置",
            TermuxCommandTimeoutPolicy.operationLockMillis("tavern-node-memory-reset"),
            false,
        ) { guardedUpdate ->
            state.nodeMemoryStatus = state.nodeMemoryStatus.copy(checking = true, message = "正在恢复默认内存设置…")
            onCommand("tavern-node-memory-reset", guardedUpdate)
        }
    }
}
