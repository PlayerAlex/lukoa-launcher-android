package moe.lukoa.launcher

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberTransientHint(): (String) -> Unit {
    val appContext = LocalContext.current.applicationContext
    val activeToast = remember { arrayOfNulls<Toast>(1) }

    DisposableEffect(Unit) {
        onDispose {
            activeToast[0]?.cancel()
            activeToast[0] = null
        }
    }

    return remember(appContext) {
        { message ->
            activeToast[0]?.cancel()
            activeToast[0] = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).also(Toast::show)
        }
    }
}
