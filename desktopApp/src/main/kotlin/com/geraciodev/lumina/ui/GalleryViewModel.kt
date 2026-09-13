package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.GalleryRepository
import com.geraciodev.lumina.data.model.GalleryImage
import java.io.File

/**
 * Biblioteca de imágenes cargadas por el usuario para usarlas como fondo de la proyección
 * bíblica. [onImageDeleted] permite a quien componga este ViewModel (ver [MainViewModel])
 * limpiar el fondo activo en [SettingsViewModel] si la imagen borrada era la seleccionada.
 */
class GalleryViewModel(
    private val galleryRepository: GalleryRepository,
    private val onImageDeleted: (GalleryImage) -> Unit = {}
) {
    var images by mutableStateOf(emptyList<GalleryImage>())
        private set

    private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "gif", "webp")

    init {
        images = galleryRepository.loadImages()
    }

    fun addImages(files: List<File>) {
        val existingPaths = images.map { it.filePath }.toSet()
        val newFiles = files.filter { file ->
            file.isFile &&
                imageExtensions.contains(file.extension.lowercase()) &&
                file.absolutePath !in existingPaths
        }
        if (newFiles.isEmpty()) return

        images = images + galleryRepository.createImageEntries(newFiles)
        galleryRepository.saveImages(images)
    }

    fun removeImage(image: GalleryImage) {
        images = images.filter { it.id != image.id }
        galleryRepository.saveImages(images)
        onImageDeleted(image)
    }
}
