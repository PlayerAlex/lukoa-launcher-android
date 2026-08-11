package moe.lukoa.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LukoaPaletteScreenshotTest {
    @Test
    fun renderTavernHubScreen() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            LukoaTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 42.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Header(
                            tavernRunning = false,
                            tavernStarting = false,
                            showVersionUpdateBadge = false,
                            onVersionClick = {},
                        )
                        TavernHubSection(
                            tavernRunning = false,
                            tavernStarting = false,
                            actionInProgress = false,
                            onOpenVersionManagement = {},
                            onOpenBackup = {},
                            onOpenExtensionManagement = {},
                        )
                    }
                    LauncherBottomBar(
                        selectedTab = LauncherTab.Tavern,
                        onSelectTab = {},
                    )
                }
            }
        }

        renderActivity(
            activity = activity,
            outputPath = "build/reports/tavern-hub-actual.png",
            width = 390,
            height = 844,
        )
        controller.close()
    }

    @Test
    fun renderToolboxScreen() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            LukoaTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, top = 42.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Header(
                            tavernRunning = false,
                            tavernStarting = false,
                            showVersionUpdateBadge = true,
                            onVersionClick = {},
                        )
                        ToolboxSection()
                    }
                    LauncherBottomBar(
                        selectedTab = LauncherTab.Toolbox,
                        onSelectTab = {},
                    )
                }
            }
        }

        renderActivity(
            activity = activity,
            outputPath = "build/reports/toolbox-actual.png",
            width = 390,
            height = 844,
        )
        controller.close()
    }

    @Test
    fun renderSharedLauncherChrome() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            LukoaTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SectionPanel(
                        title = "酒馆控制",
                        accentColor = LukoaColors.Primary,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = LukoaColors.Elevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LukoaColors.Border),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "当前控制状态",
                                    color = LukoaColors.TextSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    text = "酒馆尚未运行，可以直接启动。",
                                    color = LukoaColors.TextPrimary,
                                )
                            }
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LukoaColors.Primary,
                                contentColor = LukoaColors.OnPrimary,
                            ),
                        ) {
                            Text("启动酒馆", fontWeight = FontWeight.Bold)
                        }
                        SecondaryActionButton(
                            text = "查看技术信息",
                            enabled = true,
                            accentColor = LukoaColors.Primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {},
                        )
                    }
                    SectionPanel(
                        title = "实例与运行环境",
                        accentColor = LukoaColors.Primary,
                        containerColor = LukoaColors.Elevated,
                    ) {
                        SettingsEntryGroup {
                            SettingsEntryRow(
                                title = "当前实例",
                                detail = "管理酒馆目录和端口",
                                value = "主实例",
                            )
                            SettingsEntryDivider()
                            SettingsEntryRow(
                                title = "启动器更新",
                                value = "有新版本",
                                valueColor = LukoaColors.Accent,
                                valueAsPill = true,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    LauncherBottomBar(
                        selectedTab = LauncherTab.Launch,
                        onSelectTab = {},
                    )
                }
            }
        }

        shadowOf(activity.mainLooper).idle()
        val decor = activity.window.decorView
        val width = 400
        val height = 800
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))

        val output = File("build/reports/lukoa-palette-actual.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        assertEquals(LukoaColors.Background.toArgb(), bitmap.getPixel(4, 400))
        val baseRatio = bitmap.ratioOf(LukoaColors.Background.toArgb(), LukoaColors.Surface.toArgb())
        val supportRatio = bitmap.ratioOf(LukoaColors.Elevated.toArgb(), LukoaColors.Border.toArgb())
        val attentionRatio = bitmap.ratioOf(
            LukoaColors.Primary.toArgb(),
            LukoaColors.PrimarySoft.toArgb(),
            LukoaColors.Accent.toArgb(),
            LukoaColors.AccentSoft.toArgb(),
        )
        assertTrue("基础深色应约占六成，实际为 $baseRatio", baseRatio in 0.55f..0.72f)
        assertTrue("支撑层级应约占三成，实际为 $supportRatio", supportRatio in 0.20f..0.38f)
        assertTrue("强调色不应超过一成，实际为 $attentionRatio", attentionRatio in 0.03f..0.12f)
        controller.close()
    }

    private fun Bitmap.ratioOf(vararg colors: Int): Float {
        val palette = colors.toSet()
        var matches = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (getPixel(x, y) in palette) matches += 1
            }
        }
        return matches.toFloat() / (width * height).toFloat()
    }

    private fun renderActivity(
        activity: ComponentActivity,
        outputPath: String,
        width: Int,
        height: Int,
    ) {
        shadowOf(activity.mainLooper).idle()
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        val output = File(outputPath)
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
