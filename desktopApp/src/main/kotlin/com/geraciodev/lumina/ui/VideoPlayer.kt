package com.geraciodev.lumina.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geraciodev.lumina.player.VideoManager
import kotlin.math.floor

@Composable
fun VideoPlayer(
    viewModel: MainViewModel,
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
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
            )
        }
    }
}

@Composable
fun VideoControls(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isPlaying = VideoManager.isPlaying
    val position = VideoManager.currentPosition
    val currentTime = VideoManager.currentTime
    val duration = VideoManager.duration
    val audioTracks = VideoManager.audioTracks
    val subtitleTracks = VideoManager.subtitleTracks
    val currentAudio = VideoManager.currentAudioTrack
    val currentSubs = VideoManager.currentSubtitleTrack
    val playbackMode = viewModel.playbackMode

    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubsMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Seek bar
            Slider(
                value = position,
                onValueChange = { VideoManager.seekTo(it) },
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White)
                    }

                    IconButton(onClick = { VideoManager.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { viewModel.stopVideo() }) {
                        Icon(Icons.Filled.Stop, "Detener", tint = Color.White)
                    }

                    IconButton(onClick = { viewModel.playNext() }) {
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
                    IconButton(onClick = { viewModel.togglePlaybackMode() }) {
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

                    // Volume slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vol: ", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = VideoManager.currentVolume.toFloat(),
                            onValueChange = { VideoManager.updateVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = floor(totalSeconds.toDouble() / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return "%02d:%02d".format(minutes, seconds)
}
