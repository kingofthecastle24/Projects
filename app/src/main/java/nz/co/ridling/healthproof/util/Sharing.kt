package nz.co.ridling.healthproof.util

import android.content.Context
import android.content.Intent

/** Opens the system share sheet with [text] as plain text - e.g. to send a diagnostics report. */
fun shareText(context: Context, text: String, chooserTitle: String = "Share diagnostics") {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}
