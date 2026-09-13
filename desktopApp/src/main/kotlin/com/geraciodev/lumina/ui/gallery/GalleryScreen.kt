package com.geraciodev.lumina.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.geraciodev.lumina.data.model.GalleryImage
import com.geraciodev.lumina.ui.MainViewModel
import com.geraciodev.lumina.ui.filepicker.FilePickerDialog
import com.geraciodev.lumina.ui.filepicker.FilePickerMode
import com.geraciodev.lumina.util.decodeScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val galleryImageExtensions = setOf("jpg", "jpeg", "png", "bmp", "gif", "webp")

@Composable
fun GalleryScreen(viewModel: MainViewModel) {
    val images = viewModel.gallery.images
    val activeBackground = viewModel.settings.projectionBackgroundImage
    var showAddImagesPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "GALERÍA",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Fondos para la proyección bíblica. Selecciona una imagen para usarla de fondo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showAddImagesPicker = true },
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("AÑADIR IMÁGENES")
            }

            if (activeBackground != null) {
                TextButton(onClick = { viewModel.settings.selectProjectionBackgroundImage(null) }) {
                    Icon(Icons.Default.HideImage, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("QUITAR FONDO")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "SIN IMÁGENES. AÑADE ALGUNAS PARA USARLAS DE FONDO.",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(images, key = { it.id }) { image ->
                    GalleryThumbnail(
                        image = image,
                        isSelected = image.filePath == activeBackground,
                        onSelect = {
                            viewModel.settings.selectProjectionBackgroundImage(
                                if (image.filePath == activeBackground) null else image.filePath
                            )
                        },
                        onDelete = { viewModel.gallery.removeImage(image) }
                    )
                }
            }
        }
    }

    if (showAddImagesPicker) {
        FilePickerDialog(
            title = "Añadir imágenes a la galería",
            mode = FilePickerMode.FILES,
            allowMultiple = true,
            extensionFilter = galleryImageExtensions,
            onDismiss = { showAddImagesPicker = false },
            onConfirm = { files ->
                if (files.isNotEmpty()) viewModel.gallery.addImages(files)
                showAddImagesPicker = false
            }
        )
    }
}

@Composable
private fun GalleryThumbnail(
    image: GalleryImage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var bitmap by remember(image.filePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(image.filePath) {
        bitmap = loadThumbnail(image.filePath)
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                Image(
                    bitmap = currentBitmap,
                    contentDescription = image.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "SIN VISTA PREVIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Fondo activo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(2.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = image.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

private suspend fun loadThumbnail(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
    decodeScaledBitmap(File(path))?.toComposeImageBitmap()
}
