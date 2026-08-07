package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color

object LukoaColors {
    // 页面层级使用冷青黑，避免灰黄绿造成陈旧感。
    val Background = Color(0xFF071311)
    val Surface = Color(0xFF0D1D1A)
    val Elevated = Color(0xFF142824)
    val Border = Color(0xFF21423B)

    // 柔和冷白控制暗色界面的眩光，辅助文字继续降低一级亮度。
    val TextPrimary = Color(0xFFCADAD5)
    val TextSecondary = Color(0xFF8FA9A2)
    val Dim = Color(0xFF596D67)

    // 中等明度的清透薄荷用于主操作，避免大面积按钮产生荧光感。
    val Primary = Color(0xFF4EB89F)
    val PrimaryHover = Color(0xFF66CBB1)
    val OnPrimary = Background
    val PrimarySoft = Color(0xFF10382F)

    // 明亮柑橘黄与冷薄荷形成小范围冷暖对比，不使用做旧金色。
    val Accent = Color(0xFFFFC857)
    val AccentSoft = Color(0xFF3A2A0B)

    // 会删除、覆盖或强制修改数据的操作使用明确的红色。
    val Danger = Color(0xFFFF5D68)
    val DangerSoft = Color(0xFF3B1117)

    // 停止酒馆使用较柔和的清晰红色，与数据危险操作分开。
    val Stop = Color(0xFFFF7A8B)

    // 终端区域继续比页面背景更深，但保持同一青绿色相。
    val Terminal = Color(0xFF040B09)
}
