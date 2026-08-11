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
}

private data class DocumentationTopic(
    val title: String,
    val body: String,
)

@Composable
fun DocumentationSection(
    modifier: Modifier = Modifier,
    initialMenuOpen: Boolean = false,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    var selectedPage by rememberSaveable { mutableStateOf(DocumentationPage.Home) }
    var menuOpen by rememberSaveable { mutableStateOf(initialMenuOpen) }
    val contentScrollState = rememberScrollState()

    BackHandler(enabled = menuOpen || selectedPage != DocumentationPage.Home) {
        if (menuOpen) {
            menuOpen = false
        } else {
            selectedPage = DocumentationPage.Home
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
                    Crossfade(
                        targetState = selectedPage,
                        label = "documentation-page-content",
                    ) { page ->
                        when (page) {
                            DocumentationPage.Home -> DocumentationHome()
                            DocumentationPage.TavernJargon -> DocumentationTopicList(TAVERN_JARGON_TOPICS)
                            DocumentationPage.LauncherQuestions -> DocumentationTopicList(LAUNCHER_QUESTION_TOPICS)
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
        verticalArrangement = Arrangement.spacedBy(15.dp),
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
