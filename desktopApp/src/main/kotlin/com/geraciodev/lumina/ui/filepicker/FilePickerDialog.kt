package com.geraciodev.lumina.ui.filepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geraciodev.lumina.ui.MediaThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

enum class FilePickerMode { FILES, FOLDER }

private enum class SearchScope { SYSTEM, FOLDER }

private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "gif", "webp")
private val audioExtensions = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "mpg", "mpeg")
private val fontExtensions = setOf("ttf", "otf")

/**
 * Lumina es un reproductor multimedia con soporte de proyección, no un explorador de archivos
 * genérico: cuando una pantalla no pide un filtro de extensión más específico (p. ej. solo
 * imágenes para la galería), igual restringimos a estas categorías en vez de mostrar
 * absolutamente cualquier archivo del sistema.
 */
private val defaultPickerExtensions = imageExtensions + audioExtensions + videoExtensions + fontExtensions

private const val MAX_SYSTEM_SEARCH_RESULTS = 300
private val systemSearchSkipNames = setOf(
    "proc", "sys", "dev", "run", "var", "tmp", "boot", "etc", "usr", "bin", "sbin",
    "lib", "lib64", "appdata", "library", "node_modules",
    "system volume information", "\$recycle.bin"
)

/**
 * Explorador de archivos propio de Lumina, con la estética monocromática de la app, para
 * reemplazar el selector nativo del sistema operativo (que desentonaba visualmente).
 *
 * La búsqueda tiene dos modos, alternables con un switch: "sistema" (recorre el equipo entero,
 * activo por defecto) y "carpeta" (filtra solo lo que ya está listado en el directorio actual).
 *
 * @param mode FILES para elegir uno o varios archivos, FOLDER para elegir la carpeta actual.
 * @param extensionFilter en modo FILES, qué extensiones listar/buscar (las carpetas siempre se
 * muestran para poder navegar). Si es null, se usa [defaultPickerExtensions] (video, audio,
 * imagen y tipografía) en vez de aceptar cualquier archivo del sistema.
 */
