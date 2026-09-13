package com.geraciodev.lumina.player

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.geraciodev.lumina.util.decodeScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.media.MediaParsedStatus
import uk.co.caprica.vlcj.media.Meta
import uk.co.caprica.vlcj.media.ParseFlag
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * Genera y cachea en disco miniaturas para archivos multimedia: un frame del video (capturado
 * fuera de pantalla, sin abrir ninguna ventana) o la carátula embebida en archivos de audio,
 * cuando el archivo la tiene.
 *
 * Usa una [MediaPlayerFactory] propia, separada de la de [VideoManager], para que cualquier
 * fallo generando miniaturas nunca pueda afectar la reproducción principal.
 */
object ThumbnailManager {
    private val cacheDir = File(System.getProperty("user.home"), ".lumina/thumbnails").apply { mkdirs() }

    // Capturar un frame de video crea un reproductor real (aunque sin ventana visible); lo
    // serializamos para no tener varias instancias nativas compitiendo a la vez. La carátula de
    // audio solo lee metadata, así que esa sí se permite en paralelo (acotada).
    private val videoCaptureMutex = Mutex()
    private val audioParseLimit = Semaphore(4)

    private val factory: MediaPlayerFactory by lazy {
        MediaPlayerFactory(listOf("--no-video-title-show", "--avcodec-hw=none", "--quiet"))
    }

    private fun cacheFile(source: File): File {
        val key = "${source.absolutePath}|${source.lastModified()}|${source.length()}".hashCode()
        return File(cacheDir, "$key.png")
    }

    suspend fun getThumbnail(source: File, isAudio: Boolean): ImageBitmap? {
        if (!source.exists()) return null

        val cached = cacheFile(source)
        readCached(cached)?.let { return it }

        val image = withTimeoutOrNull(8000) {
            if (isAudio) {
                audioParseLimit.withPermit { extractEmbeddedArtwork(source) }
            } else {
                videoCaptureMutex.withLock { captureVideoFrame(source) }
            }
        } ?: return null

        withContext(Dispatchers.IO) {
            try {
                ImageIO.write(image, "png", cached)
            } catch (e: Exception) {
                // Sin caché persistente, igual devolvemos la imagen generada esta vez.
            }
        }
        return image.toComposeImageBitmap()
    }

    private suspend fun readCached(file: File): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            ImageIO.read(file)?.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun extractEmbeddedArtwork(source: File): BufferedImage? {
        val media = try {
            withContext(Dispatchers.IO) { factory.media().newMedia(source.absolutePath) }
        } catch (e: Exception) {
            return null
        }
        return try {
            // El parseo de VLC es SIEMPRE asíncrono (parse() solo confirma que arrancó, no que
            // terminó): hay que esperar el evento mediaParsedChanged antes de poder leer
            // metadata, si no Meta.ARTWORK_URL se lee vacío porque VLC todavía no la extrajo.
            if (!awaitParsed(media)) return null
            val artworkUrl = withContext(Dispatchers.IO) { media.meta().get(Meta.ARTWORK_URL) } ?: return null
            val artworkFile = try {
                File(URI(artworkUrl))
            } catch (e: Exception) {
                null
            } ?: return null
            withContext(Dispatchers.IO) {
                if (artworkFile.exists()) decodeScaledBitmap(artworkFile) else null
            }
        } catch (e: Exception) {
            null
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                try { media.release() } catch (e: Exception) { }
            }
        }
    }

    /**
     * FETCH_LOCAL es indispensable además de PARSE_LOCAL: sin ella, VLC nunca extrae la
     * carátula embebida a un archivo temporal y Meta.ARTWORK_URL queda vacío aunque el archivo
     * sí tenga portada.
     */
    private suspend fun awaitParsed(media: Media): Boolean = suspendCancellableCoroutine { cont ->
        var listener: MediaEventAdapter? = null
        listener = object : MediaEventAdapter() {
            override fun mediaParsedChanged(media: Media, newStatus: MediaParsedStatus) {
                listener?.let { media.events().removeMediaEventListener(it) }
                if (cont.isActive) cont.resume(newStatus == MediaParsedStatus.DONE) { _, _, _ -> }
            }
        }
        media.events().addMediaEventListener(listener)
        cont.invokeOnCancellation {
            try { media.events().removeMediaEventListener(listener) } catch (e: Exception) { }
        }

        val started = try {
            media.parsing().parse(6000, ParseFlag.PARSE_LOCAL, ParseFlag.FETCH_LOCAL)
        } catch (e: Exception) {
            false
        }
        if (!started) {
            media.events().removeMediaEventListener(listener)
            if (cont.isActive) cont.resume(false) { _, _, _ -> }
        }
    }

    /**
     * Reproduce el archivo fuera de pantalla (sin ventana), avanza un poco y captura el
     * siguiente frame renderizado. Reutiliza el mismo mecanismo de [CallbackVideoSurface] que ya
     * usa [VideoManager] para dibujar cada frame en un [BufferedImage].
     */
    private suspend fun captureVideoFrame(source: File): BufferedImage? = withContext(Dispatchers.IO) {
        var player: EmbeddedMediaPlayer? = null
        try {
            val latch = CountDownLatch(1)
            var seekPosition = 0f
            var captured: BufferedImage? = null

            val mp = factory.mediaPlayers().newEmbeddedMediaPlayer()
            player = mp

            mp.videoSurface().set(
                CallbackVideoSurface(
                    object : BufferFormatCallback {
                        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat =
                            RV32BufferFormat(sourceWidth, sourceHeight)
                        override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {}
                    },
                    object : RenderCallback {
                        override fun display(
                            mediaPlayer: MediaPlayer,
                            nativeBuffers: Array<out ByteBuffer>,
                            bufferFormat: BufferFormat
                        ) {
                            // Esperamos a que el seek haya avanzado lo suficiente para no
                            // capturar un frame negro/inicial.
                            if (captured != null || seekPosition < 0.05f) return
                            val width = bufferFormat.getWidth()
                            val height = bufferFormat.getHeight()
                            if (width <= 0 || height <= 0) return
                            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                            val pixels = (image.raster.dataBuffer as DataBufferInt).data
                            nativeBuffers[0].asIntBuffer().get(pixels)
                            captured = image
                            latch.countDown()
                        }
                    },
                    true,
                    VideoSurfaceAdapters.getVideoSurfaceAdapter()
                )
            )

            mp.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer?) {
                    mediaPlayer?.controls()?.setPosition(0.15f)
                }

                override fun positionChanged(mediaPlayer: MediaPlayer?, newPosition: Float) {
                    seekPosition = newPosition
                }
            })

            mp.media().play(source.absolutePath, ":no-audio")
            latch.await(6, TimeUnit.SECONDS)
            captured
        } catch (e: Exception) {
            null
        } finally {
            try {
                player?.controls()?.stop()
            } catch (e: Exception) {
            }
            try {
                player?.release()
            } catch (e: Exception) {
            }
        }
    }
}
