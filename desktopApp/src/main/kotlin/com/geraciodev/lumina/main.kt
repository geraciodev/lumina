package com.geraciodev.lumina

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.geraciodev.lumina.data.VideoSearchRepository
import com.geraciodev.lumina.data.PlaylistRepository
import com.geraciodev.lumina.data.BibleRepository
import com.geraciodev.lumina.data.GalleryRepository
import androidx.compose.ui.input.key.*
import com.geraciodev.lumina.data.SettingsRepository
import com.geraciodev.lumina.data.model.ShortcutConfig
import com.geraciodev.lumina.player.VideoManager
import com.geraciodev.lumina.ui.MainScreen
import com.geraciodev.lumina.ui.MainViewModel
import com.geraciodev.lumina.ui.VideoPlayer
import com.geraciodev.lumina.ui.WindowTitleBar
import com.geraciodev.lumina.ui.theme.LuminaTheme
import com.geraciodev.lumina.ui.bible.BibleProjectionView
import com.geraciodev.lumina.util.resolveProjectionScreen

fun main() = application {
    val scope = rememberCoroutineScope()
    val repository = remember { VideoSearchRepository() }
    val playlistRepository = remember { PlaylistRepository() }
    val bibleRepository = remember { BibleRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val galleryRepository = remember { GalleryRepository() }
    val viewModel = remember {
        MainViewModel(repository, playlistRepository, bibleRepository, settingsRepository, galleryRepository, scope)
    }
    
    val windowState = rememberWindowState(
        placement = if (viewModel.settings.isMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = WindowPosition.PlatformDefault,
        width = viewModel.settings.windowWidth.dp,
        height = viewModel.settings.windowHeight.dp
    )

    // Ventana Principal
    Window(
        onCloseRequest = {
            // Guardar estado actual antes de cerrar
            viewModel.settings.isMaximized = windowState.placement == WindowPlacement.Maximized
            if (!viewModel.settings.isMaximized) {
                viewModel.settings.windowWidth = windowState.size.width.value.toInt()
                viewModel.settings.windowHeight = windowState.size.height.value.toInt()
            }
            viewModel.settings.saveCurrentSettings()
            VideoManager.release()
            exitApplication()
        },
        title = "Lumina - Reproductor Multimedia",
        state = windowState,
        undecorated = true,
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                val recordingName = viewModel.settings.recordingShortcutName
                if (recordingName != null) {
                    val code = keyEvent.key.nativeKeyCode
                    // Al grabar un atajo con modificador (p. ej. Ctrl+B), Ctrl dispara su propio
                    // KeyDown antes que "B". Si grabáramos ese primer evento tal cual, el atajo
                    // quedaría asociado a "solo Ctrl" en vez de "Ctrl+B". Esperamos a que la
                    // tecla presionada no sea, en sí misma, un modificador.
                    if (code !in modifierOnlyKeyCodes) {
                        viewModel.settings.updateShortcut(
                            recordingName,
                            ShortcutConfig(
                                keyCode = code,
                                ctrl = keyEvent.isCtrlPressed,
                                alt = keyEvent.isAltPressed,
                                shift = keyEvent.isShiftPressed
                            )
                        )
                        viewModel.settings.recordingShortcutName = null
                    }
                    true
                } else if (viewModel.isAnyInputFocused) {
                    false
                } else {
                    viewModel.handleKeyEvent(
                        keyCode = keyEvent.key.nativeKeyCode,
                        ctrl = keyEvent.isCtrlPressed,
                        alt = keyEvent.isAltPressed,
                        shift = keyEvent.isShiftPressed
                    )
                }
            } else false
        }
    ) {
        LuminaTheme(darkTheme = viewModel.settings.isDarkMode) {
            Column(modifier = Modifier.fillMaxSize()) {
                WindowTitleBar(
                    title = "Lumina - Reproductor Multimedia",
                    isMaximized = windowState.placement == WindowPlacement.Maximized,
                    onMinimize = { windowState.isMinimized = true },
                    onMaximize = {
                        windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    onClose = {
                        viewModel.settings.isMaximized = windowState.placement == WindowPlacement.Maximized
                        if (!viewModel.settings.isMaximized) {
                            viewModel.settings.windowWidth = windowState.size.width.value.toInt()
                            viewModel.settings.windowHeight = windowState.size.height.value.toInt()
                        }
                        viewModel.settings.saveCurrentSettings()
                        VideoManager.release()
                        exitApplication()
                    }
                )
                MainScreen(viewModel)
            }
        }
    }

    // Ventana de Proyección (Segunda Pantalla)
    val projectingFile = viewModel.player.projectingFile
    val projectingBibleVerses = viewModel.bible.projectingBibleVerses
    
    if (projectingFile != null || projectingBibleVerses != null) {
        val secondScreen = resolveProjectionScreen(viewModel.settings.projectionScreenIndex)
        val bounds = secondScreen?.defaultConfiguration?.bounds

        val windowState = rememberWindowState(
            position = if (bounds != null) WindowPosition(bounds.x.dp, bounds.y.dp) else WindowPosition.PlatformDefault,
            width = if (bounds != null) bounds.width.dp else 1280.dp,
            height = if (bounds != null) bounds.height.dp else 720.dp
        )

        Window(
            onCloseRequest = { viewModel.stopProjection() },
            title = "Proyección Lumina",
            state = windowState,
            undecorated = secondScreen != null,
            alwaysOnTop = secondScreen != null
        ) {
            // Evitar que la ventana de proyección robe el foco de la ventana principal
            LaunchedEffect(Unit) {
                window.focusableWindowState = false
            }

            if (projectingFile != null) {
                VideoPlayer(
                    playerViewModel = viewModel.player,
                    modifier = Modifier.fillMaxSize(),
                    isAudioOnly = viewModel.player.isAudioOnly
                )
            } else if (projectingBibleVerses != null) {
                BibleProjectionView(
                    verses = projectingBibleVerses,
                    reference = viewModel.bible.projectingBibleRef,
                    fontSize = viewModel.settings.projectionFontSize,
                    fontFamily = viewModel.settings.projectionFontFamily,
                    fontColor = viewModel.settings.projectionFontColor,
                    backgroundImagePath = viewModel.settings.projectionBackgroundImage,
                    backgroundOpacity = viewModel.settings.projectionBackgroundOpacity
                )
            }
        }
    }
}

private val Int.dp: Dp get() = Dp(this.toFloat())

// Códigos AWT VK_SHIFT, VK_CONTROL, VK_ALT, VK_META y VK_ALT_GRAPH: teclas que son en sí mismas
// modificadores, para no grabarlas como el atajo en sí al capturar un atajo con combinación.
private val modifierOnlyKeyCodes = setOf(16, 17, 18, 157, 65406)
