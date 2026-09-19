package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.TrackSourceType
import com.example.data.ZeroNoiseItem
import com.example.ui.MainViewModel
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RawNoiseOrange
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceCyanGlow
import com.example.ui.theme.ZeroNoiseGreen
import com.example.ui.theme.ZeroNoiseGreenGlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks by viewModel.savedTracks.collectAsState()

    var activePlayingTrackId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPlayback() {
        activePlayingTrackId = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    fun playTrack(track: ZeroNoiseItem) {
        if (activePlayingTrackId == track.id) {
            stopPlayback()
            return
        }
        stopPlayback()
        try {
            val file = File(track.filePath)
            if (file.exists()) {
                val mp = MediaPlayer()
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                mp.start()
                mp.setOnCompletionListener {
                    stopPlayback()
                }
                mediaPlayer = mp
                activePlayingTrackId = track.id
            } else {
                viewModel.showMessage("Audio file not found on device.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showMessage("Playback error.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback()
        }
    }

    fun shareTrack(track: ZeroNoiseItem) {
        val file = File(track.filePath)
        if (!file.exists()) {
            viewModel.showMessage("File not found")
            return
        }
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Clean Audio"))
        } catch (e: Exception) {
            viewModel.showMessage("Share error: ${e.message}")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
            .testTag("library_screen")
    ) {
        // Top Header
        Column {
            Text(
                text = "CLEAN AUDIO VAULT",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Persisted pristine zero-noise recordings & video audio",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioCardBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StudioCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = VoiceCyanGlow,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Vault is Empty",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Record clean voice audio from the Live Mic tab or export zero-noise video audio to view your pristine recordings here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tracks, key = { it.id }) { track ->
                    val isPlaying = activePlayingTrackId == track.id
                    TrackCard(
                        track = track,
                        isPlaying = isPlaying,
                        onPlayToggle = { playTrack(track) },
                        onDownload = {
                            val f = File(track.filePath)
                            val isVideo = track.sourceType == TrackSourceType.VIDEO_AUDIO
                            viewModel.downloadFileToDevice(
                                file = f,
                                suggestedName = "${track.title.replace(" ", "_")}_${track.id}",
                                isVideo = isVideo
                            )
                        },
                        onShare = { shareTrack(track) },
                        onDelete = {
                            if (isPlaying) stopPlayback()
                            viewModel.deleteTrack(track)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackCard(
    track: ZeroNoiseItem,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(track.timestamp) { dateFormat.format(Date(track.timestamp)) }
    val durationSec = track.durationMs / 1000

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) StudioCardElevated else StudioCardBg)
            .border(
                1.dp,
                if (isPlaying) ZeroNoiseGreen else StudioBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
            .testTag("track_item_${track.id}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) ZeroNoiseGreen else StudioCardElevated
                            )
                            .clickable { onPlayToggle() }
                            .testTag("play_button_${track.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) Color.Black else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = track.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${durationSec}s • $formattedDate",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Download Button (Direct to Downloads folder)
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download to Device",
                            tint = ZeroNoiseGreenGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Audio",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Track",
                            tint = RawNoiseOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Stats Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Noise reduction badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ZeroNoiseGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "-${"%.1f".format(track.noiseReductionDb)} dB Noise Cut",
                        color = ZeroNoiseGreenGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Voice Clarity
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VoiceCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${track.voiceClarityScore}% Voice Intact",
                        color = VoiceCyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCardElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = track.modeName,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
