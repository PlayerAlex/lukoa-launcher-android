package moe.lukoa.launcher

import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private enum class DocCategory(val label: String, val title: String) {
    NewUser("新手", "新手上手"),
    Launch("启动", "启动与 Termux"),
    Api("API", "API 与报错"),
    Role("角色", "角色、预设与上下文"),
    Backup("备份", "数据安全"),
    Troubleshooting("排错", "排错思路"),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentationSection(
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    var selectedCategory by remember { mutableStateOf(DocCategory.NewUser) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionPanel(title = "文档导航", accentColor = LukoaColors.Accent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> onPagerLockChange(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> onPagerLockChange(false)
                        }
                        false
                    }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DocCategory.entries.forEach { category ->
                    DocNavChip(
                        text = category.label,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                    )
                }
            }
            Text(
                text = "遇到问题先看对应分类，不用从头翻。",
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionPanel(title = selectedCategory.title, accentColor = LukoaColors.Accent) {
            when (selectedCategory) {
                DocCategory.NewUser -> NewUserDocs()
                DocCategory.Launch -> LaunchDocs()
                DocCategory.Api -> ApiDocs()
                DocCategory.Role -> RoleDocs()
                DocCategory.Backup -> BackupDocs()
                DocCategory.Troubleshooting -> TroubleshootingDocs()
            }
        }
    }
}

@Composable
private fun NewUserDocs() {
    DocTopicCard(
        title = "第一次使用顺序",
        body = "先安装 Termux，再打开一次 Termux，让它完成初始化。回到启动器后，按提示授予 RUN_COMMAND 权限，并开启 Termux 外部调用。\n\n都完成后，启动页会出现安装酒馆。第一次安装通常要 5-10 分钟，Termux 里还在刷字就说明它还在跑，别连续乱点。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "启动器、Termux、酒馆是什么关系",
        body = "启动器负责按钮、状态和日志；Termux 负责真正执行命令；酒馆是网页聊天界面。\n\n启动器不能替代 Termux。只要 Termux 没权限、没安装、没跑起来，启动器发出的命令就不会真正生效。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "为什么要 RUN_COMMAND 权限",
        body = "RUN_COMMAND 是 Android 允许启动器调用 Termux 的权限。没有它，按钮看起来能点，但命令不会进 Termux。\n\n如果看到缺少权限，按引导复制命令到 Termux 执行，再回启动器重新检测。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun LaunchDocs() {
    DocTopicCard(
        title = "启动酒馆后看哪里",
        body = "启动成功后，状态会变成运行中，并自动打开浏览器。浏览器没跳出来时，可以点启动页的返回酒馆。\n\n如果状态显示启动中，先等日志返回。卡很久再看露科亚问题分析辅助。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "Termux 前台日志很重要",
        body = "安装、更新、准备环境这类长命令应该在 Termux 前台看得到。Termux 里有新增日志时，启动器也会同步新增。\n\n遇到报错，优先看 Termux 调用返回和诊断日志，不要只看按钮提示。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "国内网络和镜像源",
        body = "GitHub、npm、Termux 包源都可能在国内卡住。设置页可以切换酒馆 Git 源、npm 源和 Termux 包源。\n\n不确定用哪个时，酒馆下载源选国内推荐，Termux 包源选清华源。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun ApiDocs() {
    DocTopicCard(
        title = "API 是什么",
        body = "API 是酒馆连接模型服务的入口。常见要填 API 地址、API Key、模型名。\n\n地址错会连不上，Key 错会鉴权失败，模型名少一个字也可能报错。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "429、401、404 常见含义",
        body = "429 通常是请求太多、额度不够或服务限制；401 常见于 Key 错误或没有权限；404 常见于 API 地址、路径或模型名不对。\n\n这些只是常见方向。最终还是要看完整报错和模型服务说明。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "模型名要完整复制",
        body = "很多服务的模型名不能靠猜，例如少一个字母、少一个版本后缀都可能不可用。\n\n测试 API 时，先用官方文档里的完整模型名；能发消息后再换复杂预设。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun RoleDocs() {
    DocTopicCard(
        title = "角色卡和 Persona",
        body = "角色卡写对方是谁，包括名字、性格、说话方式、背景和开场白。Persona 写你是谁，也就是你在对话里的身份。\n\n角色卡负责“对方怎么演”，Persona 负责“你是谁”。角色跑偏时，先查角色卡、系统提示词和预设。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "世界书是什么",
        body = "世界书保存设定、地点、人物关系、规则和长期记忆。它不是聊天记录，而是满足条件时塞进上下文的资料。\n\n世界书太多、太长会拖慢回复，也更容易超上下文。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "预设和上下文",
        body = "预设会影响提示词结构、回复风格、采样参数和上下文使用方式。不同模型适合的预设可能不同。\n\n上下文越长，模型看到的历史越多，但更慢、消耗更多，也更容易触发长度报错。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun BackupDocs() {
    DocTopicCard(
        title = "哪些东西最重要",
        body = "聊天记录、角色卡、世界书、插件、扩展、配置和密钥都可能是重要数据。长期使用后的 data 目录通常比源码更重要。\n\n更新、回退、装插件、导入别人配置前，先生成备份。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "手动备份与自动备份",
        body = "手动备份适合重要操作前使用，可以自己命名。自动备份适合定时兜底，会按保留数量清理最旧的自动备份。\n\n自动备份不会替你判断风险。大更新、迁移手机、导入外部备份前，仍建议手动备份一次。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "应用备份前要想清楚",
        body = "应用备份会把选中的备份恢复到当前酒馆目录。恢复后，当前数据可能被覆盖。\n\n如果只是想留一份文件，点导出；如果要把外部备份放进备份库，点导入到备份库。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun TroubleshootingDocs() {
    DocTopicCard(
        title = "先看 Termux 调用返回",
        body = "启动器按钮只是发命令，真正报错多数来自 Termux。看到 Error、failed、denied、not found，就优先看那段。\n\n找人答疑时，最好导出诊断日志，比截图一小块更有用。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "不要连续乱点",
        body = "安装、更新、回退、备份都需要时间。连续点会让多个命令排队，最后更难判断哪个失败。\n\n看到正在处理就等它结束；危险操作弹二次确认时，看清楚再点。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "区分酒馆问题和模型问题",
        body = "网页能打开但发消息报错，通常是 API、模型、额度、代理或预设问题。网页打不开，才优先怀疑酒馆没启动、端口占用或 Termux 没跑起来。\n\n简单判断：先看启动页状态，再看 Termux 调用返回。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun DocNavChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) LukoaColors.Accent else LukoaColors.Muted
    Surface(
        modifier = Modifier.clickable(onClick = rememberFeedbackClick(onClick)),
        color = if (selected) LukoaColors.Accent.copy(alpha = 0.12f) else LukoaColors.SurfaceAlt.copy(alpha = 0.5f),
        shape = LukoaCapsuleShape,
        border = BorderStroke(1.dp, if (selected) LukoaColors.Accent.copy(alpha = 0.3f) else Color.Transparent),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = color,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DocTopicCard(
    title: String,
    body: String,
    accentColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.SurfaceAlt.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(4.dp)
                    .height(18.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
