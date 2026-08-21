package com.geraciodev.lumina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val isDarkMode: Boolean = true,
    val isMaximized: Boolean = false,
    val windowWidth: Int = 1200,
    val windowHeight: Int = 850,
    val volume: Int = 100,
    val shortcuts: Map<String, ShortcutConfig> = defaultShortcuts,
    val scanFolders: List<String> = emptyList(),
    val projectionFontSize: Int = 48,
    val projectionFontFamily: String = "Inter",
    val projectionFontColor: Long = 0xFFFFFFFFL // White
)

@Serializable
data class ShortcutConfig(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false
)

val defaultShortcuts = mapOf(
    "Reproducir/Pausa" to ShortcutConfig(32), // Space
    "Detener" to ShortcutConfig(83),          // S
    "Siguiente" to ShortcutConfig(78),        // N
    "Anterior" to ShortcutConfig(80),         // P
    "Alternar Sidebar" to ShortcutConfig(66, ctrl = true), // Ctrl + B
    "Alternar Proyección" to ShortcutConfig(70, ctrl = true), // Ctrl + F
    "Silenciar" to ShortcutConfig(77),        // M
    "Adelantar 5s" to ShortcutConfig(39),     // Right Arrow
    "Retroceder 5s" to ShortcutConfig(37),    // Left Arrow
    "Subir Volumen" to ShortcutConfig(38),    // Up Arrow
    "Bajar Volumen" to ShortcutConfig(40),     // Down Arrow
    "Siguiente Capítulo" to ShortcutConfig(39, ctrl = true), // Ctrl + Right Arrow
    "Capítulo Anterior" to ShortcutConfig(37, ctrl = true),  // Ctrl + Left Arrow
    "Siguiente Libro" to ShortcutConfig(40, ctrl = true),    // Ctrl + Down Arrow
    "Libro Anterior" to ShortcutConfig(38, ctrl = true)      // Ctrl + Up Arrow
)
