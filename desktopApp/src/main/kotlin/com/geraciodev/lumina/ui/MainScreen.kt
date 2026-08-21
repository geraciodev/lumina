package com.geraciodev.lumina.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.geraciodev.lumina.ui.theme.LuminaTheme
import com.geraciodev.lumina.ui.playlist.PlaylistScreen
import com.geraciodev.lumina.ui.bible.BibleScreen
import com.geraciodev.lumina.ui.settings.SettingsScreen
import com.geraciodev.lumina.ui.about.AboutScreen

@Composable
fun MainScreen(viewModel: MainViewModel) {
    LuminaTheme(darkTheme = viewModel.isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Barra de Navegación Lateral (Sidebar)
                NavigationSidebar(
                    currentScreen = viewModel.currentScreen,
                    isExpanded = viewModel.isSidebarExpanded,
                    onScreenSelected = { viewModel.currentScreen = it },
                    onExpandToggle = { viewModel.isSidebarExpanded = !viewModel.isSidebarExpanded }
                )

                // Separador vertical minimalista
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                // Área de Contenido Principal
                Box(modifier = Modifier.weight(1f)) {
                    when (viewModel.currentScreen) {
                        LuminaScreen.SEARCH -> SearchAndPlayerView(viewModel)
                        LuminaScreen.PLAYLIST -> PlaylistScreen(viewModel)
                        LuminaScreen.BIBLE -> BibleScreen(viewModel)
                        LuminaScreen.SETTINGS -> SettingsScreen(viewModel)
                        LuminaScreen.ABOUT -> AboutScreen()
                    }
                }

                // Ventana Flotante de Playlist (Mini Sidebar)
                if (viewModel.isPlaylistWindowOpen && (viewModel.selectedPlaylist != null || viewModel.showRecentInOverlay)) {
                    PlaylistOverlay(viewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationSidebar(
    currentScreen: LuminaScreen,
    isExpanded: Boolean,
    onScreenSelected: (LuminaScreen) -> Unit,
    onExpandToggle: () -> Unit
) {
    val width by animateDpAsState(targetValue = if (isExpanded) 200.dp else 80.dp)

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(vertical = 24.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo y Botón Expandir
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isExpanded) "LUMINA" else "L",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = if (isExpanded) 2.sp else 0.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            IconButton(
                onClick = onExpandToggle,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Expandir Sidebar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            // Items Superiores
            NavIconItem(
                icon = Icons.Default.Search,
                label = "BUSCADOR",
                isSelected = currentScreen == LuminaScreen.SEARCH,
                isExpanded = isExpanded
            ) {
                onScreenSelected(LuminaScreen.SEARCH)
            }
            NavIconItem(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = "PLAYLISTS",
                isSelected = currentScreen == LuminaScreen.PLAYLIST,
                isExpanded = isExpanded
            ) {
                onScreenSelected(LuminaScreen.PLAYLIST)
            }
            NavIconItem(
                icon = Icons.Default.Book,
                label = "BIBLIA",
                isSelected = currentScreen == LuminaScreen.BIBLE,
                isExpanded = isExpanded
            ) {
                onScreenSelected(LuminaScreen.BIBLE)
            }
        }

        // Items Inferiores
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            NavIconItem(
                icon = Icons.Default.Info,
                label = "ACERCA DE",
                isSelected = currentScreen == LuminaScreen.ABOUT,
                isExpanded = isExpanded
            ) {
                onScreenSelected(LuminaScreen.ABOUT)
            }
            NavIconItem(
                icon = Icons.Default.Settings,
                label = "AJUSTES",
                isSelected = currentScreen == LuminaScreen.SETTINGS,
                isExpanded = isExpanded
            ) {
                onScreenSelected(LuminaScreen.SETTINGS)
            }
        }
    }
}

@Composable
fun NavIconItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isExpanded) 16.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(4.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
        }

        if (isExpanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun SearchAndPlayerView(viewModel: MainViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar de búsqueda (El que ya teníamos)
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .padding(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 24.dp)
        ) {
            Text(
                text = "BUSCADOR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar archivos...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                trailingIcon = {
                    if (viewModel.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.searchResults) { file ->
                    val isSelected = viewModel.selectedVideo == file
                    ListItem(
                        headlineContent = {
                            Text(
                                file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                file.parentFile.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .clickable { viewModel.selectVideo(file) }
                            .padding(vertical = 2.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                    )
                }
            }
        }

        // Main Content - Reproductor
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
                .padding(start = 12.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            if (viewModel.selectedVideo != null) {
                if (viewModel.isAudioOnly) {
                    // Vista específica para Audio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(48.dp))
                            VideoControls(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Vista de Video
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
                    ) {
                        VideoPlayer(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            showControls = true,
                            isAudioOnly = false
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.selectedVideo?.name ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = viewModel.selectedVideo?.absolutePath ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (!viewModel.isAudioOnly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.selectedPlaylist != null || viewModel.showRecentInOverlay) {
                                IconButton(
                                    onClick = { viewModel.isPlaylistWindowOpen = !viewModel.isPlaylistWindowOpen },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = "Ver Playlist",
                                        tint = if (viewModel.isPlaylistWindowOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.toggleProjection() },
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (viewModel.projectingFile != null) "DETENER PROYECCIÓN" else "PROYECTAR",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else if (viewModel.selectedPlaylist != null || viewModel.showRecentInOverlay) {
                        IconButton(
                            onClick = { viewModel.isPlaylistWindowOpen = !viewModel.isPlaylistWindowOpen }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = "Ver Playlist",
                                tint = if (viewModel.isPlaylistWindowOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SELECCIONA UN ARCHIVO PARA COMENZAR",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistOverlay(viewModel: MainViewModel) {
    val items = if (viewModel.showRecentInOverlay) viewModel.recentFiles else viewModel.selectedPlaylist?.items ?: emptyList()
    val title = if (viewModel.showRecentInOverlay) "RECIENTES" else viewModel.selectedPlaylist?.name ?: ""
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { viewModel.isPlaylistWindowOpen = false }) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { item ->
                    val isPlaying = viewModel.selectedVideo?.absolutePath == item.filePath
                    ListItem(
                        headlineContent = {
                            Text(
                                item.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier.clickable {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                // Si estamos en modo recientes, mantenemos el modo recientes al seleccionar
                                viewModel.selectVideo(file, if (viewModel.showRecentInOverlay) null else viewModel.selectedPlaylist)
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isPlaying) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        ),
                        leadingContent = {
                            Icon(
                                imageVector = if (item.fileName.endsWith(".mp3") || item.fileName.endsWith(".wav"))
                                    Icons.Default.MusicNote else Icons.Default.Movie,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            letterSpacing = 8.sp
        )
    }
}
