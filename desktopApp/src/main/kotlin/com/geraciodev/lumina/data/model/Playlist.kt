package com.geraciodev.lumina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val items: List<PlaylistItem>
)

@Serializable
data class PlaylistItem(
    val filePath: String,
    val fileName: String
)
