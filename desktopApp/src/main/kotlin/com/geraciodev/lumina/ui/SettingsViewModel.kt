package com.geraciodev.lumina.ui

import androidx.compose.runtime.*
import com.geraciodev.lumina.data.SettingsRepository
import com.geraciodev.lumina.data.model.AppSettings
import com.geraciodev.lumina.data.model.ShortcutConfig
import com.geraciodev.lumina.data.model.defaultShortcuts
import com.geraciodev.lumina.player.VideoManager

/** Preferencias persistentes: ventana, apariencia, atajos y ajustes de proyección. */
class SettingsViewModel(private val settingsRepository: SettingsRepository) {
    var isDarkMode by mutableStateOf(true)
        private set
    var isMaximized by mutableStateOf(false)
    var windowWidth by mutableStateOf(1200)
    var windowHeight by mutableStateOf(850)
    var shortcuts by mutableStateOf(defaultShortcuts)
        private set
    var recordingShortcutName by mutableStateOf<String?>(null)
    val scanFolders = mutableStateListOf<String>()

    // Ajustes de proyección
    var projectionFontSize by mutableStateOf(48)
    var projectionFontFamily by mutableStateOf("Inter")
    var projectionFontColor by mutableStateOf(0xFFFFFFFFL)
    var projectionBackgroundImage by mutableStateOf<String?>(null)
        private set
    var projectionBackgroundOpacity by mutableStateOf(0.55f)
    var projectionScreenIndex by mutableStateOf<Int?>(null)
        private set

    // Enumerar las fuentes del sistema es costoso; se difiere hasta que la pantalla de Ajustes
    // la pida realmente, en lugar de pagar ese costo en cada arranque de la app.
    val availableSystemFonts: List<String> by lazy {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList()
    }

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
        projectionBackgroundImage = settings.projectionBackgroundImage
        projectionBackgroundOpacity = settings.projectionBackgroundOpacity
        projectionScreenIndex = settings.projectionScreenIndex
        VideoManager.updateVolume(settings.volume)
    }

    /** Selecciona (o quita, con `null`) la imagen de fondo para la proyección bíblica. */
    fun selectProjectionBackgroundImage(path: String?) {
        projectionBackgroundImage = path
        saveCurrentSettings()
    }

    /** Elige qué pantalla física usar para la ventana de proyección (`null` = automático). */
    fun selectProjectionScreen(index: Int?) {
        projectionScreenIndex = index
        saveCurrentSettings()
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        saveCurrentSettings()
    }

    /** Devuelve `true` si la carpeta era nueva (y por lo tanto hay que re-escanear la biblioteca). */
    fun addScanFolder(path: String): Boolean {
        if (scanFolders.contains(path)) return false
        scanFolders.add(path)
        saveCurrentSettings()
        return true
    }

    fun removeScanFolder(path: String) {
        scanFolders.remove(path)
        saveCurrentSettings()
    }

    fun updateShortcut(name: String, config: ShortcutConfig) {
        val newShortcuts = shortcuts.toMutableMap()
        newShortcuts[name] = config
        shortcuts = newShortcuts
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
                projectionFontColor = projectionFontColor,
                projectionBackgroundImage = projectionBackgroundImage,
                projectionBackgroundOpacity = projectionBackgroundOpacity,
                projectionScreenIndex = projectionScreenIndex
            )
        )
    }
}
