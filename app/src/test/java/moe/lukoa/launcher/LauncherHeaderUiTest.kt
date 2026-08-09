package moe.lukoa.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LauncherHeaderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `launcher title stays on one complete line at phone content width`() {
        composeRule.setContent {
            LukoaTheme {
                Box(modifier = Modifier.width(328.dp)) {
                    Header(
                        tavernRunning = false,
                        tavernStarting = false,
                        showVersionUpdateBadge = false,
                        onVersionClick = {},
                    )
                }
            }
        }

        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("露科亚启动器")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        val result = results.single()

        assertEquals(1, result.lineCount)
    }
}
