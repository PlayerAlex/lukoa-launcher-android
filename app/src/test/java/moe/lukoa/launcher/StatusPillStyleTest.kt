package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusPillStyleTest {
    @Test
    fun translucentToneBackground_keepsItsLowContrastTint() {
        val requested = Color(0xFF2DD4BF).copy(alpha = 0.16f)

        val resolved = resolvedStatusPillBackground(
            active = true,
            requestedBackground = requested,
        )

        assertEquals(requested.alpha * 0.82f, resolved.alpha, 0.005f)
    }

    @Test
    fun opaqueSoftBackground_remainsClearlyVisible() {
        val resolved = resolvedStatusPillBackground(
            active = true,
            requestedBackground = LukoaColors.PrimarySoft,
        )

        assertEquals(0.82f, resolved.alpha, 0.001f)
    }
}
