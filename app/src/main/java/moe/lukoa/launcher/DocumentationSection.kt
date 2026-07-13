package moe.lukoa.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DocumentationCategory(
    val title: String,
    val topics: List<DocumentationTopic>,
)

private data class DocumentationTopic(
    val title: String,
    val body: String,
    val initiallyExpanded: Boolean = false,
)

private val documentationCategories = listOf(
    DocumentationCategory(
        title = "入门",
        topics = listOf(
            DocumentationTopic(
                title = "第一次使用顺序",
                body = "先安装 Termux，再打开一次 Termux，让它完成初始化。回到启动器后，按提示授予 RUN_COMMAND 权限，并开启 Termux 外部调用。\n\n都完成后，启动页会出现安装酒馆。第一次安装通常要 5-10 分钟，Termux 里还在刷字就说明它还在跑，别连续乱点。",
                initiallyExpanded = true,
            ),
            DocumentationTopic(
                title = "启动器、Termux、酒馆是什么关系",
                body = "启动器负责按钮、状态和日志；Termux 负责真正执行命令；酒馆是网页聊天界面。\n\n启动器不能替代 Termux。只要 Termux 没权限、没安装、没跑起来，启动器发出的命令就不会真正生效。",
            ),
            DocumentationTopic(
                title = "为什么要 RUN_COMMAND 权限",
                body = "RUN_COMMAND 是 Android 允许启动器调用 Termux 的权限。没有它，按钮看起来能点，但命令不会进 Termux。\n\n如果看到缺少权限，按引导复制命令到 Termux 执行，再回启动器重新检测。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "启动与日志",
        topics = listOf(
            DocumentationTopic(
                title = "启动酒馆后看哪里",
                body = "启动成功后，状态会变成运行中，并自动打开浏览器。浏览器没跳出来时，可以点启动页的返回酒馆。\n\n如果状态显示启动中，先等日志返回。卡很久再看露科亚问题分析辅助。",
            ),
            DocumentationTopic(
                title = "Termux 前台日志很重要",
                body = "安装、更新、准备环境这类长命令应该在 Termux 前台看得到。Termux 里有新增日志时，启动器也会同步新增。\n\n遇到报错，优先看 Termux 调用返回和诊断日志，不要只看按钮提示。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "网络与 API",
        topics = listOf(
            DocumentationTopic(
                title = "国内网络和镜像源",
                body = "GitHub、npm、Termux 包源都可能在国内卡住。设置页可以切换酒馆 Git 源、npm 源和 Termux 包源。\n\n不确定用哪个时，酒馆下载源选国内推荐，Termux 包源选清华源。",
            ),
            DocumentationTopic(
                title = "API 是什么",
                body = "API 是酒馆连接模型服务的入口。常见要填 API 地址、API Key、模型名。\n\n地址错会连不上，Key 错会鉴权失败，模型名少一个字也可能报错。",
            ),
            DocumentationTopic(
                title = "429、401、404 常见含义",
                body = "429 通常是请求太多、额度不够或服务限制；401 常见于 Key 错误或没有权限；404 常见于 API 地址、路径或模型名不对。\n\n这些只是常见方向。最终还是要看完整报错和模型服务说明。",
            ),
            DocumentationTopic(
                title = "模型名要完整复制",
                body = "很多服务的模型名不能靠猜，例如少一个字母、少一个版本后缀都可能不可用。\n\n测试 API 时，先用官方文档里的完整模型名；能发消息后再换复杂预设。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "角色与预设",
        topics = listOf(
            DocumentationTopic(
                title = "角色卡和 Persona",
                body = "角色卡写对方是谁，包括名字、性格、说话方式、背景和开场白。Persona 写你是谁，也就是你在对话里的身份。\n\n角色卡负责“对方怎么演”，Persona 负责“你是谁”。角色跑偏时，先查角色卡、系统提示词和预设。",
            ),
            DocumentationTopic(
                title = "世界书是什么",
                body = "世界书保存设定、地点、人物关系、规则和长期记忆。它不是聊天记录，而是满足条件时塞进上下文的资料。\n\n世界书太多、太长会拖慢回复，也更容易超上下文。",
            ),
            DocumentationTopic(
                title = "预设和上下文",
                body = "预设会影响提示词结构、回复风格、采样参数和上下文使用方式。不同模型适合的预设可能不同。\n\n上下文越长，模型看到的历史越多，但更慢、消耗更多，也更容易触发长度报错。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "备份与排错",
        topics = listOf(
            DocumentationTopic(
                title = "哪些东西最重要",
                body = "聊天记录、角色卡、世界书、插件、扩展、配置和密钥都可能是重要数据。长期使用后的 data 目录通常比源码更重要。\n\n更新、回退、装插件、导入别人配置前，先生成备份。",
            ),
            DocumentationTopic(
                title = "手动备份与自动备份",
                body = "手动备份适合重要操作前使用，可以自己命名。自动备份适合定时兜底，会按保留数量清理最旧的自动备份。\n\n自动备份不会替你判断风险。大更新、迁移手机、导入外部备份前，仍建议手动备份一次。",
            ),
            DocumentationTopic(
                title = "应用备份前要想清楚",
                body = "应用备份会把选中的备份恢复到当前酒馆目录。恢复后，当前数据可能被覆盖。\n\n如果只是想留一份文件，点导出；如果要把外部备份放进备份库，点导入到备份库。",
            ),
            DocumentationTopic(
                title = "先看 Termux 调用返回",
                body = "启动器按钮只是发命令，真正报错多数来自 Termux。看到 Error、failed、denied、not found，就优先看那段。\n\n找人答疑时，最好导出诊断日志，比截图一小块更有用。",
            ),
            DocumentationTopic(
                title = "不要连续乱点",
                body = "安装、更新、回退、备份都需要时间。连续点会让多个命令排队，最后更难判断哪个失败。\n\n看到正在处理就等它结束；危险操作弹二次确认时，看清楚再点。",
            ),
            DocumentationTopic(
                title = "区分酒馆问题和模型问题",
                body = "网页能打开但发消息报错，通常是 API、模型、额度、代理或预设问题。网页打不开，才优先怀疑酒馆没启动、端口占用或 Termux 没跑起来。\n\n简单判断：先看启动页状态，再看 Termux 调用返回。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "多实例与用户",
        topics = listOf(
            DocumentationTopic(
                title = "什么是分身实例",
                body = "分身实例是独立的 SillyTavern 副本，有独立的目录、端口、用户和版本状态。适合多人共用同一台设备，或想把工作和私人酒馆分开。\n\n主实例不能删除，分身实例可以根据路径类型决定是否删除酒馆目录。两个实例不能使用相同的目录或端口。",
            ),
            DocumentationTopic(
                title = "怎么切换实例",
                body = "在设置页的实例管理里点“当前实例”可以切换。切换后，启动页、版本页、备份页都会显示当前选中实例的信息。\n\n每个实例的酒馆版本、运行状态、备份目标都是独立的。",
            ),
            DocumentationTopic(
                title = "用户管理是什么",
                body = "酒馆支持多用户登录。启动器可以读取当前实例用户、新增普通用户，并删除符合保护条件的账户。\n\n登录标识是英文短名和数据目录名，不是显示昵称；SillyTavern 官方没有修改登录标识的接口。默认用户不能删除，最后一个启用的管理员也会被保护。酒馆运行时不能修改用户，请先停止酒馆再操作。",
            ),
        ),
    ),
    DocumentationCategory(
        title = "高级功能",
        topics = listOf(
            DocumentationTopic(
                title = "修复 npm 依赖什么时候用",
                body = "酒馆启动报错提到 dependency、module not found 或 ERR_REQUIRE 时，可能是依赖损坏。\n\n修复会先备份原 node_modules，再用当前 npm 源重新安装。安装失败会尽量恢复原内容。操作前请先停止酒馆。",
            ),
            DocumentationTopic(
                title = "重置网页主题",
                body = "如果酒馆网页打不开、白屏或卡死，可能是装了有问题的主题。重置主题会把 UI 恢复到默认状态，不会删除聊天记录和角色卡。\n\n这通常是网页打不开时的最后一招，重置前建议先导出备份。",
            ),
            DocumentationTopic(
                title = "Node.js 内存上限",
                body = "Node.js 默认内存上限较低，复杂角色卡或大量历史消息时可能报内存不足。可以调到 2GB、4GB 或 6GB。\n\n调高内存会让酒馆占用更多手机内存，但不一定提升速度。修改只作用于当前实例，修改前会确认。",
            ),
            DocumentationTopic(
                title = "聊天记录上传限制",
                body = "酒馆默认上传文件大小有限制（通常 500MB）。如果你需要上传大文件，可以调到 1GB 或 2GB。\n\n这个修改通过补丁实现，酒馆更新或回退前会临时卸载补丁，完成后自动重新应用。可以随时检查补丁是否仍然有效。",
            ),
            DocumentationTopic(
                title = "端口被占用怎么办",
                body = "酒馆停止后端口仍被占用，通常是残留进程没完全退出。可以在设置页的诊断区点“强制清理”释放端口或清理残留进程。\n\n启动器会尽量限定当前实例目录和端口，但强制操作仍只建议在确认是残留进程时使用，并应阅读确认弹窗里的影响说明。",
            ),
            DocumentationTopic(
                title = "Termux 配置文件冲突",
                body = "更新 Termux 软件包时，如果本地有修改过的配置文件，会弹出冲突提示。“保留当前配置”保留你的自定义，“使用新版配置”用软件包自带的最新配置。\n\n不确定时选“保留当前配置”，后续可以手动改回来。",
            ),
        ),
    ),
)

@Composable
fun DocumentationSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        documentationCategories.forEachIndexed { index, category ->
            DocumentationAccordionGroup(
                category = category,
                addTopSpacing = index > 0,
            )
        }
    }
}

@Composable
private fun DocumentationAccordionGroup(
    category: DocumentationCategory,
    addTopSpacing: Boolean,
) {
    Text(
        text = category.title,
        modifier = Modifier.padding(
            start = 2.dp,
            top = if (addTopSpacing) 24.dp else 0.dp,
            end = 2.dp,
            bottom = 10.dp,
        ),
        color = LukoaColors.Text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .documentationDashedOutline(),
    ) {
        category.topics.forEachIndexed { index, topic ->
            DocumentationAccordionItem(
                topic = topic,
                showTopDivider = index > 0,
            )
        }
    }
}

@Composable
private fun DocumentationAccordionItem(
    topic: DocumentationTopic,
    showTopDivider: Boolean,
) {
    var expanded by remember(topic.title) { mutableStateOf(topic.initiallyExpanded) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "文档展开箭头",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .documentationDashedDivider(showTopDivider),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = expanded,
                    role = Role.Button,
                    onValueChange = { expanded = it },
                )
                .semantics {
                    stateDescription = if (expanded) "已展开" else "已折叠"
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = topic.title,
                modifier = Modifier.weight(1f),
                color = LukoaColors.Text.copy(alpha = 0.80f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "›",
                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation },
                color = LukoaColors.Dim,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = topic.body,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                color = LukoaColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 20.8.sp,
            )
        }
    }
}

private fun Modifier.documentationDashedOutline(): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawRoundRect(
        color = LukoaColors.Line,
        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = androidx.compose.ui.geometry.Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        ),
        cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

private fun Modifier.documentationDashedDivider(show: Boolean): Modifier {
    if (!show) return this
    return drawBehind {
        drawLine(
            color = LukoaColors.Line,
            start = Offset(16.dp.toPx(), 0f),
            end = Offset(size.width - 16.dp.toPx(), 0f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}
