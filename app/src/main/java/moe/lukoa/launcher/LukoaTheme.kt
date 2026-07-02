package moe.lukoa.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun LukoaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LukoaColors.Accent,
            onPrimary = LukoaColors.Background,
            secondary = LukoaColors.Amber,
            onSecondary = LukoaColors.Background,
            background = LukoaColors.Background,
            onBackground = LukoaColors.Text,
            surface = LukoaColors.Surface,
            onSurface = LukoaColors.Text,
            error = LukoaColors.Danger,
        ),
        content = {
            Surface(color = LukoaColors.Background) {
                content()
            }
        },
    )
}
