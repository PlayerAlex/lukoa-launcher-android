package moe.lukoa.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class DocumentationPage(
    val menuLabel: String,
    val pageTitle: String,
    val menuDescription: String,
) {
    Home("首页", "文档", "使用范围与重要提醒"),
    TavernJargon("第一章：酒馆黑话篇", "酒馆黑话篇", "API、角色卡、世界书与预设"),
    LauncherQuestions("第二章：启动器疑问篇", "启动器疑问篇", "安装、启动、版本与备份"),
    AdvancedFeatures("第三章：进阶与维护篇", "进阶与维护篇", "局域网扫码、聊天守护、空间清理与导入"),
}

internal data class DocumentationTopic(
    val title: String,
    val body: String,
)

internal data class DocumentationSearchHit(
    val chapterTitle: String,
    val topic: DocumentationTopic,
)

internal fun searchDocumentationTopics(query: String): List<DocumentationSearchHit> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val terms = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    val all = buildList {
        DOCUMENTATION_HOME_NOTICES.forEachIndexed { idx, notice ->
            add(DocumentationSearchHit("首页提醒", DocumentationTopic("使用须知 ${idx + 1}", notice)))
        }
        TAVERN_JARGON_TOPICS.forEach { add(DocumentationSearchHit("第一章：酒馆黑话篇", it)) }
        LAUNCHER_QUESTION_TOPICS.forEach { add(DocumentationSearchHit("第二章：启动器疑问篇", it)) }
        ADVANCED_FEATURE_TOPICS.forEach { add(DocumentationSearchHit("第三章：进阶与维护篇", it)) }
    }
    return all.filter { hit ->
        val haystack = "${hit.chapterTitle}\n${hit.topic.title}\n${hit.topic.body}"
        terms.all { term -> haystack.contains(term, ignoreCase = true) }
    }
}

