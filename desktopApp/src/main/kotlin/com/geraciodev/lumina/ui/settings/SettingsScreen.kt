package com.geraciodev.lumina.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.geraciodev.lumina.ui.MainViewModel
import com.geraciodev.lumina.ui.filepicker.FilePickerDialog
import com.geraciodev.lumina.ui.filepicker.FilePickerMode
import java.awt.event.KeyEvent
import java.io.File

private val fontFileExtensions = setOf("ttf", "otf")

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    var showScanFolderPicker by remember { mutableStateOf(false) }
    var showFontFilePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "AJUSTES",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSection(title = "BIBLIOTECA") {
                    SettingsItem(
                        title = "Carpetas de escaneo",
                        description = "Gestionar directorios donde Lumina busca archivos multimedia.",
                        action = {
                            Button(
                                onClick = { showScanFolderPicker = true },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("AÑADIR CARPETA")
                            }
                        }
                    )
                    
                    if (viewModel.settings.scanFolders.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        viewModel.settings.scanFolders.forEach { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeScanFolder(path) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Usando raíces predeterminadas del sistema.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "APARIENCIA") {
                    SettingsItem(
                        title = "Modo Oscuro",
                        description = "Forzar tema oscuro en toda la aplicación.",
                        action = {
                            Switch(
                                checked = viewModel.settings.isDarkMode,
                                onCheckedChange = { viewModel.settings.toggleDarkMode(it) }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "PROYECCIÓN") {
                    SettingsItem(
                        title = "Monitor de salida",
                        description = "Seleccionar en qué pantalla se mostrará la proyección.",
                        action = {
                            Text("Monitor Secundario (Detectado)", style = MaterialTheme.typography.bodySmall)
                        }
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    SettingsItem(
                        title = "Tamaño de fuente",
                        description = "Ajustar el tamaño del texto en la proyección (${viewModel.settings.projectionFontSize}sp).",
                        action = {
                            Slider(
                                value = viewModel.settings.projectionFontSize.toFloat(),
                                onValueChange = { viewModel.settings.projectionFontSize = it.toInt() },
                                onValueChangeFinished = { viewModel.settings.saveCurrentSettings() },
                                valueRange = 20f..120f,
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    )

                    var fontMenuExpanded by remember { mutableStateOf(false) }
                    var fontSearchQuery by remember(viewModel.settings.projectionFontFamily) {
                        val file = File(viewModel.settings.projectionFontFamily)
                        mutableStateOf(if (file.exists() && file.isFile) file.name else viewModel.settings.projectionFontFamily)
                    }

                    SettingsItem(
                        title = "Tipografía",
                        description = "Seleccionar fuente del sistema o cargar archivo (.ttf, .otf).",
                        action = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box {
                                    OutlinedTextField(
                                        value = fontSearchQuery,
                                        onValueChange = {
                                            fontSearchQuery = it
                                            fontMenuExpanded = true
                                        },
                                        modifier = Modifier
                                            .width(240.dp)
                                            .onFocusChanged { 
                                                if (it.isFocused) fontMenuExpanded = true 
                                                viewModel.isAnyInputFocused = it.isFocused
                                            },
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = true,
                                        shape = RectangleShape,
                                        placeholder = { Text("Buscar fuente...", style = MaterialTheme.typography.bodySmall) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (fontSearchQuery.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = {
                                                            fontSearchQuery = ""
                                                            fontMenuExpanded = true
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { fontMenuExpanded = !fontMenuExpanded },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowDropDown, null)
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = fontMenuExpanded,
                                        onDismissRequest = { fontMenuExpanded = false },
                                        properties = PopupProperties(focusable = false),
                                        modifier = Modifier.heightIn(max = 400.dp).width(240.dp)
                                    ) {
                                        val filtered = viewModel.settings.availableSystemFonts.filterNotNull().filter {
                                            it.contains(fontSearchQuery, ignoreCase = true)
                                        }.take(50)

                                        if (filtered.isEmpty() && fontSearchQuery.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Sin resultados", style = MaterialTheme.typography.bodySmall) },
                                                onClick = { }
                                            )
                                        } else {
                                            filtered.forEach { fontName ->
                                                DropdownMenuItem(
                                                    text = { Text(fontName, style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        viewModel.settings.projectionFontFamily = fontName
                                                        fontSearchQuery = fontName
                                                        viewModel.settings.saveCurrentSettings()
                                                        fontMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { showFontFilePicker = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Cargar fuente desde archivo")
                                }
                            }
                        }
                    )

                    SettingsItem(
                        title = "Color de Texto",
                        description = "Cambiar el color del texto proyectado.",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val colors = listOf(
                                    0xFFFFFFFFL to "Blanco",
                                    0xFFFFFF00L to "Amarillo",
                                    0xFF00FF00L to "Verde",
                                    0xFF00FFFFL to "Cian"
                                )
                                colors.forEach { (colorValue, _) ->
                                    Canvas(
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.settings.projectionFontColor = colorValue
                                                viewModel.settings.saveCurrentSettings()
                                            }
                                            .size(24.dp)
                                            .border(
                                                width = 1.dp,
                                                color = if (viewModel.settings.projectionFontColor == colorValue)
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                shape = RectangleShape
                                            )
                                            .padding(2.dp)
                                    ) {
                                        drawRect(Color(colorValue))
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "ATAJOS DE TECLADO") {
                    viewModel.settings.shortcuts.forEach { (name, config) ->
                        val isRecording = viewModel.settings.recordingShortcutName == name
                        SettingsItem(
                            title = name,
                            description = if (isRecording) "Presiona una tecla..." else formatShortcut(config),
                            action = {
                                Button(
                                    onClick = { viewModel.settings.recordingShortcutName = name },
                                    colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(if (isRecording) Icons.Default.Edit else Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isRecording) "ESCUCHANDO..." else "CAMBIAR")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showScanFolderPicker) {
        FilePickerDialog(
            title = "Añadir carpeta de escaneo",
            mode = FilePickerMode.FOLDER,
            onDismiss = { showScanFolderPicker = false },
            onConfirm = { folders ->
                folders.firstOrNull()?.let { viewModel.addScanFolder(it.absolutePath) }
                showScanFolderPicker = false
            }
        )
    }
    if (showFontFilePicker) {
        FilePickerDialog(
            title = "Elegir archivo de fuente",
            mode = FilePickerMode.FILES,
            extensionFilter = fontFileExtensions,
            onDismiss = { showFontFilePicker = false },
            onConfirm = { files ->
                files.firstOrNull()?.let { file ->
                    viewModel.settings.projectionFontFamily = file.absolutePath
                    viewModel.settings.saveCurrentSettings()
                }
                showFontFilePicker = false
            }
        )
    }
}

private fun formatShortcut(config: com.geraciodev.lumina.data.model.ShortcutConfig): String {
    val sb = StringBuilder()
    if (config.ctrl) sb.append("Ctrl + ")
    if (config.alt) sb.append("Alt + ")
    if (config.shift) sb.append("Shift + ")
    
    val keyName = KeyEvent.getKeyText(config.keyCode)
    sb.append(keyName)
    
    return sb.toString()
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Box(modifier = Modifier.padding(start = 16.dp)) {
            action()
        }
    }
}
