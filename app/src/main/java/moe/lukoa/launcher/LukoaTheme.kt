package moe.lukoa.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LukoaDarkColorScheme = darkColorScheme(
    primary = LukoaColors.Primary,
    onPrimary = LukoaColors.OnPrimary,
    primaryContainer = LukoaColors.PrimarySoft,
    onPrimaryContainer = LukoaColors.TextPrimary,
    inversePrimary = LukoaColors.PrimaryHover,
    secondary = LukoaColors.PrimaryHover,
    onSecondary = LukoaColors.OnPrimary,
    secondaryContainer = LukoaColors.PrimarySoft,
    onSecondaryContainer = LukoaColors.TextPrimary,
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
    inverseSurface = LukoaColors.TextPrimary,
    inverseOnSurface = LukoaColors.Background,
    surfaceBright = LukoaColors.Elevated,
    surfaceDim = LukoaColors.Background,
    surfaceContainer = LukoaColors.Surface,
    surfaceContainerHigh = LukoaColors.Elevated,
    surfaceContainerHighest = LukoaColors.Elevated,
    surfaceContainerLow = LukoaColors.Surface,
    surfaceContainerLowest = LukoaColors.Background,
    outline = LukoaColors.Border,
    outlineVariant = LukoaColors.Border,
    error = LukoaColors.Danger,
    onError = LukoaColors.Background,
    errorContainer = LukoaColors.DangerSoft,
    onErrorContainer = LukoaColors.TextPrimary,
    scrim = Color.Black,
)

@Composable
fun LukoaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LukoaDarkColorScheme,
        content = {
            Surface(color = LukoaColors.Background) {
                content()
            }
        },
    )
}
