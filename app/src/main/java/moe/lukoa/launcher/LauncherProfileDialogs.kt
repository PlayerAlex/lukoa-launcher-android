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
fun DeleteTavernProfileConfirmDialog(
    confirmation: TavernProfileRemovalConfirmation,
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = "确认删除实例",
        titleTone = ActionTone.Warning,
        confirmText = "确认删除实例",
        confirmTone = ActionTone.Warning,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = if (confirmation.deletesProfileDirectory) {
                "会把这个分身实例从启动器配置里移除，并删除它当前的托管酒馆目录，然后切换到另一个实例继续管理。"
            } else {
                "会把这个实例从启动器配置里移除，并切换到另一个实例继续管理。"
            },
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionInfoLine("实例名称", confirmation.profileName)
                VersionInfoLine("实例目录", confirmation.profilePath)
                VersionInfoLine("实例端口", confirmation.profilePort.toString())
                VersionInfoLine("删除后切换到", confirmation.nextProfileName)
                VersionInfoLine("删除后剩余", "${confirmation.remainingProfileCount} 个实例")
                if (confirmation.deletesProfileDirectory && confirmation.deletedDirectoryPath.isNotBlank()) {
                    VersionInfoLine("将删除目录", confirmation.deletedDirectoryPath)
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Amber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
        ) {
            Text(
                text = if (confirmation.deletesProfileDirectory) {
                    "这一步会删除实例配置，并删除这个托管目录里的酒馆文件。启动器备份库不会删，但这个目录下未备份的内容会一起消失。"
                } else {
                    "这一步只会移除启动器里的实例配置，不会删除这个目录里的酒馆文件，也不会删除备份。"
                },
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = if (confirmation.deletesProfileDirectory) {
                "如果以后还想重新管理这个位置，可以再新建一个分身实例；但这次删掉的目录内容不能自动恢复，建议先备份。"
            } else {
                "如果以后还想重新管理这个目录，可以再新建一个分身实例并把路径改回来。"
            },
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun TavernProfileMigrationConfirmDialog(
    confirmation: TavernProfileMigrationConfirmation,
    actionsLocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    RiskyActionDialogScaffold(
        title = "确认迁移酒馆目录",
        titleTone = ActionTone.Warning,
        confirmText = "确认迁移目录",
        confirmTone = ActionTone.Warning,
        confirmEnabled = !actionsLocked,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Text(
            text = "这一步会真的搬动当前实例的酒馆目录，不只是改启动器里的路径配置。迁移过程中不要再重复点启动、停止或删除实例。",
            color = LukoaColors.Text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.SurfaceAlt,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VersionInfoLine("实例名称", confirmation.profileName)
                VersionInfoLine("当前目录", confirmation.sourcePath)
                VersionInfoLine("目标目录", confirmation.targetPath)
                VersionInfoLine("目标类型", confirmation.targetKindLabel)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LukoaColors.Amber.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LukoaColors.Amber.copy(alpha = 0.28f)),
        ) {
            Text(
                text = confirmation.riskNote,
                modifier = Modifier.padding(12.dp),
                color = LukoaColors.Text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "如果你还没做过备份，建议先去备份页手动备份一次，再回来迁移。",
            color = LukoaColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun CustomTavernPathMigrationDialog(
    currentPath: String,
    pathInput: String,
    pathError: String?,
    actionsLocked: Boolean,
    onPathChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Amber,
        textContentColor = LukoaColors.Text,
        title = {
            Text("迁移到自定义地址")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "这里填写的不是启动器推荐默认目录。迁移过去后，删除实例时不会自动帮你删这个目录，后续路径识别和风险判断也会更依赖你自己确认。",
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "当前目录：$currentPath",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = onPathChange,
                    enabled = !actionsLocked,
                    singleLine = true,
                    label = { Text("目标目录") },
                    placeholder = { Text("~/my-sillytavern") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LukoaColors.Text,
                        unfocusedTextColor = LukoaColors.Text,
                        disabledTextColor = LukoaColors.Dim,
                        focusedContainerColor = LukoaColors.SurfaceAlt,
                        unfocusedContainerColor = LukoaColors.SurfaceAlt,
                        disabledContainerColor = LukoaColors.Surface,
                        focusedBorderColor = LukoaColors.Amber,
                        unfocusedBorderColor = LukoaColors.Line,
                        disabledBorderColor = LukoaColors.Line,
                        focusedLabelColor = LukoaColors.Amber,
                        unfocusedLabelColor = LukoaColors.Muted,
                        cursorColor = LukoaColors.Amber,
                    ),
                )
                Text(
                    text = "如果目标目录里已经有旧文件，启动器会先把旧目录挪到安全备份名，再继续迁移。但这不是默认酒馆位置，后续问题需要你自己承担。",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                pathError?.let { error ->
                    Text(
                        text = error,
                        color = LukoaColors.Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = "继续确认",
                enabled = !actionsLocked && pathError == null,
                tone = ActionTone.Warning,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
fun TavernDirectoryChoiceDialog(
    currentPath: String,
    candidates: List<TavernDirectoryCandidateOption>,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LukoaColors.Surface,
        titleContentColor = LukoaColors.Accent,
        textContentColor = LukoaColors.Text,
        title = {
            Text("选一个酒馆目录")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val selectableCount = candidates.count { it.selectable }
                Text(
                    text = if (selectableCount > 0) {
                        "检测到多个像酒馆的目录。点一个可用目录，启动器会把当前实例切过去。"
                    } else {
                        "检测到的目录里，没有可直接分配给当前实例的候选项。请先看下面的占用提示。"
                    },
                    color = LukoaColors.Text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "当前实例路径：${TavernPathNormalizer.toDisplayPath(currentPath)}",
                    color = LukoaColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Surface(
                    color = LukoaColors.SurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LukoaColors.Line.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        candidates.forEach { candidate ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { onChoose(candidate.path) },
                                    enabled = candidate.selectable,
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(
                                        1.dp,
                                        if (candidate.selectable) {
                                            LukoaColors.Accent.copy(alpha = 0.46f)
                                        } else {
                                            LukoaColors.Line.copy(alpha = 0.5f)
                                        },
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (candidate.selectable) {
                                            LukoaColors.Accent.copy(alpha = 0.08f)
                                        } else {
                                            LukoaColors.Surface.copy(alpha = 0.5f)
                                        },
                                        contentColor = if (candidate.selectable) {
                                            LukoaColors.Accent
                                        } else {
                                            LukoaColors.Muted
                                        },
                                        disabledContentColor = LukoaColors.Muted,
                                        disabledContainerColor = LukoaColors.Surface.copy(alpha = 0.5f),
                                    ),
                                ) {
                                    Text(
                                        text = candidate.displayPath,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (candidate.selectable) LukoaColors.Text else LukoaColors.Muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!candidate.selectable && candidate.reason.isNotBlank()) {
                                    Text(
                                        text = candidate.reason,
                                        color = LukoaColors.Amber,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                if (selectableCount == 0) {
                    Text(
                        text = "如果你就是想给当前实例单独用一套环境，请先准备一个新的酒馆目录，再回到设置里手动改路径。",
                        color = LukoaColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            DialogActionButton(
                text = "取消",
                tone = ActionTone.Neutral,
                onClick = onDismiss,
            )
        },
    )
}
