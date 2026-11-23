package moe.lukoa.launcher

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal fun lukoaDarkColorScheme(
    palette: LukoaColorPalette = LukoaPalettes.forStyle(LukoaColors.currentStyle),
): ColorScheme = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primarySoft,
    onPrimaryContainer = palette.textPrimary,
    inversePrimary = palette.primaryHover,
    secondary = palette.primaryHover,
    onSecondary = palette.onPrimary,
    secondaryContainer = palette.primarySoft,
    onSecondaryContainer = palette.textPrimary,
    tertiary = palette.accent,
    onTertiary = palette.background,
    tertiaryContainer = palette.accentSoft,
    onTertiaryContainer = palette.textPrimary,
    background = palette.background,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.elevated,
    onSurfaceVariant = palette.textSecondary,
    surfaceTint = palette.primary,
    inverseSurface = palette.textPrimary,
    inverseOnSurface = palette.background,
    surfaceBright = palette.elevated,
    surfaceDim = palette.background,
    surfaceContainer = palette.surface,
    surfaceContainerHigh = palette.elevated,
    surfaceContainerHighest = palette.elevated,
    surfaceContainerLow = palette.surface,
    surfaceContainerLowest = palette.background,
    outline = palette.border,
    outlineVariant = palette.border,
    error = palette.danger,
    onError = palette.background,
    errorContainer = palette.dangerSoft,
    onErrorContainer = palette.textPrimary,
    scrim = Color.Black,
)

internal val LukoaDarkColorScheme: ColorScheme
    get() = lukoaDarkColorScheme()

@Composable
fun LukoaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lukoaDarkColorScheme(),
        content = {
            Surface(color = LukoaColors.Background) {
                content()
            }
        },
    )
}
