package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GithubReleaseNotesContent(
    document: GithubReleaseNotesDocument,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = document.versionTitle,
            modifier = Modifier.semantics { heading() },
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                lineHeight = 21.sp,
            ),
            fontWeight = FontWeight.Bold,
        )
        document.sections.forEachIndexed { sectionIndex, section ->
            if (sectionIndex > 0) {
                HorizontalDivider(color = LukoaColors.Border)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "${section.title}：",
                    modifier = Modifier.semantics { heading() },
                    color = LukoaColors.Primary,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
                section.items.forEachIndexed { index, item ->
                    val prefix = "${index + 1}. "
                    val leadSeparatorIndex = item.indexOf('：').takeIf { it in 2..24 }
                    val formatted = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = LukoaColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ) {
                            append(prefix)
                        }
                        if (leadSeparatorIndex != null) {
                            withStyle(
                                SpanStyle(
                                    color = LukoaColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            ) {
                                append(item.substring(0, leadSeparatorIndex + 1))
                            }
                            withStyle(SpanStyle(color = LukoaColors.TextSecondary)) {
                                append(item.substring(leadSeparatorIndex + 1))
                            }
                        } else {
                            withStyle(SpanStyle(color = LukoaColors.TextPrimary)) {
                                append(item)
                            }
                        }
                    }
                    Text(
                        text = formatted,
                        modifier = Modifier.padding(start = 2.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        ),
                    )
                }
            }
        }
    }
}
