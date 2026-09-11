package com.geraciodev.lumina.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.geraciodev.lumina.data.model.Playlist
import com.geraciodev.lumina.ui.MainViewModel
import java.io.File
import javax.swing.JFileChooser

@Composable
fun PlaylistScreen(viewModel: MainViewModel) {
    val focusManager = LocalFocusManager.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistSearchQuery by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    viewModel.isAnyInputFocused = false
                }
            }
    ) {
        // Lista de Playlists (Sidebar de Playlists)
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYLISTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Playlist")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.playlists) { playlist ->
                    val isSelected = viewModel.selectedPlaylist?.id == playlist.id
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.items.size} archivos") },
                        modifier = Modifier.clickable { viewModel.selectedPlaylist = playlist },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        ),
                        trailingContent = {
                            IconButton(onClick = { viewModel.deletePlaylist(playlist) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                }
            }
        }

        // Separador vertical
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        )

        // Contenido de la Playlist seleccionada
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            val selected = viewModel.selectedPlaylist
            if (selected != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected.name.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row {
                        IconButton(onClick = { 
                            val files = pickFiles()
                            if (files.isNotEmpty()) viewModel.addFilesToPlaylist(selected, files)
                        }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Añadir archivos")
                        }
                        IconButton(onClick = { 
                            val folder = pickFolder()
                            if (folder != null) {
                                val files = viewModel.loadFilesFromFolder(folder)
                                viewModel.addFilesToPlaylist(selected, files)
                            }
                        }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Añadir carpeta")
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                TextField(
                    value = playlistSearchQuery,
                    onValueChange = { playlistSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { viewModel.isAnyInputFocused = it.isFocused },
                    placeholder = { Text("Buscar en la playlist...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    },
                    trailingIcon = {
                        if (playlistSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { playlistSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(16.dp))

                val filteredItems = selected.items.filter { item ->
                    playlistSearchQuery.isBlank() ||
                        item.fileName.contains(playlistSearchQuery, ignoreCase = true) ||
                        item.filePath.contains(playlistSearchQuery, ignoreCase = true)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems) { item ->
                        ListItem(
                            headlineContent = { Text(item.fileName) },
                            supportingContent = { Text(item.filePath, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.clickable { 
                                val file = File(item.filePath)
                                if (file.exists()) {
                                    viewModel.selectVideo(file, selected)
                                    viewModel.currentScreen = com.geraciodev.lumina.ui.LuminaScreen.SEARCH
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (item.fileName.endsWith(".mp3") || item.fileName.endsWith(".wav")) 
                                        Icons.Default.MusicNote else Icons.Default.Movie,
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeItemFromPlaylist(selected, item) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Quitar de la playlist", modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("SELECCIONA O CREA UNA PLAYLIST", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, files ->
                viewModel.createPlaylist(name, files)
                showCreateDialog = false
            },
            onFocusChanged = { viewModel.isAnyInputFocused = it }
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, List<File>) -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf(emptyList<File>()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(400.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("NUEVA PLAYLIST", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la playlist") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedFiles = pickFiles() },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("ARCHIVOS")
                    }
                    Button(
                        onClick = { 
                            val folder = pickFolder()
                            if (folder != null) {
                                selectedFiles = folder.listFiles()?.filter { it.isFile } ?: emptyList()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("CARPETA")
                    }
                }
                if (selectedFiles.isNotEmpty()) {
                    Text(
                        "${selectedFiles.size} archivos seleccionados",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCELAR") }
                    Button(
                        onClick = { if (name.isNotBlank()) onCreate(name, selectedFiles) },
                        enabled = name.isNotBlank(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("CREAR")
                    }
                }
            }
        }
    }
}

private fun pickFiles(): List<File> {
    val chooser = JFileChooser().apply {
        isMultiSelectionEnabled = true
        fileSelectionMode = JFileChooser.FILES_ONLY
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFiles.toList()
    } else emptyList()
}

private fun pickFolder(): File? {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else null
}
