package moe.lukoa.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LukoaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LukoaColors.Primary,
            onPrimary = LukoaColors.OnPrimary,
            primaryContainer = LukoaColors.PrimarySoft,
            onPrimaryContainer = LukoaColors.TextPrimary,
            secondary = LukoaColors.PrimaryHover,
            onSecondary = LukoaColors.OnPrimary,
            tertiary = LukoaColors.Accent,
            onTertiary = LukoaColors.Background,
            tertiaryContainer = LukoaColors.AccentSoft,
            onTertiaryContainer = LukoaColors.TextPrimary,
            background = LukoaColors.Background,
            onBackground = LukoaColors.TextPrimary,
            surface = LukoaColors.Surface,
            onSurface = LukoaColors.TextPrimary,
            surfaceVariant = LukoaColors.Elevated,
            onSurfaceVariant = LukoaColors.TextSecondary,
            surfaceTint = LukoaColors.Primary,
            outline = LukoaColors.Border,
            outlineVariant = LukoaColors.Border,
            error = LukoaColors.Danger,
            onError = LukoaColors.Background,
            errorContainer = LukoaColors.DangerSoft,
            onErrorContainer = LukoaColors.TextPrimary,
        ),
        content = {
            Surface(color = LukoaColors.Background) {
                content()
            }
        },
    )
}
