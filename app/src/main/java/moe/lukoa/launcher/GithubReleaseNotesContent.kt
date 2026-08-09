package moe.lukoa.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun GithubReleaseNotesContent(
    document: GithubReleaseNotesDocument,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = document.versionTitle,
            modifier = Modifier.semantics { heading() },
            color = LukoaColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        document.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${section.title}：",
                    modifier = Modifier.semantics { heading() },
                    color = LukoaColors.Primary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                section.items.forEachIndexed { index, item ->
                    Text(
                        text = "${index + 1}. $item",
                        modifier = Modifier.padding(start = 2.dp),
                        color = LukoaColors.TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
