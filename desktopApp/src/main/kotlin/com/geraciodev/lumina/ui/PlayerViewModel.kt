package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.model.Playlist
import com.geraciodev.lumina.data.model.PlaylistItem
import com.geraciodev.lumina.player.VideoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

enum class PlaybackMode {
    SEQUENCE, // Salta al siguiente y se detiene al final de la lista
    LOOP_ALL,  // Salta al siguiente y vuelve al inicio al terminar la lista
    LOOP_ONE   // Repite el archivo actual infinitamente
}

/**
 * Estado y control de la reproducción actual: archivo seleccionado, proyección de video,
 * modo de reproducción y la sesión de "recientes"/playlist en curso.
 */
class PlayerViewModel(private val scope: CoroutineScope) {
    var selectedVideo by mutableStateOf<File?>(null)
        private set
    var isAudioOnly by mutableStateOf(false)
        private set
    var projectingFile by mutableStateOf<File?>(null)
        private set
    var isPlaylistWindowOpen by mutableStateOf(false)
    var playbackMode by mutableStateOf(PlaybackMode.SEQUENCE)
        private set
    var playingPlaylist by mutableStateOf<Playlist?>(null)
        private set
    val recentFiles = mutableStateListOf<PlaylistItem>()
    var showRecentInOverlay by mutableStateOf(false)
        private set

    private val audioExtensions = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")

    init {
        VideoManager.onVideoFinished = {
            scope.launch(Dispatchers.Main) { onPlaybackFinished() }
        }
    }

    fun selectVideo(file: File, fromPlaylist: Playlist? = null) {
        selectedVideo = file
        isAudioOnly = audioExtensions.contains(file.extension.lowercase())
        // Él mute es un toggle manual del usuario para la reproducción actual: al elegir un
        // archivo nuevo (no al repetir el mismo en modo bucle) partimos siempre con sonido,
        // para que abrir un video nunca "herede" en silencio un mute que ya se te olvidó.
        if (VideoManager.isMuted) {
            VideoManager.toggleMute(false)
        }
        VideoManager.play(file, isAudioOnly)

        // Gestionar lista de recientes (mover al principio si ya existe)
        val item = PlaylistItem(file.absolutePath, file.name)
        recentFiles.removeAll { it.filePath == item.filePath }
        recentFiles.add(0, item)
        if (recentFiles.size > 20) {
            recentFiles.removeAt(recentFiles.size - 1)
        }

        if (fromPlaylist != null) {
            playingPlaylist = fromPlaylist
            showRecentInOverlay = false
        } else {
            playingPlaylist = null
            showRecentInOverlay = true
        }
    }

    private fun onPlaybackFinished() {
        when (playbackMode) {
            PlaybackMode.LOOP_ONE -> {
                selectedVideo?.let { VideoManager.play(it, isAudioOnly) }
            }
            PlaybackMode.LOOP_ALL, PlaybackMode.SEQUENCE -> {
                playNext()
            }
        }
    }

    fun playNext() {
        val currentItems = if (showRecentInOverlay) recentFiles else playingPlaylist?.items ?: emptyList()
        if (currentItems.isEmpty()) return

        val currentIndex = currentItems.indexOfFirst { it.filePath == selectedVideo?.absolutePath }
        if (currentIndex != -1) {
            val nextIndex = currentIndex + 1
            if (nextIndex < currentItems.size) {
                val nextItem = currentItems[nextIndex]
                selectVideo(File(nextItem.filePath), if (showRecentInOverlay) null else playingPlaylist)
            } else if (playbackMode == PlaybackMode.LOOP_ALL) {
                // Volver al principio
                val firstItem = currentItems[0]
                selectVideo(File(firstItem.filePath), if (showRecentInOverlay) null else playingPlaylist)
            }
        }
    }

    fun playPrevious() {
        val currentItems = if (showRecentInOverlay) recentFiles else playingPlaylist?.items ?: emptyList()
        if (currentItems.isEmpty()) return

        val currentIndex = currentItems.indexOfFirst { it.filePath == selectedVideo?.absolutePath }
        if (currentIndex != -1) {
            val prevIndex = currentIndex - 1
            if (prevIndex >= 0) {
                val prevItem = currentItems[prevIndex]
                selectVideo(File(prevItem.filePath), if (showRecentInOverlay) null else playingPlaylist)
            } else if (playbackMode == PlaybackMode.LOOP_ALL) {
                // Ir al final
                val lastItem = currentItems.last()
                selectVideo(File(lastItem.filePath), if (showRecentInOverlay) null else playingPlaylist)
            }
        }
    }

    fun togglePlaybackMode() {
        playbackMode = when (playbackMode) {
            PlaybackMode.SEQUENCE -> PlaybackMode.LOOP_ALL
            PlaybackMode.LOOP_ALL -> PlaybackMode.LOOP_ONE
            PlaybackMode.LOOP_ONE -> PlaybackMode.SEQUENCE
        }
    }

    fun toggleProjection() {
        projectingFile = if (projectingFile == null) selectedVideo else null
    }

    fun stopVideoProjection() {
        projectingFile = null
    }

    fun stopVideo() {
        selectedVideo = null
        isAudioOnly = false
        playingPlaylist = null
        showRecentInOverlay = false
        isPlaylistWindowOpen = false
        projectingFile = null
        VideoManager.stop()
    }

    /** Mantiene sincronizada la playlist en reproducción cuando [PlaylistViewModel] la edita. */
    fun syncPlayingPlaylist(updated: Playlist) {
        if (playingPlaylist?.id == updated.id) {
            playingPlaylist = updated
        }
    }

    /** Limpia la referencia a la playlist en reproducción cuando [PlaylistViewModel] la elimina. */
    fun clearPlayingPlaylistIfMatches(playlist: Playlist) {
        if (playingPlaylist?.id == playlist.id) {
            playingPlaylist = null
            showRecentInOverlay = true
        }
    }
}
