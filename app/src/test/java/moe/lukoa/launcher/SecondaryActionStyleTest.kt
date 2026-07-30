package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class SecondaryActionStyleTest {
    @Test
    fun primaryAccent_isRenderedAsANeutralSecondaryAction() {
        val style = resolveSecondaryActionStyle(
            enabled = true,
            accentColor = LukoaColors.Primary,
        )

        assertEquals(Color.Transparent, style.containerColor)
        assertEquals(LukoaColors.TextPrimary, style.contentColor)
        assertEquals(LukoaColors.Border, style.borderColor)
    }

    @Test
    fun neutralTone_doesNotConsumeThePrimaryColorRole() {
        assertEquals(LukoaColors.TextSecondary, ActionTone.Neutral.color())

        val style = resolveSecondaryActionStyle(
            enabled = true,
            accentColor = ActionTone.Neutral.color(),
        )

        assertEquals(Color.Transparent, style.containerColor)
        assertEquals(LukoaColors.TextSecondary, style.contentColor)
        assertEquals(LukoaColors.Border, style.borderColor)
    }

    @Test
    fun warningAndDanger_keepTheirSemanticColors() {
        val warning = resolveSecondaryActionStyle(
            enabled = true,
            accentColor = LukoaColors.Accent,
        )
        val danger = resolveSecondaryActionStyle(
            enabled = true,
            accentColor = LukoaColors.Danger,
        )

        assertEquals(LukoaColors.AccentSoft, warning.containerColor)
        assertEquals(LukoaColors.Accent, warning.contentColor)
        assertEquals(LukoaColors.Accent, warning.borderColor)
        assertEquals(LukoaColors.DangerSoft, danger.containerColor)
        assertEquals(LukoaColors.Danger, danger.contentColor)
        assertEquals(LukoaColors.Danger, danger.borderColor)
    }

    @Test
    fun disabledAction_usesOneQuietStyleRegardlessOfRequestedAccent() {
        listOf(
            LukoaColors.Primary,
            LukoaColors.Accent,
            LukoaColors.Danger,
        ).forEach { requestedAccent ->
            val style = resolveSecondaryActionStyle(
                enabled = false,
                accentColor = requestedAccent,
            )

            assertEquals(Color.Transparent, style.containerColor)
            assertEquals(LukoaColors.Dim, style.contentColor)
            assertEquals(LukoaColors.Border, style.borderColor)
        }
    }
}
