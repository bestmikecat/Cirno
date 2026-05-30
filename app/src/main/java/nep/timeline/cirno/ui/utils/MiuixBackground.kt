package nep.timeline.cirno.ui.utils

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixBackground(content: @Composable () -> Unit) {
    val uri = BackgroundManager.currentUri
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    val surfaceColor = MiuixTheme.colorScheme.surface

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            uri?.let { loadBackgroundBitmap(context.contentResolver, it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor.copy(alpha = 0.42f))
            )
        }
        content()
    }
}

private fun loadBackgroundBitmap(contentResolver: android.content.ContentResolver, uri: Uri): ImageBitmap? {
    return try {
        val bitmap = if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path)
        } else {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
        bitmap?.asImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
