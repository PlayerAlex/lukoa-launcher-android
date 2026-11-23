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
    val isLight: Boolean,
) {
    Monochrome(
        wireValue = "monochrome",
        label = "夜间深色",
        summary = "纯正中性炭黑搭配柔和纯白，夜间使用护眼耐看。",
        isLight = false,
    ),
    Daylight(
        wireValue = "daylight",
        label = "日间浅色",
        summary = "清爽中性白底搭配深炭黑文字，白天阅读清晰通透。",
        isLight = true,
    ),
    ;

    fun toggled(): LukoaThemeStyle = when (this) {
        Monochrome -> Daylight
        Daylight -> Monochrome
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
     * 纯正中性深色系：R=G=B 零偏色炭黑层级 + 柔和哑白主色，
     * 仅在删除/停止/报错处保留低饱和柔红。
     */
    val Monochrome = LukoaColorPalette(
        background = Color(0xFF0D0D0D),
        surface = Color(0xFF161616),
        elevated = Color(0xFF202020),
        border = Color(0xFF323232),
        textPrimary = Color(0xFFF2F2F2),
        textSecondary = Color(0xFFA6A6A6),
        dim = Color(0xFF707070),
        primary = Color(0xFFEDEDED),
        primaryHover = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF111111),
        primarySoft = Color(0xFF292929),
        accent = Color(0xFFDCDCDC),
        accentSoft = Color(0xFF2C2C2C),
        danger = Color(0xFFE56B6F),
        dangerSoft = Color(0xFF31191A),
        stop = Color(0xFFEB7A7E),
        terminal = Color(0xFF080808),
    )

    /**
     * 纯正中性日间浅色系：R=G=B 柔白纸感层级 + 深炭黑主色，
     * 白天通透不刺眼，高对比度清晰易读。
     */
    val Daylight = LukoaColorPalette(
        background = Color(0xFFF2F2F2),
        surface = Color(0xFFFFFFFF),
        elevated = Color(0xFFF8F8F8),
        border = Color(0xFFDDDDDD),
        textPrimary = Color(0xFF141414),
        textSecondary = Color(0xFF5C5C5C),
        dim = Color(0xFF8A8A8A),
        primary = Color(0xFF1A1A1A),
        primaryHover = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primarySoft = Color(0xFFE6E6E6),
        accent = Color(0xFF2E2E2E),
        accentSoft = Color(0xFFE8E8E8),
        danger = Color(0xFFBC2C34),
        dangerSoft = Color(0xFFFDECEE),
        stop = Color(0xFFC2323A),
        terminal = Color(0xFFFAFAFA),
    )

    fun forStyle(style: LukoaThemeStyle): LukoaColorPalette = when (style) {
        LukoaThemeStyle.Monochrome -> Monochrome
        LukoaThemeStyle.Daylight -> Daylight
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
