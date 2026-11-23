package moe.lukoa.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Clean 24dp Lucide-style monochrome stroke icons with uniform 1.85dp stroke weight
 * so bottom navigation and hub cards share one crisp visual language.
 */
internal object LukoaIcons {
    private val strokeBrush = SolidColor(Color.White)

    private fun strokeIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    private fun ImageVector.Builder.strokePath(block: PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = strokeBrush,
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    val Book: ImageVector by lazy {
        strokeIcon("LukoaBook") {
            strokePath {
                moveTo(4f, 5.5f)
                curveTo(4f, 4.4f, 4.9f, 3.5f, 6f, 3.5f)
                horizontalLineTo(19f)
                verticalLineTo(19.5f)
                horizontalLineTo(6f)
                curveTo(4.9f, 19.5f, 4f, 20.4f, 4f, 21.5f)
                verticalLineTo(5.5f)
            }
            strokePath {
                moveTo(4f, 18.5f)
                curveTo(4f, 17.4f, 4.9f, 16.5f, 6f, 16.5f)
                horizontalLineTo(19f)
            }
            strokePath {
                moveTo(8.5f, 8f)
                horizontalLineTo(15f)
                moveTo(8.5f, 11.5f)
                horizontalLineTo(13.5f)
            }
        }
    }

    val Grid: ImageVector by lazy {
        strokeIcon("LukoaGrid") {
            strokePath {
                moveTo(4f, 5.5f); curveTo(4f, 4.67f, 4.67f, 4f, 5.5f, 4f)
                horizontalLineTo(9.5f); curveTo(10.33f, 4f, 11f, 4.67f, 11f, 5.5f)
                verticalLineTo(9.5f); curveTo(11f, 10.33f, 10.33f, 11f, 9.5f, 11f)
                horizontalLineTo(5.5f); curveTo(4.67f, 11f, 4f, 10.33f, 4f, 9.5f); close()
            }
            strokePath {
                moveTo(13f, 5.5f); curveTo(13f, 4.67f, 13.67f, 4f, 14.5f, 4f)
                horizontalLineTo(18.5f); curveTo(19.33f, 4f, 20f, 4.67f, 20f, 5.5f)
                verticalLineTo(9.5f); curveTo(20f, 10.33f, 19.33f, 11f, 18.5f, 11f)
                horizontalLineTo(14.5f); curveTo(13.67f, 11f, 13f, 10.33f, 13f, 9.5f); close()
            }
            strokePath {
                moveTo(4f, 14.5f); curveTo(4f, 13.67f, 4.67f, 13f, 5.5f, 13f)
                horizontalLineTo(9.5f); curveTo(10.33f, 13f, 11f, 13.67f, 11f, 14.5f)
                verticalLineTo(18.5f); curveTo(11f, 19.33f, 10.33f, 20f, 9.5f, 20f)
                horizontalLineTo(5.5f); curveTo(4.67f, 20f, 4f, 19.33f, 4f, 18.5f); close()
            }
            strokePath {
                moveTo(13f, 14.5f); curveTo(13f, 13.67f, 13.67f, 13f, 14.5f, 13f)
                horizontalLineTo(18.5f); curveTo(19.33f, 13f, 20f, 13.67f, 20f, 14.5f)
                verticalLineTo(18.5f); curveTo(20f, 19.33f, 19.33f, 20f, 18.5f, 20f)
                horizontalLineTo(14.5f); curveTo(13.67f, 20f, 13f, 19.33f, 13f, 18.5f); close()
            }
        }
    }

    val Power: ImageVector by lazy {
        strokeIcon("LukoaPower") {
            strokePath {
                moveTo(12f, 3.5f)
                verticalLineTo(11.5f)
            }
            strokePath {
                moveTo(16.95f, 6.05f)
                arcTo(7.5f, 7.5f, 0f, true, true, 7.05f, 6.05f)
            }
        }
    }

    val Wrench: ImageVector by lazy {
        strokeIcon("LukoaWrench") {
            strokePath {
                moveTo(14.7f, 6.3f)
                arcTo(1f, 1f, 0f, false, false, 14.7f, 7.7f)
                lineTo(16.3f, 9.3f)
                arcTo(1f, 1f, 0f, false, false, 17.7f, 9.3f)
                lineTo(20.1f, 6.9f)
                arcTo(5.2f, 5.2f, 0f, false, true, 12.9f, 13.8f)
                lineTo(6.8f, 19.9f)
                arcTo(2.12f, 2.12f, 0f, false, true, 3.8f, 16.9f)
                lineTo(9.9f, 10.8f)
                arcTo(5.2f, 5.2f, 0f, false, true, 16.8f, 3.6f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("LukoaSettings") {
            strokePath {
                moveTo(12f, 8.8f)
                arcTo(3.2f, 3.2f, 0f, true, false, 12f, 15.2f)
                arcTo(3.2f, 3.2f, 0f, true, false, 12f, 8.8f)
                close()
            }
            strokePath {
                moveTo(12f, 3f); verticalLineTo(5.3f)
                moveTo(12f, 18.7f); verticalLineTo(21f)
                moveTo(4.93f, 4.93f); lineTo(6.55f, 6.55f)
                moveTo(17.45f, 17.45f); lineTo(19.07f, 19.07f)
                moveTo(3f, 12f); horizontalLineTo(5.3f)
                moveTo(18.7f, 12f); horizontalLineTo(21f)
                moveTo(4.93f, 19.07f); lineTo(6.55f, 17.45f)
                moveTo(17.45f, 6.55f); lineTo(19.07f, 4.93f)
            }
        }
    }

    val Archive: ImageVector by lazy {
        strokeIcon("LukoaArchive") {
            strokePath {
                moveTo(3.5f, 5f); horizontalLineTo(20.5f); verticalLineTo(9f); horizontalLineTo(3.5f); close()
            }
            strokePath {
                moveTo(5f, 9f); verticalLineTo(18.5f)
                curveTo(5f, 19.33f, 5.67f, 20f, 6.5f, 20f)
                horizontalLineTo(17.5f)
                curveTo(18.33f, 20f, 19f, 19.33f, 19f, 18.5f)
                verticalLineTo(9f)
            }
            strokePath {
                moveTo(9.5f, 13.5f); horizontalLineTo(14.5f)
            }
        }
    }

    val Branch: ImageVector by lazy {
        strokeIcon("LukoaBranch") {
            strokePath {
                moveTo(7f, 4f); arcTo(2f, 2f, 0f, true, false, 7f, 8f); arcTo(2f, 2f, 0f, true, false, 7f, 4f)
                moveTo(7f, 16f); arcTo(2f, 2f, 0f, true, false, 7f, 20f); arcTo(2f, 2f, 0f, true, false, 7f, 16f)
                moveTo(17f, 7f); arcTo(2f, 2f, 0f, true, false, 17f, 11f); arcTo(2f, 2f, 0f, true, false, 17f, 7f)
            }
            strokePath {
                moveTo(7f, 8f); verticalLineTo(16f)
                moveTo(17f, 11f); curveTo(17f, 14.5f, 14f, 16f, 9f, 16f)
            }
        }
    }

    val Import: ImageVector by lazy {
        strokeIcon("LukoaImport") {
            strokePath {
                moveTo(12f, 4f); verticalLineTo(14.5f)
                moveTo(8f, 10.8f); lineTo(12f, 14.8f); lineTo(16f, 10.8f)
            }
            strokePath {
                moveTo(4f, 16.5f); verticalLineTo(18.5f)
                curveTo(4f, 19.33f, 4.67f, 20f, 5.5f, 20f)
                horizontalLineTo(18.5f)
                curveTo(19.33f, 20f, 20f, 19.33f, 20f, 18.5f)
                verticalLineTo(16.5f)
            }
        }
    }

    val Blocks: ImageVector by lazy {
        strokeIcon("LukoaBlocks") {
            strokePath {
                moveTo(4f, 13f); horizontalLineTo(11f); verticalLineTo(20f); horizontalLineTo(4f); close()
                moveTo(13f, 13f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(13f); close()
                moveTo(8.5f, 4f); horizontalLineTo(15.5f); verticalLineTo(11f); horizontalLineTo(8.5f); close()
            }
        }
    }

    val Sliders: ImageVector by lazy {
        strokeIcon("LukoaSliders") {
            strokePath {
                moveTo(4f, 7f); horizontalLineTo(8f)
                moveTo(12f, 7f); horizontalLineTo(20f)
                moveTo(10f, 5f); arcTo(2f, 2f, 0f, true, false, 10f, 9f); arcTo(2f, 2f, 0f, true, false, 10f, 5f)
                moveTo(4f, 17f); horizontalLineTo(12f)
                moveTo(16f, 17f); horizontalLineTo(20f)
                moveTo(14f, 15f); arcTo(2f, 2f, 0f, true, false, 14f, 19f); arcTo(2f, 2f, 0f, true, false, 14f, 15f)
            }
        }
    }

    val Server: ImageVector by lazy {
        strokeIcon("LukoaServer") {
            strokePath {
                moveTo(4f, 5.5f); curveTo(4f, 4.67f, 4.67f, 4f, 5.5f, 4f)
                horizontalLineTo(18.5f); curveTo(19.33f, 4f, 20f, 4.67f, 20f, 5.5f)
                verticalLineTo(9.5f); curveTo(20f, 10.33f, 19.33f, 11f, 18.5f, 11f)
                horizontalLineTo(5.5f); curveTo(4.67f, 11f, 4f, 10.33f, 4f, 9.5f); close()
            }
            strokePath {
                moveTo(4f, 14.5f); curveTo(4f, 13.67f, 4.67f, 13f, 5.5f, 13f)
                horizontalLineTo(18.5f); curveTo(19.33f, 13f, 20f, 13.67f, 20f, 14.5f)
                verticalLineTo(18.5f); curveTo(20f, 19.33f, 19.33f, 20f, 18.5f, 20f)
                horizontalLineTo(5.5f); curveTo(4.67f, 20f, 4f, 19.33f, 4f, 18.5f); close()
            }
            strokePath {
                moveTo(7.5f, 7.5f); horizontalLineTo(7.6f)
                moveTo(7.5f, 16.5f); horizontalLineTo(7.6f)
            }
        }
    }

    val ShieldCheck: ImageVector by lazy {
        strokeIcon("LukoaShieldCheck") {
            strokePath {
                moveTo(12f, 3.5f)
                lineTo(19f, 6.2f)
                verticalLineTo(11.6f)
                curveTo(19f, 16.1f, 16f, 19.2f, 12f, 20.6f)
                curveTo(8f, 19.2f, 5f, 16.1f, 5f, 11.6f)
                verticalLineTo(6.2f)
                close()
            }
            strokePath {
                moveTo(9f, 12.2f); lineTo(11.1f, 14.3f); lineTo(15.2f, 10.2f)
            }
        }
    }

    val Clean: ImageVector by lazy {
        strokeIcon("LukoaClean") {
            strokePath {
                moveTo(12f, 3.5f)
                lineTo(13.9f, 9.1f)
                lineTo(19.5f, 11f)
                lineTo(13.9f, 12.9f)
                lineTo(12f, 18.5f)
                lineTo(10.1f, 12.9f)
                lineTo(4.5f, 11f)
                lineTo(10.1f, 9.1f)
                close()
            }
            strokePath {
                moveTo(18.5f, 16.5f); verticalLineTo(20.5f)
                moveTo(16.5f, 18.5f); horizontalLineTo(20.5f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        strokeIcon("LukoaClock") {
            strokePath {
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, true, false, 12f, 20.5f)
                arcTo(8.5f, 8.5f, 0f, true, false, 12f, 3.5f)
                close()
            }
            strokePath {
                moveTo(12f, 7.8f); verticalLineTo(12.2f); lineTo(15f, 14f)
            }
        }
    }

    val Terminal: ImageVector by lazy {
        strokeIcon("LukoaTerminal") {
            strokePath {
                moveTo(4f, 6f); curveTo(4f, 5.17f, 4.67f, 4.5f, 5.5f, 4.5f)
                horizontalLineTo(18.5f); curveTo(19.33f, 4.5f, 20f, 5.17f, 20f, 6f)
                verticalLineTo(18f); curveTo(20f, 18.83f, 19.33f, 19.5f, 18.5f, 19.5f)
                horizontalLineTo(5.5f); curveTo(4.67f, 19.5f, 4f, 18.83f, 4f, 18f); close()
            }
            strokePath {
                moveTo(8f, 9.5f); lineTo(10.8f, 12f); lineTo(8f, 14.5f)
                moveTo(12.5f, 14.5f); horizontalLineTo(16f)
            }
        }
    }

    val Layers: ImageVector by lazy {
        strokeIcon("LukoaLayers") {
            strokePath {
                moveTo(12f, 4f); lineTo(20f, 8.5f); lineTo(12f, 13f); lineTo(4f, 8.5f); close()
            }
            strokePath {
                moveTo(4f, 12.5f); lineTo(12f, 17f); lineTo(20f, 12.5f)
            }
            strokePath {
                moveTo(4f, 16.5f); lineTo(12f, 21f); lineTo(20f, 16.5f)
            }
        }
    }
}
