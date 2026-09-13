package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.PlaylistRepository
import com.geraciodev.lumina.data.model.Playlist
import com.geraciodev.lumina.data.model.PlaylistItem
import java.io.File

/**
 * Gestión de playlists guardadas (crear, borrar, añadir/quitar archivos).
 *
 * [onPlaylistUpdated] y [onPlaylistDeleted] permiten a quien componga este ViewModel (ver
 * [MainViewModel]) mantener sincronizada la playlist actualmente en reproducción, que vive en
 * [PlayerViewModel], sin acoplar directamente ambas clases.
 */
class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val onPlaylistUpdated: (Playlist) -> Unit = {},
    private val onPlaylistDeleted: (Playlist) -> Unit = {}
) {
    var playlists by mutableStateOf(emptyList<Playlist>())
        private set
    var selectedPlaylist by mutableStateOf<Playlist?>(null)

    private val mediaExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg",
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
    )

    init {
        playlists = playlistRepository.loadPlaylists()
    }

    fun createPlaylist(name: String, files: List<File>) {
        val newPlaylist = playlistRepository.createPlaylist(name, files)
        playlists = playlists + newPlaylist
        playlistRepository.savePlaylists(playlists)
    }

    fun deletePlaylist(playlist: Playlist) {
        playlists = playlists.filter { it.id != playlist.id }
        playlistRepository.savePlaylists(playlists)
        if (selectedPlaylist?.id == playlist.id) {
            selectedPlaylist = null
        }
        onPlaylistDeleted(playlist)
    }

    fun addFilesToPlaylist(playlist: Playlist, files: List<File>) {
        val updatedPlaylist = playlist.copy(
            items = playlist.items + files.map { PlaylistItem(it.absolutePath, it.name) }
        )
        playlists = playlists.map { if (it.id == playlist.id) updatedPlaylist else it }
        playlistRepository.savePlaylists(playlists)
        if (selectedPlaylist?.id == playlist.id) {
            selectedPlaylist = updatedPlaylist
        }
        onPlaylistUpdated(updatedPlaylist)
    }

    fun removeItemFromPlaylist(playlist: Playlist, item: PlaylistItem) {
        val updatedPlaylist = playlist.copy(
            items = playlist.items.filter { it != item }
        )
        playlists = playlists.map { if (it.id == playlist.id) updatedPlaylist else it }
        playlistRepository.savePlaylists(playlists)
        if (selectedPlaylist?.id == playlist.id) {
            selectedPlaylist = updatedPlaylist
        }
        onPlaylistUpdated(updatedPlaylist)
    }

    fun loadFilesFromFolder(folder: File): List<File> {
        return folder.listFiles()?.filter { file ->
            file.isFile && mediaExtensions.contains(file.extension.lowercase())
        } ?: emptyList()
    }
}
