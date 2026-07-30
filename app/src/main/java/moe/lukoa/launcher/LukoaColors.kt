package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color

object LukoaColors {
    // 页面层级。
    val Background = Color(0xFF0D1412)
    val Surface = Color(0xFF151D1A)
    val Elevated = Color(0xFF1B2622)
    val Border = Color(0xFF25332E)

    // 文字层级。
    val TextPrimary = Color(0xFFE3EDE8)
    val TextSecondary = Color(0xFF8DA39A)
    val Dim = Color(0xFF5E7068)

    // 正常状态、主操作与交互反馈。
    val Primary = Color(0xFF6DB5A4)
    val PrimaryHover = Color(0xFF8AC9B8)
    val OnPrimary = Background
    val PrimarySoft = Color(0xFF19332D)

    // 提醒与小范围强调使用暖金色，不与危险操作混用。
    val Accent = Color(0xFFE8B86D)
    val AccentSoft = Color(0xFF362C1D)

    // 会删除、覆盖或强制修改数据的操作使用明确的红色。
    val Danger = Color(0xFFEF4444)
    val DangerSoft = Color(0xFF3B1518)

    // 停止酒馆沿用旧版更柔和的红色，与数据危险操作分开。
    val Stop = Color(0xFFFB7185)

    // 终端区域继续比页面背景更深，但保持同一青绿色相。
    val Terminal = Color(0xFF080D0B)
}
