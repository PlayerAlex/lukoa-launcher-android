package moe.lukoa.launcher

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object SharedFileSender {
    fun shareTextFile(
        context: Context,
        file: File,
        chooserTitle: String,
        subject: String,
    ) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
