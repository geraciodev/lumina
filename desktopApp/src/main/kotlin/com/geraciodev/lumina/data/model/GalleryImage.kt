package com.geraciodev.lumina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GalleryImage(
    val id: String,
    val filePath: String,
    val fileName: String
)
