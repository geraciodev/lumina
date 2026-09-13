package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.BibleRepository
import com.geraciodev.lumina.data.GalleryRepository
import com.geraciodev.lumina.data.PlaylistRepository
import com.geraciodev.lumina.data.SettingsRepository
import com.geraciodev.lumina.data.VideoSearchRepository
import com.geraciodev.lumina.player.VideoManager
import kotlinx.coroutines.CoroutineScope

enum class LuminaScreen {
    SEARCH, PLAYLIST, BIBLE, GALLERY, SETTINGS, ABOUT
}

/**
 * Coordinador de la app: mantiene la navegación global y compone los ViewModels de cada
 * dominio (reproducción, búsqueda, playlists, Biblia, ajustes). Solo contiene la lógica que
 * cruza esos dominios entre sí (p. ej. detener la proyección de video al proyectar un
 * versículo); el resto vive en el ViewModel correspondiente.
 */
class MainViewModel(
    repository: VideoSearchRepository,
    playlistRepository: PlaylistRepository,
    bibleRepository: BibleRepository,
    settingsRepository: SettingsRepository,
    galleryRepository: GalleryRepository,
    scope: CoroutineScope
) {
    var currentScreen by mutableStateOf(LuminaScreen.SEARCH)
    var isSidebarExpanded by mutableStateOf(false)
    var isAnyInputFocused by mutableStateOf(false)

    val settings = SettingsViewModel(settingsRepository)
    val search = SearchViewModel(repository, scope)
    val player = PlayerViewModel(scope)
    val playlist = PlaylistViewModel(
        playlistRepository = playlistRepository,
        onPlaylistUpdated = { updated -> player.syncPlayingPlaylist(updated) },
        onPlaylistDeleted = { deleted -> player.clearPlayingPlaylistIfMatches(deleted) }
    )
    val bible = BibleViewModel(
        bibleRepository = bibleRepository,
        scope = scope,
        onProjectionStarted = { player.stopVideoProjection() }
    )
    val gallery = GalleryViewModel(
        galleryRepository = galleryRepository,
        onImageDeleted = { deleted ->
            if (settings.projectionBackgroundImage == deleted.filePath) {
                settings.selectProjectionBackgroundImage(null)
            }
        }
    )

    init {
        // El buscador necesita las carpetas ya cargadas por SettingsViewModel.
        search.updateScanFolders(settings.scanFolders.toList())
    }

    fun projectSelectedVerses() {
        isAnyInputFocused = false
        bible.projectSelectedVerses()
    }

    fun stopProjection() {
        isAnyInputFocused = false
        player.stopVideoProjection()
        bible.stopBibleProjection()
    }

    fun addScanFolder(path: String) {
        if (settings.addScanFolder(path)) {
            search.updateScanFolders(settings.scanFolders.toList())
        }
    }

    fun removeScanFolder(path: String) {
        settings.removeScanFolder(path)
        search.updateScanFolders(settings.scanFolders.toList())
    }

    fun handleKeyEvent(keyCode: Int, ctrl: Boolean, alt: Boolean, shift: Boolean): Boolean {
        val shortcutEntry = settings.shortcuts.entries.find { (_, config) ->
            config.keyCode == keyCode && config.ctrl == ctrl && config.alt == alt && config.shift == shift
        }

        if (shortcutEntry != null) {
            when (shortcutEntry.key) {
                "Reproducir/Pausa" -> VideoManager.togglePlayPause()
                "Detener" -> player.stopVideo()
                "Siguiente" -> player.playNext()
                "Anterior" -> player.playPrevious()
                "Alternar Sidebar" -> isSidebarExpanded = !isSidebarExpanded
                "Alternar Proyección" -> player.toggleProjection()
                "Silenciar" -> VideoManager.toggleMute(!VideoManager.isMuted)
                "Adelantar 5s" -> VideoManager.skip(5000)
                "Retroceder 5s" -> VideoManager.skip(-5000)
                "Subir Volumen" -> VideoManager.updateVolume(VideoManager.currentVolume + 5)
                "Bajar Volumen" -> VideoManager.updateVolume(VideoManager.currentVolume - 5)
                "Siguiente Capítulo" -> bible.nextBibleChapter()
                "Capítulo Anterior" -> bible.previousBibleChapter()
                "Siguiente Libro" -> bible.nextBibleBook()
                "Libro Anterior" -> bible.previousBibleBook()
                "Siguiente Versículo" -> bible.projectNextVerse()
                "Versículo Anterior" -> bible.projectPreviousVerse()
            }
            return true
        }
        return false
    }
}
