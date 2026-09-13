package com.geraciodev.lumina.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.media.MediaSlaveType
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.TrackDescription
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
import java.nio.ByteBuffer
import javax.swing.SwingUtilities

object VideoManager {
    private var factory: MediaPlayerFactory? = null
    var mediaPlayer: EmbeddedMediaPlayer? = null
        private set

    var videoFrame by mutableStateOf<ImageBitmap?>(null)
        private set

    // Playback States
    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableStateOf(0f) // 0.0 to 1.0
        private set
    var currentTime by mutableStateOf(0L) // milliseconds
        private set
    var duration by mutableStateOf(0L) // milliseconds
        private set
    var currentVolume by mutableStateOf(100)
        private set
    var isMuted by mutableStateOf(false)
        private set

    var onVideoFinished: (() -> Unit)? = null

    // Track States
    var audioTracks by mutableStateOf<List<TrackDescription>>(emptyList())
        private set
    var subtitleTracks by mutableStateOf<List<TrackDescription>>(emptyList())
        private set
    var currentAudioTrack by mutableStateOf(-1)
        private set
    var currentSubtitleTrack by mutableStateOf(-1)
        private set

    private var bufferImage: BufferedImage? = null

    private fun updateUiState(update: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            update()
        } else {
            SwingUtilities.invokeLater(update)
        }
    }

    init {
        NativeDiscovery().discover()

        // Seleccionar el backend de audio según el sistema operativo.
        // En builds empaquetadas, VLC no siempre auto-detecta el aout correctamente.
        val os = System.getProperty("os.name", "").lowercase()
        val aoutArg = when {
            os.contains("win")   -> "--aout=directsound"
            os.contains("mac")   -> "--aout=auhal"
            else                 -> "--aout=pulse"   // Linux / otros UNIX
        }

        val factoryArgs = listOf(
            "--no-video-title-show",
            "--avcodec-hw=none",      // Crucial para CallbackVideoSurface
            "--no-stats",
            "--no-snapshot-preview",
            "--clock-jitter=0",
            "--clock-synchro=0",
            "--no-skip-frames",
            aoutArg
        )

        factory = MediaPlayerFactory(factoryArgs)
        mediaPlayer = factory?.mediaPlayers()?.newEmbeddedMediaPlayer()

        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer?) {
                val mediaDuration = mediaPlayer?.status()?.length() ?: 0L
                // Reafirmar volumen/mute explícitamente: el audio output que crea VLC para
                // cada nuevo medio puede arrancar con su propio volumen por defecto en vez de
                // heredar el nuestro, lo que se sentía como que el sonido "se muteaba solo".
                mediaPlayer?.audio()?.setVolume(currentVolume)
                mediaPlayer?.audio()?.setMute(isMuted)
                updateUiState {
                    isPlaying = true
                    duration = mediaDuration
                    updateTracks()
                }
            }

            override fun audioDeviceChanged(mediaPlayer: MediaPlayer?, audioDevice: String?) {
                // Un cambio de dispositivo de audio (p. ej. PulseAudio reasignando el sink)
                // puede resetear el volumen/mute a nivel nativo sin avisar; lo reafirmamos.
                mediaPlayer?.audio()?.setVolume(currentVolume)
                mediaPlayer?.audio()?.setMute(isMuted)
                updateUiState { updateTracks() }
            }

            override fun paused(mediaPlayer: MediaPlayer?) {
                updateUiState { isPlaying = false }
            }

            override fun stopped(mediaPlayer: MediaPlayer?) {
                updateUiState {
                    isPlaying = false
                    currentPosition = 0f
                    currentTime = 0L
                    clearTracks()
                }
            }

            override fun positionChanged(mediaPlayer: MediaPlayer?, newPosition: Float) {
                val time = mediaPlayer?.status()?.time() ?: 0L
                updateUiState {
                    currentPosition = newPosition
                    currentTime = time
                }
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                updateUiState { duration = newLength }
            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                updateUiState {
                    isPlaying = false
                    onVideoFinished?.invoke()
                }
            }

            // Nota: deliberadamente NO escuchamos volumeChanged() para reflejar su valor en
            // currentVolume. VLC dispara ese evento con valores transitorios y sin acotar al
            // iniciar cada medio nuevo (a veces 0, a veces >100% si algo mezcla el volumen del
            // stream con el del sistema), y adoptarlos causaba que el volumen apareciera en 0
            // al abrir un video o saltara por encima de 100%. currentVolume es responsabilidad
            // exclusiva de esta app (updateVolume/toggleMute); nunca se sincroniza desde VLC.
        })

        val renderCallback = object : RenderCallback {
            override fun display(
                mediaPlayer: MediaPlayer,
                nativeBuffers: Array<out ByteBuffer>,
                bufferFormat: BufferFormat
            ) {
                val buffer = nativeBuffers[0]
                val width = bufferFormat.getWidth()
                val height = bufferFormat.getHeight()

                if (bufferImage == null || bufferImage!!.width != width || bufferImage!!.height != height) {
                    bufferImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                }

                val pixels = (bufferImage!!.raster.dataBuffer as DataBufferInt).data
                buffer.asIntBuffer().get(pixels)

                val imageBitmap = bufferImage!!.toComposeImageBitmap()
                updateUiState { videoFrame = imageBitmap }
            }
        }

        val bufferFormatCallback = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
            }
        }

        mediaPlayer?.videoSurface()?.set(
            CallbackVideoSurface(
                bufferFormatCallback,
                renderCallback,
                true,
                VideoSurfaceAdapters.getVideoSurfaceAdapter()
            )
        )
    }

    fun play(file: File, isAudioOnly: Boolean = false) {
        if (isAudioOnly) {
            mediaPlayer?.media()?.play(file.absolutePath, ":no-video")
        } else {
            mediaPlayer?.media()?.play(file.absolutePath)
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer?.status()?.isPlaying == true) {
            mediaPlayer?.controls()?.pause()
        } else {
            mediaPlayer?.controls()?.play()
        }
    }

    fun seekTo(position: Float) {
        mediaPlayer?.controls()?.setPosition(position)
    }

    fun skip(millis: Long) {
        mediaPlayer?.controls()?.skipTime(millis)
    }

    fun updateVolume(value: Int) {
        currentVolume = value.coerceIn(0, 100)
        mediaPlayer?.audio()?.setVolume(currentVolume)
    }

    fun stop() {
        mediaPlayer?.controls()?.stop()
        videoFrame = null
    }

    fun toggleMute(muted: Boolean) {
        isMuted = muted
        mediaPlayer?.audio()?.setMute(muted)
    }


    fun release() {
        onVideoFinished = null
        mediaPlayer?.release()
        mediaPlayer = null
        factory?.release()
        factory = null
        bufferImage = null
        videoFrame = null
        clearTracks()
    }

    // Gestión de Pistas de Audio y Subtítulos
    fun updateTracks() {
        audioTracks = mediaPlayer?.audio()?.trackDescriptions() ?: emptyList()
        subtitleTracks = mediaPlayer?.subpictures()?.trackDescriptions() ?: emptyList()
        currentAudioTrack = mediaPlayer?.audio()?.track() ?: -1
        currentSubtitleTrack = mediaPlayer?.subpictures()?.track() ?: -1
    }

    private fun clearTracks() {
        audioTracks = emptyList()
        subtitleTracks = emptyList()
        currentAudioTrack = -1
        currentSubtitleTrack = -1
    }

    fun setAudioTrack(id: Int) {
        mediaPlayer?.audio()?.setTrack(id)
        currentAudioTrack = id
    }

    fun setSubtitleTrack(id: Int) {
        mediaPlayer?.subpictures()?.setTrack(id)
        currentSubtitleTrack = id
    }

    fun cycleAudioTrack() {
        val tracks = audioTracks
        if (tracks.size > 1) {
            val currentIndex = tracks.indexOfFirst { it.id() == currentAudioTrack }
            val nextIndex = (currentIndex + 1) % tracks.size
            setAudioTrack(tracks[nextIndex].id())
        }
    }

    fun cycleSubtitleTrack() {
        val tracks = subtitleTracks
        if (tracks.size > 1) {
            val currentIndex = tracks.indexOfFirst { it.id() == currentSubtitleTrack }
            val nextIndex = (currentIndex + 1) % tracks.size
            setSubtitleTrack(tracks[nextIndex].id())
        }
    }

    /**
     * Carga un archivo de subtítulos externo (.srt, .ass, etc.)
     */
    fun loadExternalSubtitle(file: File) {
        mediaPlayer?.media()?.addSlave(MediaSlaveType.SUBTITLE, file.absolutePath, true)
        updateTracks()
    }
}
