package moe.lukoa.launcher

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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


@Composable
fun UpdateAvailableDialog(
    updateInfo: GithubUpdateInfo,
    currentVersionName: String,
    downloading: Boolean,
    onInstall: () -> Unit,
    onOpenRelease: () -> Unit,
    onClearBadge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val publishedText = remember(updateInfo.publishedAt) {
        formatGithubPublishedTime(updateInfo.publishedAt)
    }
    val releaseNotesDocument = remember(updateInfo.versionName, updateInfo.body) {
        GithubReleaseNotesFormatter.parse(updateInfo.versionName, updateInfo.body)
    }
    val primaryActionText = when {
        downloading -> "下载中..."
        updateInfo.apkDownloadUrl.isBlank() -> "打开发布页"
        else -> "立即更新"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        titleContentColor = LukoaColors.Primary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            Text("发现${updateInfo.releaseTypeLabel} v${updateInfo.versionName}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = LukoaColors.Elevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VersionStatusValueCard(
                                label = "当前版本",
                                value = "v$currentVersionName",
                                accentColor = LukoaColors.TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            VersionStatusValueCard(
                                label = "新版本",
                                value = "v${updateInfo.versionName}",
                                accentColor = LukoaColors.Primary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        VersionInfoLine("版本类型", updateInfo.releaseTypeLabel)
                        if (publishedText.isNotBlank()) {
                            VersionInfoLine("发布时间", publishedText)
                        }
                        if (updateInfo.releaseName.isNotBlank() && updateInfo.releaseName != updateInfo.tagName) {
                            VersionInfoLine("版本标题", updateInfo.releaseName)
                        }
                    }
                }
                if (updateInfo.prerelease) {
                    Surface(
                        color = LukoaColors.AccentSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LukoaColors.Accent.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "这是测试版更新，功能会更早到，但稳定性可能不如正式版。",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = LukoaColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text(
                    text = "更新内容",
                    color = LukoaColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Surface(
                    color = LukoaColors.Elevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp, max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    ) {
                        GithubReleaseNotesContent(document = releaseNotesDocument)
                    }
                }
                Surface(
                    color = LukoaColors.Elevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Text(
                        text = "清除红点后，这个版本不会再自动弹出提醒，但你之后仍然可以手动点右上角版本查看。",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ToneActionButton(
                    text = primaryActionText,
                    enabled = !downloading,
                    tone = ActionTone.Safe,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onInstall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToneActionButton(
                        text = "详情",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelease,
                    )
                    ToneActionButton(
                        text = "清除红点",
                        enabled = true,
                        tone = ActionTone.Neutral,
                        modifier = Modifier.weight(1f),
                        onClick = onClearBadge,
                    )
                }
                ToneActionButton(
                    text = "稍后",
                    enabled = true,
                    tone = ActionTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}
@Composable
internal fun GithubUpdateStatusCard(
    githubUpdateState: GithubUpdateUiState,
) {
    val statusText: String
    val statusColor: Color
    when {
        githubUpdateState.downloading -> {
            statusText = "正在下载"
            statusColor = LukoaColors.Primary
        }
        githubUpdateState.checking -> {
            statusText = "正在检查"
            statusColor = LukoaColors.Primary
        }
        githubUpdateState.hasUpdate -> {
            statusText = "发现新版本"
            statusColor = LukoaColors.Primary
        }
        githubUpdateState.latest != null -> {
            statusText = "已是最新"
            statusColor = LukoaColors.TextSecondary
        }
        githubUpdateState.repository.isBlank() -> {
            statusText = "未配置仓库"
            statusColor = LukoaColors.Accent
        }
        else -> {
            statusText = "等待检查"
            statusColor = LukoaColors.TextSecondary
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LukoaColors.Elevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当前状态",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = LukoaCapsuleShape,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = githubUpdateState.message,
                color = if (githubUpdateState.hasUpdate) LukoaColors.TextPrimary else LukoaColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            VersionInfoLine("更新通道", githubUpdateState.channel.label)
            githubUpdateState.latest?.let { latest ->
                VersionInfoLine("GitHub 最新", "v${latest.versionName}")
                VersionInfoLine("版本类型", latest.releaseTypeLabel)
                if (latest.prerelease) {
                    Text(
                        text = "当前读到的是测试版发布，可能比稳定版更早，但不保证和正式版一样稳定。",
                        color = LukoaColors.Accent,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            githubUpdateState.lastCheckedText
                .takeIf { it.isNotBlank() }
                ?.let { checkedText ->
                    VersionInfoLine("上次检查", checkedText)
                }
        }
    }
}

private val GITHUB_UPDATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatGithubPublishedTime(text: String): String {
    if (text.isBlank()) return ""
    return runCatching {
        GITHUB_UPDATE_TIME_FORMATTER.format(
            Instant.parse(text).atZone(ZoneId.systemDefault()),
        )
    }.getOrElse {
        text.replace("T", " ").removeSuffix("Z").take(16)
    }
}
