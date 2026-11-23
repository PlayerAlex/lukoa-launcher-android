package moe.lukoa.launcher

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberTransientHint(): (String) -> Unit {
    val appContext = LocalContext.current.applicationContext
    val topBanner = LocalLukoaTopBanner.current
    val activeToast = remember { arrayOfNulls<Toast>(1) }

    DisposableEffect(Unit) {
        onDispose {
            activeToast[0]?.cancel()
            activeToast[0] = null
        }
    }

    return remember(appContext, topBanner) {
        { message ->
            if (topBanner != null) {
                topBanner.show(title = message)
            } else {
                activeToast[0]?.cancel()
                activeToast[0] = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).also(Toast::show)
            }
        }
    }
}
