package com.geraciodev.lumina

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.geraciodev.lumina.data.VideoSearchRepository
import com.geraciodev.lumina.data.PlaylistRepository
import com.geraciodev.lumina.data.BibleRepository
import androidx.compose.ui.input.key.*
import com.geraciodev.lumina.data.SettingsRepository
import com.geraciodev.lumina.data.model.ShortcutConfig
import com.geraciodev.lumina.ui.MainScreen
import com.geraciodev.lumina.ui.MainViewModel
import com.geraciodev.lumina.ui.VideoPlayer
import com.geraciodev.lumina.ui.bible.BibleProjectionView
import java.awt.GraphicsEnvironment

fun main() = application {
    val scope = rememberCoroutineScope()
    val repository = remember { VideoSearchRepository() }
    val playlistRepository = remember { PlaylistRepository() }
    val bibleRepository = remember { BibleRepository() }
    val settingsRepository = remember { SettingsRepository() }
    val viewModel = remember { 
        MainViewModel(repository, playlistRepository, bibleRepository, settingsRepository, scope) 
    }
    
    val windowState = rememberWindowState(
        placement = if (viewModel.isMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = WindowPosition.PlatformDefault,
        width = viewModel.windowWidth.dp,
        height = viewModel.windowHeight.dp
    )

    // Ventana Principal
    Window(
        onCloseRequest = {
            // Guardar estado actual antes de cerrar
            viewModel.isMaximized = windowState.placement == WindowPlacement.Maximized
            if (!viewModel.isMaximized) {
                viewModel.windowWidth = windowState.size.width.value.toInt()
                viewModel.windowHeight = windowState.size.height.value.toInt()
            }
            viewModel.saveCurrentSettings()
            exitApplication()
        },
        title = "Lumina - Reproductor Multimedia",
        state = windowState,
        onPreviewKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                val recordingName = viewModel.recordingShortcutName
                if (recordingName != null) {
                    viewModel.updateShortcut(
                        recordingName,
                        ShortcutConfig(
                            keyCode = keyEvent.key.nativeKeyCode,
                            ctrl = keyEvent.isCtrlPressed,
                            alt = keyEvent.isAltPressed,
                            shift = keyEvent.isShiftPressed
                        )
                    )
                    viewModel.recordingShortcutName = null
                    true
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
        MainScreen(viewModel)
    }

    // Ventana de Proyección (Segunda Pantalla)
    val projectingFile = viewModel.projectingFile
    val projectingBibleVerses = viewModel.projectingBibleVerses
    
    if (projectingFile != null || projectingBibleVerses != null) {
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        val secondScreen = if (screens.size > 1) screens[1] else null
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
            if (projectingFile != null) {
                VideoPlayer(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    isAudioOnly = viewModel.isAudioOnly
                )
            } else if (projectingBibleVerses != null) {
                BibleProjectionView(
                    verses = projectingBibleVerses,
                    reference = viewModel.projectingBibleRef,
                    fontSize = viewModel.projectionFontSize,
                    fontFamily = viewModel.projectionFontFamily,
                    fontColor = viewModel.projectionFontColor
                )
            }
        }
    }
}

private val Int.dp: Dp get() = Dp(this.toFloat())
