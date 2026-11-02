package app.vidown.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.MusicNote
import com.adamglin.phosphoricons.regular.Video
import java.io.File
import java.util.Locale

@Composable
fun MediaThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    mediaUri: String? = null,
    isAudioOnly: Boolean = false,
    durationSeconds: Long = 0L,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val context = LocalContext.current
    val model: Any? = when {
        !thumbnailUrl.isNullOrBlank() -> thumbnailUrl
        !mediaUri.isNullOrBlank() && mediaUri.startsWith("content://") -> Uri.parse(mediaUri)
        !mediaUri.isNullOrBlank() -> File(mediaUri)
        else -> null
    }

    val imageRequest = ImageRequest.Builder(context)
        .data(model)
        .crossfade(true)
        .decoderFactory(VideoFrameDecoder.Factory())
        .build()

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                ThumbnailPlaceholder(isAudioOnly = isAudioOnly)
            },
            error = {
                ThumbnailPlaceholder(isAudioOnly = isAudioOnly)
            }
        )

        if (durationSeconds > 0 && !isAudioOnly) {
            val min = durationSeconds / 60
            val sec = durationSeconds % 60
            val durationText = if (min >= 60) {
                val hr = min / 60
                val remMin = min % 60
                String.format(Locale.US, "%d:%02d:%02d", hr, remMin, sec)
            } else {
                String.format(Locale.US, "%02d:%02d", min, sec)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ThumbnailPlaceholder(isAudioOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAudioOnly) PhosphorIcons.Regular.MusicNote else PhosphorIcons.Regular.Video,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}
