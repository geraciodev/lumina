package com.geraciodev.lumina.data

import com.geraciodev.lumina.data.model.GalleryImage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class GalleryRepository {
    private val appDataDir = File(System.getProperty("user.home"), ".lumina")
    private val galleryFile = File(appDataDir, "gallery.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
    }

    /** Carga las imágenes guardadas, descartando las que ya no existen en disco. */
    fun loadImages(): List<GalleryImage> {
        if (!galleryFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<GalleryImage>>(galleryFile.readText())
                .filter { File(it.filePath).exists() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveImages(images: List<GalleryImage>) {
        try {
            galleryFile.writeText(json.encodeToString(images))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createImageEntries(files: List<File>): List<GalleryImage> =
        files.map { GalleryImage(UUID.randomUUID().toString(), it.absolutePath, it.name) }
}
