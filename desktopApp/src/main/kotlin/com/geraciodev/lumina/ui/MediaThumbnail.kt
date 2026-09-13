package com.geraciodev.lumina.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.geraciodev.lumina.player.ThumbnailManager
import com.geraciodev.lumina.util.decodeScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val imageExtensionsForThumbnail = setOf("jpg", "jpeg", "png", "bmp", "gif", "webp")
private val audioExtensionsForThumbnail = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
private val videoExtensionsForThumbnail = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg")
private val fontExtensionsForThumbnail = setOf("ttf", "otf")

private enum class MediaKind { IMAGE, AUDIO, VIDEO, OTHER }

private fun mediaKindOf(file: File): MediaKind {
    val ext = file.extension.lowercase()
    return when {
        imageExtensionsForThumbnail.contains(ext) -> MediaKind.IMAGE
        audioExtensionsForThumbnail.contains(ext) -> MediaKind.AUDIO
        videoExtensionsForThumbnail.contains(ext) -> MediaKind.VIDEO
        else -> MediaKind.OTHER
    }
}

private fun fallbackIconFor(file: File): ImageVector {
    if (file.isDirectory) return Icons.Default.Folder
    return when (mediaKindOf(file)) {
        MediaKind.IMAGE -> Icons.Default.Image
        MediaKind.AUDIO -> Icons.Default.MusicNote
        MediaKind.VIDEO -> Icons.Default.Movie
        MediaKind.OTHER -> if (fontExtensionsForThumbnail.contains(file.extension.lowercase())) {
            Icons.Default.FontDownload
        } else {
            Icons.AutoMirrored.Filled.InsertDriveFile
        }
    }
}

/**
 * Miniatura de un archivo: carpeta, imagen (decodificada directamente), un frame del video, o la
 * carátula embebida si es audio y la tiene. Mientras se genera (o si no se pudo obtener ninguna)
 * muestra un ícono de respaldo según el tipo de archivo.
 */
@Composable
fun MediaThumbnail(file: File, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        if (file.isDirectory) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            val kind = remember(file) { mediaKindOf(file) }
            var bitmap by remember(file) { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(file.absolutePath) {
                bitmap = when (kind) {
                    MediaKind.IMAGE -> loadImageDirect(file)
                    MediaKind.AUDIO -> ThumbnailManager.getThumbnail(file, isAudio = true)
                    MediaKind.VIDEO -> ThumbnailManager.getThumbnail(file, isAudio = false)
                    MediaKind.OTHER -> null
                }
            }

            val current = bitmap
            if (current != null) {
                Image(
                    bitmap = current,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = fallbackIconFor(file),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private suspend fun loadImageDirect(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
    decodeScaledBitmap(file)?.toComposeImageBitmap()
}
