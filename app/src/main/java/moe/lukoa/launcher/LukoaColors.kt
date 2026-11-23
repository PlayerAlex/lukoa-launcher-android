package moe.lukoa.launcher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class LukoaThemeStyle(
    val wireValue: String,
    val label: String,
    val summary: String,
) {
    Monochrome(
        wireValue = "monochrome",
        label = "极简黑白",
        summary = "温润炭黑底色搭配柔和暖白主色，舒适耐看、无杂色干扰。",
    ),
    ClassicTeal(
        wireValue = "classic_teal",
        label = "原版冷青",
        summary = "原有的深青黑底色搭配清透薄荷绿主题。",
    ),
    ;

    fun toggled(): LukoaThemeStyle = when (this) {
        Monochrome -> ClassicTeal
        ClassicTeal -> Monochrome
    }

    companion object {
        fun fromWireValue(value: String?): LukoaThemeStyle =
            entries.firstOrNull { it.wireValue == value?.trim()?.lowercase() } ?: Monochrome
    }
}

data class LukoaColorPalette(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val dim: Color,
    val primary: Color,
    val primaryHover: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val stop: Color,
    val terminal: Color,
)

object LukoaPalettes {
    /**
     * 暖调极简黑白暗色系：温润深炭黑层级 + 柔和暖纸白主操作色，
     * 告别冰冷蓝灰、克制杂色，仅在真正需要警示的删除/停止操作使用低饱和暖珊瑚红。
     */
    val Monochrome = LukoaColorPalette(
        background = Color(0xFF0E0D0C),
        surface = Color(0xFF171614),
        elevated = Color(0xFF211F1C),
        border = Color(0xFF34312C),
        textPrimary = Color(0xFFEEEBE5),
        textSecondary = Color(0xFFABA69F),
        dim = Color(0xFF777169),
        primary = Color(0xFFEBE7DF),
        primaryHover = Color(0xFFFAF7F2),
        onPrimary = Color(0xFF12110F),
        primarySoft = Color(0xFF2B2824),
        accent = Color(0xFFDBD7D0),
        accentSoft = Color(0xFF2F2B25),
        danger = Color(0xFFE67873),
        dangerSoft = Color(0xFF331B1A),
        stop = Color(0xFFEB847E),
        terminal = Color(0xFF0A0908),
    )

    /** 原版深冷青黑 + 清透薄荷绿主题。 */
    val ClassicTeal = LukoaColorPalette(
        background = Color(0xFF071311),
        surface = Color(0xFF0D1D1A),
        elevated = Color(0xFF142824),
        border = Color(0xFF21423B),
        textPrimary = Color(0xFFCADAD5),
        textSecondary = Color(0xFF8FA9A2),
        dim = Color(0xFF596D67),
        primary = Color(0xFF4EB89F),
        primaryHover = Color(0xFF66CBB1),
        onPrimary = Color(0xFF071311),
        primarySoft = Color(0xFF10382F),
        accent = Color(0xFFFFC857),
        accentSoft = Color(0xFF3A2A0B),
        danger = Color(0xFFFF5D68),
        dangerSoft = Color(0xFF3B1117),
        stop = Color(0xFFFF7A8B),
        terminal = Color(0xFF040B09),
    )

    fun forStyle(style: LukoaThemeStyle): LukoaColorPalette = when (style) {
        LukoaThemeStyle.Monochrome -> Monochrome
        LukoaThemeStyle.ClassicTeal -> ClassicTeal
    }
}

object LukoaThemeStore {
    private const val PREFS = "lukoa_theme_prefs"
    private const val KEY_STYLE = "theme_style"

    fun load(context: Context): LukoaThemeStyle {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STYLE, null)
        return LukoaThemeStyle.fromWireValue(raw)
    }

    fun save(context: Context, style: LukoaThemeStyle): LukoaThemeStyle {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STYLE, style.wireValue)
            .apply()
        LukoaColors.applyStyle(style)
        return style
    }
}

object LukoaColors {
    var currentStyle by mutableStateOf(LukoaThemeStyle.Monochrome)
        private set

    fun applyStyle(style: LukoaThemeStyle) {
        currentStyle = style
    }

    private val palette: LukoaColorPalette
        get() = LukoaPalettes.forStyle(currentStyle)

    val Background: Color get() = palette.background
    val Surface: Color get() = palette.surface
    val Elevated: Color get() = palette.elevated
    val Border: Color get() = palette.border

    val TextPrimary: Color get() = palette.textPrimary
    val TextSecondary: Color get() = palette.textSecondary
    val Dim: Color get() = palette.dim

    val Primary: Color get() = palette.primary
    val PrimaryHover: Color get() = palette.primaryHover
    val OnPrimary: Color get() = palette.onPrimary
    val PrimarySoft: Color get() = palette.primarySoft

    val Accent: Color get() = palette.accent
    val AccentSoft: Color get() = palette.accentSoft

    val Danger: Color get() = palette.danger
    val DangerSoft: Color get() = palette.dangerSoft

    val Stop: Color get() = palette.stop

    val Terminal: Color get() = palette.terminal
}
