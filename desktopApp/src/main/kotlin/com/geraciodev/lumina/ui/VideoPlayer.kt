package com.geraciodev.lumina.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geraciodev.lumina.player.VideoManager
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayer(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    showControls: Boolean = false,
    isAudioOnly: Boolean = false
) {
    val frame = VideoManager.videoFrame

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isAudioOnly) {
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = "Video Frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando video o sin señal...", color = Color.White)
                }
            }
        }

        if (showControls) {
            VideoControls(
                playerViewModel = playerViewModel,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoControls(playerViewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    val isPlaying = VideoManager.isPlaying
    val position = VideoManager.currentPosition
    val currentTime = VideoManager.currentTime
    val duration = VideoManager.duration
    val audioTracks = VideoManager.audioTracks
    val subtitleTracks = VideoManager.subtitleTracks
    val currentAudio = VideoManager.currentAudioTrack
    val currentSubs = VideoManager.currentSubtitleTrack
    val playbackMode = playerViewModel.playbackMode

    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubsMenu by remember { mutableStateOf(false) }

    var hoverTime by remember { mutableStateOf<Long?>(null) }
    var hoverX by remember { mutableStateOf(0f) }
    var sliderWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Seek bar with Hover Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp) // Aumentamos altura para el tooltip
                    .onGloballyPositioned { sliderWidthPx = it.size.width }
                    .onPointerEvent(PointerEventType.Move) { event ->
                        val x = event.changes.first().position.x
                        if (sliderWidthPx > 0 && duration > 0) {
                            val ratio = (x / sliderWidthPx).coerceIn(0f, 1f)
                            hoverTime = (ratio * duration).toLong()
                            hoverX = x
                        }
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        hoverTime = null
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Tooltip de tiempo
                hoverTime?.let { time ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                val tooltipWidth = with(density) { 60.dp.toPx() }
                                val offsetX = (hoverX - tooltipWidth / 2).coerceIn(0f, sliderWidthPx - tooltipWidth)
                                IntOffset(offsetX.roundToInt(), 0)
                            },
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = formatTime(time),
                            color = Color.Black,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                val seekBarColors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
                val seekBarInteractionSource = remember { MutableInteractionSource() }
                Slider(
                    value = position,
                    onValueChange = { VideoManager.seekTo(it) },
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = seekBarColors,
                    interactionSource = seekBarInteractionSource,
                    // Mismo thumb circular y compacto que el control de volumen, en vez del
                    // pill fino y alargado por defecto de Material3.
                    thumb = { sliderState ->
                        SliderDefaults.Thumb(
                            interactionSource = seekBarInteractionSource,
                            sliderState = sliderState,
                            colors = seekBarColors,
                            thumbSize = DpSize(18.dp, 18.dp)
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playerViewModel.playPrevious() }) {
                        Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White)
                    }

                    IconButton(onClick = { VideoManager.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { playerViewModel.stopVideo() }) {
                        Icon(Icons.Filled.Stop, "Detener", tint = Color.White)
                    }

                    IconButton(onClick = { playerViewModel.playNext() }) {
                        Icon(Icons.Filled.SkipNext, "Siguiente", tint = Color.White)
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "${formatTime(currentTime)} / ${formatTime(duration)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.width(16.dp))

                    // Botón de Modo de Reproducción
                    IconButton(onClick = { playerViewModel.togglePlaybackMode() }) {
                        val icon = when (playbackMode) {
                            PlaybackMode.LOOP_ONE -> Icons.Default.RepeatOne
                            PlaybackMode.LOOP_ALL -> Icons.Default.Repeat
                            PlaybackMode.SEQUENCE -> Icons.AutoMirrored.Filled.List
                        }
                        Icon(icon, contentDescription = "Modo Reproducción", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    Text(
                        text = when(playbackMode) {
                            PlaybackMode.SEQUENCE -> "SEQ"
                            PlaybackMode.LOOP_ALL -> "ALL"
                            PlaybackMode.LOOP_ONE -> "ONE"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Tracks Menu
                    Box {
                        IconButton(onClick = { 
                            VideoManager.updateTracks()
                            showAudioMenu = true 
                        }) {
                            Icon(Icons.Filled.Audiotrack, "Audio", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showAudioMenu,
                            onDismissRequest = { showAudioMenu = false }
                        ) {
                            if (audioTracks.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay pistas de audio") },
                                    onClick = { showAudioMenu = false },
                                    enabled = false
                                )
                            }
                            audioTracks.forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track.description() ?: "Pista ${track.id()}") },
                                    onClick = {
                                        VideoManager.setAudioTrack(track.id())
                                        showAudioMenu = false
                                    },
                                    trailingIcon = {
                                        if (track.id() == currentAudio) {
                                            Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Subtitles Menu
                    Box {
                        IconButton(onClick = { 
                            VideoManager.updateTracks()
                            showSubsMenu = true 
                        }) {
                            Icon(Icons.Filled.Subtitles, "Subtítulos", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showSubsMenu,
                            onDismissRequest = { showSubsMenu = false }
                        ) {
                            // Opción para desactivar subtítulos
                            DropdownMenuItem(
                                text = { Text("Desactivar") },
                                onClick = {
                                    VideoManager.setSubtitleTrack(-1)
                                    showSubsMenu = false
                                },
                                trailingIcon = {
                                    if (currentSubs == -1) {
                                        Text("✓", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                            if (subtitleTracks.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay subtítulos") },
                                    onClick = { showSubsMenu = false },
                                    enabled = false
                                )
                            }
                            subtitleTracks.forEach { track ->
                                DropdownMenuItem(
                                    text = { Text(track.description() ?: "Pista ${track.id()}") },
                                    onClick = {
                                        VideoManager.setSubtitleTrack(track.id())
                                        showSubsMenu = false
                                    },
                                    trailingIcon = {
                                        if (track.id() == currentSubs) {
                                            Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Control de volumen: botón de mute (con indicador visual real) + slider
                    // más ancho para que sea más fácil de manipular con precisión, además de
                    // soporte para la rueda del mouse.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isMuted = VideoManager.isMuted
                        IconButton(
                            onClick = { VideoManager.toggleMute(!VideoManager.isMuted) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isMuted || VideoManager.currentVolume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                                    VideoManager.currentVolume < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                                    else -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                                tint = if (isMuted) MaterialTheme.colorScheme.error else Color.White
                            )
                        }
                        val volumeThumbInteractionSource = remember { MutableInteractionSource() }
                        Slider(
                            value = VideoManager.currentVolume.toFloat(),
                            onValueChange = { VideoManager.updateVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            interactionSource = volumeThumbInteractionSource,
                            // El thumb "pill" por defecto de Material3 es muy fino y alargado
                            // para agarrarlo con el mouse; lo cambiamos por uno circular y más
                            // grande, más cómodo de manipular.
                            thumb = { sliderState ->
                                SliderDefaults.Thumb(
                                    interactionSource = volumeThumbInteractionSource,
                                    sliderState = sliderState,
                                    thumbSize = DpSize(18.dp, 18.dp)
                                )
                            },
                            modifier = Modifier
                                .width(140.dp)
                                .onPointerEvent(PointerEventType.Scroll) { event ->
                                    val delta = event.changes.first().scrollDelta.y
                                    if (delta != 0f) {
                                        VideoManager.updateVolume(VideoManager.currentVolume - (delta * 5).toInt())
                                    }
                                }
                        )
                        Text(
                            text = "${VideoManager.currentVolume}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(34.dp).padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
