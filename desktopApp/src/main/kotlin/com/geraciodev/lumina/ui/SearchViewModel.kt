package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.VideoSearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Búsqueda incremental de archivos multimedia sobre las carpetas configuradas. */
class SearchViewModel(
    private val repository: VideoSearchRepository,
    private val scope: CoroutineScope
) {
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf(emptyList<File>())
        private set
    var isSearching by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    fun updateScanFolders(folders: List<String>) {
        repository.updateScanFolders(folders)
    }

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
}
