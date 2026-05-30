package nep.timeline.cirno.ui.utils

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import java.io.File

object BackgroundManager {
    var currentUri by mutableStateOf<Uri?>(null)
        private set

    private const val BACKGROUND_FILE_NAME = "background.jpg"

    const val topAppBarAlpha = 0.55f
    const val cardAlpha = 0.55f
    const val forceSmallTop = false

    val topAppBarBlurRadius = 15.dp
    val cardBlurRadius = 15.dp

    fun init(context: Context) {
        val file = backgroundFile(context)
        currentUri = if (file.exists()) fileUriWithRevision(file) else null
    }

    fun set(context: Context, sourceUri: Uri): Boolean {
        return try {
            val file = backgroundFile(context)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            currentUri = fileUriWithRevision(file)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun remove(context: Context) {
        backgroundFile(context).delete()
        currentUri = null
    }

    private fun backgroundFile(context: Context): File = File(context.filesDir, BACKGROUND_FILE_NAME)

    private fun fileUriWithRevision(file: File): Uri = Uri.fromFile(file).buildUpon()
        .appendQueryParameter("rev", file.lastModified().toString())
        .build()
}
