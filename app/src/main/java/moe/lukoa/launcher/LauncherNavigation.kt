package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


enum class LauncherTab(
    val label: String,
    val shortLabel: String,
) {
    Docs("文档", "文"),
    Tavern("酒馆", "酒"),
    Launch("启动", "启"),
    Toolbox("工具箱", "工"),
    Settings("设置", "设"),
}

enum class LauncherSecondaryPage(
    val title: String,
    val infoText: String? = null,
) {
    VersionManagement(
        title = "版本管理",
        infoText = "在这里可以更新或回退 SillyTavern 程序版本，不会删除你的聊天记录、角色卡、世界书和扩展。\n每次切换前都会先自动创建安全备份；如果目录里有你自己改过的程序文件，可以点「处理指引」或先暂存改动后再更新。",
    ),
    Backup(
        title = "备份",
        infoText = "「创建手动备份」会打包整个酒馆程序与全部数据；「轻量备份数据」只打包聊天、角色卡、世界书、预设、配置和扩展，体积小、速度快。\n应用或删除备份都需要二次确认，避免误操作。",
    ),
    ExtensionManagement(
        title = "扩展管理",
        infoText = "这里只管理当前酒馆的第三方网页扩展，不会处理服务器插件。\n可粘贴公开 GitHub 仓库地址安装扩展；检查到更新后可安全替换并保留最近一次回退快照。变更后刷新网页或重启酒馆即可生效。",
    ),
    TavernConfig(
        title = "酒馆设置",
        infoText = "这里可以直接开关常用的酒馆功能，不用手动打开配置文件修改。每次保存前都会自动备份原配置；改完后重启酒馆即可生效。",
    ),
    ServerPlugins(
        title = "服务器插件",
        infoText = "平时一般用不到这里，普通酒馆网页扩展请去「扩展管理」安装。\n只有少数插件明确要求安装「后端 / 服务器插件」时，才来这里粘贴网址安装，并在「酒馆设置」的更多进阶设置里开启后端插件开关。",
    ),
    QuickImport(
        title = "快捷导入",
        infoText = "直接从手机文件里选角色卡（PNG / JSON / CHARX）、世界书、预设、主题（JSON）或聊天记录（JSONL），支持一次多选批量导入。\n启动器会自动放进酒馆对应位置；若已有同名文件会自动改名保留，绝不覆盖旧档。",
    ),
    HealthCheck(
        title = "一键体检",
        infoText = "把权限、酒馆路径、网络镜像源和运行环境统一检查一遍，不会修改酒馆文件。遇到问题可直接按结果给出的建议处理。",
    ),
    ChatGuard(
        title = "聊天守护",
        infoText = "为什么需要守护：手机浏览器卡死重刷时，酒馆有时会把没加载完的空白聊天存回去，覆盖原本很长的对话。\n守护如何工作：酒馆运行时会给长聊天建立不额外占空间的硬链接保底；一旦发现聊天体积骤降一半以上，会自动把旧档救回为同目录的「原名 [露科亚救回 时间]」文件。\n安全保障：绝不删除你的原始聊天；每条聊天最多保留 5 份救回，且永远保护体积最大的那份完整旧档不被清理。",
    ),
    StorageCleanup(
        title = "空间清理",
        infoText = "只清理可安全重建的缓存、旧日志和升级残留临时文件，绝不会碰聊天记录、角色卡、世界书、预设或备份库。",
    ),
    RepairTools(
        title = "修复工具",
        infoText = "安装中断或启动缺依赖时可用「修复 npm 依赖」；网页主题损坏打不开时再点「重置主题」。\n内存上限和聊天文件大小一般保持默认或常用值即可，所有修改前都会先备份并确认。",
    ),
    Debug(
        title = "Debug 区",
        infoText = "集中放置故障自动诊断、日志导出与最后手段。\n遇到启动失败先看诊断建议或导出诊断日志；只有普通停止无效或确认端口被占时，才使用强制处理。",
    ),
    GitStash(
        title = "暂存管理",
        infoText = "平时一般不需要打开这里。\n只有当你更新或回退酒馆版本时选择了「先把本地改动收起来」，记录才会出现在这里，方便之后放回或清理。",
    ),
    TaskCenter(
        title = "任务中心",
        infoText = "这里读取启动器已经保存的任务记录和 Termux 返回，不会重新执行任务。切到后台期间产生的结果，回来后仍可在这里补查。",
    ),
    UserManagement(
        title = "用户管理",
        infoText = "这里管理当前酒馆实例中的登录账户。酒馆运行中可直接读取用户列表，新增或删除账户前需要先停止酒馆；删除账户时会保留数据目录。",
    ),
    MirrorSettings(
        title = "网络与镜像源",
        infoText = "下载源决定安装、更新酒馆和下载依赖时走哪条线路，切换不会影响已有聊天或数据。\n不确定选哪个时，直接点「一键测速优选」，启动器会并行测速并自动挑出最快的可用源；Termux 包源点一下即可直接切换。",
    ),
}

@Composable
fun LauncherBottomBar(
    selectedTab: LauncherTab,
    onSelectTab: (LauncherTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
    ) {
        Column {
            HorizontalDivider(color = LukoaColors.Border)
            NavigationBar(
                containerColor = LukoaColors.Elevated,
                contentColor = LukoaColors.TextPrimary,
                tonalElevation = 0.dp,
            ) {
                LauncherTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Text(
                                text = tab.shortLabel,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LukoaColors.Primary,
                            selectedTextColor = LukoaColors.TextPrimary,
                            indicatorColor = LukoaColors.PrimarySoft,
                            unselectedIconColor = LukoaColors.TextSecondary,
                            unselectedTextColor = LukoaColors.TextSecondary,
                        ),
                    )
                }
            }
        }
    }
}


@Composable
fun BusyPanel(label: String, startedAtMillis: Long) {
    var nowMillis by remember(label, startedAtMillis) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    LaunchedEffect(label, startedAtMillis) {
        while (true) {
            nowMillis = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    val elapsedSeconds = if (startedAtMillis > 0L) {
        ((nowMillis - startedAtMillis).coerceAtLeast(0L) / 1000L).toInt()
    } else {
        0
    }
    val elapsedText = BusyPanelPresentationResolver.formatElapsed(elapsedSeconds)
    val presentation = BusyPanelPresentationResolver.resolve(label, elapsedSeconds)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.PrimarySoft,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LukoaColors.Primary.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(LukoaColors.Primary, RoundedCornerShape(4.dp)),
                    )
                    Text(
                        text = "命令执行中",
                        color = LukoaColors.Primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StatusPill(
                    text = elapsedText,
                    active = true,
                    toneColor = LukoaColors.Primary,
                    activeBackground = LukoaColors.Elevated,
                )
            }
            Text(
                text = label,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = LukoaColors.Primary,
                trackColor = LukoaColors.Elevated,
            )
            Text(
                text = presentation.activityText,
                color = LukoaColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = presentation.helperText,
                color = LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
