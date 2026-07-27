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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


private enum class DocCategory(val label: String, val title: String) {
    NewUser("新手", "新手上手"),
    Launch("启动", "启动与运行"),
    Instance("实例", "多实例与设置"),
    Version("更新", "安装、更新与回退"),
    Api("API", "API 与报错"),
    Role("角色", "角色、预设与上下文"),
    Backup("备份", "备份与恢复"),
    Troubleshooting("排错", "排错思路"),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DocumentationSection(
    pageScrollState: ScrollState,
    onPagerLockChange: (Boolean) -> Unit = {},
) {
    var selectedCategory by remember { mutableStateOf(DocCategory.NewUser) }
    val sectionWindowPositions = remember { mutableMapOf<DocCategory, Float>() }
    val sectionTopOffsetPx = with(LocalDensity.current) { 56.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(onPagerLockChange) {
        onDispose { onPagerLockChange(false) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.46f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "文档目录",
                        color = LukoaColors.Text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${DocCategory.entries.size} 章",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInteropFilter { event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> onPagerLockChange(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_OUTSIDE -> onPagerLockChange(false)
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
                        onClick = {
                            selectedCategory = category
                            coroutineScope.launch {
                                val sectionWindowY = sectionWindowPositions[category] ?: return@launch
                                val targetScroll = (
                                    pageScrollState.value + sectionWindowY - sectionTopOffsetPx
                                    ).roundToInt().coerceAtLeast(0)
                                pageScrollState.animateScrollTo(targetScroll)
                            }
                        },
                    )
                }
            }
            }
        }

        DocCategory.entries.forEachIndexed { index, category ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sectionWindowPositions[category] = coordinates.positionInWindow().y
                    },
            ) {
                DocChapterPanel(
                    number = index + 1,
                    title = category.title,
                ) {
                    when (category) {
                        DocCategory.NewUser -> NewUserDocs()
                        DocCategory.Launch -> LaunchDocs()
                        DocCategory.Instance -> InstanceDocs()
                        DocCategory.Version -> VersionDocs()
                        DocCategory.Api -> ApiDocs()
                        DocCategory.Role -> RoleDocs()
                        DocCategory.Backup -> BackupDocs()
                        DocCategory.Troubleshooting -> TroubleshootingDocs()
                    }
                }
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
    DocTopicCard(
        title = "权限处理顺序",
        body = "先确认 Termux 已安装并打开过一次，再处理 RUN_COMMAND 和 Termux 外部调用。需要自动备份或长任务时，再放行启动器与 Termux 的后台运行；导入、导出和应用备份时再检查文件权限。\n\n设置页的权限中心会标出仍需处理的项目，不必一次猜完所有系统开关。",
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
        body = "安装、更新、准备环境这类长命令应该在 Termux 前台看得到。Termux 里有新增日志时，启动器也会同步新增。\n\n遇到报错，优先看 Termux 前台回传和诊断日志，不要只看按钮提示。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "启动中和运行中不一样",
        body = "启动中表示命令已经发出，但酒馆端口还没有确认可用；运行中才表示网页服务已经能连接。\n\n启动中请先等，不要连续点启动。长时间不结束时，再检查 Termux 前台日志和端口占用。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "普通停止与强制清理",
        body = "普通停止只结束当前实例对应的酒馆进程，日常优先使用。强制清理用于状态卡住、端口残留或普通停止无效时，会更广泛地检查残留进程。\n\n不确定时先普通停止；只有启动器明确建议或端口一直被占用，再到修复工具使用强制清理。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "国内网络和镜像源",
        body = "GitHub、npm、Termux 包源都可能在国内卡住。设置页可以切换酒馆 Git 源、npm 源和 Termux 包源。\n\n不确定用哪个时，酒馆下载源选国内推荐，Termux 包源选清华源。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun InstanceDocs() {
    DocTopicCard(
        title = "路径、端口和实例的关系",
        body = "每个实例都有自己的酒馆目录和端口。目录决定使用哪套程序与数据，端口决定浏览器访问哪个服务；改目录不会自动改端口，改端口也不会移动文件。\n\n多个实例必须使用不同端口，否则后启动的实例会因为端口占用而失败。",
        accentColor = LukoaColors.Info,
    )
    DocTopicCard(
        title = "切换实例会影响什么",
        body = "切换后，启动、停止、版本读取、更新回退、用户管理和备份恢复都会针对新实例。操作前先看设置页显示的当前实例、目录和端口。\n\n备份包不会自动区分你心里想操作哪个实例，应用前务必再次确认目标实例。",
        accentColor = LukoaColors.Info,
    )
    DocTopicCard(
        title = "托管目录与自定义目录",
        body = "托管目录由启动器按实例分配，适合大多数人；传统默认目录是 ~/SillyTavern；自定义目录适合已经有现成安装或明确知道路径的人。\n\n保存路径只修改配置，不会搬文件。需要移动数据时必须使用迁移功能，并先做备份。",
        accentColor = LukoaColors.Info,
    )
}

@Composable
private fun VersionDocs() {
    DocTopicCard(
        title = "安装、更新和回退的区别",
        body = "安装用于当前实例还没有酒馆时；更新把已安装酒馆切到较新的官方版本；回退把它切到较旧版本。版本页管理的是 SillyTavern，不是启动器本身。\n\n启动器自身更新在设置页处理。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "更新或回退前检查",
        body = "先停止酒馆，确认当前实例和目标版本，再检查是否有本地源码改动。启动器会在执行前创建安全备份，但重要数据仍建议额外做一次手动备份。\n\n如果版本列表来自旧下载源，先刷新列表再选择。",
        accentColor = LukoaColors.Amber,
    )
    DocTopicCard(
        title = "本地改动为什么会阻止切换",
        body = "手动修改酒馆源码或部分插件直接改动程序文件后，Git 可能无法安全切换版本。强行继续可能覆盖你的修改或让程序处于混合状态。\n\n先根据版本页列出的文件处理改动，确认干净后再更新或回退。",
        accentColor = LukoaColors.Amber,
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
    DocTopicCard(
        title = "导入、导出和应用的区别",
        body = "导入只是把外部备份复制进启动器的手动备份库；导出是把备份库里的文件另存到你选择的位置；应用才会把备份内容恢复到当前酒馆目录。\n\n想留文件就导出，想加入备份库就导入，只有确认要覆盖当前数据时才应用。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "迁移到新手机",
        body = "旧手机先停止酒馆并生成手动备份，再把备份导出到可靠位置。新手机安装启动器与 Termux、完成权限和环境准备后，把备份导入备份库，确认目标实例再应用。\n\n恢复后先读取酒馆版本和用户，再尝试启动；旧手机的数据先不要急着删除。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun TroubleshootingDocs() {
    DocTopicCard(
        title = "先看 Termux 前台回传",
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
        body = "网页能打开但发消息报错，通常是 API、模型、额度、代理或预设问题。网页打不开，才优先怀疑酒馆没启动、端口占用或 Termux 没跑起来。\n\n简单判断：先看启动页状态，再看 Termux 前台回传。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "一键体检的使用顺序",
        body = "先在设置页运行一键体检，根据失败项目执行主操作，再重新体检。环境、权限或路径问题没有解决前，不要反复安装或更新。\n\n体检通过但仍打不开网页时，再看 Termux 前台回传、端口和诊断日志。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "端口占用怎么排查",
        body = "先确认是否有另一个实例或旧进程正在使用同一端口。普通停止后重新检测；仍占用时，再使用修复工具里的强制清理建议。\n\n如果两个实例配置了相同端口，给其中一个改成未使用的端口，再重新启动。",
        accentColor = LukoaColors.Accent,
    )
    DocTopicCard(
        title = "反馈问题前准备什么",
        body = "记录当时操作的实例、按钮和报错时间，保留 Termux 前台完整错误，并从设置页导出诊断日志。\n\n不要只截最后一行，也不要公开 API Key、账号密码或私人文件内容。",
        accentColor = LukoaColors.Accent,
    )
}

@Composable
private fun DocNavChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) LukoaColors.Accent else LukoaColors.Text
    Surface(
        modifier = Modifier.clickable(onClick = rememberFeedbackClick(onClick)),
        color = if (selected) LukoaColors.AccentSoft else Color.Transparent,
        shape = LukoaCapsuleShape,
        border = BorderStroke(
            1.dp,
            if (selected) LukoaColors.Accent.copy(alpha = 0.36f) else LukoaColors.Line.copy(alpha = 0.48f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DocChapterPanel(
    number: Int,
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.46f)),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = LukoaColors.AccentSoft,
                    shape = LukoaCapsuleShape,
                ) {
                    Text(
                        text = number.toString().padStart(2, '0'),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = LukoaColors.Accent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = title,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = LukoaColors.Line.copy(alpha = 0.38f))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DocTopicCard(
    title: String,
    body: String,
    accentColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .background(accentColor, RoundedCornerShape(50))
                    .padding(3.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}
