package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color

object LukoaColors {
    // 新版暗色基底，与确认后的 UI 设计保持一致。
    val Background = Color(0xFF0E1116)
    val Surface = Color(0xFF181D24)
    val SurfaceAlt = Color(0xFF20252E)
    val DialogSurface = Color(0xFF1D232C)
    val Line = Color(0xFF313946)
    val Text = Color(0xFFF0F3F6)
    val Muted = Color(0xFFA3ADBA)
    val Dim = Color(0xFF768292)

    // 核心高亮：露科亚发梢的青绿色 (Teal/Cyan)，非常适合暗色模式的科幻感
    val Accent = Color(0xFF2DD4BF)
    val AccentSoft = Color(0xFF0E332E)
    val AccentDark = Color(0xFF0A2E29)

    // 兼容旧组件的提示色，统一到新版青绿色视觉。
    val Info = Color(0xFF2DD4BF)
    val InfoSoft = Color(0xFF0E332E)

    // 警告点缀：金发与瞳孔的琥珀金 (Gold)
    val Amber = Color(0xFFFBBF24)
    val AmberSoft = Color(0xFF3B2D0C)

    // 危险操作：警示红偏粉
    val Danger = Color(0xFFFB7185)
    val DangerSoft = Color(0xFF3E1B22)

    // 终端黑洞
    val Terminal = Color(0xFF06080B)
}