@Composable
fun DocumentationSection(
    modifier: Modifier = Modifier,
    initialMenuOpen: Boolean = false,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    var selectedPage by rememberSaveable { mutableStateOf(DocumentationPage.Home) }
    var menuOpen by rememberSaveable { mutableStateOf(initialMenuOpen) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val contentScrollState = rememberScrollState()
    val searchHits = remember(searchQuery) { searchDocumentationTopics(searchQuery) }

    BackHandler(enabled = menuOpen || searchQuery.isNotBlank() || selectedPage != DocumentationPage.Home) {
        when {
            menuOpen -> menuOpen = false
            searchQuery.isNotBlank() -> searchQuery = ""
            else -> selectedPage = DocumentationPage.Home
        }
    }
    LaunchedEffect(selectedPage) {
        contentScrollState.scrollTo(0)
    }
    LaunchedEffect(menuOpen) {
        onPagerLockChange(menuOpen)
    }
    DisposableEffect(onPagerLockChange) {
        onDispose { onPagerLockChange(false) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("documentation-section"),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("documentation-content-frame"),
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(contentScrollState)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    DocumentationPageHeader(
                        title = selectedPage.pageTitle,
                        onOpenMenu = { menuOpen = true },
                    )
                    LukoaSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.testTag("documentation-search-input"),
                        placeholder = "搜索关键词（如 429、备份、局域网）",
                    )
                    if (searchQuery.isNotBlank()) {
                        DocumentationSearchResults(
                            query = searchQuery,
                            hits = searchHits,
                            onClear = { searchQuery = "" },
                        )
                    } else {
                        Crossfade(
                            targetState = selectedPage,
                            label = "documentation-page-content",
                        ) { page ->
                            when (page) {
                                DocumentationPage.Home -> DocumentationHome()
                                DocumentationPage.TavernJargon -> DocumentationTopicList(TAVERN_JARGON_TOPICS)
                                DocumentationPage.LauncherQuestions -> DocumentationTopicList(LAUNCHER_QUESTION_TOPICS)
                                DocumentationPage.AdvancedFeatures -> DocumentationTopicList(ADVANCED_FEATURE_TOPICS)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = menuOpen,
                    modifier = Modifier.matchParentSize(),
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                ) {
                    DocumentationMenuDrawer(
                        selectedPage = selectedPage,
                        onSelectPage = { page ->
                            selectedPage = page
                            menuOpen = false
                        },
                        onDismiss = { menuOpen = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentationPageHeader(
    title: String,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .testTag("documentation-menu-button")
                .clickable(
                    role = Role.Button,
                    onClick = rememberFeedbackClick(onOpenMenu),
                ),
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, LukoaColors.Border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "打开文档目录",
                    tint = LukoaColors.TextPrimary,
                    modifier = Modifier.size(25.dp),
                )
            }
        }
        Text(
            text = title,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DocumentationHome() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DOCUMENTATION_HOME_NOTICES.forEachIndexed { index, notice ->
            Text(
                text = "${index + 1}. $notice",
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun DocumentationTopicList(topics: List<DocumentationTopic>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        topics.forEachIndexed { index, topic ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = topic.title,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = topic.body,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                )
            }
            if (index != topics.lastIndex) {
                HorizontalDivider(color = LukoaColors.Border)
            }
        }
    }
}

@Composable
private fun DocumentationMenuDrawer(
    selectedPage: DocumentationPage,
    onSelectPage: (DocumentationPage) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .testTag("documentation-menu-drawer"),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.78f),
            color = LukoaColors.Surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = rememberFeedbackClick(onDismiss),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "关闭文档目录",
                            tint = LukoaColors.TextPrimary,
                        )
                    }
                    Text(
                        text = "文档目录",
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                DocumentationPage.entries.forEach { page ->
                    val selected = selectedPage == page
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { this.selected = selected }
                            .clickable(
                                role = Role.Button,
                                onClick = rememberFeedbackClick(
                                    onClick = { onSelectPage(page) },
                                ),
                            ),
                        color = if (selected) LukoaColors.PrimarySoft else LukoaColors.Surface,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = page.menuLabel,
                                color = if (selected) LukoaColors.Primary else LukoaColors.TextPrimary,
                                style = if (page == DocumentationPage.Home) {
                                    MaterialTheme.typography.titleLarge
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                fontWeight = if (selected || page == DocumentationPage.Home) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.SemiBold
                                },
                            )
                            Text(
                                text = page.menuDescription,
                                color = LukoaColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }
            }
        }
        VerticalDivider(color = LukoaColors.Border)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.22f)
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss,
                ),
        )
    }
}

@Composable
private fun DocumentationSearchResults(
    query: String,
    hits: List<DocumentationSearchHit>,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "“${query.trim()}” 找到 ${hits.size} 条结果",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "清空搜索",
                modifier = Modifier.clickable(role = Role.Button, onClick = onClear),
                color = LukoaColors.Primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (hits.isEmpty()) {
            Text(
                text = "没有找到相关条目，可以换个更简短的关键词试试（比如“代理”“备份”“内存”）。",
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }
        hits.forEachIndexed { index, hit ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = hit.chapterTitle,
                    color = LukoaColors.Primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = hit.topic.title,
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = hit.topic.body,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                )
            }
            if (index != hits.lastIndex) {
                HorizontalDivider(color = LukoaColors.Border)
            }
        }
    }
}

private val DOCUMENTATION_HOME_NOTICES = listOf(
    "文档主要记录了一些酒馆常见的黑话和配置。如果仍有疑问，建议询问熟悉酒馆的答疑人员。",
    "本软件当前仅适配原生酒馆，不支持云酒馆、电脑酒馆以及各类二创酒馆。",
    "本软件不建议宣传，只是为了方便各位用户使用酒馆，也不推荐因此引流。",
    "询问答疑人员时请保持礼貌。答疑人员只是凭个人热心帮忙，请勿消磨他人耐心。",
    "请勿使用中转站等付费第三方 Key，这可能导致数据泄露。你的请求内容会经过中转服务，请勿在其中发送隐私、账号或其他敏感信息。",
    "本软件虽然使用 AI 协助制作，但也投入了很长时间。请勿随意盗用或商业化；本项目未在 GitHub 上使用 MIT 协议。",
)

