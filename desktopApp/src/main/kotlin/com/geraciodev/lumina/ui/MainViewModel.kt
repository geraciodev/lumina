package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.VideoSearchRepository
import com.geraciodev.lumina.data.PlaylistRepository
import com.geraciodev.lumina.data.BibleRepository
import com.geraciodev.lumina.data.SettingsRepository
import com.geraciodev.lumina.data.model.AppSettings
import com.geraciodev.lumina.data.model.Playlist
import com.geraciodev.lumina.data.model.ShortcutConfig
import com.geraciodev.lumina.data.model.defaultShortcuts
import com.geraciodev.lumina.data.model.bible.*
import com.geraciodev.lumina.player.VideoManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

enum class LuminaScreen {
    SEARCH, PLAYLIST, BIBLE, SETTINGS, ABOUT
}

enum class PlaybackMode {
    SEQUENCE, // Salta al siguiente y se detiene al final de la lista
    LOOP_ALL,  // Salta al siguiente y vuelve al inicio al terminar la lista
    LOOP_ONE   // Repite el archivo actual infinitamente
}

class MainViewModel(
    private val repository: VideoSearchRepository,
    private val playlistRepository: PlaylistRepository,
    private val bibleRepository: BibleRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    var currentScreen by mutableStateOf(LuminaScreen.SEARCH)
    var isSidebarExpanded by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf(emptyList<File>())
    var isSearching by mutableStateOf(false)
    var isSearchInputFocused by mutableStateOf(false)
    var selectedVideo by mutableStateOf<File?>(null)
    var isAudioOnly by mutableStateOf(false)
    var projectingFile by mutableStateOf<File?>(null)
    var projectingBibleVerses by mutableStateOf<List<BibleItem>?>(null)
    var projectingBibleRef by mutableStateOf("")
    var isPlaylistWindowOpen by mutableStateOf(false)
    var playbackMode by mutableStateOf(PlaybackMode.SEQUENCE)
    
    // Settings state
    var isDarkMode by mutableStateOf(true)
    var isMaximized by mutableStateOf(false)
    var windowWidth by mutableStateOf(1200)
    var windowHeight by mutableStateOf(850)
    var shortcuts by mutableStateOf(defaultShortcuts)
    var recordingShortcutName by mutableStateOf<String?>(null)
    var scanFolders = mutableStateListOf<String>()
    
    // Projection Settings
    var projectionFontSize by mutableStateOf(48)
    var projectionFontFamily by mutableStateOf("Inter")
    var projectionFontColor by mutableStateOf(0xFFFFFFFFL)
    
    val availableSystemFonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        .availableFontFamilyNames.toList()

    // Playlist States
    var playlists by mutableStateOf(emptyList<Playlist>())
    var selectedPlaylist by mutableStateOf<Playlist?>(null)
    var playingPlaylist by mutableStateOf<Playlist?>(null)
    var recentFiles = mutableStateListOf<com.geraciodev.lumina.data.model.PlaylistItem>()
    var showRecentInOverlay by mutableStateOf(false)

    // Bible States
    var bibleVersion by mutableStateOf<BibleVersion?>(null)
    var selectedBibleBook by mutableStateOf<BibleBook?>(null)
    var selectedBibleChapter by mutableStateOf<BibleChapter?>(null)
    var selectedVerses = mutableStateListOf<Int>()
    var bibleSearchQuery by mutableStateOf("")
    var bibleSearchResults by mutableStateOf(emptyList<BibleRepository.SearchResult>())
    var isBibleSearching by mutableStateOf(false)

    init {
        val settings = settingsRepository.loadSettings()
        isDarkMode = settings.isDarkMode
        isMaximized = settings.isMaximized
        windowWidth = settings.windowWidth
        windowHeight = settings.windowHeight
        shortcuts = settings.shortcuts
        scanFolders.addAll(settings.scanFolders)
        projectionFontSize = settings.projectionFontSize
        projectionFontFamily = settings.projectionFontFamily
        projectionFontColor = settings.projectionFontColor
        VideoManager.updateVolume(settings.volume)

        // Inicializar repositorio con carpetas guardadas
        repository.updateScanFolders(settings.scanFolders)

        playlists = playlistRepository.loadPlaylists()
        loadBible()
        VideoManager.onVideoFinished = {
            scope.launch(Dispatchers.Main) {
                onPlaybackFinished()
            }
        }
    }

    private fun loadBible() {
        scope.launch(Dispatchers.IO) {
            val bible = bibleRepository.loadBible("RVR1960_vid_149.json")
            withContext(Dispatchers.Main) {
                bibleVersion = bible
                selectedBibleBook = bible?.books?.firstOrNull()
                selectedBibleChapter = selectedBibleBook?.chapters?.firstOrNull()
            }
        }
    }

    fun selectBibleBook(book: BibleBook) {
        selectedBibleBook = book
        selectedBibleChapter = book.chapters.firstOrNull()
        selectedVerses.clear()
    }

    fun selectBibleChapter(chapter: BibleChapter) {
        selectedBibleChapter = chapter
        selectedVerses.clear()
    }

    fun nextBibleChapter() {
        val book = selectedBibleBook ?: return
        val currentChapter = selectedBibleChapter ?: return
        val currentIndex = book.chapters.indexOf(currentChapter)
        
        if (currentIndex < book.chapters.size - 1) {
            selectBibleChapter(book.chapters[currentIndex + 1])
        } else {
            // Ir al siguiente libro
            val books = bibleVersion?.books ?: return
            val bookIndex = books.indexOf(book)
            if (bookIndex < books.size - 1) {
                selectBibleBook(books[bookIndex + 1])
            }
        }
    }

    fun previousBibleChapter() {
        val book = selectedBibleBook ?: return
        val currentChapter = selectedBibleChapter ?: return
        val currentIndex = book.chapters.indexOf(currentChapter)
        
        if (currentIndex > 0) {
            selectBibleChapter(book.chapters[currentIndex - 1])
        } else {
            // Ir al libro anterior
            val books = bibleVersion?.books ?: return
            val bookIndex = books.indexOf(book)
            if (bookIndex > 0) {
                val prevBook = books[bookIndex - 1]
                selectBibleBook(prevBook)
                // Ir al último capítulo del libro anterior
                selectBibleChapter(prevBook.chapters.last())
            }
        }
    }

    fun nextBibleBook() {
        val books = bibleVersion?.books ?: return
        val currentBook = selectedBibleBook ?: return
        val currentIndex = books.indexOf(currentBook)
        
        if (currentIndex < books.size - 1) {
            selectBibleBook(books[currentIndex + 1])
        }
    }

    fun previousBibleBook() {
        val books = bibleVersion?.books ?: return
        val currentBook = selectedBibleBook ?: return
        val currentIndex = books.indexOf(currentBook)
        
        if (currentIndex > 0) {
            selectBibleBook(books[currentIndex - 1])
        }
    }

    fun toggleVerseSelection(verseNumber: Int) {
        if (selectedVerses.contains(verseNumber)) {
            selectedVerses.remove(verseNumber)
        } else {
            selectedVerses.add(verseNumber)
            selectedVerses.sort()
        }
    }

    fun projectSelectedVerses() {
        val chapter = selectedBibleChapter ?: return
        if (selectedVerses.isEmpty()) return

        val versesToProject = chapter.items.filter { item ->
            item.type == "verse" && item.verse_numbers.any { it in selectedVerses }
        }
        
        projectingBibleVerses = versesToProject
        val chapterNum = chapter.current.human.split(" ").last()
        projectingBibleRef = "${selectedBibleBook?.name} $chapterNum:${formatVerseRange(selectedVerses)}"
        projectingFile = null // Stop video projection if any
    }

    private fun formatVerseRange(verses: List<Int>): String {
        if (verses.isEmpty()) return ""
        val sorted = verses.sorted()
        val groups = mutableListOf<Pair<Int, Int>>()
        
        var start = sorted[0]
        var end = sorted[0]
        
        for (i in 1 until sorted.size) {
            if (sorted[i] == end + 1) {
                end = sorted[i]
            } else {
                groups.add(start to end)
                start = sorted[i]
                end = sorted[i]
            }
        }
        groups.add(start to end)
        
        return groups.joinToString(", ") { (s, e) ->
            if (s == e) "$s" else "$s-$e"
        }
    }

    fun stopProjection() {
        projectingFile = null
        projectingBibleVerses = null
        projectingBibleRef = ""
    }

    private val audioExtensions = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
    private val mediaExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg",
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
    )

    private var searchJob: Job? = null

    @OptIn(FlowPreview::class)
    fun onSearchQueryChanged(newQuery: String) {
        searchQuery = newQuery
        searchJob?.cancel()

        if (newQuery.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return
        }

        searchJob = scope.launch {
            try {
                delay(300)
                isSearching = true
                repository.searchMediaFlow(newQuery).collect { results ->
                    searchResults = results
                    isSearching = false
                }
            } finally {
                isSearching = false
            }
        }
    }

    fun selectVideo(file: File, fromPlaylist: Playlist? = null) {
        selectedVideo = file
        isAudioOnly = audioExtensions.contains(file.extension.lowercase())
        VideoManager.play(file, isAudioOnly)

        // Gestionar lista de recientes (mover al principio si ya existe)
        val item = com.geraciodev.lumina.data.model.PlaylistItem(file.absolutePath, file.name)
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

    fun stopVideo() {
        selectedVideo = null
        isAudioOnly = false
        playingPlaylist = null
        showRecentInOverlay = false
        isPlaylistWindowOpen = false
        projectingFile = null
        VideoManager.stop()
    }

    // Playlist Methods
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
        if (playingPlaylist?.id == playlist.id) {
            playingPlaylist = null
            showRecentInOverlay = true
        }
    }

    fun addFilesToPlaylist(playlist: Playlist, files: List<File>) {
        val updatedPlaylist = playlist.copy(
            items = playlist.items + files.map { com.geraciodev.lumina.data.model.PlaylistItem(it.absolutePath, it.name) }
        )
        playlists = playlists.map { if (it.id == playlist.id) updatedPlaylist else it }
        playlistRepository.savePlaylists(playlists)
        if (selectedPlaylist?.id == playlist.id) {
            selectedPlaylist = updatedPlaylist
        }
        if (playingPlaylist?.id == playlist.id) {
            playingPlaylist = updatedPlaylist
        }
    }

    fun removeItemFromPlaylist(playlist: Playlist, item: com.geraciodev.lumina.data.model.PlaylistItem) {
        val updatedPlaylist = playlist.copy(
            items = playlist.items.filter { it != item }
        )
        playlists = playlists.map { if (it.id == playlist.id) updatedPlaylist else it }
        playlistRepository.savePlaylists(playlists)
        if (selectedPlaylist?.id == playlist.id) {
            selectedPlaylist = updatedPlaylist
        }
        if (playingPlaylist?.id == playlist.id) {
            playingPlaylist = updatedPlaylist
        }
    }
    
    fun loadFilesFromFolder(folder: File): List<File> {
        return folder.listFiles()?.filter { file ->
            file.isFile && mediaExtensions.contains(file.extension.lowercase())
        } ?: emptyList()
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        saveCurrentSettings()
    }

    fun addScanFolder(path: String) {
        if (!scanFolders.contains(path)) {
            scanFolders.add(path)
            repository.updateScanFolders(scanFolders.toList())
            saveCurrentSettings()
        }
    }

    fun removeScanFolder(path: String) {
        scanFolders.remove(path)
        repository.updateScanFolders(scanFolders.toList())
        saveCurrentSettings()
    }

    fun saveCurrentSettings() {
        settingsRepository.saveSettings(
            AppSettings(
                isDarkMode = isDarkMode,
                isMaximized = isMaximized,
                windowWidth = windowWidth,
                windowHeight = windowHeight,
                volume = VideoManager.currentVolume,
                shortcuts = shortcuts,
                scanFolders = scanFolders.toList(),
                projectionFontSize = projectionFontSize,
                projectionFontFamily = projectionFontFamily,
                projectionFontColor = projectionFontColor
            )
        )
    }

    fun handleKeyEvent(keyCode: Int, ctrl: Boolean, alt: Boolean, shift: Boolean): Boolean {
        val shortcutEntry = shortcuts.entries.find { (_, config) ->
            config.keyCode == keyCode && config.ctrl == ctrl && config.alt == alt && config.shift == shift
        }

        if (shortcutEntry != null) {
            when (shortcutEntry.key) {
                "Reproducir/Pausa" -> VideoManager.togglePlayPause()
                "Detener" -> stopVideo()
                "Siguiente" -> playNext()
                "Anterior" -> playPrevious()
                "Alternar Sidebar" -> isSidebarExpanded = !isSidebarExpanded
                "Alternar Proyección" -> toggleProjection()
                "Silenciar" -> VideoManager.toggleMute(!VideoManager.isMuted())
                "Adelantar 5s" -> VideoManager.skip(5000)
                "Retroceder 5s" -> VideoManager.skip(-5000)
                "Subir Volumen" -> VideoManager.updateVolume(VideoManager.currentVolume + 5)
                "Bajar Volumen" -> VideoManager.updateVolume(VideoManager.currentVolume - 5)
                "Siguiente Capítulo" -> nextBibleChapter()
                "Capítulo Anterior" -> previousBibleChapter()
                "Siguiente Libro" -> nextBibleBook()
                "Libro Anterior" -> previousBibleBook()
            }
            return true
        }
        return false
    }

    fun updateShortcut(name: String, config: ShortcutConfig) {
        val newShortcuts = shortcuts.toMutableMap()
        newShortcuts[name] = config
        shortcuts = newShortcuts
        saveCurrentSettings()
    }

    private var bibleSearchJob: Job? = null

    fun onBibleSearchQueryChanged(query: String) {
        bibleSearchQuery = query
        bibleSearchJob?.cancel()

        if (query.length < 3) {
            bibleSearchResults = emptyList()
            isBibleSearching = false
            return
        }

        bibleSearchJob = scope.launch(Dispatchers.Default) {
            isBibleSearching = true
            val version = bibleVersion
            if (version != null) {
                val results = bibleRepository.search(version, query)
                withContext(Dispatchers.Main) {
                    bibleSearchResults = results
                }
            }
            isBibleSearching = false
        }
    }

    fun navigateToSearchResult(result: BibleRepository.SearchResult) {
        val book = bibleVersion?.books?.find { it.book_usfm == result.bookUsfm }
        if (book != null) {
            selectedBibleBook = book
            val chapter = book.chapters.find { it.chapter_usfm == result.chapterUsfm }
            if (chapter != null) {
                selectedBibleChapter = chapter
                selectedVerses.clear()
                selectedVerses.addAll(result.verseNumbers)
            }
        }
        // Limpiar búsqueda al navegar
        bibleSearchQuery = ""
        bibleSearchResults = emptyList()
    }
}