@Composable
fun FilePickerDialog(
    title: String,
    mode: FilePickerMode,
    allowMultiple: Boolean = false,
    extensionFilter: Set<String>? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<File>) -> Unit
) {
    val homeDir = remember { File(System.getProperty("user.home")) }
    var currentDir by remember { mutableStateOf(homeDir) }
    var entries by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val selected = remember { mutableStateListOf<File>() }
    var searchScope by remember { mutableStateOf(SearchScope.SYSTEM) }

    val quickAccess = remember(homeDir) { buildQuickAccess(homeDir) }
    val systemSearchRootsList = remember(homeDir) { buildSystemSearchRoots(homeDir) }
    val effectiveExtensionFilter = extensionFilter ?: defaultPickerExtensions

    LaunchedEffect(currentDir, mode) {
        isLoading = true
        entries = withContext(Dispatchers.IO) {
            val all = currentDir.listFiles()?.filter { !it.isHidden } ?: emptyList()
            val filtered = when (mode) {
                FilePickerMode.FOLDER -> all.filter { it.isDirectory }
                FilePickerMode.FILES -> all.filter { file ->
                    file.isDirectory || effectiveExtensionFilter.contains(file.extension.lowercase())
                }
            }
            filtered.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
        }
        isLoading = false
    }

    fun navigateTo(dir: File) {
        currentDir = dir
        selected.clear()
        searchScope = SearchScope.FOLDER
    }

    fun onEntryClick(file: File) {
        if (file.isDirectory) {
            navigateTo(file)
        } else if (mode == FilePickerMode.FILES) {
            val isSelected = selected.contains(file)
            if (allowMultiple) {
                if (isSelected) selected.remove(file) else selected.add(file)
            } else {
                selected.clear()
                selected.add(file)
            }
        }
    }

    // Búsqueda: se reinicia al navegar a otra carpeta, pero persiste al alternar el switch.
    var searchQuery by remember(currentDir) { mutableStateOf("") }
    var systemSearchResults by remember { mutableStateOf<List<File>>(emptyList()) }
    var isSystemSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchScope, searchQuery) {
        if (searchScope != SearchScope.SYSTEM) return@LaunchedEffect
        if (searchQuery.length < 2) {
            systemSearchResults = emptyList()
            isSystemSearching = false
            return@LaunchedEffect
        }
        delay(300)
        isSystemSearching = true
        systemSearchResults = searchSystemWide(systemSearchRootsList, searchQuery, mode, effectiveExtensionFilter)
        isSystemSearching = false
    }

    val filteredFolderEntries = remember(entries, searchQuery, mode) {
        if (searchQuery.isBlank()) {
            entries
        } else {
            val matches = entries.filter { it.name.contains(searchQuery, ignoreCase = true) }
            // Al buscar (no al simplemente navegar) en modo FILES, solo interesan archivos:
            // mezclar carpetas en los resultados de búsqueda no aporta nada seleccionable.
            if (mode == FilePickerMode.FILES) matches.filter { !it.isDirectory } else matches
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.width(920.dp).height(640.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (extensionFilter != null) {
                    Text(
                        text = "Filtro: ${extensionFilter.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Barra de ruta actual
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { currentDir.parentFile?.let { navigateTo(it) } },
                        enabled = currentDir.parentFile != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Subir un nivel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = currentDir.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Búsqueda: por defecto a nivel de sistema; el switch cambia a solo esta carpeta.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).height(52.dp),
                        placeholder = {
                            Text(
                                text = if (searchScope == SearchScope.SYSTEM) "Buscar en todo el sistema..." else "Buscar en esta carpeta...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (isSystemSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp), strokeWidth = 2.dp)
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
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (searchScope == SearchScope.SYSTEM) "SISTEMA" else "CARPETA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = searchScope == SearchScope.SYSTEM,
                            onCheckedChange = { checked -> searchScope = if (checked) SearchScope.SYSTEM else SearchScope.FOLDER }
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { FilePickerPreferences.selectViewMode(FilePickerViewMode.LIST) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Vista de lista",
                                tint = if (FilePickerPreferences.viewMode == FilePickerViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { FilePickerPreferences.selectViewMode(FilePickerViewMode.GRID) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Vista de cuadrícula",
                                tint = if (FilePickerPreferences.viewMode == FilePickerViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Accesos rápidos
                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .fillMaxHeight()
                    ) {
                        quickAccess.forEach { (dir, label) ->
                            val isCurrent = searchScope == SearchScope.FOLDER && dir == currentDir
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navigateTo(dir) }
                                    .background(if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    )

                    // Listado: resultados de búsqueda global o contenido de la carpeta actual
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 12.dp)
                    ) {
                        if (searchScope == SearchScope.SYSTEM) {
                            when {
                                searchQuery.length < 2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "ESCRIBE AL MENOS 2 CARACTERES\nPARA BUSCAR EN TODO EL EQUIPO",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                isSystemSearching && systemSearchResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                                systemSearchResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("SIN RESULTADOS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                }
                                else -> FileResultsView(
                                    files = systemSearchResults,
                                    selected = selected,
                                    showSubtitle = true,
                                    onClick = ::onEntryClick
                                )
                            }
                        } else {
                            when {
                                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                }
                                filteredFolderEntries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (searchQuery.isBlank()) "CARPETA VACÍA" else "SIN RESULTADOS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                else -> FileResultsView(
                                    files = filteredFolderEntries,
                                    selected = selected,
                                    showSubtitle = false,
                                    onClick = ::onEntryClick
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            FilePickerMode.FOLDER -> ""
                            FilePickerMode.FILES -> if (selected.isNotEmpty()) "${selected.size} seleccionado(s)" else ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        TextButton(onClick = onDismiss) { Text("CANCELAR") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                when (mode) {
                                    FilePickerMode.FOLDER -> onConfirm(listOf(currentDir))
                                    FilePickerMode.FILES -> onConfirm(selected.toList())
                                }
                            },
                            enabled = mode == FilePickerMode.FOLDER || selected.isNotEmpty(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (mode == FilePickerMode.FOLDER) "SELECCIONAR ESTA CARPETA" else "SELECCIONAR")
                        }
                    }
                }
            }
        }
    }
}

/** Lista o cuadrícula de resultados, según [FilePickerPreferences.viewMode]. */
@Composable
private fun FileResultsView(
    files: List<File>,
    selected: List<File>,
    showSubtitle: Boolean,
    onClick: (File) -> Unit
) {
    if (FilePickerPreferences.viewMode == FilePickerViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(files, key = { it.absolutePath }) { file ->
                FileGridTile(
                    file = file,
                    isSelected = selected.contains(file),
                    onClick = { onClick(file) }
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(files, key = { it.absolutePath }) { file ->
                FileEntryRow(
                    file = file,
                    isSelected = selected.contains(file),
                    subtitle = if (showSubtitle) file.parentFile?.absolutePath else null,
                    onClick = { onClick(file) }
                )
            }
        }
    }
}

@Composable
private fun FileGridTile(file: File, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            MediaThumbnail(file, modifier = Modifier.fillMaxSize())
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun FileEntryRow(file: File, isSelected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ext = file.extension.lowercase()
        Icon(
            imageVector = when {
                file.isDirectory -> Icons.Default.Folder
                ext in imageExtensions -> Icons.Default.Image
                ext in audioExtensions -> Icons.Default.MusicNote
                ext in videoExtensions -> Icons.Default.Movie
                ext in fontExtensions -> Icons.Default.FontDownload
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun buildQuickAccess(homeDir: File): List<Pair<File, String>> {
    val namedFolders = listOf(
        "Escritorio" to listOf("Escritorio", "Desktop"),
        "Documentos" to listOf("Documentos", "Documents"),
        "Descargas" to listOf("Descargas", "Downloads"),
        "Imágenes" to listOf("Imágenes", "Pictures"),
        "Música" to listOf("Música", "Music"),
        "Vídeos" to listOf("Vídeos", "Videos")
    )
    return buildList {
        add(homeDir to "Inicio")
        namedFolders.forEach { (label, candidateNames) ->
            candidateNames.map { File(homeDir, it) }
                .firstOrNull { it.exists() && it.isDirectory }
                ?.let { add(it to label) }
        }
        File.listRoots().forEach { root ->
            add(root to root.absolutePath)
        }
    }
}

private fun buildSystemSearchRoots(homeDir: File): List<File> {
    val roots = mutableSetOf(homeDir)
    val user = System.getProperty("user.name")
    listOf("/media", "/media/$user", "/mnt", "/Volumes", "/run/media", "/run/media/$user").forEach { mountPoint ->
        val dir = File(mountPoint)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { sub ->
                if (sub.isDirectory && !sub.isHidden) roots.add(sub.absoluteFile)
            }
        }
    }
    return roots.filter { it.exists() && it.canRead() }.distinctBy { it.absolutePath }
}

/** Recorre [roots] buscando archivos/carpetas cuyo nombre contenga [query], acotado a
 * [MAX_SYSTEM_SEARCH_RESULTS] resultados y evitando directorios de sistema (proc, node_modules, etc.). */
private suspend fun searchSystemWide(
    roots: List<File>,
    query: String,
    mode: FilePickerMode,
    extensionFilter: Set<String>
): List<File> = withContext(Dispatchers.IO) {
    val normalizedQuery = query.lowercase()
    val results = mutableListOf<File>()

    for (root in roots) {
        if (results.size >= MAX_SYSTEM_SEARCH_RESULTS) break
        try {
            Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (results.size >= MAX_SYSTEM_SEARCH_RESULTS) return FileVisitResult.TERMINATE
                    val dirFile = dir.toFile()
                    val name = dirFile.name
                    if (dirFile != root) {
                        if (dirFile.isHidden || systemSearchSkipNames.contains(name.lowercase())) {
                            return FileVisitResult.SKIP_SUBTREE
                        }
                        // En modo FILES la búsqueda solo debe devolver archivos: una carpeta
                        // encontrada no es algo seleccionable en ese modo.
                        if (mode == FilePickerMode.FOLDER && name.lowercase().contains(normalizedQuery)) {
                            results.add(dirFile)
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (results.size >= MAX_SYSTEM_SEARCH_RESULTS) return FileVisitResult.TERMINATE
                    if (mode == FilePickerMode.FILES) {
                        val f = file.toFile()
                        if (!f.isHidden && f.name.lowercase().contains(normalizedQuery) &&
                            extensionFilter.contains(f.extension.lowercase())
                        ) {
                            results.add(f)
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.SKIP_SUBTREE
            })
        } catch (_: Exception) {
            // Una carpeta inaccesible no debe interrumpir la búsqueda restante.
        }
    }

    results.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
}
