package com.geraciodev.lumina.util

import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle

data class ScreenInfo(
    val index: Int,
    val isPrimary: Boolean,
    val bounds: Rectangle
)

/** Enumera las pantallas físicas conectadas ahora mismo (vía AWT), en el mismo orden e índices
 * que usa [resolveProjectionScreen], para que lo que se ve en Ajustes coincida con lo que
 * realmente se usa al proyectar. */
fun detectScreens(): List<ScreenInfo> {
    val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val primary = graphicsEnvironment.defaultScreenDevice
    return graphicsEnvironment.screenDevices.mapIndexed { index, device ->
        ScreenInfo(
            index = index,
            isPrimary = device == primary,
            bounds = device.defaultConfiguration.bounds
        )
    }
}

/**
 * Resuelve qué pantalla física usar para la ventana de proyección.
 *
 * Si [preferredIndex] sigue apuntando a una pantalla actualmente conectada, se usa esa.
 * Si no hay preferencia guardada, o la pantalla elegida ya no está conectada (se desconectó el
 * monitor/proyector), se recurre a la primera pantalla que no sea la principal; si solo hay una
 * pantalla conectada, no hay dónde proyectar por separado y se devuelve `null`.
 */
fun resolveProjectionScreen(preferredIndex: Int?): GraphicsDevice? {
    val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    if (preferredIndex != null && preferredIndex in devices.indices) {
        return devices[preferredIndex]
    }
    return if (devices.size > 1) devices[1] else null
}