private val TAVERN_JARGON_TOPICS = listOf(
    DocumentationTopic(
        title = "原生酒馆与二创酒馆",
        body = "原生酒馆通常指 SillyTavern 官方项目。云酒馆、电脑整合包以及经过他人修改的二创版本，目录、启动方式和文件结构可能不同，露科亚启动器目前不保证兼容。",
    ),
    DocumentationTopic(
        title = "API、API Key 与模型名",
        body = "API 是酒馆连接模型服务的入口，API Key 是服务用来确认账号与权限的密钥，模型名则决定实际调用哪个模型。地址、Key 或模型名任意一项填错，都可能无法聊天。",
    ),
    DocumentationTopic(
        title = "角色卡与 Persona",
        body = "角色卡写对方是谁，包括性格、背景、说话方式和开场白；Persona 写你是谁，也就是你在对话中的身份。角色跑偏时，通常要一起检查角色卡、Persona 和预设。",
    ),
    DocumentationTopic(
        title = "世界书",
        body = "世界书用来保存设定、地点、人物关系和规则。它会在满足条件时把相关资料加入上下文，并不是聊天记录；内容过多或过长也会增加模型负担。",
    ),
    DocumentationTopic(
        title = "预设与上下文",
        body = "预设会影响提示词结构、回复风格和采样参数。上下文是模型本次能够看到的聊天与资料范围；上下文越长，消耗通常越大，也更容易遇到长度限制。",
    ),
    DocumentationTopic(
        title = "401、404 与 429",
        body = "401 常见于 Key 错误或没有权限；404 常见于 API 地址、路径或模型名不对；429 通常表示请求过多、额度不足或服务限制。最终原因仍要以完整报错和服务方说明为准。",
    ),
)

private val LAUNCHER_QUESTION_TOPICS = listOf(
    DocumentationTopic(
        title = "第一次应该怎么做",
        body = "先安装并打开一次 Termux，再按启动器提示处理调用权限和外部应用调用。环境准备完成后再安装酒馆；第一次安装可能需要几分钟，Termux 仍在输出内容时不要重复点击。",
    ),
    DocumentationTopic(
        title = "启动器、Termux 和酒馆是什么关系",
        body = "启动器负责按钮、状态与提示，Termux 负责真正执行命令，酒馆则是最终打开的网页聊天界面。Termux 没有安装、没有权限或被系统限制时，启动器无法替代它执行命令。",
    ),
    DocumentationTopic(
        title = "启动中和运行中有什么区别",
        body = "启动中表示命令已经发出，但网页服务还没有确认可用；运行中才表示酒馆网页已经能够连接。处于启动中时先观察日志，不要连续发送启动命令。",
    ),
    DocumentationTopic(
        title = "实例、目录和端口",
        body = "每个实例都有自己的酒馆目录和访问端口。切换实例后，启动、停止、版本、用户和备份操作都会针对新实例；多个实例不能使用相同端口。",
    ),
    DocumentationTopic(
        title = "安装、更新和回退",
        body = "安装用于当前实例还没有酒馆时；更新会切换到较新的 SillyTavern 版本；回退会切换到较旧版本。操作前应停止酒馆、确认目标实例，并保留一份手动备份。",
    ),
    DocumentationTopic(
        title = "备份、导入、导出和应用",
        body = "备份会保存当前酒馆数据；导入是把外部备份加入备份库；导出是把库里的备份另存到其他位置；应用才会把备份恢复到当前实例，可能覆盖现有内容。",
    ),
    DocumentationTopic(
        title = "遇到问题先看哪里",
        body = "先查看启动页状态和 Termux 运行日志，再到工具箱执行一键体检。需要求助时建议导出诊断日志，并注意遮住 API Key、账号、密码和私人聊天内容。",
    ),
)

