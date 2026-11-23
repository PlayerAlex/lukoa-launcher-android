package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun TavernConfigManagementSection(
    state: TavernConfigManagementState,
    proxyTestState: TavernProxyTestState = TavernProxyTestState(),
    instanceLabel: String,
    activePort: Int,
    actionsLocked: Boolean,
    tavernRunning: Boolean,
    onRefresh: () -> Unit,
    onSave: (TavernConfigPatch) -> Unit,
    onTestProxy: (String) -> Unit = {},
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
    var showQrCode by rememberSaveable(instanceLabel) { mutableStateOf(false) }
    var showAdvanced by rememberSaveable(instanceLabel) { mutableStateOf(false) }

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

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsGroupLabel(
                    title = "当前实例",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = when {
                        state.loading -> "读取中"
                        snapshot == null -> "未读取"
                        snapshot.exists -> "已读取"
                        else -> "默认配置"
                    },
                    color = if (state.loading || snapshot != null) LukoaColors.Primary else LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SettingsEntryGroup {
                SettingsEntryRow(
                    title = "当前实例",
                    detail = state.message,
                    detailMaxLines = 3,
                    value = instanceLabel,
                    valueColor = LukoaColors.TextPrimary,
                )
            }
        }

        if (state.savedNeedsRestart && tavernRunning) {
            NoticeCard(
                title = "设置已保存，重启酒馆后生效",
                detail = "酒馆启动时才会重新读取设置，点击下方可直接重启。",
                accentColor = LukoaColors.Primary,
                actionTone = ActionTone.Safe,
                actionLabel = "立即重启酒馆",
                onAction = onRestartTavern,
            )
        }

        if (snapshot == null) {
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "读取当前设置",
                enabled = !actionsLocked && !state.loading,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = if (state.loading) "正在读取设置，请稍等。" else lockedHint,
                onShowHint = onShowHint,
                onClick = onRefresh,
            )
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("用电脑 / 平板一起玩")
            SettingsEntryGroup {
            LukoaSwitchRow(
                title = "允许同一 Wi-Fi 下的设备访问",
                detail = "开启后，电脑或平板连同一个 Wi-Fi 就能在浏览器打开手机上的酒馆。",
                checked = listen,
                enabled = !actionsLocked,
                onCheckedChange = { listen = it },
            )
            if (listen) {
                SettingsEntryDivider()
                val primaryLanUrl = lanAddresses.firstOrNull()?.let { "http://$it:$activePort" }
                SettingsEntryRow(
                    title = "本机访问地址（点按复制）",
                    detail = if (primaryLanUrl != null) {
                        "在电脑或平板浏览器输入这个地址即可打开。"
                    } else {
                        "暂未检测到 Wi-Fi，请确认手机已连接 Wi-Fi。"
                    },
                    detailMaxLines = 2,
                    value = primaryLanUrl ?: "未连 Wi-Fi",
                    valueLayout = SettingsValueLayout.Supporting,
                    valueColor = if (primaryLanUrl != null) LukoaColors.Primary else LukoaColors.TextSecondary,
                    enabled = primaryLanUrl != null,
                    onClick = primaryLanUrl?.let { url ->
                        {
                            val copied = onCopyText("局域网酒馆地址", url)
                            onShowHint(if (copied) "已复制访问地址：$url" else "复制地址失败。")
                        }
                    },
                )
                if (primaryLanUrl != null) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SettingsFeedbackActionButton(
                            text = if (showQrCode) "收起扫码二维码" else "显示扫码二维码",
                            enabled = true,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            unavailableHint = null,
                            onShowHint = onShowHint,
                            onClick = { showQrCode = !showQrCode },
                        )
                        if (showQrCode) {
                            LanQrCodeCard(url = primaryLanUrl)
                        }
                    }
                }
                SettingsEntryDivider()
                LukoaSwitchRow(
                    title = "仅允许白名单里的设备访问",
                    detail = "保护同 Wi-Fi 下的隐私；关闭后建议开启下方密码保护。",
                    checked = whitelistMode,
                    enabled = !actionsLocked,
                    onCheckedChange = { whitelistMode = it },
                )
                if (whitelistMode) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        lanAddresses.firstOrNull()?.let { lanIp ->
                            val subnet = lanIp.substringBeforeLast('.') + ".*"
                            if (subnet !in draftPatch.whitelist) {
                                SettingsFeedbackActionButton(
                                    text = "一键放行当前 Wi-Fi 设备（$subnet）",
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
                        LukoaTextField(
                            value = whitelistInput,
                            onValueChange = { whitelistInput = it },
                            label = "允许访问的 IP / 网段（每行一个）",
                            placeholder = "127.0.0.1\n192.168.1.*",
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            enabled = !actionsLocked,
                        )
                    }
                }
                SettingsEntryDivider()
                LukoaSwitchRow(
                    title = "打开酒馆时要求输入账号密码",
                    detail = if (snapshot.basicAuthHasPassword) {
                        "已设置密码；下方新密码留空可保持不变。"
                    } else {
                        "开启前请在下方填写用户名和密码。"
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
                        LukoaTextField(
                            value = basicAuthUsername,
                            onValueChange = { basicAuthUsername = it },
                            label = "访问用户名",
                            enabled = !actionsLocked,
                        )
                        LukoaTextField(
                            value = newBasicAuthPassword,
                            onValueChange = { newBasicAuthPassword = it },
                            label = if (snapshot.basicAuthHasPassword) "新密码（留空不改）" else "设置密码",
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !actionsLocked,
                        )
                    }
                }
            }
        }
        }

        securityWarnings.forEach { warning ->
            NoticeCard(
                title = "访问提醒",
                detail = warning,
                accentColor = LukoaColors.Accent,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsGroupLabel("流畅度与聊天保护")
            SettingsEntryGroup {
                LukoaSwitchRow(
                    title = "角色卡很多时省内存加速",
                    detail = "按需加载角色卡，角色多时能明显加快打开速度、减少卡顿。",
                    checked = lazyLoadCharacters,
                    enabled = !actionsLocked,
                    onCheckedChange = { lazyLoadCharacters = it },
                )
                SettingsEntryDivider()
                LukoaSwitchRow(
                    title = "酒馆自带聊天自动留底",
                    detail = "每次保存聊天时额外留一份旧档，防止误删或覆盖。",
                    checked = chatBackupEnabled,
                    enabled = !actionsLocked,
                    onCheckedChange = { chatBackupEnabled = it },
                )
            }
        }

        LukoaCollapsibleCard(
            title = "更多进阶设置",
            subtitle = "网络代理、旧档保留份数、多用户与插件总开关",
            expanded = showAdvanced,
            onToggle = { showAdvanced = !showAdvanced },
        ) {
            Text(
                text = "旧档保留上限（当前：${if (chatMaxTotalBackups == -1) "不限" else "$chatMaxTotalBackups 份"}）",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
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
                        selected = selected,
                        accentColor = if (selected) LukoaColors.Primary else LukoaColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                        unavailableHint = lockedHint,
                        onShowHint = onShowHint,
                        onClick = { chatMaxTotalBackups = limit },
                    )
                }
            }

            SettingsEntryDivider()
            LukoaSwitchRow(
                title = "让酒馆网络请求走本地代理",
                detail = "需要使用 HTTP 或 SOCKS5 代理连接模型接口时开启。",
                checked = requestProxyEnabled,
                enabled = !actionsLocked,
                onCheckedChange = { requestProxyEnabled = it },
            )
            if (requestProxyEnabled || requestProxyUrl.isNotBlank()) {
                LukoaTextField(
                    value = requestProxyUrl,
                    onValueChange = { requestProxyUrl = it },
                    label = "代理地址",
                    placeholder = "http://127.0.0.1:7890 或 socks5://127.0.0.1:1080",
                    enabled = !actionsLocked,
                )
                val proxyValidationError = TavernProxyTestCommandCodec.validateProxyUrl(requestProxyUrl)
                SettingsFeedbackActionButton(
                    text = if (proxyTestState.testing) "正在测试连通性..." else "测试代理连通性",
                    enabled = !actionsLocked && !proxyTestState.testing && proxyValidationError == null,
                    accentColor = LukoaColors.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    unavailableHint = if (proxyTestState.testing) "正在测试代理，请稍等。" else lockedHint ?: proxyValidationError,
                    onShowHint = onShowHint,
                    onClick = { onTestProxy(requestProxyUrl.trim()) },
                )
                if (proxyTestState.message.isNotBlank()) {
                    val result = proxyTestState.result
                    NoticeCard(
                        title = when {
                            proxyTestState.testing -> "代理测试中"
                            result?.ok == true -> "代理可用${result.latencyMs?.let { "（${it}ms）" }.orEmpty()}"
                            result != null -> "代理暂不可用"
                            else -> "代理提示"
                        },
                        detail = proxyTestState.message,
                        accentColor = if (result?.ok == false) LukoaColors.Danger else LukoaColors.Primary,
                    )
                }
                LukoaTextField(
                    value = requestProxyBypassInput,
                    onValueChange = { requestProxyBypassInput = it },
                    label = "不走代理的地址（每行一个）",
                    placeholder = "localhost\n127.0.0.1",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    enabled = !actionsLocked,
                )
            }

            SettingsEntryDivider()
            LukoaSwitchRow(
                title = "开启多用户账号登录",
                detail = "需要分多个账号隔离角色卡和聊天时开启。",
                checked = enableUserAccounts,
                enabled = !actionsLocked,
                onCheckedChange = { enableUserAccounts = it },
            )
            SettingsEntryDivider()
            LukoaSwitchRow(
                title = "启用网页扩展",
                detail = "建议保持开启，用于加载酒馆助手、正则等扩展。",
                checked = enableExtensions,
                enabled = !actionsLocked,
                onCheckedChange = { enableExtensions = it },
            )
            SettingsEntryDivider()
            LukoaSwitchRow(
                title = "启用后端服务器插件",
                detail = "仅在安装了「服务器插件」时需要开启。",
                checked = enableServerPlugins,
                enabled = !actionsLocked,
                onCheckedChange = { enableServerPlugins = it },
            )
            SettingsFeedbackActionButton(
                text = if (state.loading) "读取中..." else "重新读取配置文件",
                enabled = !actionsLocked && !state.loading,
                accentColor = LukoaColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onClick = onRefresh,
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
                text = "还原改动",
                enabled = !actionsLocked,
                accentColor = LukoaColors.TextSecondary,
                modifier = Modifier.weight(1f),
                unavailableHint = lockedHint,
                onShowHint = onShowHint,
                onClick = {
                    syncFromSnapshot(snapshot)
                    onShowHint("已还原为当前保存的设置。")
                },
            )
            SettingsFeedbackActionButton(
                text = "保存设置",
                enabled = !actionsLocked && validationError == null,
                accentColor = LukoaColors.Primary,
                modifier = Modifier.weight(1.3f),
                unavailableHint = lockedHint ?: validationError,
                onShowHint = onShowHint,
                onClick = { onSave(draftPatch) },
            )
        }
    }
}
