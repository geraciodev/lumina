package com.geraciodev.lumina.data

import com.geraciodev.lumina.data.model.Playlist
import com.geraciodev.lumina.data.model.PlaylistItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class PlaylistRepository {
    private val appDataDir = File(System.getProperty("user.home"), ".lumina")
    private val playlistsFile = File(appDataDir, "playlists.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    init {
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
    }

    fun loadPlaylists(): List<Playlist> {
        return if (playlistsFile.exists()) {
            try {
                json.decodeFromString<List<Playlist>>(playlistsFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        playlistsFile.writeText(json.encodeToString<List<Playlist>>(playlists))
    }

    fun createPlaylist(name: String, files: List<File>): Playlist {
        val items = files.map { PlaylistItem(it.absolutePath, it.name) }
        return Playlist(UUID.randomUUID().toString(), name, items)
    }
}