private val ADVANCED_FEATURE_TOPICS = listOf(
    DocumentationTopic(
        title = "前台窗口 vs 后台静默启动与一键重启",
        body = "在“设置 → 实例与运行环境 → 启动与外观”里可以切换启动方式：前台窗口模式会在 Termux 终端里实时显示酒馆日志，适合排错；后台静默模式不跳转 Termux，启动就绪后自动拉起浏览器。修改设置或插件后，可在启动页直接点“重启酒馆”。",
    ),
    DocumentationTopic(
        title = "局域网访问、扫码打开与请求代理",
        body = "在“酒馆 → 酒馆设置”中开启“允许局域网访问（listen）”后，手机会显示本机 Wi-Fi 地址并生成扫码二维码，电脑或平板连同一 Wi-Fi 即可扫码打开。建议同步添加局域网白名单或开启访问账号密码保护。如果需要让酒馆后端走本地代理，可填写代理地址并先点“测试代理连通性”。",
    ),
    DocumentationTopic(
        title = "聊天守护与“露科亚救回”文件",
        body = "“工具箱 → 聊天守护”会在酒馆运行时为较长的聊天记录保留同盘硬链接（不额外占空间）。当网页卡死重刷导致聊天被空档或残缺档覆盖（体积骤降一半以上）时，会自动在原聊天夹救回覆盖前的旧版本（文件名带“露科亚救回”）。聊天守护绝不删除原始聊天，也对瞬间写入空窗做了宽限保护；每条聊天最多保留 5 份救回，且永远保护体积最大的那份完整旧档不被覆盖清理。",
    ),
    DocumentationTopic(
        title = "轻量用户数据备份 vs 完整备份",
        body = "在“酒馆 → 备份”中，完整手动备份会保存酒馆源码与全部数据；“轻量备份数据”只打包聊天、角色卡、世界书、预设、config.yaml 和第三方扩展，跳过源码与依赖，体积小且速度快。恢复轻量备份时会自动锁定为“只恢复用户数据”，保留当前酒馆程序版本。",
    ),
    DocumentationTopic(
        title = "常用数据快捷导入（支持批量与聊天记录）",
        body = "在“酒馆 → 快捷导入”中，可直接从手机文件管理器单选或多选导入角色卡（PNG/JSON/CHARX）、世界书、对话预设、网页主题，以及指定角色目录下的聊天记录（.jsonl）。存在同名文件时会自动添加时间戳后缀，绝不覆盖旧文件。",
    ),
    DocumentationTopic(
        title = "空间体检与安全清理",
        body = "“工具箱 → 空间清理”会统计聊天记录、角色卡、缩略图、node_modules、旧日志与 npm 缓存等占用情况。只允许勾选清理缓存、旧日志、回滚副本和残留临时目录，聊天、角色卡、世界书、预设与配置永远在保护区内不会被清理。",
    ),
    DocumentationTopic(
        title = "扩展批量更新、服务器插件与暂存管理",
        body = "扩展管理支持一键批量更新所有没有本地改动的第三方扩展，并在更新失败时回滚；服务端插件可在“酒馆 → 服务器插件”安装或更新（记得在“酒馆设置”打开服务器插件总开关）。更新酒馆时自动暂存的本地改动，可以在“工具箱 → 暂存管理”中查看、恢复或清理。",
    ),
    DocumentationTopic(
        title = "通知栏快捷控制与桌面一键启动",
        body = "在“设置 → 实例与运行环境”中可以开关“运行状态通知栏控制”，酒馆运行时下拉通知栏即可快速打开酒馆、重启或停止；也可以点“添加桌面一键启动图标”把快捷方式放到手机桌面，或长按启动器图标直接一键启动。",
    ),
    DocumentationTopic(
        title = "夜间深色与日间浅色切换",
        body = "启动器提供纯正中性的夜间深色与日间浅色两套极简外观，可以在“设置 → 启动与外观 → 界面配色风格”里一键切换，重启后也会记住你的选择。",
    ),
    DocumentationTopic(
        title = "指定浏览器、屏幕常亮与自动启动",
        body = "在“设置 → 实例与运行环境 → 启动与外观”里，可指定每次打开酒馆时使用的浏览器（如 Chrome、Edge、Via 等，不再每次弹系统选择框），可开启前台屏幕常亮防止安装或备份中途息屏，也可按需开启打开启动器时自动启动酒馆；运行中还可在启动页一键复制本机地址。",
    ),
)
