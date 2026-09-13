package com.geraciodev.lumina.util

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Decodifica [file] como miniatura acotada a [targetSize] píxeles de lado. Fotos de cámara o
 * wallpapers pueden pesar varios megapíxeles; decodificarlos a resolución completa solo para
 * mostrarlos en una celda pequeña gasta memoria y CPU de forma innecesaria. Usamos el
 * subsampling del propio decodificador de ImageIO para que ya decodifique menos píxeles en vez
 * de decodificar todo y recién ahí achicar.
 */
fun decodeScaledBitmap(file: File, targetSize: Int = 256): BufferedImage? {
    if (!file.exists()) return null
    return try {
        ImageIO.createImageInputStream(file).use { input ->
            if (input == null) return null
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            try {
                reader.input = input
                val originalWidth = reader.getWidth(0)
                val originalHeight = reader.getHeight(0)
                val subsampling = (minOf(originalWidth, originalHeight) / targetSize).coerceAtLeast(1)
                val param = reader.defaultReadParam.apply {
                    if (subsampling > 1) setSourceSubsampling(subsampling, subsampling, 0, 0)
                }
                downscaleIfNeeded(reader.read(0, param), targetSize)
            } finally {
                reader.dispose()
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun downscaleIfNeeded(image: BufferedImage, targetSize: Int): BufferedImage {
    if (image.width <= targetSize && image.height <= targetSize) return image
    val ratio = targetSize.toFloat() / maxOf(image.width, image.height)
    val newWidth = (image.width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (image.height * ratio).toInt().coerceAtLeast(1)
    val scaled = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
    val g = scaled.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(image, 0, 0, newWidth, newHeight, null)
    g.dispose()
    return scaled
}
