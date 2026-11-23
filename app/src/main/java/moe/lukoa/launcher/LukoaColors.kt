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
        summary = "柔和中性深灰搭配雾白文字，告别刺眼纯黑，夜间护眼耐看。",
        isLight = false,
    ),
    Daylight(
        wireValue = "daylight",
        label = "日间浅色",
        summary = "柔和护眼浅灰纸感底色，不刺眼、通透耐看。",
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
     * 柔和中性深灰暗色系：R=G=B 零偏色哑光深灰层级 + 柔雾白主色，
     * 避免纯黑高反差刺眼，也不偏黄或偏冷蓝；仅在删除/停止/报错处保留低饱和柔红。
     */
    val Monochrome = LukoaColorPalette(
        background = Color(0xFF1C1C1C),
        surface = Color(0xFF242424),
        elevated = Color(0xFF2C2C2C),
        border = Color(0xFF3A3A3A),
        textPrimary = Color(0xFFDFDFDF),
        textSecondary = Color(0xFFA3A3A3),
        dim = Color(0xFF757575),
        primary = Color(0xFFDCDCDC),
        primaryHover = Color(0xFFEAEAEA),
        onPrimary = Color(0xFF1A1A1A),
        primarySoft = Color(0xFF353535),
        accent = Color(0xFFCFCFCF),
        accentSoft = Color(0xFF383838),
        danger = Color(0xFFC97A7D),
        dangerSoft = Color(0xFF332627),
        stop = Color(0xFFC47A7D),
        terminal = Color(0xFF161616),
    )

    /**
     * 柔和中性日间浅灰系：用低眩光雾灰纸感替代刺眼纯白，
     * R=G=B 零偏色，搭配克制柔红警示色，白天长时间观看依然舒适。
     */
    val Daylight = LukoaColorPalette(
        background = Color(0xFFE6E6E6),
        surface = Color(0xFFEEEEEE),
        elevated = Color(0xFFF3F3F3),
        border = Color(0xFFD2D2D2),
        textPrimary = Color(0xFF262626),
        textSecondary = Color(0xFF5A5A5A),
        dim = Color(0xFF828282),
        primary = Color(0xFF333333),
        primaryHover = Color(0xFF242424),
        onPrimary = Color(0xFFEFEFEF),
        primarySoft = Color(0xFFDCDCDC),
        accent = Color(0xFF3D3D3D),
        accentSoft = Color(0xFFDFDFDF),
        danger = Color(0xFF9E4448),
        dangerSoft = Color(0xFFEDE2E2),
        stop = Color(0xFF9E484C),
        terminal = Color(0xFFEAEAEA),
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
