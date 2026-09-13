package com.geraciodev.lumina.ui.filepicker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

enum class FilePickerViewMode { LIST, GRID }

/**
 * Recuerda qué vista (lista o cuadrícula) usó el usuario por última vez en el selector de
 * archivos, para que la próxima vez que lo abra —en cualquier pantalla— aparezca con esa misma
 * vista. Se persiste en su propio archivo, independiente de `settings.json`, ya que es una
 * preferencia de un componente reutilizable y no de la app en su conjunto.
 */
object FilePickerPreferences {
    private val prefsFile = File(File(System.getProperty("user.home"), ".lumina"), "filepicker_prefs.txt")

    var viewMode: FilePickerViewMode by mutableStateOf(loadViewMode())
        private set

    fun selectViewMode(mode: FilePickerViewMode) {
        viewMode = mode
        try {
            prefsFile.parentFile?.mkdirs()
            prefsFile.writeText(mode.name)
        } catch (e: Exception) {
            // Preferencia no crítica: si no se pudo persistir, sigue funcionando en memoria.
        }
    }

    private fun loadViewMode(): FilePickerViewMode {
        return try {
            if (prefsFile.exists()) FilePickerViewMode.valueOf(prefsFile.readText().trim())
            else FilePickerViewMode.LIST
        } catch (e: Exception) {
            FilePickerViewMode.LIST
        }
    }
}
