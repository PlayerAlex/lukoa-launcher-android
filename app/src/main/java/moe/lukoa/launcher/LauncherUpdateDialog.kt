package moe.lukoa.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onIgnoreVersion: () -> Unit,
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
    val primaryClick = rememberFeedbackClick(onInstall)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        shape = RoundedCornerShape(16.dp),
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            ReleaseNotesDialogHeader(
                title = "发现${updateInfo.releaseTypeLabel} v${updateInfo.versionName}",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReleaseNotesMetadataCard(
                    versionLine = "v$currentVersionName → v${updateInfo.versionName}",
                    releaseTypeLabel = updateInfo.releaseTypeLabel,
                    prerelease = updateInfo.prerelease,
                    publishedText = publishedText,
                    releaseTitle = updateInfo.releaseName
                        .takeIf { it.isNotBlank() && it != updateInfo.tagName && it != updateInfo.versionName },
                    showPrereleaseNotice = updateInfo.prerelease,
                )

                ReleaseNotesBodyCard(document = releaseNotesDocument)

                Button(
                    onClick = primaryClick,
                    enabled = !downloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("update-dialog-primary"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LukoaColors.Primary,
                        contentColor = LukoaColors.OnPrimary,
                        disabledContainerColor = LukoaColors.Surface,
                        disabledContentColor = LukoaColors.Dim,
                    ),
                ) {
                    Text(
                        text = primaryActionText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryActionButton(
                        text = "打开发布页",
                        enabled = true,
                        accentColor = LukoaColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenRelease,
                    )
                    SecondaryActionButton(
                        text = "忽略这个版本",
                        enabled = true,
                        accentColor = LukoaColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onIgnoreVersion,
                    )
                }

                SecondaryActionButton(
                    text = "稍后",
                    enabled = true,
                    accentColor = LukoaColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )

                Text(
                    text = "忽略后不再自动提醒此版本，之后仍可点右上角版本号查看。",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 17.sp,
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
internal fun CurrentVersionReleaseNotesDialog(
    releaseInfo: GithubUpdateInfo,
    onOpenRelease: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val publishedText = remember(releaseInfo.publishedAt) {
        formatGithubPublishedTime(releaseInfo.publishedAt)
    }
    val releaseNotesDocument = remember(releaseInfo.versionName, releaseInfo.body) {
        GithubReleaseNotesFormatter.parse(releaseInfo.versionName, releaseInfo.body)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Elevated,
        shape = RoundedCornerShape(16.dp),
        titleContentColor = LukoaColors.TextPrimary,
        textContentColor = LukoaColors.TextPrimary,
        title = {
            ReleaseNotesDialogHeader(
                title = "v${releaseInfo.versionName} 更新内容",
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReleaseNotesMetadataCard(
                    versionLine = "当前版本 v${releaseInfo.versionName}",
                    releaseTypeLabel = releaseInfo.releaseTypeLabel,
                    prerelease = releaseInfo.prerelease,
                    publishedText = publishedText,
                    releaseTitle = null,
                    showPrereleaseNotice = false,
                )

                ReleaseNotesBodyCard(document = releaseNotesDocument)
            }
        },
        confirmButton = {
            SecondaryActionButton(
                text = "关闭",
                enabled = true,
                accentColor = LukoaColors.Primary,
                onClick = onDismiss,
            )
        },
        dismissButton = if (onOpenRelease != null && releaseInfo.releaseUrl.isNotBlank()) {
            {
                SecondaryActionButton(
                    text = "打开发布页",
                    enabled = true,
                    accentColor = LukoaColors.TextSecondary,
                    onClick = onOpenRelease,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun ReleaseNotesDialogHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(color = LukoaColors.Border)
    }
}

@Composable
private fun ReleaseNotesMetadataCard(
    versionLine: String,
    releaseTypeLabel: String,
    prerelease: Boolean,
    publishedText: String,
    releaseTitle: String?,
    showPrereleaseNotice: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("release-notes-meta-card"),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = versionLine,
                    modifier = Modifier.weight(1f),
                    color = LukoaColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (publishedText.isNotBlank() && releaseTitle.isNullOrBlank() && !showPrereleaseNotice) {
                    Text(
                        text = publishedText.take(10),
                        color = LukoaColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
                Surface(
                    color = if (prerelease) LukoaColors.AccentSoft else LukoaColors.PrimarySoft,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, LukoaColors.Border),
                ) {
                    Text(
                        text = releaseTypeLabel,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = if (prerelease) LukoaColors.Accent else LukoaColors.Primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (publishedText.isNotBlank() && (!releaseTitle.isNullOrBlank() || showPrereleaseNotice)) {
                Text(
                    text = "发布时间：$publishedText",
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!releaseTitle.isNullOrBlank()) {
                Text(
                    text = releaseTitle,
                    color = LukoaColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showPrereleaseNotice) {
                Text(
                    text = "测试版会更早提供新功能，稳定性可能低于正式版。",
                    color = LukoaColors.Accent,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ReleaseNotesBodyCard(
    document: GithubReleaseNotesDocument,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("release-notes-body-card"),
        color = LukoaColors.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LukoaColors.Border),
    ) {
        GithubReleaseNotesContent(
            document = document,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
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
