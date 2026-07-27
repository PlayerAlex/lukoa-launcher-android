package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color

object LukoaColors {
    // 深色基底：取自黑背心与暗黑科技风的融合，带有极微弱的蓝调
    val Background = Color(0xFF0F1115)
    val Surface = Color(0xFF161920)
    val SurfaceAlt = Color(0xFF20252E)
    val Line = Color(0xFF313946)
    val Text = Color(0xFFF8FAFC)
    val Muted = Color(0xFFA3B1C6)
    val Dim = Color(0xFF6E7A8F)

    // 正常状态与可执行操作统一使用绿色。
    val Accent = Color(0xFF22C55E)
    val AccentSoft = Color(0xFF12351F)

    // 普通信息沿用正常绿色，不再引入额外的粉色语义。
    val Info = Accent
    val InfoSoft = AccentSoft

    // 警告点缀：金发与瞳孔的琥珀金 (Gold)
    val Amber = Color(0xFFFBBF24)
    val AmberSoft = Color(0xFF3B2D0C)

    // 会删除、覆盖或强制修改数据的操作使用明确的红色。
    val Danger = Color(0xFFEF4444)
    val DangerSoft = Color(0xFF3B1518)

    // 终端黑洞
    val Terminal = Color(0xFF080A0C)
}
