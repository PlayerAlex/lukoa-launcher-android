package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun TavernConfigManagementSection(
    state: TavernConfigManagementState,
    instanceLabel: String,
    activePort: Int,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onSave: (TavernConfigPatch) -> Unit,
    onRestartTavern: () -> Unit,
    onCopyText: (String, String) -> Boolean,
    onShowHint: (String) -> Unit,
) {
    val snapshot = state.snapshot
    var listen by remember(instanceLabel) { mutableStateOf(snapshot?.listen ?: false) }
    var whitelistMode by remember(instanceLabel) { mutableStateOf(snapshot?.whitelistMode ?: true) }
    var whitelistInput by remember(instanceLabel) {
        mutableStateOf((snapshot?.whitelist ?: listOf("::1", "127.0.0.1")).joinToString("\n"))
    }
    var basicAuthMode by remember(instanceLabel) { mutableStateOf(snapshot?.basicAuthMode ?: false) }
    var basicAuthUsername by remember(instanceLabel) { mutableStateOf(snapshot?.basicAuthUsername ?: "user") }
    var newBasicAuthPassword by remember(instanceLabel) { mutableStateOf("") }
    var enableUserAccounts by remember(instanceLabel) { mutableStateOf(snapshot?.enableUserAccounts ?: false) }
    var enableExtensions by remember(instanceLabel) { mutableStateOf(snapshot?.enableExtensions ?: true) }
    var enableServerPlugins by remember(instanceLabel) { mutableStateOf(snapshot?.enableServerPlugins ?: false) }
    var lazyLoadCharacters by remember(instanceLabel) { mutableStateOf(snapshot?.lazyLoadCharacters ?: false) }
    var chatBackupEnabled by remember(instanceLabel) { mutableStateOf(snapshot?.chatBackupEnabled ?: true) }
    var chatMaxTotalBackups by remember(instanceLabel) { mutableIntStateOf(snapshot?.chatMaxTotalBackups ?: 50) }
    var requestProxyEnabled by remember(instanceLabel) { mutableStateOf(snapshot?.requestProxyEnabled ?: false) }
    var requestProxyUrl by remember(instanceLabel) { mutableStateOf(snapshot?.requestProxyUrl ?: "") }
    var requestProxyBypassInput by remember(instanceLabel) {
        mutableStateOf((snapshot?.requestProxyBypass ?: listOf("localhost", "127.0.0.1")).joinToString("\n"))
    }

    fun syncFromSnapshot(source: TavernConfigSnapshot) {
        listen = source.listen
        whitelistMode = source.whitelistMode
        whitelistInput = source.whitelist.joinToString("\n")
        basicAuthMode = source.basicAuthMode
        basicAuthUsername = source.basicAuthUsername
        newBasicAuthPassword = ""
        enableUserAccounts = source.enableUserAccounts
        enableExtensions = source.enableExtensions
        enableServerPlugins = source.enableServerPlugins
        lazyLoadCharacters = source.lazyLoadCharacters
        chatBackupEnabled = source.chatBackupEnabled
        chatMaxTotalBackups = source.chatMaxTotalBackups
        requestProxyEnabled = source.requestProxyEnabled
        requestProxyUrl = source.requestProxyUrl
        requestProxyBypassInput = source.requestProxyBypass.joinToString("\n")
    }

    LaunchedEffect(snapshot) {
        if (snapshot != null) {
            syncFromSnapshot(snapshot)
        }
    }

    val lanAddresses = remember(listen, activePort) { LocalNetworkAddressResolver.localLanIpv4Addresses() }
    val draftPatch = TavernConfigPatch(
        listen = listen,
        whitelistMode = whitelistMode,
        whitelist = TavernConfigPolicy.parseLineList(whitelistInput),
        basicAuthMode = basicAuthMode,
        basicAuthUsername = basicAuthUsername.trim(),
        updateBasicAuthPassword = newBasicAuthPassword.isNotBlank(),
        basicAuthPassword = newBasicAuthPassword,
        enableUserAccounts = enableUserAccounts,
        enableExtensions = enableExtensions,
        enableServerPlugins = enableServerPlugins,
        lazyLoadCharacters = lazyLoadCharacters,
        chatBackupEnabled = chatBackupEnabled,
        chatMaxTotalBackups = chatMaxTotalBackups,
        requestProxyEnabled = requestProxyEnabled,
        requestProxyUrl = requestProxyUrl.trim(),
        requestProxyBypass = TavernConfigPolicy.parseLineList(requestProxyBypassInput),
    )
    val validationError = TavernConfigPolicy.validatePatch(
        patch = draftPatch,
        currentlyHasPassword = snapshot?.basicAuthHasPassword == true,
    )
    val securityWarnings = TavernConfigPolicy.securityWarnings(draftPatch)
    val lockedHint = if (actionsLocked) "当前有其他任务正在处理，请等任务完成后再试。" else null

    SectionPanel(
        title = "酒馆核心设置",
        accentColor = LukoaColors.Primary,
        headerAction = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = when {
                        state.loading -> "读取中"
                        snapshot == null -> "未读取"
                        snapshot.exists -> "已读取"
                        else -> "默认模板"
                    },
                    active = state.loading || snapshot != null,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.PrimarySoft,
                )
                InfoPopoverButton(
                    contentDescription = "查看酒馆核心设置说明",
                    title = "酒馆核心设置",
                    body = "这里可视化修改当前实例的 config.yaml 常用项。每次保存前都会自动备份原 config.yaml，只修改目标字段，保留原有注释和其他配置。修改后需重启酒馆生效。",
                )
            }
        },
    ) {
        SettingsEntryGroup {
            SettingsEntryRow(
                title = "当前实例",
                detail = state.message,
                detailMaxLines = Int.MAX_VALUE,
                value = instanceLabel,
                valueColor = LukoaColors.Primary,
                valueAsPill = true,
            )
            if (snapshot?.lastBackupPath?.isNotBlank() == true) {
                SettingsEntryDivider()
                SettingsEntryRow(
                    title = "最近自动备份",
                    detail = snapshot.lastBackupPath.substringAfterLast('/'),
                    detailMaxLines = 2,
                )
            }
        }

        if (state.savedNeedsRestart && tavernRunning) {
            NoticeCard(
                title = "配置已保存，重启酒馆后生效",
                detail = "酒馆只在启动时读取 config.yaml，点下方按钮可直接重启当前实例。",
                accentColor = LukoaColors.Accent,
                actionLabel = "立即重启酒馆",
                onAction = onRestartTavern,
            )
        }

        SettingsFeedbackActionButton(
            text = if (state.loading) "读取中..." else "读取当前配置",
            enabled = !actionsLocked && !state.loading,
            accentColor = LukoaColors.Primary,
            modifier = Modifier.fillMaxWidth(),
            unavailableHint = if (state.loading) "正在读取 config.yaml，请稍等。" else lockedHint,
            onShowHint = onShowHint,
            onClick = onRefresh,
        )

        if (snapshot == null) {
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "尚未读取 config.yaml",
                    detail = "点击上方“读取当前配置”后，可可视化修改局域网访问、角色卡懒加载、代理地址和核心功能开关。",
                    detailMaxLines = 3,
                )
            }
            return@SectionPanel
        }

        ManagementDialogSectionTitle("局域网访问与安全")
        SettingsEntryGroup {
            ConfigSwitchRow(
                title = "允许局域网访问（listen）",
                detail = "开启后，同一 Wi-Fi 下的电脑或平板可以直接在浏览器打开手机上的酒馆。",
                checked = listen,
                enabled = !actionsLocked,
                onCheckedChange = { listen = it },
            )
            if (listen) {
                SettingsEntryDivider()
                val primaryLanUrl = lanAddresses.firstOrNull()?.let { "http://$it:$activePort" }
                SettingsEntryRow(
                    title = "本机局域网地址",
                    detail = if (primaryLanUrl != null) {
                        val extras = lanAddresses.drop(1).joinToString("、") { "http://$it:$activePort" }
                        if (extras.isBlank()) "在电脑或平板浏览器输入此地址访问（需与手机连同一 Wi-Fi）。"
                        else "其他候选地址：$extras"
                    } else {
                        "暂未检测到 Wi-Fi 局域网 IP，请确认手机已连接 Wi-Fi。"
                    },
                    detailMaxLines = 3,
                    value = primaryLanUrl ?: "未连 Wi-Fi",
                    valueLayout = SettingsValueLayout.Supporting,
                    valueColor = if (primaryLanUrl != null) LukoaColors.Primary else LukoaColors.TextSecondary,
                    enabled = primaryLanUrl != null,
                    onClick = primaryLanUrl?.let { url ->
                        {
                            val copied = onCopyText("局域网酒馆地址", url)
                            onShowHint(if (copied) "已复制局域网访问地址：$url" else "复制地址失败。")
                        }
                    },
                )
            }
            SettingsEntryDivider()
            ConfigSwitchRow(
                title = "仅允许白名单 IP 访问（whitelistMode）",
                detail = "关闭后任意同局域网设备都能访问；若关闭，强烈建议开启下方密码保护。",
                checked = whitelistMode,
                enabled = !actionsLocked,
                onCheckedChange = { whitelistMode = it },
            )
            if (whitelistMode) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = whitelistInput,
                        onValueChange = { whitelistInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("允许访问的 IP / 网段（每行一个）") },
                        placeholder = { Text("127.0.0.1\n::1\n192.168.1.*") },
                        minLines = 2,
                        maxLines = 4,
                        enabled = !actionsLocked,
                    )
                    lanAddresses.firstOrNull()?.let { lanIp ->
                        val subnet = lanIp.substringBeforeLast('.') + ".*"
                        if (subnet !in draftPatch.whitelist) {
                            SettingsFeedbackActionButton(
                                text = "一键加入当前网段（$subnet）",
                                enabled = !actionsLocked,
                                accentColor = LukoaColors.Primary,
                                modifier = Modifier.fillMaxWidth(),
                                unavailableHint = lockedHint,
                                onShowHint = onShowHint,
                                onClick = {
                                    whitelistInput = (draftPatch.whitelist + subnet).distinct().joinToString("\n")
                                },
                            )
                        }
                    }
                }
            }
            SettingsEntryDivider()
            ConfigSwitchRow(
                title = "开启访问账号密码保护（basicAuthMode）",
                detail = if (snapshot.basicAuthHasPassword) {
                    "已配置密码；留空新密码框可保持原密码不变。"
                } else {
                    "当前未设置密码，开启前请在下方填写新密码。"
                },
                checked = basicAuthMode,
                enabled = !actionsLocked,
                onCheckedChange = { basicAuthMode = it },
            )
            if (basicAuthMode) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = basicAuthUsername,
                        onValueChange = { basicAuthUsername = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("访问用户名") },
                        enabled = !actionsLocked,
                    )
                    OutlinedTextField(
                        value = newBasicAuthPassword,
                        onValueChange = { newBasicAuthPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = {
                            Text(if (snapshot.basicAuthHasPassword) "新访问密码（留空不改）" else "设置访问密码")
                        },
                        enabled = !actionsLocked,
                    )
                }
            }
        }

        securityWarnings.forEach { warning ->
            NoticeCard(
                title = "局域网访问提醒",
                detail = warning,
                accentColor = LukoaColors.Accent,
            )
        }

        ManagementDialogSectionTitle("流畅度与聊天滚动备份")
        SettingsEntryGroup {
            ConfigSwitchRow(
                title = "角色卡懒加载（lazyLoadCharacters）",
                detail = "角色卡很多时开启可明显加快页面打开速度、降低手机内存占用。",
                checked = lazyLoadCharacters,
                enabled = !actionsLocked,
                onCheckedChange = { lazyLoadCharacters = it },
            )
            SettingsEntryDivider()
            ConfigSwitchRow(
                title = "酒馆自带聊天滚动备份",
                detail = "每次保存聊天时由酒馆自动留底一份到 backups 目录。",
                checked = chatBackupEnabled,
                enabled = !actionsLocked,
                onCheckedChange = { chatBackupEnabled = it },
            )
        }

        ManagementDialogActionCard(
            title = "聊天滚动备份上限（当前：${if (chatMaxTotalBackups == -1) "不限" else "$chatMaxTotalBackups 份"}）",
        ) {
            Text(
                text = "限制滚动备份总数可避免手机空间被旧副本慢慢占满（默认 50 份）。",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TavernConfigPolicy.CHAT_BACKUP_LIMIT_PRESETS.forEach { limit ->
                    val selected = chatMaxTotalBackups == limit
                    SettingsCompactChoiceButton(
                        text = if (limit == -1) "不限" else "$limit",
                        enabled = !actionsLocked,
                        accentColor = if (selected) LukoaColors.Accent else LukoaColors.Primary,
                        modifier = Modifier.weight(1f),
                        unavailableHint = lockedHint,
                        onShowHint = onShowHint,
                        onClick = { chatMaxTotalBackups = limit },
                    )
                }
            }
        }

        ManagementDialogSectionTitle("酒馆网络请求代理（requestProxy）")
        SettingsEntryGroup {
            ConfigSwitchRow(
                title = "启用酒馆请求代理",
                detail = "让酒馆后端请求 API 或下载内容时走指定 HTTP / SOCKS5 代理。",
                checked = requestProxyEnabled,
                enabled = !actionsLocked,
                onCheckedChange = { requestProxyEnabled = it },
            )
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = requestProxyUrl,
                    onValueChange = { requestProxyUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("代理地址") },
                    placeholder = { Text("http://127.0.0.1:7890 或 socks5://127.0.0.1:1080") },
                    enabled = !actionsLocked,
                )
                OutlinedTextField(
                    value = requestProxyBypassInput,
                    onValueChange = { requestProxyBypassInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    label = { Text("不走代理的地址（每行一个）") },
                    placeholder = { Text("localhost\n127.0.0.1") },
                    enabled = !actionsLocked,
                )
            }
        }

        ManagementDialogSectionTitle("核心功能开关")
        SettingsEntryGroup {
            ConfigSwitchRow(
                title = "多用户账号登录（enableUserAccounts）",
                detail = "开启后进入酒馆需选择或登录账户，可配合设置里的“用户管理”使用。",
                checked = enableUserAccounts,
                enabled = !actionsLocked,
                onCheckedChange = { enableUserAccounts = it },
            )
            SettingsEntryDivider()
            ConfigSwitchRow(
                title = "启用网页扩展（enableExtensions）",
                detail = "保持开启以正常加载酒馆助手、正则等第三方前端扩展。",
                checked = enableExtensions,
                enabled = !actionsLocked,
                onCheckedChange = { enableExtensions = it },
            )
            SettingsEntryDivider()
            ConfigSwitchRow(
                title = "启用服务器插件（enableServerPlugins）",
                detail = "允许加载 plugins/ 目录中的服务端插件（如部分高级连接插件）。",
                checked = enableServerPlugins,
                enabled = !actionsLocked,
                onCheckedChange = { enableServerPlugins = it },
            )
        }

        if (validationError != null) {
            Text(
                text = validationError,
                color = LukoaColors.Danger,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsFeedbackActionButton(
                text = "还原当前读取值",
                enabled = !actionsLocked,
                accentColor = LukoaColors.TextSecondary,
                modifier = Modifier.weight(1f),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onClick = {
                    syncFromSnapshot(snapshot)
                    onShowHint("已还原为刚读取的配置。")
                },
            )
            SettingsFeedbackActionButton(
                text = "保存配置",
                enabled = !actionsLocked && validationError == null,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1.2f),
                unavailableHint = lockedHint ?: validationError,
                onShowHint = onShowHint,
                onClick = { onSave(draftPatch) },
            )
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = if (enabled) LukoaColors.TextPrimary else LukoaColors.Dim,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LukoaColors.Background,
                checkedTrackColor = LukoaColors.Primary,
                uncheckedThumbColor = LukoaColors.TextSecondary,
                uncheckedTrackColor = LukoaColors.Elevated,
            ),
        )
    }
}
