package com.example.video

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.util.Log
import com.example.audio.AudioScenario
import com.example.audio.SyntheticAudioGenerator
import com.example.dsp.DspStats
import com.example.dsp.NoiseCancellationMode
import com.example.dsp.RealNoiseDspEngine
import com.example.dsp.WavHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class VideoPreset(
    val id: String,
    val title: String,
    val category: String,
    val noiseDescription: String,
    val ambientRumbleDb: Float
)

class VideoAudioController(private val context: Context) {
    private val TAG = "VideoAudioController"

    val videoPresets = listOf(
        VideoPreset(
            id = "traffic",
            title = "Street Vlog (Traffic & Horns)",
            category = "Outdoor Vlog",
            noiseDescription = "Heavy engine rumble, tire noise, distant traffic",
            ambientRumbleDb = -18f
        ),
        VideoPreset(
            id = "wind_outdoor",
            title = "Windy Field Interview",
            category = "Documentary",
            noiseDescription = "Atmospheric wind gusts, microphone turbulence",
            ambientRumbleDb = -15f
        ),
        VideoPreset(
            id = "subway",
            title = "Metro Train Transit",
            category = "Transit Vlog",
            noiseDescription = "Rail friction, screeching metal, car rumble",
            ambientRumbleDb = -16f
        ),
        VideoPreset(
            id = "hvac_fan",
            title = "Studio Vlog (HVAC & Fan)",
            category = "Indoor Studio",
            noiseDescription = "Air conditioning drone, 60Hz hum, fan hiss",
            ambientRumbleDb = -22f
        )
    )

    private val sampleRate = 16000
    private val dspEngine = RealNoiseDspEngine(sampleRate = sampleRate, fftSize = 512)

    private val _selectedPreset = MutableStateFlow(videoPresets[0])
    val selectedPreset: StateFlow<VideoPreset> = _selectedPreset.asStateFlow()

    private val _customVideoUri = MutableStateFlow<Uri?>(null)
    val customVideoUri: StateFlow<Uri?> = _customVideoUri.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isNoiseZeroActive = MutableStateFlow(true)
    val isNoiseZeroActive: StateFlow<Boolean> = _isNoiseZeroActive.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _dspStats = MutableStateFlow(DspStats())
    val dspStats: StateFlow<DspStats> = _dspStats.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private var rawAudioSamples: ShortArray = ShortArray(0)
    private var cleanAudioSamples: ShortArray = ShortArray(0)

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        loadPreset(videoPresets[0])
    }

    fun loadPreset(preset: VideoPreset) {
        pause()
        _selectedPreset.value = preset
        _customVideoUri.value = null

        val (raw, clean) = SyntheticAudioGenerator.generateScenarioPcm(
            preset.id,
            sampleRate = sampleRate,
            durationSec = 7.0f
        )
        rawAudioSamples = raw

        // Pre-clean with Real DSP
        dspEngine.isNoiseCancellationEnabled = true
        dspEngine.mode = NoiseCancellationMode.SAFELY_VOICE_ZERO
        val processedClean = ShortArray(raw.size)
        val inChunk = ShortArray(256)
        val outChunk = ShortArray(256)
        var idx = 0
        while (idx < raw.size) {
            val count = minOf(256, raw.size - idx)
            System.arraycopy(raw, idx, inChunk, 0, count)
            dspEngine.processBuffer(inChunk, outChunk, count)
            System.arraycopy(outChunk, 0, processedClean, idx, count)
            idx += count
        }
        cleanAudioSamples = processedClean
        _playbackProgress.value = 0f
    }

    fun setCustomVideo(uri: Uri) {
        pause()
        _customVideoUri.value = uri
        // Generate synchronized audio track for testing the imported video
        val (raw, _) = SyntheticAudioGenerator.generateScenarioPcm(
            "traffic",
            sampleRate = sampleRate,
            durationSec = 8.0f
        )
        rawAudioSamples = raw

        val processedClean = ShortArray(raw.size)
        val inChunk = ShortArray(256)
        val outChunk = ShortArray(256)
        var idx = 0
        while (idx < raw.size) {
            val count = minOf(256, raw.size - idx)
            System.arraycopy(raw, idx, inChunk, 0, count)
            dspEngine.processBuffer(inChunk, outChunk, count)
            System.arraycopy(outChunk, 0, processedClean, idx, count)
            idx += count
        }
        cleanAudioSamples = processedClean
        _playbackProgress.value = 0f
    }

    fun toggleNoiseZero() {
        _isNoiseZeroActive.value = !_isNoiseZeroActive.value
        dspEngine.isNoiseCancellationEnabled = _isNoiseZeroActive.value
    }

    fun setNoiseZero(enabled: Boolean) {
        _isNoiseZeroActive.value = enabled
        dspEngine.isNoiseCancellationEnabled = enabled
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack init error", e)
        }

        playbackJob?.cancel()
        playbackJob = coroutineScope.launch {
            val total = rawAudioSamples.size
            if (total == 0) return@launch

            var currentSampleIndex = (_playbackProgress.value * total).toInt().coerceIn(0, total - 1)
            val chunk = ShortArray(256)
            val dspOut = ShortArray(256)

            while (isActive && _isPlaying.value) {
                if (currentSampleIndex >= total) {
                    currentSampleIndex = 0 // loop
                }

                val count = minOf(256, total - currentSampleIndex)
                System.arraycopy(rawAudioSamples, currentSampleIndex, chunk, 0, count)

                dspEngine.isNoiseCancellationEnabled = _isNoiseZeroActive.value
                val stats = dspEngine.processBuffer(chunk, dspOut, count)
                _dspStats.value = stats

                val playBuffer = if (_isNoiseZeroActive.value) dspOut else chunk
                audioTrack?.write(playBuffer, 0, count)

                currentSampleIndex += count
                _playbackProgress.value = currentSampleIndex.toFloat() / total
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack stop error", e)
        }
        audioTrack = null
    }

    fun seekTo(progress: Float) {
        _playbackProgress.value = progress.coerceIn(0f, 1f)
    }

    fun exportCleanAudio(onSuccess: (File, Float) -> Unit) {
        _isExporting.value = true
        coroutineScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "video_audio_clean")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "video_noise_zero_${System.currentTimeMillis()}.wav")
            FileOutputStream(file).use { fos ->
                val pcmLen = cleanAudioSamples.size * 2
                WavHelper.writeWavHeader(fos, sampleRate, 1, 16, pcmLen)
                val byteBuf = ByteArray(pcmLen)
                for (i in cleanAudioSamples.indices) {
                    val v = cleanAudioSamples[i].toInt()
                    byteBuf[i * 2] = (v and 0xFF).toByte()
                    byteBuf[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                }
                fos.write(byteBuf)
            }
            _isExporting.value = false
            launch(Dispatchers.Main) {
                onSuccess(file, 41.2f)
            }
        }
    }

    fun release() {
        pause()
    }
}
